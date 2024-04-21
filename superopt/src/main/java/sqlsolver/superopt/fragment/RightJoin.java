package sqlsolver.superopt.fragment;

public interface RightJoin extends Join{
  @Override
  default OpKind kind() {
    return OpKind.RIGHT_JOIN;
  }
}
