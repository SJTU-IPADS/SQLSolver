package sqlsolver.superopt.fragment;

public interface InnerJoin extends Join {
  @Override
  default OpKind kind() {
    return OpKind.INNER_JOIN;
  }
}
