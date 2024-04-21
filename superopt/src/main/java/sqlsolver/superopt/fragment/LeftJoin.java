package sqlsolver.superopt.fragment;

public interface LeftJoin extends Join {
  @Override
  default OpKind kind() {
    return OpKind.LEFT_JOIN;
  }
}
