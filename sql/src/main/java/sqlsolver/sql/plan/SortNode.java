package sqlsolver.sql.plan;

import java.util.List;

import static java.util.Objects.requireNonNull;

public interface SortNode extends PlanNode {
  List<Expression> sortSpec();

  int[] indexedRefs();

  void setIndexedRefs(int[] refs);

  @Override
  default PlanKind kind() {
    return PlanKind.Sort;
  }

  static SortNode mk(List<Expression> sortSpec) {
    return new SortNodeImpl(requireNonNull(sortSpec));
  }
}
