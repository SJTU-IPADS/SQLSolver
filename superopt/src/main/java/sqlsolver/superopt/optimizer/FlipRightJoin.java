package sqlsolver.superopt.optimizer;

import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanKind;

import static sqlsolver.sql.plan.PlanSupport.joinKindOf;

class FlipRightJoin {
  private final PlanContext plan;
  private boolean isFlipped;

  FlipRightJoin(PlanContext plan) {
    this.plan = plan;
    this.isFlipped = false;
  }

  int flip(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) flip(plan.childOf(nodeId, i));

    if (joinKindOf(plan, nodeId) == JoinKind.RIGHT_JOIN) {
      final int newRhs = plan.childOf(nodeId, 0);
      final int newLhs = plan.childOf(nodeId, 1);
      plan.detachNode(newLhs);
      plan.detachNode(newRhs);
      plan.setChild(nodeId, 0, newLhs);
      plan.setChild(nodeId, 1, newRhs);
      plan.infoCache().putJoinKindOf(nodeId, JoinKind.LEFT_JOIN);

      final var joinKeys = plan.infoCache().getJoinKeyOf(nodeId);
      if (joinKeys != null)
        plan.infoCache().putJoinKeyOf(nodeId, joinKeys.getRight(), joinKeys.getLeft());

      isFlipped = true;
    }

    return nodeId;
  }

  boolean isFlipped() {
    return isFlipped;
  }
}
