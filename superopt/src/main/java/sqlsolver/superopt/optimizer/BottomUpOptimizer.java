package sqlsolver.superopt.optimizer;

import sqlsolver.common.utils.Lazy;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanKind;
import sqlsolver.sql.plan.PlanNode;
import sqlsolver.sql.plan.PlanSupport;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.substitution.SubstitutionBank;
import sqlsolver.superopt.util.Fingerprint;

import java.util.*;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.sql.plan.PlanSupport.stringifyTree;

class BottomUpOptimizer implements Optimizer {
  private final SubstitutionBank rules;

  private Memo memo;

  private long startAt;
  private long timeout;

  private boolean tracing, verbose, extended, keepOriginal;
  private final Lazy<Map<String, OptimizationStep>> traces;

  BottomUpOptimizer(SubstitutionBank rules) {
    this.rules = requireNonNull(rules);
    this.traces = Lazy.mk(HashMap::new);
    this.startAt = Long.MIN_VALUE;
    this.timeout = Long.MAX_VALUE;
  }

  @Override
  public void setTracing(boolean flag) {
    tracing = flag;
  }

  @Override
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public void setExtended(boolean extension) {
    this.extended = extension;
  }

  @Override
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public void setKeepOriginal(boolean keepOriginal) {
    this.keepOriginal = keepOriginal;
  }

  @Override
  public List<OptimizationStep> traceOf(PlanContext plan) {
    return collectTrace(plan);
  }

  @Override
  public Set<PlanContext> optimize(PlanContext plan) {
    setExtended(rules.isExtended());
    setKeepOriginal((OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_KEEP_ORIGINAL_PLAN) != 0);

    final PlanContext originalPlan = plan;

    plan = plan.copy();
    int planRoot = preprocess(plan);

    memo = new Memo();
    startAt = System.currentTimeMillis();

    final Set<SubPlan> results = optimize0(new SubPlan(plan, planRoot));
    return collectRewritten(originalPlan, results);
  }

  @Override
  public Set<PlanContext> optimizePartial(PlanContext plan, int rootId) {
    setExtended((OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_ENABLE_EXTENSIONS) != 0);
    setKeepOriginal((OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_KEEP_ORIGINAL_PLAN) != 0);

    final PlanContext originalPlan = plan;
    plan = plan.copy();

    final PlanNode subTreeRootNode = plan.nodeAt(rootId);

    int planRoot = preprocess(plan);

    final int subTreeRoot = plan.nodeIdOf(subTreeRootNode);

    memo = new Memo();
    startAt = System.currentTimeMillis();

    final Set<SubPlan> results = optimize0(new SubPlan(plan, subTreeRoot));
    return collectRewritten(originalPlan, results);
  }

  private Set<SubPlan> optimize0(SubPlan subPlan) {
    // A plan is "fully optimized" if:
    // 1. itself has been registered in memo, and
    // 2. all its children have been registered in memo.
    // Such a plan won't be transformed again.
    //
    // Note that the other plans in the same group of a fully-optimized
    // plan still have chance to be transformed.
    //
    // Example:
    // P0(cost=2) -> P1(cost=2),P2(cost=2); P2 -> P3(cost=1)
    // Now, P1 cannot be further transformed (nor its children), thus "fully-optimized"
    // Obviously, P1 and P3 is equivalent. P1 and P3 thus reside in the same group.
    // Then, when P2 are transformed to P3, the group is accordingly updated.

    if (isTimedOut() || isFullyOptimized(subPlan)) return memo.eqClassOf(subPlan);
    else return dispatch(subPlan);
  }

  private Set<SubPlan> optimizeChild(SubPlan n) {
    final Set<SubPlan> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final Set<SubPlan> group = memo.mkEqClass(n);
    group.addAll(candidates);

    return group;
  }

