package sqlsolver.superopt.fragment;

public interface Intersect extends SetOp{
  @Override
  default OpKind kind() {
    return OpKind.INTERSECT;
  }
}
