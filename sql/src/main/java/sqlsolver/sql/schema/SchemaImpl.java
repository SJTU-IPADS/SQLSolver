package sqlsolver.sql.schema;

import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlKind;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodeFields;

import java.util.*;
import java.util.function.Function;

class SchemaImpl implements Schema {
  private final String dbType;
  private final Map<String, TableImpl> tables;

  private SchemaImpl(String dbType) {
    this.dbType = dbType;
    this.tables = new HashMap<>();
  }

  static Schema build(String dbType, Iterable<SqlNode> defs) {
    final Map<String, TableBuilder> builders = new HashMap<>();
    for (SqlNode def : defs) {
      if (def == null) continue; // skip comment
      final SqlKind type = def.kind();
      if (type == SqlKind.CreateTable) addCreateTable(def, builders);
      else if (type == SqlKind.AlterSeq) addAlterSequence(def, builders);
      else if (type == SqlKind.AlterTable) addAlterTable(def, builders);
      else if (type == SqlKind.IndexDef) addIndexDef(def, builders);
    }
    final SchemaImpl schema = new SchemaImpl(dbType);
    builders.values().forEach(it -> schema.addTable(it.table()));
    schema.buildRef();
    return schema;
  }

  private static void addCreateTable(SqlNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = TableBuilder.fromCreateTable(node);
    builders.put(builder.table().name(), builder);
  }

  private static void addAlterSequence(SqlNode node, Map<String, TableBuilder> builders) {
    final String operation = node.$(SqlNodeFields.AlterSeq_Op);
    final Object payload = node.$(SqlNodeFields.AlterSeq_Payload);
    if ("owned_by".equals(operation)) {
      final SqlNode colName = (SqlNode) payload;

      final TableBuilder builder = builders.get(colName.$(SqlNodeFields.ColName_Table));
      if (builder == null) return;

      final ColumnImpl column = builder.table().column(colName.$(SqlNodeFields.ColName_Col));
      if (column == null) return;

      column.flag(Column.Flag.AUTO_INCREMENT);
    }
  }

  private static void addAlterTable(SqlNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.$(SqlNodeFields.AlterTable_Name).$(SqlNodeFields.TableName_Table));
    if (builder != null) builder.fromAlterTable(node);
  }

  private static void addIndexDef(SqlNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.$(SqlNodeFields.IndexDef_Table).$(SqlNodeFields.TableName_Table));
    if (builder != null) builder.fromCreateIndex(node);
  }

  private void buildRef() {
    for (TableImpl table : tables.values())
      if (table.constraints() != null)
        for (Constraint constraint0 : table.constraints()) {
          final ConstraintImpl constraint = (ConstraintImpl) constraint0;
          final SqlNode refTableName = constraint.refTableName();
          if (refTableName != null) {
            final Table ref = table(refTableName.$(SqlNodeFields.TableName_Table));
            if (ref == null) continue;
            constraint.setRefTable(ref);
            constraint.setRefColumns(
                    ListSupport.map((Iterable<SqlNode>) constraint.refColNames(), (Function<? super SqlNode, ? extends Column>) it -> ref.column(it.$(SqlNodeFields.ColName_Col))));
          }
        }
  }

  @Override
  public String dbType() {
    return dbType;
  }

  @Override
  public Collection<? extends Table> tables() {
    return tables.values();
  }

  @Override
  public TableImpl table(String name) {
    return tables.get(SqlSupport.simpleName(name));
  }

  @Override
  public void patch(Iterable<SchemaPatch> patches) {
    for (SchemaPatch patch : patches) {
      final TableImpl table = table(patch.table());
      if (table != null) table.addPatch(patch);
    }
    buildRef();
  }

  @Override
  public void addTables(List<Table> tables) {
    for (final Table table : tables) {
      this.addTable((TableImpl) table);
    }
  }

  private void addTable(TableImpl table) {
    tables.put(table.name(), table);
  }

  @Override
  public StringBuilder toDdl(String dbType, StringBuilder buffer) {
    for (TableImpl value : tables.values()) {
      value.toDdl(dbType, buffer);
      buffer.append('\n');
    }
    final Set<Constraint> done = new HashSet<>();
    for (TableImpl value : tables.values()) {
      for (Constraint constraint : value.constraints()) {
        if (done.contains(constraint)) continue;
        done.add(constraint);
        constraint.toDdl(dbType, buffer);
      }
    }
    return buffer;
  }

  public Schema copy() {
    SchemaImpl result = new SchemaImpl(dbType);
    for(TableImpl table : tables.values()) {
      result.addTable(table);
    }
    return result;
  }
}
