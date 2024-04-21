package sqlsolver.superopt.fragment;

public interface InSubFilter extends AttrsFilter {
  @Override
  default OpKind kind() {
    return OpKind.IN_SUB_FILTER;
  }

}
