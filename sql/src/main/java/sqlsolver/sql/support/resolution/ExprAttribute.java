package sqlsolver.sql.support.resolution;

import sqlsolver.sql.schema.Column;
import sqlsolver.sql.ast.SqlNode;

class ExprAttribute extends AttributeBase {
  private final SqlNode expr;

  ExprAttribute(Relation owner, String name, SqlNode expr) {
    super(owner, name);
    this.expr = expr;
  }

  @Override
  public SqlNode expr() {
    return expr;
  }

  @Override
  public Column column() {
    return null;
  }
}
