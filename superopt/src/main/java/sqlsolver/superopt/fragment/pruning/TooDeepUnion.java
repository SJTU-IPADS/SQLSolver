package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Union;

public class TooDeepUnion extends BaseMatchingRule {
  @Override
  public boolean enterUnion(Union op) {
    if (op.successor() != null && op.successor().successor() != null) {
      matched = true;
      return false;
    }
    return true;
  }
}
