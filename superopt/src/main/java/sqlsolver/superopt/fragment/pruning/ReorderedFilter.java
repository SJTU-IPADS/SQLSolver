package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.InSubFilter;
import sqlsolver.superopt.fragment.OpKind;

public class ReorderedFilter extends BaseMatchingRule {
  @Override
  public boolean enterInSubFilter(InSubFilter op) {
    if (op.predecessors()[0] != null && op.predecessors()[0].kind() == OpKind.SIMPLE_FILTER) {
      matched = true;
      return false;
    }
    return true;
  }
}
