package sqlsolver.superopt.fragment;

public interface ExistsFilter extends Filter {
  @Override
  default OpKind kind() {
    return OpKind.EXISTS_FILTER;
  }

}
