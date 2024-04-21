package sqlsolver.superopt.fragment;

public interface Input extends Op {
  Symbol table();

  @Override
  default OpKind kind() {
    return OpKind.INPUT;
  }

}
