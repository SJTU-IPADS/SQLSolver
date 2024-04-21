package sqlsolver.sql.ast;

import sqlsolver.common.field.Fields;
import sqlsolver.common.tree.LabeledTreeFields;

class SqlNodeField<T> extends SqlFieldBase<T> {
  private final SqlKind ownerKind;

  protected SqlNodeField(String name, Class<?> type, SqlKind ownerKind) {
    super(ownerKind.name() + "." + name, type);
    this.ownerKind = ownerKind;
  }

  @Override
  public T getFrom(Fields target) {
    if (ownerKind != ((LabeledTreeFields) target).kind()) return null;
    else return target.$(this);
  }

  @Override
  public T setTo(Fields target, T value) {
    checkValueType(value);
    if (ownerKind == ((LabeledTreeFields) target).kind()) return target.$(this, value);
    throw new IllegalArgumentException(
        "cannot set field. %s %s".formatted(name(), ((LabeledTreeFields<?>) target).kind()));
  }
}
