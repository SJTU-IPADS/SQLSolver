package sqlsolver.sql.ast;

import sqlsolver.common.field.Fields;

import static sqlsolver.sql.ast.SqlNodeFields.Expr_Kind;

class ExprField<T> extends SqlFieldBase<T> {
  private final ExprKind ownerKind;

  protected ExprField(String name, Class<?> type, ExprKind ownerKind) {
    super(ownerKind.name() + "." + name, type);
    this.ownerKind = ownerKind;
  }

  @Override
  public T getFrom(Fields target) {
    if (ownerKind != target.$(Expr_Kind)) return null;
    else return target.$(this);
  }

  @Override
  public T setTo(Fields target, T value) {
    checkValueType(value);
    if (ownerKind == target.$(Expr_Kind)) return target.$(this, value);
    throw new IllegalArgumentException(
        "cannot set field. %s %s".formatted(name(), target.$(Expr_Kind)));
  }
}
