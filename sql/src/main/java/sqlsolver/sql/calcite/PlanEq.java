package sqlsolver.sql.calcite;
import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanSupport;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;

public class PlanEq {

  private PlanContext sqlPlan0, sqlPlan1;
  public PlanEq(String sql0, String sql1, String schema) {
    SqlSupport.muteParsingError();
    final Schema wetuneSchema = SchemaSupport.parseSchema(DbSupport.MySQL, schema);
    final SqlNode sqlAst0 = SqlSupport.parseSql(DbSupport.MySQL, sql0);
    if (sqlAst0 != null) {
      sqlPlan0 = PlanSupport.assemblePlan(sqlAst0, wetuneSchema);
    } else {
      sqlPlan0 = null;
    }
    final SqlNode sqlAst1 = SqlSupport.parseSql(DbSupport.MySQL, sql1);
    if (sqlAst1 != null) {
      sqlPlan1 = PlanSupport.assemblePlan(sqlAst1, wetuneSchema);
    } else {
      sqlPlan1 = null;
    }
    SqlSupport.unMuteParsingError();
  }

  public boolean isEqTree() {
    if (sqlPlan0 == null || sqlPlan1 == null) return false;
    return PlanSupport.isLiteralEq(sqlPlan0, sqlPlan1);
  }
}
