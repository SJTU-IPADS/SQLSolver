package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Op;
import sqlsolver.superopt.fragment.OpKind;
import sqlsolver.superopt.fragment.Proj;

public class MeaninglessDedup extends BaseMatchingRule {
  @Override
  public boolean enterProj(Proj op) {
    if (!op.deduplicated()) return true;

    final Op successor = op.successor();
    if (successor == null || !successor.kind().isSubquery() || successor.predecessors()[1] != op)
      return true;

    if (successor.successor() != null
        || (op.predecessors()[0] != null && op.predecessors()[0].kind() != OpKind.INPUT)) {
      matched = true;
      return false;
    }
    return true;
  }
}
