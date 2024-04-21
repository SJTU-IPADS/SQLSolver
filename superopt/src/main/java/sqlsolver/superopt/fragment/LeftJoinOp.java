package sqlsolver.superopt.fragment;

class LeftJoinOp extends JoinOp implements LeftJoin {
  LeftJoinOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterLeftJoin(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveLeftJoin(this);
  }
}
