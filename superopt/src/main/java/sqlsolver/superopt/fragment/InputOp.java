package sqlsolver.superopt.fragment;

class InputOp extends BaseOp implements Input {
  InputOp() {}

  @Override
  public Symbol table() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.TABLE, 0);
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterInput(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveInput(this);
  }
}
