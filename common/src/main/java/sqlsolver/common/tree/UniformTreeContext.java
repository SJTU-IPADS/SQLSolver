package sqlsolver.common.tree;

public interface UniformTreeContext<Kind> extends TreeContext<Kind> {
  int childOf(int nodeId, int index);

  int[] childrenOf(int nodeId);

  void setChild(int parentNodeId, int childIndex, int childNodeId);
}
