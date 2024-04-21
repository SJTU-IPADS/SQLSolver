package sqlsolver.superopt.fragment;

public interface Except extends SetOp {
  @Override
  default OpKind kind() {
    return OpKind.EXCEPT;
  }
}
