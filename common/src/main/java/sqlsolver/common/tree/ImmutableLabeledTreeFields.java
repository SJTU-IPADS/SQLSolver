package sqlsolver.common.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sqlsolver.common.field.FieldKey;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class ImmutableLabeledTreeFields<Kind> extends MutableLabeledTreeFields<Kind>
    implements LabeledTreeFields<Kind> {

  ImmutableLabeledTreeFields(
      LabeledTreeContext<Kind> context, int nodeId, Map<FieldKey<?>, Object> fields) {
    super(context, nodeId, fields);
  }

  @Override
  public <T> T setField(FieldKey<T> field, T value) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Object put(FieldKey<?> key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(@NotNull Map<? extends FieldKey<?>, ?> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public Set<FieldKey<?>> keySet() {
    return Collections.unmodifiableMap(delegation()).keySet();
  }

  @NotNull
  @Override
  public Collection<Object> values() {
    return Collections.unmodifiableMap(delegation()).values();
  }

  @NotNull
  @Override
  public Set<Map.Entry<FieldKey<?>, Object>> entrySet() {
    return Collections.unmodifiableMap(delegation()).entrySet();
  }
}
