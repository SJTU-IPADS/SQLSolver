package sqlsolver.superopt.fragment;

public class RightJoinOp extends JoinOp implements RightJoin{
  RightJoinOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterRightJoin(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveRightJoin(this);
  }
}