  private Set<SubPlan> optimizeChild0(SubPlan n) {
    final PlanKind kind = n.rootKind();
    assert kind != PlanKind.Input;
    final int numChildren = kind.numChildren();

    // 1. Recursively optimize the children (or retrieve from memo)
    Set<SubPlan> lhsOpts = emptySet(), rhsOpts = emptySet();
    if (numChildren >= 1) lhsOpts = optimize0(n.child(0));
    if (numChildren >= 2) rhsOpts = optimize0(n.child(1));

    Set<SubPlan> opts = new HashSet<>(lhsOpts.size());
    if (numChildren >= 1) {
      for (SubPlan lhsOpt : lhsOpts) {
        final SubPlan replaced = replaceChild(n, 0, lhsOpt);
        if (replaced != null) opts.add(replaced);
      }
    }
    if (numChildren >= 2) {
      final Set<SubPlan> newOpts = new HashSet<>(opts.size() * rhsOpts.size());
      for (SubPlan opt : opts) {
        for (SubPlan rhsOpt : rhsOpts) {
          final SubPlan replaced = replaceChild(opt, 1, rhsOpt);
          if (replaced != null) newOpts.add(replaced);
        }
      }
      opts = newOpts;
    }

    return opts;
  }

  private Set<SubPlan> optimizeFull(SubPlan n) {
    final Set<SubPlan> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final Set<SubPlan> group = memo.mkEqClass(n);
    group.addAll(candidates);
    // Note: `group` may contain plans that has been fully-optimized.
    // To get rid of duplicated calculation, don't pass the whole `group` to `transform`,
    // pass the `candidates` instead.

    // 4. do transformation
    final List<SubPlan> transformed = ListSupport.flatMap(candidates, this::transform);

    // 5. recursively optimize the transformed plan
    for (SubPlan p : transformed) optimize0(p);

    group.addAll(transformed);
    return group;
  }

  /* find eligible substitutions and use them to transform `n` and generate new plans */
  private Set<SubPlan> transform(SubPlan subPlan) {
    if (isTimedOut()) return emptySet();

    final PlanContext plan = subPlan.plan();
    final PlanKind kind = subPlan.rootKind();
    final int root = subPlan.nodeId();
    if (kind.isFilter() && plan.kindOf(plan.parentOf(root)).isFilter()) return emptySet();

    final Set<SubPlan> group = memo.eqClassOf(subPlan);
    final Set<SubPlan> transformed = new MinCostSet();
    // 1. fast search for candidate substitution by fingerprint
    final Iterable<Substitution> rules = fastMatchRules(subPlan);
    for (Substitution rule : rules) {
      if (isTimedOut()) break;

      // 2. full match
      final Match baseMatch = new Match(rule).setSourcePlan(plan).setMatchRootNode(root);
      final List<Match> fullMatches = Match.match(baseMatch, rule._0().root(), root);

      for (Match match : fullMatches) {
        if (match.assembleModifiedPlan()) {
          // 3. generate new plan according to match
          final PlanContext newPlan = match.modifiedPlan();
          int newSubPlanRoot = match.modifiedRootNode();

          final int normalizedRoot = OptimizerSupport.normalizePlan(newPlan, newSubPlanRoot);
          if (normalizedRoot == NO_SUCH_NODE) continue;

          final SubPlan newSubPlan = new SubPlan(newPlan, normalizedRoot);
          // If the `newNode` has been bound with a group, then no need to further optimize it.
          // (because it must either have been or is being optimized.)
          final boolean registered = memo.isRegistered(newSubPlan);

          if (group.add(newSubPlan)) {
            if (!registered) transformed.add(newSubPlan);
            traceStep(subPlan.plan(), newSubPlan.plan(), rule);
          }

        } else if (verbose) {
          System.err.printf("instantiation failed: %s\n%s\n%s\n", subPlan, rule, OptimizerSupport.getLastError());
        }
      }
    }

    transformed.addAll(ListSupport.flatMap(transformed, this::transform));
    return transformed;
  }

  protected Set<SubPlan> onInput(SubPlan input) {
    return singleton(input);
  }

