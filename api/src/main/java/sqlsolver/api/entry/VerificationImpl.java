package sqlsolver.api.entry;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.Planner;
import sqlsolver.api.query.QueryPair;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.preprocess.rewrite.SqlNodePreprocess;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.logic.LogicSupport;
import sqlsolver.superopt.logic.SqlSolver;
import sqlsolver.superopt.logic.VerificationResult;
import sqlsolver.superopt.uexpr.normalizer.QueryUExprICRewriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static sqlsolver.sql.SqlSupport.parsePreprocess;
import static sqlsolver.sql.plan.PlanSupport.isLiteralEq;

final public class VerificationImpl implements Verification {

  /**
   * Transform sql string into Query pairs.
   */
  private static List<QueryPair> readPairs(List<String> sqlList, Schema schema) {
    final CalciteSchema schemaPlus = CalciteSupport.getCalciteSchema(schema);

    SqlNodePreprocess.setSchema(schema);

    final List<QueryPair> pairs = new ArrayList<>(sqlList.size() >> 1);
    for (int i = 0, bound = sqlList.size(); i < bound; i += 2) {
      final Planner planner0 = CalciteSupport.getPlanner(schemaPlus);
      final Planner planner1 = CalciteSupport.getPlanner(schemaPlus);
      String sql0 = sqlList.get(i), sql1 = sqlList.get(i + 1);
      String originSql0 = sql0, originSql1 = sql1;
      sql0 = parsePreprocess(sql0, CalciteSupport.getPlanner(schemaPlus));
      sql1 = parsePreprocess(sql1, CalciteSupport.getPlanner(schemaPlus));
      // String[] pair = parseCoPreprocess(sql0, sql1);
      final SqlNode q0 = CalciteSupport.parseAST(sql0, planner0);
      final SqlNode q1 = CalciteSupport.parseAST(sql1, planner1);

      if (q0 == null) {
        final int ruleId = (i + 1) + 1 >> 1;
        if (LogicSupport.dumpLiaFormulas)
          System.err.printf("Rule id: %d has unsupported query at line %d \n", ruleId, i + 1);
        pairs.add(QueryPair.mk(i + 1, schema, sql0, sql1, originSql0, originSql1));
        continue;
      }
      if (q1 == null) {
        final int ruleId = (i + 1) + 1 >> 1;
        if (LogicSupport.dumpLiaFormulas)
          System.err.printf("Rule id: %d has unsupported query at line %d \n", ruleId, i + 2);
        pairs.add(QueryPair.mk(i + 1, schema, sql0, sql1, originSql0, originSql1));
        continue;
      }

      // construct plan
      final RelNode p0 = CalciteSupport.parseRel(q0, planner0);
      if (p0 == null) {
        final int ruleId = (i + 1) + 1 >> 1;
        if (LogicSupport.dumpLiaFormulas)
          System.err.printf("Rule id: %d has wrong query at line %d \n", ruleId, i + 1);
        pairs.add(QueryPair.mk(i + 1, schema, sql0, sql1, originSql0, originSql1, q0, q1));
        continue;
      }


      final RelNode p1 = CalciteSupport.parseRel(q1, planner1);
      if (p1 == null) {
        final int ruleId = (i + 1) + 1 >> 1;
        if (LogicSupport.dumpLiaFormulas)
          System.err.printf("Rule id: %d has wrong query at line %d \n", ruleId, i + 2);
        pairs.add(QueryPair.mk(i + 1, schema, sql0, sql1, originSql0, originSql1, q0, q1));
        continue;
      }

      pairs.add(QueryPair.mk(i + 1, schema, sql0, sql1, originSql0, originSql1, q0, q1, p0, p1));
    }
    return pairs;
  }

  /**
   * Get verify result of a query pair by using Lia Star method.
   * EQ            imply two sql are equivalent.
   * NEQ           imply two sql are not equivalent.
   * UNKNOWN       imply SqlSolver doesn't know the result.
   */
  private static VerificationResult getVerifyLiaStarResult(QueryPair pair) {
    try {
      return LogicSupport.proveEqByLIAStarConcrete(pair.getPlan0(), pair.getPlan1(), pair.getSchema());
    } catch (Exception | Error e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  /**
   * Get verify result of query pairs.
   * if both sql have semantic errors, they both return empty set -> EQ.
   * if both sql have the same plans -> EQ.
   * otherwise, use the Lia Star method to verify two sql.
   */
  private static List<VerificationResult> getVerifyResult(List<QueryPair> pairs, String schemaString) {
    final List<VerificationResult> results = new ArrayList<>();

    int count = 0;
    for (QueryPair pair : pairs) {
      System.out.println("Verifying pair " + ++count);
      SqlSolver.initialize();
      QueryUExprICRewriter.selectIC(-1);

      // Some special cases
      if (pair.getPlan0() == null || pair.getPlan1() == null) {
        // both two plan tree are literal same -> EQ
        if (isLiteralEq(pair.getSql0(), pair.getSql1(), schemaString)) {
          results.add(VerificationResult.EQ);
          System.out.println(pair.pairId() + " " + VerificationResult.EQ);
          continue;
        }
        if (pair.getSql0().contains("VALUES") || pair.getSql1().contains("VALUES")) {
          final VerificationResult result = getVerifyLiaStarResult(pair);
          results.add(result);
          System.out.println(pair.pairId() + " " + result);
        } else {
          // cannot process other cases currently
          System.out.println(pair.pairId() + " " + VerificationResult.UNKNOWN);
          results.add(VerificationResult.UNKNOWN);
        }
        continue;
      }

      // both two plan tree are literal same -> EQ
      if (isLiteralEq(pair.getSql0(), pair.getSql1(), schemaString)) {
        results.add(VerificationResult.EQ);
        System.out.println(pair.pairId() + " " + VerificationResult.EQ);
        continue;
      }
      // verify the sql pair
      VerificationResult result = getVerifyLiaStarResult(pair);
      System.out.println(pair.pairId() + " " + result);
      results.add(result);
    }

    return results;
  }

  /**
   * Verify two sql equivalence.
   */
  static VerificationResult verify(String sql0, String sql1, String schemaString) {
    try {
      final Schema schema = CalciteSupport.getSchema(schemaString);
      final List<QueryPair> pairs = readPairs(Arrays.asList(sql0, sql1), schema);
      return getVerifyResult(pairs, schemaString).get(0);
    } catch (Exception | Error e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  /**
   * Verify two sqlList equivalence.
   * The two sql to verify are in the same index of both lists.
   * The two sqlList should have same size.
   */
  static List<VerificationResult> verify(List<String> sqlList0, List<String> sqlList1, String schemaString) {
    final Schema schema = CalciteSupport.getSchema(schemaString);

    final List<String> mergedSqlList = new ArrayList<>();
    for (int i = 0; i < sqlList0.size() << 1; i++) {
      if (i % 2 == 0) {
        mergedSqlList.add(sqlList0.get(i >> 1));
      } else {
        mergedSqlList.add(sqlList1.get(i >> 1));
      }
    }

    final List<QueryPair> pairs = readPairs(mergedSqlList, schema);
    return getVerifyResult(pairs, schemaString);
  }
}
