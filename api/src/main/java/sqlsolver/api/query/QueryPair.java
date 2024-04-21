package sqlsolver.api.query;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.schema.Schema;

/**
 * Basic structure about queries.
 * Includes two SQL statements to demonstrate equivalence.
 */
public interface QueryPair {

  static QueryPair mk(int lineNum, Schema schema, String sql0, String sql1) {
    return new QueryPairImpl(lineNum, schema, sql0, sql1, null, null, null, null, null, null);
  }

  static QueryPair mk(int lineNum, Schema schema, String sql0, String sql1, String originSql0, String originSql1) {
    return new QueryPairImpl(lineNum, schema, sql0, sql1, originSql0, originSql1, null, null, null, null);
  }

  static QueryPair mk(int lineNum, Schema schema, String sql0, String sql1, SqlNode q0, SqlNode q1) {
    return new QueryPairImpl(lineNum, schema, sql0, sql1, null, null, q0, q1, null, null);
  }

  static QueryPair mk(int lineNum, Schema schema, String sql0, String sql1, String originSql0, String originSql1, SqlNode q0, SqlNode q1) {
    return new QueryPairImpl(lineNum, schema, sql0, sql1, originSql0, originSql1, q0, q1, null, null);
  }

  static QueryPair mk(int lineNum, Schema schema, String sql0, String sql1, String originSql0, String originSql1, SqlNode q0, SqlNode q1, RelNode p0, RelNode p1) {
    return new QueryPairImpl(lineNum, schema, sql0, sql1, originSql0, originSql1, q0, q1, p0, p1);
  }

  int pairId();

  Schema getSchema();

  String getSql0();

  String getSql1();

  String getOriginSql0();

  String getOriginSql1();

  SqlNode getAst0();

  SqlNode getAst1();

  RelNode getPlan0();

  RelNode getPlan1();
}
