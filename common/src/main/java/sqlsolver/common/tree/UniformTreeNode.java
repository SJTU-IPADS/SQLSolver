package sqlsolver.common.tree;

import java.util.List;

/** A convenient wrapper for a tree node. Operations are delegated to the context. */
public interface UniformTreeNode<
    Kind, C extends UniformTreeContext<Kind>, N extends UniformTreeNode<Kind, C, N>> {
  C context();

  int nodeId();

  Kind kind();

  N parent();

  List<N> children();

  N child(int index);

  void setChild(int index, N child);

  N copyTree();
}
