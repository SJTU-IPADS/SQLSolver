package sqlsolver.superopt.optimizer;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import sqlsolver.common.utils.ArraySupport;
import sqlsolver.common.utils.Lazy;
import sqlsolver.sql.plan.*;
import sqlsolver.superopt.constraint.Constraints;
import sqlsolver.superopt.fragment.OpKind;
import sqlsolver.superopt.fragment.AttrsFilter;
import sqlsolver.superopt.fragment.Filter;
import sqlsolver.superopt.fragment.Op;
import sqlsolver.superopt.fragment.Symbol;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.plan.PlanSupport.stringifyTree;

class FilterMatcher {
  private final List<Filter> opChain;
  private final FilterChain nodeChain;
  private final FilterAssignments assignments;
  private final boolean isTrailing, isLeading;
  private final List<Match> results;

  private final Lazy<TIntSet[]> initiators;

  FilterMatcher(Filter opHead, PlanContext ctx, int chainHead) {
    this.opChain = mkOpChain(opHead);
    this.nodeChain = FilterChain.mk(ctx, chainHead);
    this.assignments = new FilterAssignments(nodeChain, opChain.size());
    this.isLeading = head(opChain).successor() == null;
    this.isTrailing = tail(opChain).predecessors()[0].kind() == OpKind.INPUT;
    this.results = new LinkedList<>();
    this.initiators = Lazy.mk(this::calcInitiators);
  }

  List<Match> matchBasedOn(Match baseMatch) {
    if (opChain.size() > nodeChain.size()) return emptyList();
    if (isSimpleFullCover()) return fullCover(baseMatch); // fast path

    final List<SubMatcher> matchers = mkSubMatchers(baseMatch);
    if (!isSubMatchersValid(matchers)) return emptyList();

    matchers.get(0).match(baseMatch);
    return results;
  }

  private boolean isSimpleFullCover() {
    return !isTrailing
        && !isLeading
        && opChain.size() == 1
        && opChain.get(0).kind() == OpKind.SIMPLE_FILTER;
  }

  private List<Match> fullCover(Match match) {
    assignments.setCombined(0, ArraySupport.range(0, nodeChain.size()));
    assert assignments.numUnused() == 0;
    final FilterChain chain = assignments.mkChain(true);
    final PlanContext newPlan = chain.assemble();
    final Match derivedMatch = match.derive().setSourcePlan(newPlan);
    return tryMatch(opChain.get(0), chain.at(0), derivedMatch);
  }

  private static List<Filter> mkOpChain(Op op) {
    final List<Filter> opChain = new ArrayList<>(5);
    while (op.kind().isFilter()) {
      opChain.add((Filter) op);
      op = op.predecessors()[0];
    }
    return opChain;
  }

  private List<SubMatcher> mkSubMatchers(Match baseMatch) {
    final Model model = baseMatch.model();
    final boolean[] done = new boolean[opChain.size()];
    final List<SubMatcher> matchers = new ArrayList<>(opChain.size() + 1);
    for (int i = 0; i < done.length; i++) {
      if (!done[i]) matchers.add(mkSubMatcherFor(i, done, model));
    }

    matchers.sort(Comparator.naturalOrder());
    matchers.add(new Terminator(baseMatch));
    for (int i = 1, bound = matchers.size(); i < bound; i++)
      matchers.get(i - 1).setNext(matchers.get(i));

    return matchers;
  }

  private SubMatcher mkSubMatcherFor(int opIdx, boolean[] done, Model model) {
    final Filter op = opChain.get(opIdx);

    if (op.kind() == OpKind.EXISTS_FILTER) {
      done[opIdx] = true;
      return new ExistsFilterSubMatcher(opIdx);
    }

    final Symbol attrsSym = ((AttrsFilter) op).attrs();
    final TIntList buddies = new TIntArrayList(done.length);
    final Constraints constraints = model.constraints();
    boolean isFreeAttr = constraints.eqClassOf(attrsSym).size() == 1;

    for (int i = 0, bound = opChain.size(); i < bound; i++) {
      final Filter otherOp = opChain.get(i);
      if (!(otherOp instanceof AttrsFilter)) continue;

      if (constraints.isEq(attrsSym, ((AttrsFilter) otherOp).attrs())) {
        done[i] = true;
        buddies.add(i);
      }
    }

    if (model.isAssigned(attrsSym)) return new GroupedFilterSubMatcher(buddies.toArray(), 0);
    if (isFreeAttr && !op.kind().isSubquery())
      return new FreeFilterSubMatcher(opIdx, !isTrailing && !isLeading);
    else return new GroupedFilterSubMatcher(buddies.toArray(), op.kind().isSubquery() ? 2 : 1);
  }

