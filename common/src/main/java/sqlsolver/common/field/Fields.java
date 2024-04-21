package sqlsolver.common.field;

import java.util.EnumSet;
import java.util.Map;

public interface Fields extends Map<FieldKey<?>, Object> {
  <T> T field(FieldKey<T> field);

  <T> T setField(FieldKey<T> field, T value);

  default <T> T $(FieldKey<T> field) {
    return field(field);
  }

  default <T> T $(FieldKey<T> field, T value) {
    return setField(field, value);
  }

  default <T extends Enum<T>> void flag(FieldKey<EnumSet<T>> key, T value) {
    EnumSet<T> set = $(key);
    if (set == null) $(key, set = EnumSet.noneOf(value.getDeclaringClass()));
    set.add(value);
  }

  default void flag(FieldKey<Boolean> key) {
    $(key, true);
  }

  default void flag(FieldKey<Boolean> key, boolean flag) {
    if (flag) $(key, true);
    else remove(key);
  }

  default boolean isFlag(FieldKey<Boolean> key) {
    return $(key) == Boolean.TRUE;
  }

  default <T extends Enum<T>> boolean isFlag(FieldKey<EnumSet<T>> key, T value) {
    final EnumSet<T> set = $(key);
    return set != null && set.contains(value);
  }
}
