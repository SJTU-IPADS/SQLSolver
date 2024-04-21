package sqlsolver.common.tree;

import java.util.Arrays;

public abstract class TreeContextBase<Kind, Nd extends TreeContextBase.NdBase<Kind>>
    implements TreeContext<Kind> {
  protected int maxNodeId;
  protected Nd[] nodes;

  protected TreeContextBase(Nd[] nodes) {
    this.maxNodeId = 0;
    this.nodes = nodes;
  }

  @Override
  public int maxNodeId() {
    return maxNodeId;
  }

  @Override
  public boolean isPresent(int nodeId) {
    return nodeId > 0 && nodeId <= maxNodeId && nodes[nodeId] != null;
  }

  @Override
  public Kind kindOf(int nodeId) {
    TreeSupport.checkNodePresent(this, nodeId);
    return nodes[nodeId].kind();
  }

  @Override
  public int parentOf(int nodeId) {
    TreeSupport.checkNodePresent(this, nodeId);
    return nodes[nodeId].parentId();
  }

  @Override
  public boolean isChildOf(int parentId, int nodeId) {
    TreeSupport.checkNodePresent(this, nodeId);
    TreeSupport.checkNodePresent(this, parentId);
    return nodes[nodeId].parentId() == parentId;
  }

  @Override
  public int mkNode(Kind kind) {
    final int newNodeId = ++maxNodeId;
    nodes = ensureCapacity(nodes, newNodeId, nodes.length << 1);
    nodes[newNodeId] = mk(kind);
    return newNodeId;
  }

  @Override
  public void deleteNode(int nodeId) {
    detachNode(nodeId);
    nodes[nodeId] = null;
    if (nodeId == maxNodeId) --maxNodeId;
    for (Nd node : nodes)
      if (node != null && node.parentId() == nodeId) node.setParent(NO_SUCH_NODE);
  }

  public void deleteNode(int nodeId, int parentId) {
    myDetachNode(nodeId);
    nodes[nodeId] = null;
    if (nodeId == maxNodeId) --maxNodeId;
    for (Nd node : nodes)
      if (node != null && node.parentId() == nodeId) node.setParent(parentId);
  }


  @Override
  public void compact() {
    if (maxNodeId <= 1) return;

    int forwardIdx = 1, backwardIdx = maxNodeId;
    while (true) {
      while (nodes[forwardIdx] != null && forwardIdx < backwardIdx) ++forwardIdx;
      while (nodes[backwardIdx] == null && backwardIdx > forwardIdx) --backwardIdx;
      if (forwardIdx == backwardIdx) break;
      relocate(backwardIdx, forwardIdx);
    }

    maxNodeId = nodes[backwardIdx] == null ? backwardIdx - 1 : backwardIdx;
    if ((maxNodeId + 1) <= (nodes.length >> 1)) nodes = Arrays.copyOf(nodes, maxNodeId + 1);
  }

  protected abstract void relocate(int from, int to);

  protected abstract Nd mk(Kind kind);

  protected static <T> T[] ensureCapacity(T[] array, int requirement, int newCapacity) {
    if (array.length <= requirement)
      return Arrays.copyOf(array, newCapacity <= requirement ? (requirement + 1) : newCapacity);
    else return array;
  }

  protected static int[] ensureCapacity(int[] array, int requirement, int newCapacity) {
    if (array.length <= requirement)
      return Arrays.copyOf(array, newCapacity <= requirement ? (requirement + 1) : newCapacity);
    else return array;
  }

  public interface NdBase<Kind> {
    void setParent(int parentId);

    int parentId();

    Kind kind();
  }
}
