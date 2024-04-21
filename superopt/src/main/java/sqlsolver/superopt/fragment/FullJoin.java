package sqlsolver.superopt.fragment;

public interface FullJoin extends Join{
  @Override
  default OpKind kind() {
    return OpKind.FULL_JOIN;
  }
}
