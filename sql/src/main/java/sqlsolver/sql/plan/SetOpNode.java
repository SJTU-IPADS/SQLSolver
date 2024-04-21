package sqlsolver.sql.plan;

import sqlsolver.sql.ast.constants.SetOpKind;

public interface SetOpNode extends PlanNode {
  boolean deduplicated();

  SetOpKind opKind();

  @Override
  default PlanKind kind() {
    return PlanKind.SetOp;
  }

  static SetOpNode mk(boolean deduplicated, SetOpKind opKind) {
    return new SetOpNodeImpl(deduplicated, opKind);
  }
}
