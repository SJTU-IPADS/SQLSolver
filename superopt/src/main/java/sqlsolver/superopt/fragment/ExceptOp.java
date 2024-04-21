package sqlsolver.superopt.fragment;

public class ExceptOp extends SetOpImpl implements Except {
  ExceptOp() {}

  @Override
  protected Op copy0() {
    final Except copy = new ExceptOp();
    copy.setDeduplicated(deduplicated());
    return copy;
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterExcept(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveExcept(this);
  }
}
