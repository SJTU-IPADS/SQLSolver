package sqlsolver.sql.support.resolution;

import sqlsolver.sql.schema.Column;
import sqlsolver.sql.ast.SqlNode;

public interface Attribute {
  String name();

  Relation owner();

  SqlNode expr();

  Column column();
}
