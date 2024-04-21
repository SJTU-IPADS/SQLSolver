package sqlsolver.sql.plan;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.List;
import java.util.Objects;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.join;

public class PlanEq {
  private final PlanContext plan0, plan1;

  public PlanEq(PlanContext plan0, PlanContext plan1) {
    this.plan0 = plan0;
    this.plan1 = plan1;
  }

  boolean isEqTree() {
    return isEqTree(plan0.root(), plan1.root());
  }

  boolean isEqTree(int root0, int root1) {
    return isStructuralEq(root0, root1) && isSemanticEq(root0, root1);
  }

  private boolean isStructuralEq(int node0, int node1) {
    if (plan0.kindOf(node0) != plan1.kindOf(node1)) return false;
    final PlanKind kind = plan0.kindOf(node0);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      if (!isStructuralEq(plan0.childOf(node0, i), plan1.childOf(node1, i))) return false;
    }
    return true;
  }

  public boolean isSemanticEqForNodes(int node0, int node1) {
    final PlanKind kind0 = plan0.kindOf(node0);
    final PlanKind kind1 = plan1.kindOf(node1);
    if(kind0 != kind1)
      return false;

    return switch (kind0) {
      case SetOp -> isEqSetOp(node0, node1);
      case Sort -> isEqSort(node0, node1);
      case Limit -> isEqLimit(node0, node1);
      case Filter -> isEqFilter(node0, node1);
      case Agg -> isEqAgg(node0, node1);
      case Proj -> isEqProj(node0, node1);
      case InSub -> isEqInSub(node0, node1);
      case Exists -> isEqExists(node0, node1);
      case Join -> isEqJoin(node0, node1);
      case Input -> isEqInput(node0, node1);
    };
  }

  private boolean isSemanticEq(int node0, int node1) {
    final PlanKind kind = plan0.kindOf(node0);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      if (!isSemanticEq(plan0.childOf(node0, i), plan1.childOf(node1, i))) return false;
    }

    return switch (kind) {
      case SetOp -> isEqSetOp(node0, node1);
      case Sort -> isEqSort(node0, node1);
      case Limit -> isEqLimit(node0, node1);
      case Filter -> isEqFilter(node0, node1);
      case Agg -> isEqAgg(node0, node1);
      case Proj -> isEqProj(node0, node1);
      case InSub -> isEqInSub(node0, node1);
      case Exists -> isEqExists(node0, node1);
      case Join -> isEqJoin(node0, node1);
      case Input -> isEqInput(node0, node1);
    };
  }

  private boolean isEqInput(int node0, int node1) {
    final InputNode input0 = (InputNode) plan0.nodeAt(node0);
    final InputNode input1 = (InputNode) plan1.nodeAt(node1);

    return input0.table().equals(input1.table());
  }

  private boolean isEqJoin(int node0, int node1) {
    if (PlanSupport.joinKindOf(plan0, node0) != PlanSupport.joinKindOf(plan1, node1)) return false;

    final Expression joinCond0 = ((JoinNode) plan0.nodeAt(node0)).joinCond();
    final Expression joinCond1 = ((JoinNode) plan1.nodeAt(node1)).joinCond();
    return isEqJoinPred(node0, joinCond0, node1, joinCond1);
  }

  private boolean isEqExists(int node0, int node1) {
    return true;
  }

  private boolean isEqInSub(int node0, int node1) {
    final Expression expr0 = ((InSubNode) plan0.nodeAt(node0)).expr();
    final Expression expr1 = ((InSubNode) plan1.nodeAt(node1)).expr();
    return isEqExpr(node0, expr0, node1, expr1);
  }

  private boolean isEqProj(int node0, int node1) {
    if (PlanSupport.isDedup(plan0, node0) != PlanSupport.isDedup(plan1, node1)) return false;

    final List<Expression> exprs0 = ((ProjNode) plan0.nodeAt(node0)).attrExprs();
    final List<Expression> exprs1 = ((ProjNode) plan1.nodeAt(node1)).attrExprs();
    if (exprs0.size() != exprs1.size()) return false;

    return all(
        zip(exprs0, exprs1), pair -> isEqExpr(node0, pair.getLeft(), node1, pair.getRight()));
  }

  private boolean isEqAgg(int node0, int node1) {
    final AggNode agg0 = (AggNode) plan0.nodeAt(node0);
    final AggNode agg1 = (AggNode) plan1.nodeAt(node1);

    final List<Expression> exprs0 = agg0.attrExprs();
    final List<Expression> exprs1 = agg1.attrExprs();
    if (exprs0.size() != exprs1.size()) return false;

    final List<Expression> group0 = agg0.groupByExprs();
    final List<Expression> group1 = agg1.groupByExprs();
    if (group0.isEmpty() != group1.isEmpty()) return false;

    final Expression having0 = agg0.havingExpr();
    final Expression having1 = agg1.havingExpr();
    if ((having0 == null) != (having1 == null)) return false;

    if (!all(zip(exprs0, exprs1), pair -> isEqExpr(node0, pair.getLeft(), node1, pair.getRight())))
      return false;

    if (any(group0, g0 -> none(group1, g1 -> isEqExpr(node0, g0, node1, g1)))) return false;

    return having0 == null || isEqExpr(node0, having0, node1, having1);
  }

  private boolean isEqFilter(int node0, int node1) {
    final Expression expr0 = ((SimpleFilterNode) plan0.nodeAt(node0)).predicate();
    final Expression expr1 = ((SimpleFilterNode) plan1.nodeAt(node1)).predicate();
    return isEqExpr(node0, expr0, node1, expr1);
  }

  private boolean isEqLimit(int node0, int node1) {
    final LimitNode n0 = (LimitNode) plan0.nodeAt(node0);
    final LimitNode n1 = (LimitNode) plan1.nodeAt(node1);

    final Expression limit0 = n0.limit();
    final Expression limit1 = n1.limit();
    if ((limit0 == null) != (limit1 == null)) return false;

    final Expression offset0 = n0.offset();
    final Expression offset1 = n1.offset();
    if ((offset0 == null) != (offset1 == null)) return false;

    return (limit0 == null || limit0.toString().equals(limit1.toString()))
        && (offset0 == null || offset0.toString().equals(offset1.toString()));
  }

  private boolean isEqSort(int node0, int node1) {
    final List<Expression> exprs0 = ((SortNode) plan0.nodeAt(node0)).sortSpec();
    final List<Expression> exprs1 = ((SortNode) plan1.nodeAt(node1)).sortSpec();

    if (exprs0.size() != exprs1.size()) return false;

    return all(
        zip(exprs0, exprs1), pair -> isEqExpr(node0, pair.getLeft(), node1, pair.getRight()));
  }

  private boolean isEqSetOp(int node0, int node1) {
    if (PlanSupport.isDedup(plan0, node0) != PlanSupport.isDedup(plan1, node1)) return false;
    return ((SetOpNode) plan0.nodeAt(node0)).opKind() == ((SetOpNode) plan1.nodeAt(node1)).opKind();
  }

  private boolean isEqExpr(int node0, Expression expr0, int node1, Expression expr1) {
    if (!Objects.equals(expr0.toString(), expr1.toString())) return false;

    final Values refs0 = plan0.valuesReg().valueRefsOf(expr0);
    final Values refs1 = plan1.valuesReg().valueRefsOf(expr1);
    if (refs0.size() != refs1.size()) return false;

    final List<Value> ctx0 = getRefBindingContext(plan0, node0);
    final List<Value> ctx1 = getRefBindingContext(plan1, node1);
    if (ctx0.size() != ctx1.size()) return false;

    final TIntList indexedRefs0 = computeIndexedRefs(refs0, ctx0);
    final TIntList indexedRefs1 = computeIndexedRefs(refs1, ctx1);
    return indexedRefs0.equals(indexedRefs1);
  }

  private boolean isEqJoinPred(int node0, Expression expr0, int node1, Expression expr1) {
    if (expr0 == null && expr1 == null) return true;
    if ((expr0 == null) != (expr1 == null)) return false;
    if (!Objects.equals(expr0.toString(), expr1.toString())) return false;

    if (plan0.infoCache().isEquiJoin(node0) && plan1.infoCache().isEquiJoin(node1)) {
      final var keys0 = plan0.infoCache().getJoinKeyOf(node0);
      final var keys1 = plan1.infoCache().getJoinKeyOf(node1);
      final List<Value> ctx0 = getRefBindingContext(plan0, node0);
      final List<Value> ctx1 = getRefBindingContext(plan1, node1);
      return computeIndexedRefs(keys0.getLeft(), ctx0)
              .equals(computeIndexedRefs(keys1.getLeft(), ctx1))
          && computeIndexedRefs(keys0.getRight(), ctx0)
              .equals(computeIndexedRefs(keys1.getRight(), ctx1));

    } else {
      return isEqExpr(node0, expr0, node1, expr1);
    }
  }

  private static List<Value> getRefBindingContext(PlanContext plan, int nodeId) {
    final List<Value> lookup0 = PlanSupport.getRefBindingLookup(plan, nodeId);
    if (!plan.kindOf(nodeId).isFilter()) return lookup0;
    return join(lookup0, PlanSupport.getRefBindingForeignLookup(plan, nodeId));
  }

  private TIntList computeIndexedRefs(List<Value> refs, List<Value> values) {
    final TIntList indexedRefs = new TIntArrayList(refs.size());
    for (Value ref : refs) {
      final int index = values.indexOf(ref);
      assert index >= 0;
      indexedRefs.add(index);
    }
    return indexedRefs;
  }
}
