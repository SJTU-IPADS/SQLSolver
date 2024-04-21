package sqlsolver.sql.schema;

import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.ast.constants.KeyDirection;

import java.util.List;

import static sqlsolver.common.utils.IterableSupport.lazyFilter;

public interface Constraint {
  List<Column> columns();

  List<KeyDirection> directions();

  ConstraintKind kind();

  Table refTable();

  List<Column> refColumns();

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  default boolean isIndex() {
    return kind() != ConstraintKind.NOT_NULL && kind() != ConstraintKind.CHECK;
  }

  default boolean isUnique() {
    return kind() == ConstraintKind.PRIMARY || kind() == ConstraintKind.UNIQUE;
  }

  static Iterable<Constraint> filterUniqueKey(Iterable<Constraint> constraints) {
    return lazyFilter(constraints, Constraint::isUnique);
  }
}
