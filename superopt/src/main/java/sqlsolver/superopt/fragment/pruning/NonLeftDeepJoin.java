package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.InnerJoin;
import sqlsolver.superopt.fragment.LeftJoin;
import sqlsolver.superopt.fragment.Op;

/** Rule that matches non-left-deep Join tree. */
public class NonLeftDeepJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Op right = op.predecessors()[1];
    if (right != null && right.kind().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final Op right = op.predecessors()[1];
    if (right != null && right.kind().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }
}
