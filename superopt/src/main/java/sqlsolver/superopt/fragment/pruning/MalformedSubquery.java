package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.InSubFilter;
import sqlsolver.superopt.fragment.Op;

/** Rule that matches a InSubFilter with Filter or Join as its second child. */
public class MalformedSubquery extends BaseMatchingRule {
  @Override
  public boolean enterInSubFilter(InSubFilter op) {
    final Op in = op.predecessors()[1];
    if (!in.kind().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
