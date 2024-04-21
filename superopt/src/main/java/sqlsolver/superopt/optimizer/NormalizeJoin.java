package sqlsolver.superopt.optimizer;

import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.plan.*;

import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.sql.plan.PlanKind.Join;

class NormalizeJoin {
  private final PlanContext plan;

  private int rootId;

  NormalizeJoin(PlanContext plan) {
    this.plan = plan;
  }

  int normalizeTree(int rootId) {
    this.rootId = rootId;
    return normalizeTree0(rootId);
  }

  private int normalize(int nodeId) {
    if (plan.kindOf(nodeId) != Join) return nodeId;
    //    if (plan.kindOf(plan.childOf(nodeId, 1)) != Join) return nodeId;

    @SuppressWarnings("unused")
    final int lhs = normalize(plan.childOf(nodeId, 0));
    final int rhs = normalize(plan.childOf(nodeId, 1));

    if (plan.kindOf(rhs) != Join) return nodeId;

    final int b = plan.childOf(rhs, 0), c = plan.childOf(rhs, 1);
    final int join0 = nodeId, join1 = rhs;

    final int parent = plan.parentOf(join0), childIdx = indexOfChild(plan, join0);
    assert plan.kindOf(c) != Join;

    plan.detachNode(join1);
    plan.setChild(parent, childIdx, join1); // meanwhile, join0 is detached
    plan.setChild(join1, 0, join0); // meanwhile, b is detached

    final List<Value> rhsRefs = getRhsRefs(join0);
    if (plan.valuesReg().valuesOf(b).containsAll(rhsRefs)) {
      // 1. join0<lhs.x=b.y>(lhs,join1<b.z=c.w>(b,c)) => join1<b.z=c.w>(join0<lhs.x=b.y>(lhs,b),c)
      plan.setChild(join0, 1, b);
      normalize(join0);
      return join1;

    } else {
      // 2. join0<lhs.x=c.y>(lhs,join1<b.z=c.w>(b,c)) => join1<c.w=b.z>(join0<lhs.x=c.y>(lhs,c),b)
      plan.setChild(join1, 1, b); // meanwhile, c is detached
      plan.setChild(join0, 1, c);

      final InfoCache infoCache = plan.infoCache();
      final var joinKeys = infoCache.getJoinKeyOf(join1);
      if (joinKeys != null) infoCache.putJoinKeyOf(join1, joinKeys.getRight(), joinKeys.getLeft());

      return normalize(join1);
    }
  }

  private int normalizeTree0(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    final int numChildren = kind.numChildren();
    for (int i = 0; i < numChildren; ++i) normalizeTree0(plan.childOf(nodeId, i));

    if (plan.kindOf(nodeId) == Join && (nodeId == rootId || isJoinTreeRoot(nodeId))) {
      return normalize(nodeId);
    }

    return nodeId;
  }

  private boolean isJoinTreeRoot(int nodeId) {
    final int parentId = plan.parentOf(nodeId);
    return parentId == NO_SUCH_NODE || plan.kindOf(parentId) != Join;
  }

  private List<Value> getRhsRefs(int nodeId) {
    final var cachedJoinKey = plan.infoCache().getJoinKeyOf(nodeId);
    if (cachedJoinKey != null) return cachedJoinKey.getRight();

    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values refs = valuesReg.valueRefsOf(((JoinNode) plan.nodeAt(nodeId)).joinCond());
    final Values rhsValues = valuesReg.valuesOf(plan.childOf(nodeId, 1));
    return ListSupport.filter(refs, rhsValues::contains);
  }
}
