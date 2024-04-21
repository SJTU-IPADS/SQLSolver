package sqlsolver.common.tree;

import java.util.Arrays;

import static sqlsolver.common.utils.ArraySupport.linearFind;
import static sqlsolver.common.utils.ArraySupport.safeGet;

public class UniformTreeContextBase<Kind>
    extends TreeContextBase<Kind, UniformTreeContextBase.Nd<Kind>>
    implements UniformTreeContext<Kind> {
  private final int expectedFanOut;

  protected UniformTreeContextBase(int expectedNumNodes, int expectedFanOut) {
    this(new Nd[(expectedNumNodes <= 0 ? 16 : expectedNumNodes) + 1], expectedFanOut);
  }

  protected UniformTreeContextBase(Nd<Kind>[] nodes, int expectedFanOut) {
    super(nodes);
    this.expectedFanOut = expectedFanOut;
  }

  @Override
  protected Nd<Kind> mk(Kind kind) {
    return new Nd<>(kind);
  }

  @Override
  public int childOf(int nodeId, int index) {
    return safeGet(childrenOf(nodeId), index, NO_SUCH_NODE);
  }

  @Override
  public int[] childrenOf(int nodeId) {
    TreeSupport.checkNodePresent(this, nodeId);
    return nodes[nodeId].childrenIds;
  }

  @Override
  public void setChild(int parentNodeId, int childIndex, int childNodeId) {
    TreeSupport.checkNodePresent(this, parentNodeId);
    TreeSupport.checkNodePresent(this, childNodeId);
    TreeSupport.checkParentNotSet(this, childNodeId);

    nodes[childNodeId].parentId = parentNodeId;

    final Nd<Kind> parent = nodes[parentNodeId];
    parent.childrenIds = ensureCapacity(parent.childrenIds, childIndex, expectedFanOut);

    final int existing = parent.childrenIds[childIndex];
    if (existing != NO_SUCH_NODE) detachNode(existing);

    parent.childrenIds[childIndex] = childNodeId;
  }

  @Override
  public void detachNode(int nodeId) {
    final int parentId = parentOf(nodeId);
    if (parentId == NO_SUCH_NODE) return;

    TreeSupport.checkNodePresent(this, parentId);

    nodes[nodeId].parentId = NO_SUCH_NODE;

    final int[] childrenIds = nodes[parentId].childrenIds;
    final int childIndex = linearFind(childrenIds, nodeId, 0);
    if (childIndex >= 0) childrenIds[childIndex] = NO_SUCH_NODE;
  }


  public void myDetachNode(int nodeId) {
    final int parentId = parentOf(nodeId);
    if (parentId == NO_SUCH_NODE) return;

    TreeSupport.checkNodePresent(this, parentId);

    nodes[nodeId].parentId = NO_SUCH_NODE;

    final int[] childrenIds = nodes[parentId].childrenIds;
    final int childIndex = linearFind(childrenIds, nodeId, 0);
    if (childIndex >= 0) childrenIds[childIndex] = nodes[nodeId].childrenIds[0];
  }

  @Override
  protected void relocate(int from, int to) {
    nodes[to] = nodes[from];
    nodes[from] = null;
    for (Nd<Kind> node : nodes) {
      if (node == null) continue;
      if (node.parentId == from) node.parentId = to;
      final int[] childrenIds = node.childrenIds;
      for (int i = 0; i < childrenIds.length; i++) if (childrenIds[i] == from) childrenIds[i] = to;
    }
  }

  protected static class Nd<Kind> implements NdBase<Kind> {
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private final Kind kind;
    private int parentId;
    public int[] childrenIds;

    protected Nd(Kind kind) {
      this.kind = kind;
      this.parentId = NO_SUCH_NODE;
      this.childrenIds = EMPTY_INT_ARRAY;
    }

    protected Nd(Nd<Kind> other) {
      this.kind = other.kind;
      this.parentId = other.parentId;

      if (other.childrenIds.length == 0) {
        this.childrenIds = EMPTY_INT_ARRAY;
      } else {
        this.childrenIds = Arrays.copyOf(other.childrenIds, other.childrenIds.length);
      }
    }

    @Override
    public void setParent(int parentId) {
      this.parentId = parentId;
    }

    @Override
    public int parentId() {
      return parentId;
    }

    @Override
    public Kind kind() {
      return kind;
    }
  }
}
