package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

public class BetweenAndRewriter extends RecursiveRewriter {
  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlBasicCall call
            && call.getOperator().equals(SqlStdOperatorTable.BETWEEN)) {
      SqlNode x = call.operand(0);
      SqlNode l = call.operand(1);
      SqlNode r = call.operand(2);
      // x BETWEEN l AND r -> x >= l AND x <= r
      SqlCall left = SqlStdOperatorTable.GREATER_THAN_OR_EQUAL
              .createCall(SqlParserPos.ZERO, x, l);
      SqlCall right = SqlStdOperatorTable.LESS_THAN_OR_EQUAL
              .createCall(SqlParserPos.ZERO, x, r);
      return SqlStdOperatorTable.AND.createCall(SqlParserPos.ZERO,
              left, right);
    }
    return node;
  }
}
