package sqlsolver.sql.schema;

import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.ast.SqlNode;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static sqlsolver.sql.SqlSupport.parseSql;
import static sqlsolver.sql.SqlSupport.splitSql;

public interface Schema {
  Collection<? extends Table> tables();

  String dbType();

  Table table(String name);

  void patch(Iterable<SchemaPatch> patches);

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  void addTables(List<Table> tables);

  static Schema parse(String dbType, String str) {
    return SchemaImpl.build(dbType, ListSupport.map((Iterable<String>) splitSql(str), (Function<? super String, ? extends SqlNode>) s -> parseSql(dbType, s)));
  }

  public Schema copy();
}
