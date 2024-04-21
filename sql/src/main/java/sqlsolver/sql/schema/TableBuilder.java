package sqlsolver.sql.schema;

import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.ast.constants.KeyDirection;
import sqlsolver.sql.ast.SqlNodeFields;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static sqlsolver.common.utils.Commons.coalesce;
import static sqlsolver.sql.ast.constants.ConstraintKind.FOREIGN;
import static sqlsolver.sql.ast.constants.KeyDirection.ASC;

class TableBuilder {
  private final TableImpl table;

  private TableBuilder(TableImpl table) {
    this.table = table;
  }

  static TableBuilder fromCreateTable(SqlNode tableDef) {
    final TableBuilder builder = new TableBuilder(TableImpl.build(tableDef));

    tableDef.$(SqlNodeFields.CreateTable_Cols).forEach(builder::setColumn);
    tableDef.$(SqlNodeFields.CreateTable_Cons).forEach(builder::setConstraint);

    return builder;
  }

  TableBuilder fromAlterTable(SqlNode alterTable) {
    for (SqlNode action : alterTable.$(SqlNodeFields.AlterTable_Actions))
      switch (action.$(SqlNodeFields.AlterTableAction_Name)) {
        case "add_constraint" -> setConstraint((SqlNode) action.$(SqlNodeFields.AlterTableAction_Payload));
        case "modify_column" -> setColumn((SqlNode) action.get(SqlNodeFields.AlterTableAction_Payload));
      }
    return this;
  }

  TableBuilder fromCreateIndex(SqlNode createIndex) {
    setConstraint(createIndex);
    return this;
  }

  TableImpl table() {
    return table;
  }

  private void setColumn(SqlNode colDef) {
    final ColumnImpl column = ColumnImpl.build(table.name(), colDef);
    table.addColumn(column);

    final EnumSet<ConstraintKind> constraints = colDef.$(SqlNodeFields.ColDef_Cons);
    if (constraints != null)
      for (ConstraintKind cType : constraints) {
        final ConstraintImpl c = ConstraintImpl.build(cType, singletonList(column));

        table.addConstraint(c);
        column.addConstraint(c);
      }

    final SqlNode references = colDef.$(SqlNodeFields.ColDef_Ref);
    if (references != null) {
      final ConstraintImpl c = ConstraintImpl.build(FOREIGN, singletonList(column));
      c.setRefTableName(references.$(SqlNodeFields.Reference_Table));
      c.setRefColNames(references.$(SqlNodeFields.Reference_Cols));

      table.addConstraint(c);
      column.addConstraint(c);
    }
  }

  private void setConstraint(SqlNode constraintDef) {
    final SqlNodes keys = constraintDef.$(SqlNodeFields.IndexDef_Keys);
    final List<Column> columns = new ArrayList<>(keys.size());
    final List<KeyDirection> directions = new ArrayList<>(keys.size());
    for (SqlNode key : keys) {
      final String columnName = key.$(SqlNodeFields.KeyPart_Col);
      final ColumnImpl column = table.column(columnName);
      if (column == null) return;
      columns.add(column);
      directions.add(coalesce(key.$(SqlNodeFields.KeyPart_Direction), ASC));
    }

    final ConstraintImpl c = ConstraintImpl.build(constraintDef.$(SqlNodeFields.IndexDef_Cons), columns);
    c.setIndexType(constraintDef.$(SqlNodeFields.IndexDef_Kind));
    c.setDirections(directions);

    final SqlNode refs = constraintDef.$(SqlNodeFields.IndexDef_Refs);
    if (refs != null) {
      c.setRefTableName(refs.$(SqlNodeFields.Reference_Table));
      c.setRefColNames(refs.$(SqlNodeFields.Reference_Cols));
    }

    columns.forEach(col -> ((ColumnImpl) col).addConstraint(c));
    table.addConstraint(c);
  }
}
