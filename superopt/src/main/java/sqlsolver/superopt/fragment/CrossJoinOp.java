package sqlsolver.superopt.fragment;

public class CrossJoinOp extends JoinOp implements CrossJoin{
  CrossJoinOp() {}

  @Override
  public Symbol lhsAttrs() {
    assert false : "Cross Join has no attrs symbol";
    return null;
  }

  @Override
  public Symbol rhsAttrs() {
    assert false : "Cross Join has no attrs symbol";
    return null;
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterCrossJoin(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveCrossJoin(this);
  }
}
