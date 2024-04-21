package sqlsolver.sql.schema;

import sqlsolver.sql.ast.constants.ConstraintKind;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import static sqlsolver.common.utils.IterableSupport.lazyFilter;
import static sqlsolver.sql.ast.constants.ConstraintKind.*;

public interface Table {
  String schema();

  String name();

  String engine();

  Column column(String name);

  Collection<Column> columns();

  Collection<Constraint> constraints();

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  default Iterable<Constraint> constraints(ConstraintKind kind) {
    if (kind == UNIQUE)
      return lazyFilter(constraints(), it -> UNIQUE_CONSTRAINT.contains(it.kind()));
    else return lazyFilter(constraints(), it -> it.kind() == kind);
  }

  static Table mk(String schema, String name, String engine, List<Column> columns) {
    final TableImpl table = new TableImpl(schema, name, engine);
    for (Column column : columns) table.addColumn(column);
    return table;
  }

  EnumSet<ConstraintKind> INDEXED_CONSTRAINT = EnumSet.of(UNIQUE, PRIMARY, FOREIGN);
  EnumSet<ConstraintKind> UNIQUE_CONSTRAINT = EnumSet.of(UNIQUE, PRIMARY);
}
