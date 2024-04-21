package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;

import static java.util.Objects.requireNonNull;

abstract class SqlFieldBase<T> implements FieldKey<T> {
  protected final String name;
  protected final Class<?> type;

  protected SqlFieldBase(String name, Class<?> type) {
    this.name = requireNonNull(name);
    this.type = requireNonNull(type);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  protected void checkValueType(Object value) {
    if (value != null && !type.isInstance(value))
      throw new IllegalArgumentException(
          "incompatible value for field %s: %s".formatted(name, value));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SqlFieldBase<?> that = (SqlFieldBase<?>) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
