package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.InnerJoin;
import sqlsolver.superopt.fragment.Join;
import sqlsolver.superopt.fragment.LeftJoin;

public class TooManyJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Join op) {
    return op.successor() != null && op.successor().kind().isJoin();
  }
}
