package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Op;
import sqlsolver.superopt.fragment.Union;

public class MeaninglessUnionDedup extends BaseMatchingRule {
  @Override
  public boolean enterUnion(Union op) {
    if (!op.deduplicated()) return true;

    final Op successor = op.successor();
    if (successor == null || !successor.kind().isSubquery() || successor.predecessors()[1] != op)
      return true;

    matched = true;
    return false;
  }
}
