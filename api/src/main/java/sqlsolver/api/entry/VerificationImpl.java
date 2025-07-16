package sqlsolver.api.entry;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.RelNode;
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
import sqlsolver.superopt.util.Timeout;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static sqlsolver.sql.SqlSupport.parsePreprocess;
import static sqlsolver.sql.plan.PlanSupport.isLiteralEq;

final public class VerificationImpl implements Verification {

  /**
   * Transform sql string into Query pairs.
   */
  private static List<QueryPair> readPairs(List<String> sqlList, Schema schema) {
    final CalciteSchema schemaPlus = CalciteSupport.getCalciteSchema(schema);

    SqlNodePreprocess.setSchema(schema);
    CalciteSupport.USER_DEFINED_FUNCTIONS.clear();
    CalciteSupport.addUserDefinedFunctions(sqlList);

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
    } catch (Throwable e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return Timeout.isTimeout(e) ? VerificationResult.TIMEOUT : VerificationResult.UNKNOWN;
    }
  }

  /**
   * Get verify result of query pairs.
   * if both sql have semantic errors, they both return empty set -> EQ.
   * if both sql have the same plans -> EQ.
   * otherwise, use the Lia Star method to verify two sql.
   * Each pair has a time budget {@code timeout} if {@code timeout} is positive;
   * pairs exceeding this budget return UNKNOWN.
   */
  private static List<VerificationResult> getVerifyResult(List<QueryPair> pairs, String schemaString, long timeout) {
    final List<VerificationResult> results = new ArrayList<>();
    final List<Long> times = new ArrayList<>();

    int count = 0;
    final Timer timer = new Timer(true);
    for (QueryPair pair : pairs) {
      System.out.println("Verifying pair " + ++count);
      if (timeout > 0) {
        final AtomicReference<VerificationResult> atomicResult = new AtomicReference<>();
        final Thread worker = new Thread(() -> {
          final VerificationResult result = getVerifyResult(pair, schemaString);
          atomicResult.set(result);
        });
        final long timeStart = System.currentTimeMillis();
        worker.start();
        final AtomicLong timeInterrupt = new AtomicLong();
        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            worker.interrupt();
            timeInterrupt.set(System.currentTimeMillis());
          }
        }, timeout * 1000);
        try {
          worker.join();
          final long timeEnd = System.currentTimeMillis();
          final long timeVerify = timeEnd - timeStart;
          times.add(timeVerify);
          System.out.println("Verification time: " + timeVerify + " ms");
          final VerificationResult result = atomicResult.get();
          results.add(result);
          //if (result == VerificationResult.TIMEOUT)
          //  System.out.println("Timeout delay: " + (timeEnd - timeInterrupt.get()) + " ms");
        } catch (InterruptedException e) {
          // should not be interrupted
          System.out.println("Verification is interrupted");
          results.add(VerificationResult.UNKNOWN);
        }
        System.out.println(pair.pairId() + " " + results.get(count - 1));
      } else {
        final long timeStart = System.currentTimeMillis();
        final VerificationResult result = getVerifyResult(pair, schemaString);
        final long timeEnd = System.currentTimeMillis();
        final long timeVerify = timeEnd - timeStart;
        times.add(timeVerify);
        System.out.println("Verification time: " + timeVerify + " ms");
        results.add(result);
        System.out.println(pair.pairId() + " " + result);
      }
    }

    /*System.out.println("===== Time stats (ms) =====");
    for (long time : times) {
      System.out.println(time);
    }*/

    return results;
  }

  /**
   * Get verify result of a query pair.
   * if both sql have semantic errors, they both return empty set -> EQ.
   * if both sql have the same plans -> EQ.
   * otherwise, use the Lia Star method to verify two sql.
   */
  private static VerificationResult getVerifyResult(QueryPair pair, String schemaString) {
    SqlSolver.initialize();
    QueryUExprICRewriter.selectIC(-1);

    // Some special cases
    if (pair.getPlan0() == null || pair.getPlan1() == null) {
      // both two plan tree are literal same -> EQ
      if (isLiteralEq(pair.getSql0(), pair.getSql1(), schemaString)) {
        return VerificationResult.EQ;
      }
      if (pair.getSql0().contains("VALUES") || pair.getSql1().contains("VALUES")) {
        return getVerifyLiaStarResult(pair);
      } else {
        // cannot process other cases currently
        return VerificationResult.UNKNOWN;
      }
    }

    // both two plan tree are literal same -> EQ
    if (isLiteralEq(pair.getSql0(), pair.getSql1(), schemaString)) {
      return VerificationResult.EQ;
    }
    // verify the sql pair
    return getVerifyLiaStarResult(pair);
  }

  /**
   * Verify two sql equivalence.
   */
  static VerificationResult verify(String sql0, String sql1, String schemaString) {
    try {
      final Schema schema = CalciteSupport.getSchema(schemaString);
      final List<QueryPair> pairs = readPairs(Arrays.asList(sql0, sql1), schema);
      return getVerifyResult(pairs, schemaString, -1).get(0);
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
  static List<VerificationResult> verify(List<String> sqlList0, List<String> sqlList1, String schemaString, long timeout) {
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
    return getVerifyResult(pairs, schemaString, timeout);
  }

  /**
   * Verify two sql plan equivalence.
   */
  static VerificationResult verify(RelNode plan0, RelNode plan1, String schemaString) {
    try {
      final Schema schema = CalciteSupport.getSchema(schemaString);
      SqlSolver.initialize();
      QueryUExprICRewriter.selectIC(-1);
      return LogicSupport.proveEqByLIAStarConcrete(plan0, plan1, schema);
    } catch (Exception | Error e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  /**
   * Verify two sql plan equivalence.
   */
  static VerificationResult verify(RelNode plan0, RelNode plan1, Schema schema) {
    try {
      SqlSolver.initialize();
      QueryUExprICRewriter.selectIC(-1);
      return LogicSupport.proveEqByLIAStarConcrete(plan0, plan1, schema);
    } catch (Exception | Error e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }
}
