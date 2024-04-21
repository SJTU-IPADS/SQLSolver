package sqlsolver.superopt.util;

import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.fragment.*;

class FragmentComplexity implements Complexity {
  private final int[] opCounts = new int[OpKind.values().length + 2];

  FragmentComplexity(Op tree) {
    tree.acceptVisitor(OpVisitor.traverse(this::incrementOpCount));
    final int projCount = opCounts[OpKind.PROJ.ordinal()];
    opCounts[opCounts.length - 1] = tree.kind() == OpKind.PROJ ? projCount - 1 : projCount;
  }

  FragmentComplexity(Fragment fragment) {
    this(fragment.root());
  }

  private void incrementOpCount(Op op) {
    ++opCounts[op.kind().ordinal()];
    // Treat deduplication as an operator.
    if (op.kind() == OpKind.PROJ && ((Proj) op).deduplicated()) ++opCounts[opCounts.length - 2];
  }

  @Override
  public int[] opCounts() {
    return opCounts;
  }
}
