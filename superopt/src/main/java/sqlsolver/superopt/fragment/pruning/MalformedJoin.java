package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.InnerJoin;
import sqlsolver.superopt.fragment.LeftJoin;
import sqlsolver.superopt.fragment.Op;

/** Rule that matches a Join with Filter as its second child. */
public class MalformedJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Op[] in = op.predecessors();
    if (in[0].kind().isFilter() || in[1].kind().isFilter()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final Op[] in = op.predecessors();
    if (in[0].kind().isFilter() || in[1].kind().isFilter()) {
      matched = true;
      return false;
    }
    return true;
  }
}
