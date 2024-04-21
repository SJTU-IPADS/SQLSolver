package sqlsolver.common.tree;

/**
 * A general tree whose nodes are typed. Nodes are uniquely identified by an integer. Methods of
 * this class take such an id to operate nodes.
 */
public interface TreeContext<Kind> {
  int NO_SUCH_NODE = 0;

  int maxNodeId();

  boolean isPresent(int nodeId);

  Kind kindOf(int nodeId);

  int parentOf(int nodeId);

  boolean isChildOf(int parentId, int nodeId);

  int mkNode(Kind kind);

  /** Detach a node from its parent. */
  void detachNode(int nodeId);

  default void myDetachNode(int nodeIs) {

  }

  /** Delete a node from this context. */
  void deleteNode(int nodeId);

  /**
   * For some implementation, after detachments the memory usage becomes sparse. This method then
   * compacts.
   *
   * <p>All node-ids are invalidated after compaction. Be careful.
   */
  void compact();

  /**
   * The root of the tree.
   *
   * <p>Note: when called on an incomplete tree (actually a forest), the result is undefined.
   */
  default int root() {
    if (maxNodeId() == 0) return NO_SUCH_NODE;
    else return TreeSupport.rootOf(this, 1);
  }

  default int numNodes() {
    return TreeSupport.countNodes(this);
  }

  default void deleteDetached(int rootId) {
    TreeSupport.deleteDetached(this, rootId);
  }
}
