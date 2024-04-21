package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.Collections;

/**
 * This co-works with SemiAntiJoinHandler.
 * It is the second step of semi/anti-join rewrites.
 * <p/>
 * <code>LEFT JOIN (R) A ON SEMI_JOIN() AND P
 * <br/>-><br/>
 * WHERE EXISTS (SELECT 1 FROM (R) A WHERE P)</code>
 */
public class SemiAntiJoinRewriter extends RecursiveRewriter {
  // not support recursive semi/anti-join

  private static boolean enabled = false;

  public static void setEnabled(boolean enabled) {
    SemiAntiJoinRewriter.enabled = enabled;
  }

  public SemiAntiJoinRewriter() {
    setAllowsMultipleApplications(true);
  }

  // P AND (...) ... -> P
  // (P AND ...) ... -> P
  private SqlNode getLeftmostPredicate(SqlNode predicate) {
    if (predicate instanceof SqlBasicCall call) {
      if (call.operandCount() >= 2) {
        return getLeftmostPredicate(call.operand(0));
      }
    }
    return predicate;
  }

  private boolean isSpecialJoinSign(SqlNode node, String sign) {
    if (node instanceof SqlBasicCall call) {
      if (call.getOperator().toString().equals(sign)
              && call.operandCount() == 0) {
        return true;
      }
    }
    return false;
  }

  private SqlNode removeSpecialJoinSign(SqlNode predicate, String sign) {
    if (predicate instanceof SqlBasicCall call) {
      if (call.operandCount() >= 2) {
        SqlNode left = call.operand(0);
        // SEMI_JOIN() AND ... -> ...
        if (isSpecialJoinSign(left, sign)) {
          assert call.operandCount() == 2;
          return call.operand(1);
        }
        call.setOperand(0, removeSpecialJoinSign(left, sign));
      }
    }
    return predicate;
  }

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlSelect select) {
      SqlNode from = select.getFrom();
      if (from instanceof SqlJoin join) {
        SqlNode predicate = join.getCondition();
        SqlNode sign = getLeftmostPredicate(predicate);
        boolean isSpecialJoin = false, isAntiJoin = false;
        String signStr = "SEMI_JOIN";
        if (isSpecialJoinSign(sign, "SEMI_JOIN")) {
          isSpecialJoin = true;
        } else if (isSpecialJoinSign(sign, "ANTI_JOIN")) {
          isSpecialJoin = true;
          isAntiJoin = true;
          signStr = "ANTI_JOIN";
        }
        if (isSpecialJoin && join.getJoinType().equals(JoinType.LEFT)) {
          // SELECT ... FROM A LEFT JOIN B ON XX_JOIN() AND P
          // ->
          // SELECT ... FROM A WHERE [NOT] EXISTS (SELECT 1 FROM B WHERE P)

          // inner SELECT
          SqlLiteral one = SqlLiteral.createExactNumeric("1", SqlParserPos.ZERO);
          SqlNode purePredicate = removeSpecialJoinSign(predicate, signStr);
          SqlNode innerSelect = new SqlSelect(SqlParserPos.ZERO,
                  new SqlNodeList(SqlParserPos.ZERO),
                  new SqlNodeList(Collections.singletonList(one), SqlParserPos.ZERO),
                  join.getRight(), purePredicate, null, null,
                  new SqlNodeList(SqlParserPos.ZERO),
                  null, null, null, null);
          SqlNode newWhere = SqlStdOperatorTable.EXISTS.createCall(SqlParserPos.ZERO,
                  Collections.singletonList(innerSelect));
          if (isAntiJoin) {
            // negate
            newWhere = SqlStdOperatorTable.NOT.createCall(SqlParserPos.ZERO,
                    Collections.singletonList(newWhere));
          }
          // FROM
          select.setFrom(join.getLeft());
          // append to WHERE if present
          // or directly set WHERE if absent
          SqlNode where = select.getWhere();
          if (where != null)
            newWhere = SqlStdOperatorTable.AND.createCall(SqlParserPos.ZERO,
                    where, newWhere);
          select.setWhere(newWhere);
          return handleNode(select);
        }
      }
    }
    return node;
  }

  @Override
  protected boolean prepare(SqlNode node) {
    return enabled;
  }

}
