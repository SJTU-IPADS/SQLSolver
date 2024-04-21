package sqlsolver.common.tree;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.utils.Commons;

import java.util.AbstractList;

import static sqlsolver.common.utils.Commons.coalesce;

public abstract class LabeledTreeNodesBase<
        Kind, C extends LabeledTreeContext<Kind>, N extends LabeledTreeNode<Kind, C, N>>
    extends AbstractList<N> implements LabeledTreeNodes<Kind, C, N> {
  private final C context;
  private final TIntList nodeIds;
  private int parentId;

  protected LabeledTreeNodesBase(C context) {
    this(context, new TIntArrayList());
  }

  protected LabeledTreeNodesBase(C context, TIntList nodeIds) {
    this.context = context;
    this.nodeIds = Commons.coalesce(nodeIds, new TIntArrayList(0));
    this.parentId = TreeContext.NO_SUCH_NODE;
  }

  @Override
  public C context() {
    return context;
  }

  @Override
  public int parentId() {
    return parentId;
  }

  @Override
  public TIntList nodeIds() {
    return nodeIds;
  }

  @Override
  public void setParentId(int parentId) {
    assert parentId != TreeContext.NO_SUCH_NODE;
    this.parentId = parentId;
  }

  @Override
  public N get(int index) {
    return mk(context, nodeIds.get(index));
  }

  @Override
  public int size() {
    return nodeIds.size();
  }

  @Override
  public boolean contains(int nodeId) {
    return nodeIds.contains(nodeId);
  }

  @Override
  public int indexOf(int nodeId) {
    return nodeIds.indexOf(nodeId);
  }

  @Override
  public boolean erase(int nodeId) {
    if (nodeIds.remove(nodeId)) {
      // This method may be in turn called from LabeledTreeContext::detachNode.
      // `parentOf == null` here is to prevent recursion.
      if (parentId != TreeContext.NO_SUCH_NODE && context.parentOf(nodeId) != TreeContext.NO_SUCH_NODE)
        context.detachNode(nodeId);

      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean add(N n) {
    nodeIds.add(n.nodeId());
    if (parentId != TreeContext.NO_SUCH_NODE) context.setParentOf(n.nodeId(), parentId);
    return true;
  }

  @Override
  public int set(int index, int newNodeId) {
    TreeSupport.checkNodePresent(context, newNodeId);

    final int oldNodeId = nodeIds.set(index, newNodeId);

    if (parentId != TreeContext.NO_SUCH_NODE) {
      context.setParentOf(newNodeId, parentId);
      context.detachNode(oldNodeId);
    }

    return oldNodeId;
  }

  @Override
  public N set(int index, N element) {
    return mk(context, set(index, element.nodeId()));
  }

  @Override
  public boolean remove(Object o) {
    if (!(o instanceof LabeledTreeNode)) return false;
    final int nodeId = ((LabeledTreeNode<?, ?, ?>) o).nodeId();

    if (nodeIds.remove(nodeId)) {
      if (parentId != TreeContext.NO_SUCH_NODE) context.detachNode(nodeId);
      return true;
    } else {
      return false;
    }
  }

  protected abstract N mk(C context, int nodeId);
}
