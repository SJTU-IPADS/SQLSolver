package sqlsolver.sql.schema;

import sqlsolver.common.utils.IterableSupport;
import sqlsolver.sql.ast.SqlDataType;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.constants.ConstraintKind;

import java.util.Collection;

public interface Column {
  enum Flag {
    UNIQUE,
    INDEXED,
    FOREIGN_KEY,
    PRIMARY,
    NOT_NULL,
    GENERATED,
    HAS_DEFAULT,
    HAS_CHECK,
    AUTO_INCREMENT,
    IS_BOOLEAN,
    IS_ENUM
  }

  String tableName();

  String name();

  String rawDataType();

  SqlDataType dataType();

  Collection<SchemaPatch> patches();

  boolean isFlag(Flag flag);

  Collection<Constraint> constraints();

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  default Iterable<Constraint> constraints(ConstraintKind type) {
    return IterableSupport.lazyFilter(constraints(), it -> it.kind() == type);
  }

  static Column mk(String table, SqlNode colDef) {
    return ColumnImpl.build(table, colDef);
  }

  static Column mk(String table, String name, String rawDataType, SqlDataType dataType) {
    return new ColumnImpl(table, name, rawDataType, dataType);
  }
}
