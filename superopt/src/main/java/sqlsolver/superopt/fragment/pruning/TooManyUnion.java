package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.OpKind;
import sqlsolver.superopt.fragment.Union;

public class TooManyUnion extends BaseMatchingRule {

  @Override
  public boolean enterUnion(Union op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Union op) {
    return op.successor() != null && op.successor().kind() == OpKind.UNION;
  }
}
