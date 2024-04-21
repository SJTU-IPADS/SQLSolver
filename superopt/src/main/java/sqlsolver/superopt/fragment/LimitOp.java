package sqlsolver.superopt.fragment;

class LimitOp extends BaseOp implements Limit {
  LimitOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterLimit(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveLimit(this);
  }
}
