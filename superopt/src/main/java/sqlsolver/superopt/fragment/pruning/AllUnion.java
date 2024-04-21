package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Fragment;
import sqlsolver.superopt.fragment.Input;
import sqlsolver.superopt.fragment.Op;
import sqlsolver.superopt.fragment.Union;

/** Rule that matches a fragment with only Union operators. */
public class AllUnion extends BaseMatchingRule {
  @Override
  public boolean enter(Op op) {
    if (!(op instanceof Union) && !(op instanceof Input)) {
      matched = false;
      return false;
    }
    return true;
  }

  @Override
  public boolean match(Fragment g) {
    matched = true;
    g.acceptVisitor(this);
    return matched;
  }
}
