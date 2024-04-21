package sqlsolver.sql.ast;

import sqlsolver.common.field.Fields;

class TableSourceField<T> extends SqlFieldBase<T> {
  private final TableSourceKind ownerKind;

  protected TableSourceField(String name, Class<?> type, TableSourceKind ownerKind) {
    super(name, type);
    this.ownerKind = ownerKind;
  }

  @Override
  public T getFrom(Fields target) {
    if (ownerKind != target.$(SqlNodeFields.TableSource_Kind)) return null;
    else return target.$(this);
  }

  @Override
  public T setTo(Fields target, T value) {
    checkValueType(value);
    if (ownerKind == target.$(SqlNodeFields.TableSource_Kind)) return target.$(this, value);
    throw new IllegalArgumentException(
        "cannot set field. %s %s".formatted(name(), target.$(SqlNodeFields.TableSource_Kind)));
  }
}
