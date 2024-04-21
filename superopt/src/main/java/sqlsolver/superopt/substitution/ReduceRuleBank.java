package sqlsolver.superopt.substitution;

import me.tongfei.progressbar.ProgressBar;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.plan.*;
import sqlsolver.superopt.constraint.Constraints;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.optimizer.Optimizer;
import sqlsolver.superopt.fragment.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static sqlsolver.common.utils.IterableSupport.zip;
import static sqlsolver.sql.plan.PlanSupport.stringifyTree;
import static sqlsolver.superopt.optimizer.OptimizerSupport.*;

class ReduceRuleBank {
  private final SubstitutionBank bank;

  ReduceRuleBank(SubstitutionBank bank) {
    this.bank = bank;
  }

  SubstitutionBank reduce() {
    addOptimizerTweaks(TWEAK_DISABLE_JOIN_FLIP);
    if (bank.isExtended()) addOptimizerTweaks(TWEAK_ENABLE_EXTENSIONS);

    bank.removeIf(ReduceRuleBank::isUselessHeuristic1);
    bank.removeIf(ReduceRuleBank::isUselessHeuristic2);
    bank.removeIf(ReduceRuleBank::isJoinFlipRule);
    if (bank.isExtended()) bank.removeIf(ReduceRuleBank::isWrongDueToBug);

    try (final ProgressBar pb = new ProgressBar("Reduce", bank.size())) {
      final List<Substitution> rules = new ArrayList<>(bank.rules());
      for (int i = 0, bound = rules.size(); i < bound; i++) {
        pb.step();

        final Substitution rule = rules.get(i);
        try {
          if (isImpliedRule(rule)) bank.remove(rule);
          else bank.add(rule);

        } catch (Throwable ex) {
          System.err.println(i + " " + rule);
          //          ex.printStackTrace();
          bank.remove(rule);
          //          throw ex;
        }
      }
    }

    return bank;
  }

  private static boolean isUselessHeuristic1(Substitution rule) {
    // All LHS attrs symbols are required equal.
    final Constraints constraints = rule.constraints();
    final List<Symbol> attrs = rule._0().symbols().symbolsOf(Symbol.Kind.ATTRS);
    if (attrs.size() <= 5) return false;

    for (int i = 0, bound = attrs.size() - 1; i <= bound; i++) {
      for (int j = i + 1; j < bound; j++)
        if (!constraints.isEq(attrs.get(i), attrs.get(j))) return false;
    }
    return true;
  }

  private static boolean isUselessHeuristic2(Substitution rule) {
    // Two RHS Pred or Func symbols share the same instantiation.
    final Constraints constraints = rule.constraints();

    final List<Symbol> preds = rule._1().symbols().symbolsOf(Symbol.Kind.PRED);
    final Set<Symbol> predInstantiations = SetSupport.map(preds, constraints::instantiationOf);
    if (predInstantiations.size() < preds.size()) return true;

    final List<Symbol> funcs = rule._1().symbols().symbolsOf(Symbol.Kind.FUNC);
    final Set<Symbol> funcInstantiations = SetSupport.map(preds, constraints::instantiationOf);
    if (funcInstantiations.size() < funcs.size()) return true;

    return false;
  }

  private static boolean isJoinFlipRule(Substitution rule) {
    return new CheckFlipJoin(rule).check();
  }

  private static boolean isWrongDueToBug(Substitution rule) {
    return rule.toString().contains("InSub");
  }

  private boolean isImpliedRule(Substitution rule) {
    final PlanContext plan = mkProbingPlan(rule);
    if (plan == null) return true;

    final boolean isCappedByProj = completePlan(plan);

    final String str = stringifyTree(plan, plan.root());
    final Set<String> optimized0 = optimizeAsString(plan, bank, isCappedByProj);
    bank.remove(rule);
    final Set<String> optimized1 = optimizeAsString(plan, bank, isCappedByProj);
    optimized0.remove(str);
    optimized1.remove(str);

    return !optimized1.isEmpty() && optimized1.containsAll(optimized0);
  }

