package sqlsolver.sql.schema;

import sqlsolver.common.utils.Commons;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.ast.constants.IndexKind;
import sqlsolver.sql.ast.constants.KeyDirection;

import java.util.List;

import static sqlsolver.common.utils.Commons.joining;
import static sqlsolver.sql.SqlSupport.quoted;
import static sqlsolver.sql.ast.constants.ConstraintKind.FOREIGN;

class ConstraintImpl implements Constraint {
  private final ConstraintKind type;
  private final List<Column> columns;
  private List<KeyDirection> directions;
  private IndexKind indexType;

  private SqlNode refTableName;
  private SqlNodes refColNames;

  private Table refTable;
  private List<Column> refColumns;

  private ConstraintImpl(ConstraintKind type, List<Column> columns) {
    this.type = type;
    this.columns = columns;
  }

  static ConstraintImpl build(ConstraintKind type, List<Column> columns) {
    return new ConstraintImpl(type, columns);
  }

  @Override
  public List<Column> columns() {
    return columns;
  }

  @Override
  public List<KeyDirection> directions() {
    return directions;
  }

  @Override
  public ConstraintKind kind() {
    return type;
  }

  @Override
  public Table refTable() {
    return refTable;
  }

  @Override
  public List<Column> refColumns() {
    return refColumns;
  }

  SqlNode refTableName() {
    return refTableName;
  }

  SqlNodes refColNames() {
    return refColNames;
  }

  void setRefTable(Table refTable) {
    this.refTable = refTable;
  }

  void setRefColumns(List<Column> refColumns) {
    this.refColumns = refColumns;
  }

  void setRefTableName(SqlNode refTableName) {
    this.refTableName = refTableName;
  }

  void setRefColNames(SqlNodes refColNames) {
    this.refColNames = refColNames;
  }

  void setIndexType(IndexKind indexType) {
    this.indexType = indexType;
  }

  void setDirections(List<KeyDirection> directions) {
    this.directions = directions;
  }

  @Override
  public String toString() {
    final StringBuilder builder =
        new StringBuilder(32).append(type == null ? "INDEX" : type.name()).append(' ');
    Commons.joining("[", ",", "]", false, columns, builder);
    if (type == FOREIGN) {
      builder.append(" -> ");
      Commons.joining("[", ",", "]", false, refColumns, builder);
    }
    return builder.toString();
  }

  @Override
  public StringBuilder toDdl(String dbType, StringBuilder buffer) {
    if (type == FOREIGN) {
      buffer
          .append("ALTER TABLE ")
          .append(quoted(dbType, columns.get(0).tableName()))
          .append(" ADD FOREIGN KEY (");
      joining(",", columns, buffer, Column::name);
      buffer.append(") REFERENCES ").append(quoted(dbType, refTable.name())).append('(');
      joining(",", refColumns, buffer, Column::name);
      buffer.append(");\n");
    }

    return buffer;
  }
}
