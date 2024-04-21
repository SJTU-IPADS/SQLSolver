package sqlsolver.superopt.fragment;

public abstract class SetOpImpl extends BaseOp implements SetOp {
  private boolean deduplicated = false;

  @Override
  public boolean deduplicated() {
    return deduplicated;
  }

  @Override
  public void setDeduplicated(boolean flag) {
    this.deduplicated = flag;
  }
}
