package sqlsolver.superopt.fragment;

public class IntersectOp extends SetOpImpl implements Intersect{
  IntersectOp() {}

  @Override
  protected Op copy0() {
    final IntersectOp copy = new IntersectOp();
    copy.setDeduplicated(deduplicated());
    return copy;
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterIntersect(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveIntersect(this);
  }
}
