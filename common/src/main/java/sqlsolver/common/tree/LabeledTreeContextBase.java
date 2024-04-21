package sqlsolver.common.tree;

import sqlsolver.common.field.FieldKey;
import sqlsolver.common.utils.MapSupport;

import java.util.Map;

public class LabeledTreeContextBase<Kind>
    extends TreeContextBase<Kind, LabeledTreeContextBase.Nd<Kind>>
    implements LabeledTreeContext<Kind> {
  protected LabeledTreeContextBase(int expectedNumNodes) {
    super(new Nd[(expectedNumNodes <= 0 ? 16 : expectedNumNodes) + 1]);
  }

  @Override
  protected Nd<Kind> mk(Kind kind) {
    return new Nd<>(kind);
  }

  @Override
  public LabeledTreeFields<Kind> fieldsOf(int nodeId) {
    TreeSupport.checkNodePresent(this, nodeId);
    return new ImmutableLabeledTreeFields<>(this, nodeId, nodes[nodeId].fields);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> T setFieldOf(int nodeId, FieldKey<T> field, T value) {
    if (value instanceof LabeledTreeNode) {
      return (T) setChild(nodeId, (FieldKey) field, (LabeledTreeNode) value);
    } else if (value instanceof LabeledTreeNodes) {
      return (T) setChildren(nodeId, (FieldKey) field, (LabeledTreeNodes) value);
    } else {
      TreeSupport.checkNodePresent(this, nodeId);
      return field.setTo(new MutableLabeledTreeFields<>(this, nodeId, nodes[nodeId].fields), value);
    }
  }

  @Override
  public <T> T unsetFieldOf(int nodeId, FieldKey<T> fieldKey) {
    TreeSupport.checkNodePresent(this, nodeId);
    return (T) nodes[nodeId].fields.remove(fieldKey);
  }

  @Override
  public <N extends LabeledTreeNode<Kind, ?, N>> N setChild(int nodeId, FieldKey<N> key, N v) {
    TreeSupport.checkNodePresent(this, nodeId);
    final int childId = v.nodeId();
    TreeSupport.checkNodePresent(this, childId);
    TreeSupport.checkParentNotSet(this, childId);

    setParentOf(childId, nodeId);
    final N n = key.setTo(new MutableLabeledTreeFields<>(this, nodeId, nodes[nodeId].fields), v);
    if (n != null && n.nodeId() != NO_SUCH_NODE) detachNode(n.nodeId());
    return n;
  }

  @Override
  public <Ns extends LabeledTreeNodes<Kind, ?, ?>> Ns setChildren(
      int nodeId, FieldKey<Ns> key, Ns v) {
    TreeSupport.checkNodePresent(this, nodeId);

    for (LabeledTreeNode<Kind, ?, ?> child : (LabeledTreeNodes<Kind, ?, ?>) v) {
      final int childId = child.nodeId();
      TreeSupport.checkNodePresent(this, childId);
      TreeSupport.checkParentNotSet(this, childId);
    }

    for (LabeledTreeNode<Kind, ?, ?> child : (LabeledTreeNodes<Kind, ?, ?>) v) {
      setParentOf(child.nodeId(), nodeId);
    }

    final Ns existing =
        key.setTo(new MutableLabeledTreeFields<>(this, nodeId, nodes[nodeId].fields), v);

    if (existing != null) {
      for (LabeledTreeNode<Kind, ?, ?> removed : (LabeledTreeNodes<Kind, ?, ?>) existing)
        if (removed != null && removed.nodeId() != NO_SUCH_NODE) {
          detachNode(removed.nodeId());
        }
    }

    return existing;
  }

  @Override
  public void detachNode(int nodeId) {
    final int parentId = parentOf(nodeId);
    if (parentId == NO_SUCH_NODE) return;

    // This line must reside here. See LabeledTreeNodes::erase for details.
    nodes[nodeId].parentId = NO_SUCH_NODE;

    final Nd<Kind> parent = nodes[parentId];
    for (Map.Entry<FieldKey<?>, Object> pair : parent.fields.entrySet()) {
      final Object value = pair.getValue();

      if (value instanceof LabeledTreeNode) {
        if (((LabeledTreeNode<?, ?, ?>) value).nodeId() == nodeId) {
          pair.setValue(null);
          break;
        }
      }

      if (value instanceof LabeledTreeNodes) {
        if (((LabeledTreeNodes<?, ?, ?>) value).erase(nodeId)) break;
      }
    }
  }

  @Override
  protected void relocate(int from, int to) {
    nodes[to] = nodes[from];
    nodes[from] = null;

    for (Nd<Kind> node : nodes) {
      if (node == null) continue;

      if (node.parentId == from) node.parentId = to;

      for (Map.Entry<FieldKey<?>, Object> pair : node.fields.entrySet()) {
        final Object value = pair.getValue();

        if (value instanceof LabeledTreeNode) {
          final LabeledTreeNodeBase treeNode = (LabeledTreeNodeBase) value;
          if (treeNode.nodeId() == from) {
            pair.setValue(treeNode.mk(this, to));
            break;
          }
        }

        if (value instanceof LabeledTreeNodes) {
          final LabeledTreeNodes<?, ?, ?> nodes = (LabeledTreeNodes<?, ?, ?>) value;
          final int index = nodes.indexOf(from);
          if (index >= 0) {
            nodes.set(index, to);
            break;
          }
        }
      }
    }
  }

  @Override
  public final void setParentOf(int childId, int parentId) {
    nodes[childId].parentId = parentId;
  }

  protected static final class Nd<Kind> implements NdBase<Kind> {
    private final Kind kind;
    private int parentId;
    private final Map<FieldKey<?>, Object> fields;

    private Nd(Kind kind) {
      this.kind = kind;
      this.parentId = NO_SUCH_NODE;
      this.fields = MapSupport.mkLate();
    }

    public Map<FieldKey<?>, Object> fields() {
      return fields;
    }

    @Override
    public void setParent(int parentId) {
      this.parentId = parentId;
    }

    @Override
    public Kind kind() {
      return kind;
    }

    @Override
    public int parentId() {
      return parentId;
    }
  }
}
