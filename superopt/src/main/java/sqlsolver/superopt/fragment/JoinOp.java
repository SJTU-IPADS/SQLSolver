package sqlsolver.superopt.fragment;

public abstract class JoinOp extends BaseOp implements Join {
  @Override
  public Symbol lhsAttrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 0);
  }

  @Override
  public Symbol rhsAttrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 1);
  }
}
