package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Agg;
import sqlsolver.superopt.fragment.OpKind;

public class TooManyAgg extends BaseMatchingRule {
  @Override
  public boolean enterAgg(Agg op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Agg op) {
    if (op.successor() != null && op.successor().kind() == OpKind.AGG)
      return true;

    return false;
  }
}
