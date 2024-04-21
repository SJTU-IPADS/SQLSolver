package sqlsolver.common.tree;

import sqlsolver.common.field.FieldKey;
import sqlsolver.common.utils.DelegatedMap;

import java.util.Map;

class MutableLabeledTreeFields<Kind> extends DelegatedMap<FieldKey<?>, Object>
    implements LabeledTreeFields<Kind> {
  private final LabeledTreeContext<Kind> context;
  private final int nodeId;
  private final Map<FieldKey<?>, Object> fields;

  MutableLabeledTreeFields(
      LabeledTreeContext<Kind> context, int nodeId, Map<FieldKey<?>, Object> fields) {
    this.context = context;
    this.nodeId = nodeId;
    this.fields = fields;
  }

  @Override
  protected Map<FieldKey<?>, Object> delegation() {
    return fields;
  }

  @Override
  public <T> T field(FieldKey<T> field) {
    return (T) fields.get(field);
  }

  @Override
  public Kind kind() {
    return context.kindOf(nodeId);
  }

  @Override
  public int parent() {
    return context.parentOf(nodeId);
  }

  @Override
  public <T> T setField(FieldKey<T> field, T value) {
    return (T) fields.put(field, value);
  }
}