  protected Set<SubPlan> onFilter(SubPlan filter) {
    // only do full optimization at filter chain head
    final PlanContext plan = filter.plan();
    if (plan.kindOf(plan.parentOf(filter.nodeId())).isFilter()) return optimizeChild0(filter);
    else return optimizeFull(filter);
  }

  protected Set<SubPlan> onJoin(SubPlan join) {
    // only do full optimization at join tree root
    final PlanContext plan = join.plan();
    if (plan.kindOf(plan.parentOf(join.nodeId())) == PlanKind.Join) return optimizeChild0(join);
    else return optimizeFull(join);
  }

  protected Set<SubPlan> onProj(SubPlan proj) {
    return optimizeFull(proj);
  }

  protected Set<SubPlan> onLimit(SubPlan limit) {
    return optimizeChild(limit);
  }

  protected Set<SubPlan> onSort(SubPlan sort) {
    return optimizeChild(sort);
  }

  protected Set<SubPlan> onAgg(SubPlan agg) {
    return extended ? optimizeFull(agg) : optimizeChild(agg);
  }

  protected Set<SubPlan> onSetOp(SubPlan setOp) {
    return extended ? optimizeFull(setOp) : optimizeChild(setOp);
  }

  private Set<SubPlan> dispatch(SubPlan node) {
    switch (node.rootKind()) {
      case Input:
        return onInput(node);
      case Filter:
      case InSub:
      case Exists:
        return onFilter(node);
      case Join:
        return onJoin(node);
      case Proj:
        return onProj(node);
      case Limit:
        return onLimit(node);
      case Sort:
        return onSort(node);
      case Agg:
        return onAgg(node);
      case SetOp:
        return onSetOp(node);
      default:
        throw new IllegalArgumentException();
    }
  }

