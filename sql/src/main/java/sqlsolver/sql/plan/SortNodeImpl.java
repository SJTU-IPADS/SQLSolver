package sqlsolver.sql.plan;

import java.util.List;

class SortNodeImpl implements SortNode {
  private final List<Expression> sortSpec;
  private int[] indexedRefs;

  SortNodeImpl(List<Expression> sortSpec) {
    this.sortSpec = sortSpec;
  }

  @Override
  public int[] indexedRefs() {
    return indexedRefs;
  }

  @Override
  public void setIndexedRefs(int[] indexedRefs) {
    this.indexedRefs = indexedRefs;
  }

  @Override
  public List<Expression> sortSpec() {
    return sortSpec;
  }
}