  private boolean isSubMatchersValid(List<SubMatcher> matchers) {
    // disallow multiple greedy sub-matchers
    boolean isGreedyPresent = false;
    for (SubMatcher matcher : matchers) {
      if (matcher instanceof FreeFilterSubMatcher && ((FreeFilterSubMatcher) matcher).forceGreedy)
        if (isGreedyPresent) return false;
        else isGreedyPresent = true;
    }
    return true;
  }

  private abstract static class SubMatcher implements Comparable<SubMatcher> {
    protected SubMatcher next;

    abstract int priority();

    abstract boolean match(Match whatIf);

    @Override
    public int compareTo(FilterMatcher.SubMatcher o) {
      return Integer.compare(priority(), o.priority());
    }

    public void setNext(SubMatcher next) {
      this.next = next;
    }
  }

  private class GroupedFilterSubMatcher extends SubMatcher {
    private final int[] indices;
    private final int priority;

    private GroupedFilterSubMatcher(int[] indices, int priority) {
      this.indices = indices;
      this.priority = priority;
    }

    @Override
    int priority() {
      return priority;
    }

    @Override
    boolean match(Match whatIf) {
      return match0(whatIf, 0, 0);
    }

    private boolean match0(Match whatIf, int opOrdinal, int nextNodeIdx) {
      if (opOrdinal >= indices.length) return next.match(whatIf);
      if (nextNodeIdx >= nodeChain.size()) return false;

      final int opIdx = indices[opOrdinal];
      final Op op = opChain.get(opIdx);

      boolean ret = false;
      for (int nodeIdx = nextNodeIdx, bound = nodeChain.size(); nodeIdx < bound; ++nodeIdx) {
        if (assignments.isUsed(nodeIdx)) continue;

        final Match derivedMatch = whatIf.derive();
        final int filterNode = nodeChain.at(nodeIdx);
        if (derivedMatch.matchOne(op, filterNode)) {
          assignments.setExact(opIdx, nodeIdx);
          ret |= match0(derivedMatch, opOrdinal + 1, nodeIdx + 1);
          assignments.unset(opIdx);
        }
      }

      return ret;
    }
  }

  private class FreeFilterSubMatcher extends SubMatcher {
    private final int opIdx;
    private final boolean forceGreedy;

    private FreeFilterSubMatcher(int opIdx, boolean forceGreedy) {
      this.opIdx = opIdx;
      this.forceGreedy = forceGreedy;
    }

    @Override
    int priority() {
      return Integer.MAX_VALUE - 1;
    }

    @Override
    boolean match(Match match) {
      return greedyMatch(match) && (forceGreedy || nonGreedyMatch(match));
    }

    private boolean greedyMatch(Match match) {
      final TIntList toMergeFilters = new TIntArrayList(assignments.numUnused());
      for (int i = 0, bound = nodeChain.size(); i < bound; ++i)
        if (!assignments.isUsed(i)) toMergeFilters.add(i);

      if (toMergeFilters.isEmpty()) return false;

      assignments.setCombined(opIdx, toMergeFilters.toArray());
      final boolean result = next.match(match);
      assignments.unset(opIdx);

      return result;
    }

    private boolean nonGreedyMatch(Match match) {
      final TIntSet[] initiators = FilterMatcher.this.initiators.get();
      final boolean[] checked = new boolean[nodeChain.size()];
      boolean ret = false;

      for (int i = 0, bound = nodeChain.size(); i < bound; ++i) {
        if (assignments.isUsed(i) || checked[i]) continue;

        final TIntList toMergeFilters = new TIntArrayList(assignments.numUnused());
        for (int j = i; j < bound; ++j) {
          if (!assignments.isUsed(j) && initiators[i].equals(initiators[j])) {
            toMergeFilters.add(j);
            checked[j] = true;
          }
        }

        assignments.setCombined(opIdx, toMergeFilters.toArray());
        ret |= next.match(match.derive());
        assignments.unset(opIdx);
      }

      return ret;
    }
  }