  private static Set<String> optimizeAsString(
      PlanContext plan, SubstitutionBank rules, boolean isCappedByProj) {
    final Optimizer optimizer = Optimizer.mk(rules);
    //    optimizer.setTracing(true);
    final Set<PlanContext> optimized;
    if (!isCappedByProj) {
      optimized = optimizer.optimize(plan);
    } else {
      optimized = optimizer.optimizePartial(plan, plan.childOf(plan.root(), 0));
    }
    //    for (PlanContext opt : optimized) OptimizerSupport.dumpTrace(optimizer, opt);
    return SetSupport.map(optimized, it -> stringifyTree(it, it.root(), true));
  }

  private static boolean completePlan(PlanContext plan) {
    final int oldRoot = plan.root();
    final PlanKind oldRootKind = plan.kindOf(oldRoot);
    if (oldRootKind != PlanKind.Join && !oldRootKind.isFilter()) return false;

    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values inValues = valuesReg.valuesOf(oldRoot);
    final List<String> names = ListSupport.map(inValues, Value::name);
    final List<Expression> exprs = ListSupport.map(inValues, PlanSupport::mkColRefExpr);
    zip(exprs, inValues, (e, v) -> valuesReg.bindValueRefs(e, newArrayList(v)));

    final ProjNode proj = ProjNode.mk(false, names, exprs);
    final int projNode = plan.bindNode(proj);
    plan.setChild(projNode, 0, oldRoot);
    plan.setRoot(projNode);
    return true;
  }

  private static PlanContext mkProbingPlan(Substitution rule) {
    final var pair = rule.isExtended() ? SubstitutionSupport.translateAsPlan2(rule) : SubstitutionSupport.translateAsPlan(rule);
    final PlanContext left = pair.getLeft(), right = pair.getRight();
    if (left == null || right == null) return null;
    if (PlanSupport.isLiteralEq(left, right)) return null;
    return left;
  }

  private static List<Join> collectJoins(Fragment fragment) {
    final CollectJoin collectJoin = new CollectJoin();
    fragment.acceptVisitor(collectJoin);
    return collectJoin.joins;
  }

  private static class CollectJoin implements OpVisitor {
    private final List<Join> joins = new ArrayList<>();

    @Override
    public void leaveLeftJoin(LeftJoin op) {
      joins.add(op);
    }

    @Override
    public void leaveInnerJoin(InnerJoin op) {
      joins.add(op);
    }
  }

  private static class CheckFlipJoin {
    private final Substitution rule;
    private boolean containsFlip;

    private CheckFlipJoin(Substitution rule) {
      this.rule = rule;
      this.containsFlip = false;
    }

    private boolean check() {
      final List<Join> joins0 = collectJoins(rule._0());
      final List<Join> joins1 = collectJoins(rule._1());
      if (joins0.isEmpty() || joins1.isEmpty()) return false;
      for (Join join0 : joins0) {
        for (Join join1 : joins1) {
          if (isEqOpTree(join0, join1) && containsFlip) return true;
        }
      }
      return false;
    }

    private boolean isEqOpTree(Op op0, Op op1) {
      if ((!op0.kind().isJoin() || !op1.kind().isJoin()) && op0.kind() != op1.kind()) return false;

      switch (op0.kind()) {
        case PROJ:
          return isEqProj((Proj) op0, (Proj) op1);
        case LEFT_JOIN:
        case INNER_JOIN:
          if (isEqJoin((Join) op0, (Join) op1)) return true;
          else if (isFlippedJoin((Join) op0, (Join) op1)) {
            containsFlip = true;
            return true;
          } else return false;
        case SIMPLE_FILTER:
          return isEqFilter((SimpleFilter) op0, (SimpleFilter) op1);
        case IN_SUB_FILTER:
          return isEqInSub((InSubFilter) op0, (InSubFilter) op1);
        case INPUT:
          return isEqInput((Input) op0, (Input) op1);
        case UNION:
          return isEqSetOp((Union) op0, (Union) op1);
        case AGG:
          return isEqAgg((Agg) op0, (Agg) op1);
        default:
          throw new IllegalArgumentException("unsupported op: " + op0);
      }
    }

    private boolean isEqProj(Proj proj0, Proj proj1) {
      final Constraints constraints = rule.constraints();
      if (!constraints.isEq(proj0.attrs(), constraints.instantiationOf(proj1.attrs())))
        return false;
      return isEqOpTree(proj0.predecessors()[0], proj1.predecessors()[0]);
    }

