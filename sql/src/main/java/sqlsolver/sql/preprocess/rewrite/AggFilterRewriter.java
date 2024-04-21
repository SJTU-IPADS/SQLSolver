package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import sqlsolver.sql.calcite.CalciteSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * The rewriter class is for handling FILTER of aggregation functions.
 * e.g. agg(expr) FILTER(WHERE pred)
 * ->
 * agg(CASE WHEN pred THEN expr ELSE NULL END)
 */
public class AggFilterRewriter extends RecursiveRewriter {

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlSelect select) {
      SqlNodeList newSelectList = new SqlNodeList(SqlParserPos.ZERO);
      for (SqlNode expr : select.getSelectList()) {
        newSelectList.add(handleFilter(expr));
      }
      select.setSelectList(newSelectList);
    }
    return node;
  }

  private SqlNode handleFilter(SqlNode expr) {
    if (expr instanceof SqlBasicCall call) {
      if (call.getOperator() instanceof SqlFilterOperator
            && call.operand(0) instanceof SqlBasicCall agg
            && CalciteSupport.isAggOperator(agg.getOperator())
            && agg.operandCount() >= 1) {
        if (!agg.getOperator().getName().equalsIgnoreCase("COUNT"))
          assert agg.operandCount() == 1;
        // agg(a0,a1,...,aN-1) FILTER pred ->
        // agg(CASE WHEN pred AND (a1 IS NOT NULL) AND ... AND (aN-1 IS NOT NULL) THEN a0
        //          ELSE NULL END)
        SqlNode cond = call.operand(1);
        final List<SqlNode> args = agg.getOperandList();
        for (SqlNode arg : args.subList(1, args.size())) {
          final List<SqlNode> isNotNullList = new ArrayList<>();
          isNotNullList.add(arg);
          final SqlNode argIsNotNull = new SqlBasicCall(SqlStdOperatorTable.IS_NOT_NULL, isNotNullList, SqlParserPos.ZERO);
          final List<SqlNode> newCondList = new ArrayList<>();
          newCondList.add(cond);
          newCondList.add(argIsNotNull);
          cond = new SqlBasicCall(SqlStdOperatorTable.AND, newCondList, SqlParserPos.ZERO);
        }
        SqlCase caseWhen = caseWhenElseNull(cond, agg.operand(0));
        final List<SqlNode> newArgs = new ArrayList<>();
        newArgs.add(caseWhen);
        return new SqlBasicCall(agg.getOperator(), newArgs, SqlParserPos.ZERO, agg.getFunctionQuantifier());
      } else {
        // recursion
        for (int i = 0; i < call.operandCount(); i++) {
          call.setOperand(i, handleFilter(call.operand(i)));
        }
      }
    }
    return expr;
  }

  // CASE WHEN pred THEN expr ELSE NULL END
  private SqlCase caseWhenElseNull(SqlNode pred, SqlNode expr) {
    SqlNodeList whenList = SqlNodeList.of(pred);
    SqlNodeList thenList = SqlNodeList.of(expr);
    SqlNode elseExpr = SqlLiteral.createNull(SqlParserPos.ZERO);
    return new SqlCase(SqlParserPos.ZERO, null, whenList, thenList, elseExpr);
  }

}