  private class ExistsFilterSubMatcher extends SubMatcher {
    private final int opIdx;

    private ExistsFilterSubMatcher(int opIdx) {
      this.opIdx = opIdx;
    }

    int priority() {
      return Integer.MAX_VALUE - 2;
    }

    boolean match(Match match) {
      boolean ret = false;
      for (int i = 0, bound = nodeChain.size(); i < bound; i++) {
        if (assignments.isUsed(i)) continue;
        if (!assignments.isUsed(i) && nodeChain.get(i).kind() == PlanKind.Exists) {
          assignments.setExact(opIdx, i);
          ret |= next.match(match);
          assignments.unset(opIdx);
        }
      }

      return ret;
    }
  }

  private class Terminator extends SubMatcher {
    private final Match baseMatch;
    private final Set<String> seenPlans;

    private Terminator(Match baseMatch) {
      this.baseMatch = baseMatch;
      this.seenPlans = new HashSet<>(4);
    }

    @Override
    int priority() {
      return Integer.MAX_VALUE;
    }

    @Override
    boolean match(Match whatIf) {
      final int numUnused = assignments.numUnused();
      // If the op-chain is not leading and is not trailing,
      // then it is required to occupy the whole node-chain.
      if (!isLeading && !isTrailing && numUnused != 0) return false;

      final FilterChain newChain = assignments.mkChain(isTrailing);
      final PlanContext matchedPlan = newChain.assemble();
      if (!seenPlans.add(stringifyTree(matchedPlan, matchedPlan.root(), true))) return false;

      final int chainLength = newChain.size();
      final int firstMatchPointOffset = (isLeading && !isTrailing) ? numUnused : 0;
      final int lastMatchPointOffset = isTrailing ? chainLength - numUnused - 1 : chainLength - 1;
      final int lastMatchedNode = newChain.at(lastMatchPointOffset);

      final Match derivedMatch = baseMatch.derive().setSourcePlan(matchedPlan);
      List<Match> matches = singletonList(derivedMatch);

      for (int i = 0, bound = opChain.size(); i < bound; i++) {
        final Filter op = opChain.get(i);
        final int filterNode = newChain.at(firstMatchPointOffset + i);
        matches = linkedListFlatMap(matches, m -> tryMatch(op, filterNode, m));
        if (matches.isEmpty()) return false;
      }

      if (isLeading) for (Match m : matches) m.setMatchRootNode(newChain.at(0));
      for (Match m : matches) m.setLastMatchPoint(lastMatchedNode, opChain.get(opChain.size() - 1));

      results.addAll(matches);
      return true;
    }
  }

  private TIntSet[] calcInitiators() {
    final TIntSet[] ret = new TIntSet[nodeChain.size()];
    for (int i = 0, bound = nodeChain.size(); i < bound; ++i) {
      ret[i] = getRefInitiatorOf(getExprOf(nodeChain.at(i)));
    }
    return ret;
  }

  private Expression getExprOf(int nodeId) {
    final PlanContext plan = nodeChain.plan();
    final PlanNode node = plan.nodeAt(nodeId);

    if (node.kind() == PlanKind.Filter) {
      return ((SimpleFilterNode) node).predicate();
    } else {
      return plan.infoCache().getSubqueryExprOf(nodeId);
    }
  }

  private TIntSet getRefInitiatorOf(Expression expr) {
    final ValuesRegistry valuesReg = nodeChain.plan().valuesReg();
    final Values values = valuesReg.valueRefsOf(expr);
    return ArraySupport.mapSet(values, valuesReg::initiatorOf);
  }

  private List<Match> tryMatch(Op op, int nodeId, Match match) {
    if (!match.matchOne(op, nodeId)) return emptyList();
    if (!op.kind().isSubquery()) return singletonList(match);
    else return Match.match(match, op.predecessors()[1], match.sourcePlan().childOf(nodeId, 1));
  }
}
