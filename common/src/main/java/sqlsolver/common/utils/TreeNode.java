package sqlsolver.common.utils;

public interface TreeNode<C extends TreeContext<C>, T extends TreeNode<C, T>> {
  T successor();

  T[] predecessors();

  C context();

  void setSuccessor(T successor);

  void setPredecessor(int index, T predecessor);

  void setContext(C context);

  T copy(C context);

  static <C extends TreeContext<C>, T extends TreeNode<C, T>> T treeRootOf(T node) {
    while (node.successor() != null) node = node.successor();
    return node;
  }

  static <C extends TreeContext<C>, T extends TreeNode<C, T>> T copyTree(T node, C context) {
    final T copy = node.copy(context);
    final T[] predecessors = node.predecessors();
    for (int i = 0; i < predecessors.length; i++)
      copy.setPredecessor(i, copyTree(predecessors[i], context));
    return copy;
  }
}
