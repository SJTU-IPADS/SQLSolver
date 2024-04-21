package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Fragment;
import sqlsolver.superopt.fragment.OpKind;
import sqlsolver.superopt.fragment.OpVisitor;
import sqlsolver.superopt.fragment.Op;

public abstract class BaseMatchingRule implements OpVisitor, Rule {
  protected boolean matched;

  public boolean match(Fragment g) {
    matched = false;

    g.acceptVisitor(this);
    return matched;
  }

  protected static boolean isInput(Op op) {
    return op == null || op.kind() == OpKind.INPUT;
  }
}
