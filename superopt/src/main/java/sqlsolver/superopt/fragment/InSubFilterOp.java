package sqlsolver.superopt.fragment;

class InSubFilterOp extends BaseOp implements InSubFilter {
  InSubFilterOp() {}

  @Override
  public Symbol attrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 0);
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterInSubFilter(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveInSubFilter(this);
  }
}
