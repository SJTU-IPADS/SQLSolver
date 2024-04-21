package sqlsolver.sql.plan;

import sqlsolver.sql.ast.constants.JoinKind;

public interface JoinNode extends PlanNode {
  JoinKind joinKind();

  Expression joinCond();

  @Override
  default PlanKind kind() {
    return PlanKind.Join;
  }

  static JoinNode mk(JoinKind kind, Expression joinCond) {
    return new JoinNodeImpl(kind, joinCond);
  }
}
