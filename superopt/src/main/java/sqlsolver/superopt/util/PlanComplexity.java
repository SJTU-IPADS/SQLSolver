package sqlsolver.superopt.util;

import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.SetOpKind;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanKind;
import sqlsolver.sql.plan.SetOpNode;
import sqlsolver.superopt.fragment.OpKind;

import static sqlsolver.sql.plan.PlanSupport.isDedup;
import static sqlsolver.sql.plan.PlanSupport.joinKindOf;

public class PlanComplexity implements Complexity {
  private final int[] opCounts;

  PlanComplexity(PlanContext plan, int rootId) {
    this.opCounts = new int[OpKind.values().length + 1];
    countOps(plan, rootId);
  }

  private void countOps(PlanContext plan, int nodeId) {
    final PlanKind nodeKind = plan.kindOf(nodeId);
    // if (nodeKind == PlanKind.Join) {
    //   final JoinKind joinKind = joinKindOf(plan, nodeId);
    //   if (joinKind.isInner()) ++opCounts[OpKind.INNER_JOIN.ordinal()];
    //   else ++opCounts[OpKind.LEFT_JOIN.ordinal()];
    //
    // } else {
    //   final int joinOpKindNum = filter(Arrays.stream(OpKind.values()).toList(), OpKind::isJoin).size();
    //   if (nodeKind.ordinal() > PlanKind.Join.ordinal())
    //     ++opCounts[nodeKind.ordinal() + (joinOpKindNum - 1)];
    //   else ++opCounts[nodeKind.ordinal()];
    //   // Treat deduplication as an operator.
    //   if (nodeKind == PlanKind.Proj && isDedup(plan, nodeId)) ++opCounts[opCounts.length - 1];
    // }
    ++opCounts[castKind(plan, nodeId).ordinal()];
    if (nodeKind == PlanKind.Proj && isDedup(plan, nodeId)) ++opCounts[opCounts.length - 1];

    for (int i = 0, bound = nodeKind.numChildren(); i < bound; i++)
      countOps(plan, plan.childOf(nodeId, i));
  }

  private OpKind castKind(PlanContext plan, int nodeId) {
    final PlanKind nodeKind = plan.kindOf(nodeId);
    switch (nodeKind) {
      case Input: return OpKind.INPUT;
      case Join: {
        final JoinKind joinKind = joinKindOf(plan, nodeId);
        return joinKind.isInner() ? OpKind.INNER_JOIN : OpKind.LEFT_JOIN;
      }
      case Filter: return OpKind.SIMPLE_FILTER;
      case InSub: return OpKind.IN_SUB_FILTER;
      case Exists: return OpKind.EXISTS_FILTER;
      case Proj: return OpKind.PROJ;
      case Agg: return OpKind.AGG;
      case Sort: return OpKind.SORT;
      case Limit: return OpKind.LIMIT;
      case SetOp: {
        final SetOpKind setOpKind = ((SetOpNode) plan.nodeAt(nodeId)).opKind();
        return switch (setOpKind) {
          case UNION -> OpKind.UNION;
          case INTERSECT -> OpKind.INTERSECT;
          case EXCEPT -> OpKind.EXCEPT;
        };
      }
      default: assert false; return null;
    }
  }

  @Override
  public int[] opCounts() {
    return opCounts;
  }
}
