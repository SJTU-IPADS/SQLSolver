package sqlsolver.sql.plan;

import sqlsolver.common.utils.ListSupport;

import java.util.Arrays;
import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;

public interface PlanNode {
  PlanKind kind();

  default int nodeId(PlanContext context) {
    return context.nodeIdOf(this);
  }

  default PlanNode parent(PlanContext context) {
    return context.nodeAt(context.parentOf(nodeId(context)));
  }

  default int numChildren(PlanContext context) {
    int[] childrenIds =
            Arrays.stream(context.childrenOf(nodeId(context))).filter(i -> i != NO_SUCH_NODE).toArray();
    return childrenIds.length;
  }

  default List<PlanNode> children(PlanContext context) {
    int[] childrenIds =
        Arrays.stream(context.childrenOf(nodeId(context))).filter(i -> i != NO_SUCH_NODE).toArray();
    return ListSupport.map(childrenIds, context::nodeAt);
  }

  default PlanNode child(PlanContext context, int index) {
    return context.nodeAt(context.childOf(nodeId(context), index));
  }
}
