package sqlsolver.common.tree;

import sqlsolver.common.field.FieldKey;

public interface LabeledTreeContext<Kind> extends TreeContext<Kind> {
  LabeledTreeFields<Kind> fieldsOf(int nodeId); // immutable

  <T> T setFieldOf(int nodeId, FieldKey<T> field, T value);

  <T> T unsetFieldOf(int nodeId, FieldKey<T> fieldKey);

  <N extends LabeledTreeNode<Kind, ?, N>> N setChild(int nodeId, FieldKey<N> key, N v);

  <Ns extends LabeledTreeNodes<Kind, ?, ?>> Ns setChildren(int nodeId, FieldKey<Ns> key, Ns v);

  /** For internal use. Avoid call this directly. */
  void setParentOf(int childId, int parentId);

  default <T> T fieldOf(int nodeId, FieldKey<T> field) {
    return fieldsOf(nodeId).$(field);
  }
}
