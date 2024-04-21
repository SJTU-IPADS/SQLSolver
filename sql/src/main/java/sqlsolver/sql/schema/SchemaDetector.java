package sqlsolver.sql.schema;

import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.Category;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static sqlsolver.common.utils.Commons.*;
import static sqlsolver.sql.support.locator.LocatorSupport.gatherColRefs;
import static sqlsolver.sql.support.locator.LocatorSupport.gatherSimpleSources;

public class SchemaDetector {
  /*
   * Detect underlying schema given a raw SQL.
   * e.g. SELECT t1.c1 FROM t1 JOIN t2 ON t1.k1 = t2.k2
   * => t1 schema: [c1: INTEGER(32), k1: INTEGER(32)]
   *    t2 schema: [k2: INTEGER(32)]
   * (INTEGER(32) is the default data type)
   *
   * Currently, support SQLs with such features:
   * - All columns are of type: [table/table_alias].[column]
   * - Raw table name or table alias:
   *   e.g. SELECT t.col FROM t
   *   or   SELECT a.col FROM t AS a
   *
   * P.S. alias name should be unique.
   *
   * Temporarily, do not support sub-query alias now.
   */

  private static final SqlDataType defaultDataType =
      SqlDataType.mk(Category.INTEGRAL, "integer", -1, -1);

  private final SqlNode sqlAst;
  private final String dbType;
  private final Map<String, Set<String>> tableInfo; // table -> columns list
  private final Map<String, String> aliasInfo; // alias -> table

  private Schema schema;
  private String error;

  SchemaDetector(SqlNode sqlAst, String dbType) {
    this.sqlAst = requireNonNull(sqlAst);
    this.dbType = requireNonNull(coalesce(sqlAst.dbType(), dbType));
    this.tableInfo = new HashMap<>();
    this.aliasInfo = new HashMap<>();
  }

  boolean detect() {
    try {
      if (!registerTableSources()) return false;

      final List<SqlNode> colRefs = gatherColRefs(sqlAst, false);
      for (SqlNode colRef : colRefs) {
        final String qualName = colRef.$(ExprFields.ColRef_ColName).$(SqlNodeFields.ColName_Table);
        final String colName = colRef.$(ExprFields.ColRef_ColName).$(SqlNodeFields.ColName_Col);
        if (qualName == null) return false;

        final String tableName = aliasInfo.getOrDefault(qualName, qualName);
        if (!tableInfo.containsKey(tableName)) return false;

        tableInfo.get(tableName).add(colName);
      }

      this.schema = Schema.parse(dbType, toDDL());
      return true;
    } catch (Exception ex) {
      error = dumpException(ex);
      return false;
    }
  }

  Schema getSchema() {
    return schema;
  }

  String lastError() {
    return error;
  }

  private boolean registerTableSources() {
    tableInfo.clear();
    aliasInfo.clear();

    final List<SqlNode> tables = gatherSimpleSources(sqlAst, false);
    for (SqlNode table : tables) {
      final String tableName =
          table.$(TableSourceFields.Simple_Table).$(SqlNodeFields.TableName_Table);
      final String tableAlias = table.$(TableSourceFields.Simple_Alias);
      if (tableName == null) return false;
      tableInfo.put(tableName, new HashSet<>());

      if (tableAlias != null) {
        if (aliasInfo.containsKey(tableAlias)) return false;
        aliasInfo.put(tableAlias, tableName);
      }
    }
    return true;
  }

  private String toDDL() {
    final StringBuilder buffer = new StringBuilder();
    for (String tableName : tableInfo.keySet()) {
      buffer.append("CREATE TABLE ").append(SqlSupport.quoted(dbType, tableName)).append(" (");
      buffer.append(
          joining(
                  ", ",
                  tableInfo.get(tableName),
                  buffer,
                  (col, builder) ->
                      builder
                          .append(SqlSupport.quoted(dbType, col))
                          .append(' ')
                          .append(defaultDataType.toString()))
              .append(");"));
    }
    return buffer.toString();
  }
}
