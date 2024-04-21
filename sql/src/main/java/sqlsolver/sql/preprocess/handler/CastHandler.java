package sqlsolver.sql.preprocess.handler;

import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.plan.PlanSupport;
import sqlsolver.sql.preprocess.rewrite.SqlNodePreprocess;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.support.action.NormalizationSupport;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.util.CastRemover;

import static sqlsolver.common.datasource.DbSupport.MySQL;

// It is a stand-alone handler apart from SqlNodePreprocess.
// It is the pre-"parsePreprocess" version of CASTSupport.
public class CastHandler {

  private static Schema schema;

  public static String handle(String sql) {
    schema = SqlNodePreprocess.getSchema();
    sql = ConvertCastIntegerToSigned(sql);
    sql = CastRemover.removeUselessCastNull(sql);
    sql = removeUselessCastSigned(sql);
    if (!needsHandle(sql)) return sql;
    try {
      PlanContext planContext = sqlToPlanContext(sql);
      String strOld = planContextToSql(planContext);
      CastRemover.removeUselessCast(planContext);
      String strNew = planContextToSql(planContext);
      if (strNew.equals(strOld)) {
        // unchanged
        return sql;
      }
      return strNew.replace("\n", " ").replace("`", "");
    } catch (Exception e) {
      //e.printStackTrace();
      return sql;
    }
  }

  // only handle when there is something like CAST(agg)
  private static boolean needsHandle(String sql) {
    sql = sql.toLowerCase();
    int index = sql.indexOf("cast");
    while (index >= 0) {
      index += 4;
      sql = sql.substring(index).strip();
      if (sql.contains("(")) {
        // fetch the whole CAST expression
        int leftParen = 1, cursor = sql.indexOf('(') + 1;
        int startIndex = cursor;
        char ch;
        do {
          ch = sql.charAt(cursor);
          if (ch == '(') leftParen++;
          else if (ch == ')') leftParen--;
        } while (leftParen > 0 & ++cursor < sql.length());
        assert leftParen == 0;
        String castBody = sql.substring(startIndex, cursor - 1);
        // SUM|MAX|MIN|COUNT|AVG
        if (castBody.contains("sum")
                || castBody.contains("max")
                || castBody.contains("min")
                || castBody.contains("count")
                || castBody.contains("avg")
                || castBody.contains("is null")) {
          return true;
        }
      }
      index = sql.indexOf("cast");
    }
    return false;
  }

  private static PlanContext sqlToPlanContext(String sql) {
    final SqlNode q0 = SqlSupport.parseSql(MySQL, sql);
    q0.context().setSchema(schema);
    NormalizationSupport.normalizeAst(q0);
    return PlanSupport.assemblePlan(q0, schema);
  }

  private static String planContextToSql(PlanContext planContext) {
    SqlNode sqlNode = PlanSupport.translateAsAst(planContext, planContext.root(), true);
    return sqlNode.toString();
  }

  private static String ConvertCastIntegerToSigned(String sql) {
    sql = sql.replaceAll("CAST\\((.*?) AS INTEGER\\)", "CAST\\($1 AS SIGNED\\)");
    return sql;
  }

  private static String removeUselessCastSigned(String sql) {
    sql = sql.replaceAll("CAST\\((\\d+) AS SIGNED\\)", "$1");
    return sql;
  }

}
