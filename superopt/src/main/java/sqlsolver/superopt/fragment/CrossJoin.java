package sqlsolver.superopt.fragment;

public interface CrossJoin extends Join{
  @Override
  default OpKind kind() {
    return OpKind.CROSS_JOIN;
  }
}
