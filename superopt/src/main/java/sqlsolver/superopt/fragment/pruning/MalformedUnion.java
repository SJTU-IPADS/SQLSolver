package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Op;
import sqlsolver.superopt.fragment.Union;

/** Rule that matches a InSubFilter with Filter or Join as its second child. */
public class MalformedUnion extends BaseMatchingRule {
  @Override
  public boolean enterUnion(Union op) {
    final Op[] in = op.predecessors();
    if (!in[0].kind().isValidOutput() || !in[1].kind().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
