package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Agg;
import sqlsolver.superopt.fragment.Op;

/** Rule that matches an Agg with Join as its parent. */
public class MalformedAgg extends BaseMatchingRule{
  @Override
  public boolean enterAgg(Agg op) {
    final Op successor = op.successor();
    if (successor != null && successor.kind().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }
}
