package sqlsolver.superopt.fragment;

class UnionOp extends SetOpImpl implements Union {
  UnionOp() {}

  @Override
  protected Op copy0() {
    final UnionOp copy = new UnionOp();
    copy.setDeduplicated(deduplicated());
    return copy;
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterUnion(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveUnion(this);
  }
}
