package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Agg;

public class TooDeepAgg extends BaseMatchingRule {
  @Override
  public boolean enterAgg(Agg op) {
    if (op.successor() != null
        && op.successor().successor() != null
        && op.successor().successor().successor() != null) {
      matched = true;
      return false;
    }
    return true;
  }
}