  private boolean isFullyOptimized(SubPlan subPlan) {
    if (!memo.isRegistered(subPlan)) return false;
    final PlanContext plan = subPlan.plan();
    final int node = subPlan.nodeId();
    final PlanKind kind = subPlan.rootKind();
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      if (!memo.isRegistered(plan, plan.childOf(node, i))) return false;
    }
    return true;
  }

  private Iterable<Substitution> fastMatchRules(SubPlan subPlan) {
    final Set<Fingerprint> fingerprints = Fingerprint.mk(subPlan.plan(), subPlan.nodeId());
    return ListSupport.flatMap(fingerprints, rules::ruleOfFingerprint);
  }

  private List<OptimizationStep> collectTrace(PlanContext plan) {
    return collectTrace0(plan, 0);
  }

  private List<OptimizationStep> collectTrace0(PlanContext key, int depth) {
    if (!traces.isInitialized()) return emptyList();

    final OptimizationStep step = traces.get().get(stringifyTree(key, key.root(), true));
    if (step == null) return new ArrayList<>(depth);
    final List<OptimizationStep> trace = collectTrace0(step.source(), depth + 1);
    trace.add(step);
    return trace;
  }

  private SubPlan replaceChild(SubPlan replaced, int childIdx, SubPlan replacement) {
    if (replacement.plan() == replaced.plan()) {
      if (replaced.plan().childOf(replaced.nodeId(), childIdx) == replacement.nodeId())
        return replaced;
    }
    if (replaced.child(childIdx).toString().equals(replacement.toString())) {
      return replaced;
    }

    final PlanContext replacedPlan = replaced.plan().copy(), replacementPlan = replacement.plan();
    final int replacedSubPlan = replacedPlan.childOf(replaced.nodeId(), childIdx);
    final int replacementSubPlan = replacement.nodeId();
    final ReplaceSubPlan replace = new ReplaceSubPlan(replacedPlan, replacementPlan);
    final int result = replace.replace(replacedSubPlan, replacementSubPlan);
    if (!replacedPlan.isPresent(result)) return null;
    return new SubPlan(replacedPlan, replacedPlan.parentOf(result));
  }

  private int preprocess(PlanContext plan) {
    int planRoot = plan.root();

    if ((OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_SORT_FILTERS_DURING_REWRITE) != 0)
      planRoot = OptimizerSupport.normalizeFilter(plan, planRoot);

    planRoot = enforceInnerJoin(plan, planRoot);
    planRoot = reduceSort(plan, planRoot);
    planRoot = reduceDedup(plan, planRoot);
    planRoot = convertExists(plan, planRoot);
    planRoot = flipRightJoin(plan, planRoot);
    return planRoot;
  }

  private int enforceInnerJoin(PlanContext plan, int planRoot) {
    final PlanContext original = tracing ? plan.copy() : null;
    final InnerJoinInference inference = new InnerJoinInference(plan);
    inference.inferenceAndEnforce(planRoot);
    if (inference.isModified() && tracing) traceStep(original, plan, PreprocessRule.EnforceInnerJoin.ruleId());
    return planRoot;
  }

  private int reduceSort(PlanContext plan, int planRoot) {
    final PlanContext original = tracing ? plan.copy() : null;
    final ReduceSort reduceSort = new ReduceSort(plan);
    planRoot = reduceSort.reduce(planRoot);
    if (reduceSort.isReduced() && tracing) traceStep(original, plan, PreprocessRule.ReduceSort.ruleId());
    return planRoot;
  }

  private int reduceDedup(PlanContext plan, int planRoot) {
    final PlanContext original = tracing ? plan.copy() : null;
    final ReduceDedup reduceDedup = new ReduceDedup(plan);
    planRoot = reduceDedup.reduce(planRoot);
    if (reduceDedup.isReduced() && tracing) traceStep(original, plan, PreprocessRule.ReduceDedup.ruleId());
    return planRoot;
  }

  private int convertExists(PlanContext plan, int planRoot) {
    final PlanContext original = tracing ? plan.copy() : null;
    final ConvertExists converter = new ConvertExists(plan);
    planRoot = converter.convert(planRoot);
    if (converter.isConverted() && tracing) traceStep(original, plan, PreprocessRule.ConvertExists.ruleId());
    return planRoot;
  }

  private int flipRightJoin(PlanContext plan, int planRoot) {
    final PlanContext original = tracing ? plan.copy() : null;
    final FlipRightJoin converter = new FlipRightJoin(plan);
    planRoot = converter.flip(planRoot);
    if (converter.isFlipped() && tracing) traceStep(original, plan, PreprocessRule.FlipRightJoin.ruleId());
    return planRoot;
  }

  private Set<PlanContext> collectRewritten(PlanContext origin, Set<SubPlan> subPlans) {
    final boolean shouldSortFilters = (OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_SORT_FILTERS_BEFORE_OUTPUT) != 0;
    if (shouldSortFilters) {
      origin = origin.copy();
      OptimizerSupport.normalizeFilter(origin, origin.root());
    }

    final Set<String> known = new HashSet<>(subPlans.size());
    final Set<PlanContext> rewritings = new HashSet<>(subPlans.size());
    for (SubPlan subPlan : subPlans) {
      // Preclude the original one
      final PlanContext plan = subPlan.plan();
      if (shouldSortFilters) OptimizerSupport.normalizeFilter(plan, plan.root());

      if (keepOriginal || !PlanSupport.isLiteralEq(origin, plan)) {
        if (known.add(stringifyTree(plan, plan.root(), true))) {
          rewritings.add(plan);
        }
      }
    }

    return rewritings;
  }

  private boolean isTimedOut() {
    return System.currentTimeMillis() - startAt >= timeout;
  }

  private void traceStep(PlanContext source, PlanContext target, Substitution rule) {
    if (!tracing) return;
    final String key = stringifyTree(target, target.root(), true);
    traces.get().computeIfAbsent(key, ignored -> new OptimizationStep(source, target, rule, 0));
  }

  private void traceStep(PlanContext source, PlanContext target, int extra) {
    if (!tracing) return;
    final String key = stringifyTree(target, target.root(), true);
    traces.get().computeIfAbsent(key, ignored -> new OptimizationStep(source, target, null, extra));
  }
}