    private boolean isEqJoin(Join join0, Join join1) {
      final Constraints constraints = rule.constraints();
      if (!constraints.isEq(join0.lhsAttrs(), constraints.instantiationOf(join1.lhsAttrs()))
          || !constraints.isEq(join0.rhsAttrs(), constraints.instantiationOf(join1.rhsAttrs())))
        return false;
      return isEqOpTree(join0.predecessors()[0], join1.predecessors()[0])
          && isEqOpTree(join0.predecessors()[1], join1.predecessors()[1]);
    }

    private boolean isFlippedJoin(Join join0, Join join1) {
      final Constraints constraints = rule.constraints();
      final Symbol lhs0 = join0.lhsAttrs(), rhs0 = join0.rhsAttrs();
      final Symbol lhs1 = constraints.instantiationOf(join1.lhsAttrs());
      final Symbol rhs1 = constraints.instantiationOf(join1.rhsAttrs());
      if (!constraints.isEq(lhs0, rhs1) || !constraints.isEq(rhs0, lhs1)) return false;
      return isEqOpTree(join0.predecessors()[0], join1.predecessors()[1])
          && isEqOpTree(join0.predecessors()[1], join1.predecessors()[0]);
    }

    private boolean isEqFilter(SimpleFilter filter0, SimpleFilter filter1) {
      final Constraints constraints = rule.constraints();
      if (!constraints.isEq(filter0.predicate(), constraints.instantiationOf(filter1.predicate()))
          || !constraints.isEq(filter0.attrs(), constraints.instantiationOf(filter1.attrs())))
        return false;
      return isEqOpTree(filter0.predecessors()[0], filter1.predecessors()[0]);
    }

    private boolean isEqInSub(InSubFilter filter0, InSubFilter filter1) {
      final Constraints constraints = rule.constraints();
      if (!constraints.isEq(filter0.attrs(), constraints.instantiationOf(filter1.attrs())))
        return false;
      return isEqOpTree(filter0.predecessors()[0], filter1.predecessors()[0])
          && isEqOpTree(filter0.predecessors()[1], filter1.predecessors()[1]);
    }

    private boolean isEqInput(Input input0, Input input1) {
      final Constraints constraints = rule.constraints();
      return constraints.isEq(input0.table(), constraints.instantiationOf(input1.table()));
    }

    private boolean isEqSetOp(Union setOp0, Union setOp1) {
      return isEqOpTree(setOp0.predecessors()[0], setOp1.predecessors()[0])
          && isEqOpTree(setOp0.predecessors()[1], setOp1.predecessors()[1]);
    }

    private boolean isEqAgg(Agg agg0, Agg agg1) {
      final Constraints C = rule.constraints();
      if (!C.isEq(agg0.aggregateAttrs(), C.instantiationOf(agg1.aggregateAttrs()))) return false;
      if (!C.isEq(agg0.groupByAttrs(), C.instantiationOf(agg1.groupByAttrs()))) return false;
      if (!C.isEq(agg0.aggFunc(), C.instantiationOf(agg1.aggFunc()))) return false;
      if (!C.isEq(agg0.havingPred(), C.instantiationOf(agg1.havingPred()))) return false;
      return isEqOpTree(agg0.predecessors()[0], agg1.predecessors()[0]);
    }
  }

  public static void main(String[] args) throws IOException {
    final SubstitutionBank bank = SubstitutionSupport.loadBank(Path.of("sqlsolver_data", "rules", "rules.spes.txt"));
    final ReduceRuleBank reducer = new ReduceRuleBank(bank);
    final Substitution rule =
        Substitution.parse(
            "Union*(Agg<a0 a1 f0 s0 p0>(Input<t0>),Agg<a2 a3 f1 s1 p1>(Input<t1>))|Union*(Agg<a4 a5 f2 s2 p2>(Input<t2>),Agg<a6 a7 f3 s3 p3>(Input<t3>))|TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);FuncEq(f0,f1);AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a3,t1);TableEq(t2,t1);TableEq(t3,t0);AttrsEq(a4,a2);AttrsEq(a5,a3);AttrsEq(a6,a0);AttrsEq(a7,a1);PredicateEq(p2,p1);PredicateEq(p3,p0);SchemaEq(s2,s0);SchemaEq(s3,s1);FuncEq(f2,f1);FuncEq(f3,f0)");
    System.out.println(reducer.isImpliedRule(rule));
  }
}
