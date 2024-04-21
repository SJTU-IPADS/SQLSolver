package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Op;
import sqlsolver.superopt.fragment.Union;

public class DiffUnionInputs extends BaseMatchingRule{
  @Override
  public boolean enterUnion(Union op) {
    Op in0 = op.predecessors()[0], in1 = op.predecessors()[1];
    while (in0.kind().isFilter()) in0 = in0.predecessors()[0];
    while (in1.kind().isFilter()) in1 = in1.predecessors()[0];

    // if (in0.kind().isFilter() && in1.kind().isFilter()) return true;
    // if (in0.kind().isSubquery() && in1.kind().isSubquery()) return true;
    if (in0.kind().isJoin() && in1.kind().isJoin()) return true;

    if (in0.kind() != in1.kind()) {
      matched = true;
      return false;
    }

    return true;
  }
}
