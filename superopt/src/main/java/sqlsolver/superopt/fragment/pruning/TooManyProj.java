package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.OpKind;
import sqlsolver.superopt.fragment.Proj;

public class TooManyProj extends BaseMatchingRule {
  @Override
  public boolean enterProj(Proj op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Proj op) {
    if (op.predecessors()[0] == null || op.predecessors()[0].kind() != OpKind.PROJ) return false;

    return op.successor() != null || !isInput(op.predecessors()[0].predecessors()[0]);
  }
}
