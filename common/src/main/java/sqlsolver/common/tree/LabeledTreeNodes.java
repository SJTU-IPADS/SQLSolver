package sqlsolver.common.tree;

import gnu.trove.list.TIntList;

import java.util.List;

/** A list of nodes of a single field. */
public interface LabeledTreeNodes<
        Kind, C extends LabeledTreeContext<Kind>, N extends LabeledTreeNode<Kind, C, N>>
    extends List<N> {
  C context();

  int parentId();

  TIntList nodeIds();

  // Should only be invoked from LabeledTreeContext.
  void setParentId(int parentId);

  boolean contains(int nodeId);

  int indexOf(int nodeId);

  /*            IMPLEMENTATION NOTE             */
  /* Modifications must reflect on the context. */

  boolean erase(int nodeId);

  int set(int index, int newNodeId);

  @Override
  boolean add(N node);

  @Override
  N set(int index, N node);

  @Override
  boolean remove(Object o);
}
