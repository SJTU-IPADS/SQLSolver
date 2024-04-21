package sqlsolver.sql.plan;

import sqlsolver.sql.ast.constants.SetOpKind;

class SetOpNodeImpl implements SetOpNode {
  private final boolean deduplicated;
  private final SetOpKind opKind;

  SetOpNodeImpl(boolean deduplicated, SetOpKind opKind) {
    this.deduplicated = deduplicated;
    this.opKind = opKind;
  }

  @Override
  public boolean deduplicated() {
    return deduplicated;
  }

  @Override
  public SetOpKind opKind() {
    return opKind;
  }
}
