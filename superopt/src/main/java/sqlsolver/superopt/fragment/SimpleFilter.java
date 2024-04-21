package sqlsolver.superopt.fragment;

public interface SimpleFilter extends AttrsFilter {
  Symbol predicate();

  @Override
  default OpKind kind() {
    return OpKind.SIMPLE_FILTER;
  }

}
