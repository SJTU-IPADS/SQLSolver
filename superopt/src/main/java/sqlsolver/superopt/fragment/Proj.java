package sqlsolver.superopt.fragment;

public interface Proj extends Op {
  Symbol attrs();

  Symbol schema();

  void setDeduplicated(boolean flag);

  boolean deduplicated();

  @Override
  default OpKind kind() {
    return OpKind.PROJ;
  }
}
