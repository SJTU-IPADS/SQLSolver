package sqlsolver.superopt.fragment;

class ExistsFilterOp extends BaseOp implements ExistsFilter {
  @Override
  protected boolean accept0(OpVisitor visitor) {
    return visitor.enterExistsFilter(this);
  }

  @Override
  protected void leave0(OpVisitor visitor) {
    visitor.leaveExistsFilter(this);
  }
}
