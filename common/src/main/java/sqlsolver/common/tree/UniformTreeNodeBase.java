package sqlsolver.common.tree;

import sqlsolver.common.utils.ListSupport;

import java.util.List;

public abstract class UniformTreeNodeBase<
        Kind, C extends UniformTreeContext<Kind>, N extends UniformTreeNode<Kind, C, N>>
    implements UniformTreeNode<Kind, C, N> {
  private final C context;
  private final int nodeId;

  protected UniformTreeNodeBase(C context, int nodeId) {
    if (context == null && nodeId != TreeContext.NO_SUCH_NODE)
      throw new IllegalArgumentException("Dangling node is not permitted.");

    this.context = context;
    this.nodeId = nodeId;
  }

  @Override
  public C context() {
    return context;
  }

  @Override
  public int nodeId() {
    return nodeId;
  }

  @Override
  public Kind kind() {
    return context().kindOf(nodeId());
  }

  @Override
  public N parent() {
    return mk(context(), context().parentOf(nodeId()));
  }

  @Override
  public List<N> children() {
    final C ctx = context();
    return ListSupport.map(ctx.childrenOf(nodeId()), x -> mk(ctx, x));
  }

  @Override
  public N child(int index) {
    return mk(context(), context().childOf(nodeId(), index));
  }

  @Override
  public void setChild(int index, N child) {
    context().setChild(nodeId(), index, child.nodeId());
  }

  @Override
  public N copyTree() {
    return mk(context(), TreeSupport.copyTree(context(), nodeId()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UniformTreeNode<?, ?, ?> that)) return false;
    return that.context() == context() && that.nodeId() == nodeId();
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(context) * 31 + Integer.hashCode(nodeId);
  }

  protected abstract N mk(C context, int nodeId);
}
