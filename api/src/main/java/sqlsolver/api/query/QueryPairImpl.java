package sqlsolver.api.query;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.schema.Schema;

public class QueryPairImpl implements QueryPair {
  /**
   * lineNum indicates the line number of the first sql.
   * The caseId corresponds to lineNum + 1 >> 1.
   * The schema stores the schema of this query pair.
   * The sql0, and sql1 store the SQL string after the rewritten.
   * The originSql0, and originSql1 store the SQL string before the rewritten.
   * The q0, and q1 store the AST of SQL.
   * the p0, and p1 store the plan of SQL.
   */
  private final int lineNum;
  private final Schema schema;
  private final String sql0, sql1;

  private final String originSql0, originSql1;

  private final SqlNode q0, q1;
  private final RelNode p0, p1;

  QueryPairImpl(
          int lineNum,
          Schema schema,
          String sql0, String sql1,
          String originSql0, String originSql1,
          SqlNode q0, SqlNode q1,
          RelNode p0, RelNode p1) {
    this.lineNum = lineNum;
    this.schema = schema;
    this.sql0 = sql0;
    this.sql1 = sql1;
    this.originSql0 = originSql0;
    this.originSql1 = originSql1;
    this.q0 = q0;
    this.q1 = q1;
    this.p0 = p0;
    this.p1 = p1;
  }

  @Override
  public int pairId() {
    return lineNum + 1 >> 1;
  }

  @Override
  public Schema getSchema() {
    return schema;
  }

  @Override
  public String getSql0() {
    return sql0;
  }

  @Override
  public String getSql1() {
    return sql1;
  }

  @Override
  public String getOriginSql0() {
    return originSql0;
  }

  @Override
  public String getOriginSql1() {
    return originSql1;
  }

  @Override
  public SqlNode getAst0() {
    return q0;
  }

  @Override
  public SqlNode getAst1() {
    return q1;
  }

  @Override
  public RelNode getPlan0() {
    return p0;
  }

  @Override
  public RelNode getPlan1() {
    return p1;
  }
}
