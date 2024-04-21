package sqlsolver.superopt.fragment;

public class FullJoinOp extends JoinOp implements FullJoin{
  FullJoinOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterFullJoin(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveFullJoin(this);
  }
}
