package sqlsolver.api.entry;

import sqlsolver.superopt.logic.VerificationResult;
import org.apache.calcite.rel.RelNode;
import sqlsolver.sql.schema.Schema;
import java.util.List;

/**
 * The entry of the SqlSolver.
 * Verify sql are equivalent or not.
 */
public interface Verification {

  /**
   * Verify two sql equivalence.
   *
   * @param sql0 given the first sql.
   * @param sql1 given the second sql.
   * @param schema  given a schema, it is the schema of all sql in the sqlList.
   * @return a list of string indicates that whether two sql are equivalent.
   */
  static VerificationResult verify(String sql0, String sql1, String schema) {
    return VerificationImpl.verify(sql0, sql1, schema);
  }

  /**
   * Verify pairwise sql equivalence in the sqlList.
   * The two sql to verify are in the same index of both lists.
   * The two sqlList (sqlList0 and sqlList1) should have same size.
   *
   * @param sqlList0 given the first list of sql.
   * @param sqlList1 given the second list of sql.
   * @param schema  given a schema, it is the schema of all sql in the sqlList.
   * @return a list of string indicates that whether two sql are equivalent.
   */
  static List<VerificationResult> verify(List<String> sqlList0, List<String> sqlList1, String schema) {
    return VerificationImpl.verify(sqlList0, sqlList1, schema, -1);
  }

  /**
   * Verify pairwise sql equivalence in the sqlList.
   * It resembles {@link Verification#verify(List, List, String)} except that it sets an intended upper bound
   * for each run of verifying a pair.
   *
   * @param timeout when verification of a pair takes this amount of time, the pair is skipped;
   *                timeout should be in seconds;
   *                negative timeout indicates no time limit
   * @return a list of string indicates that whether two sql are equivalent.
   *
   * @see Verification#verify(List, List, String)
   */
  static List<VerificationResult> verify(List<String> sqlList0, List<String> sqlList1, String schema, long timeout) {
    return VerificationImpl.verify(sqlList0, sqlList1, schema, timeout);
  }

  /**
   * Verify two sql equivalence.
   *
   * @param plan0 given the plan of the first sql.
   * @param plan1 given the plan of the second sql.
   * @param schema  given a schema string, it is the schema of all sql in the sqlList.
   * @return a list of string indicates that whether two sql are equivalent.
   */
  static VerificationResult verify(RelNode plan0, RelNode plan1, String schema) {
    return VerificationImpl.verify(plan0, plan1, schema);
  }

  /**
   * Verify two sql equivalence.
   *
   * @param plan0 given the plan of the first sql.
   * @param plan1 given the plan of the second sql.
   * @param schema  given a schema, it is the schema of all sql in the sqlList.
   * @return a list of string indicates that whether two sql are equivalent.
   */
  static VerificationResult verify(RelNode plan0, RelNode plan1, Schema schema) {
    return VerificationImpl.verify(plan0, plan1, schema);
  }
}
