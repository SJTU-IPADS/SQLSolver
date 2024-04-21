package sqlsolver.common.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sqlsolver.common.utils.DelegatedMap;
import sqlsolver.common.field.FieldKey;

import java.util.Map;

public abstract class LabeledTreeNodeBase<
        Kind, C extends LabeledTreeContext<Kind>, N extends LabeledTreeNode<Kind, C, N>>
    extends DelegatedMap<FieldKey<?>, Object> implements LabeledTreeNode<Kind, C, N> {
  private final C context;
  private final int nodeId;

  public LabeledTreeNodeBase(C context, int nodeId) {
    if (context == null && nodeId != TreeContext.NO_SUCH_NODE)
      throw new IllegalArgumentException("Dangling node is not permitted.");

    this.context = context;
    this.nodeId = nodeId;
  }

  @Override
  protected Map<FieldKey<?>, Object> delegation() {
    return context.fieldsOf(nodeId);
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
    return context.kindOf(nodeId);
  }

  @Override
  public N parent() {
    final int parentId = context.parentOf(nodeId);
    return parentId == TreeContext.NO_SUCH_NODE ? null : mk(context, parentId);
  }

  @Override
  public <T> T field(FieldKey<T> field) {
    return context.fieldOf(nodeId, field);
  }

  @Override
  public <T> T setField(FieldKey<T> field, T value) {
    return context.setFieldOf(nodeId, field, value);
  }

  @Nullable
  @Override
  public Object put(FieldKey<?> key, Object value) {
    return context.setFieldOf(nodeId, (FieldKey) key, value);
  }

  @Override
  public Object remove(Object key) {
    return context.unsetFieldOf(nodeId, (FieldKey) key);
  }

  @Override
  public void putAll(@NotNull Map<? extends FieldKey<?>, ?> m) {
    m.forEach(this::put);
  }

  @Override
  public void clear() {
    delegation().keySet().forEach(this::remove);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LabeledTreeNode)) return false;
    final LabeledTreeNode<?, ?, ?> that = (LabeledTreeNode<?, ?, ?>) o;
    return that.context() == context() && that.nodeId() == nodeId();
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(context) * 31 + Integer.hashCode(nodeId);
  }

  protected abstract N mk(C context, int nodeId);
}
