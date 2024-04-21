package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

// COUNT(c1, c2, c3, ...)
// ->
// COUNT(CASE WHEN c2 IS NOT NULL AND c3 IS NOT NULL ... THEN c1 ELSE NULL END)
public class CountArgsRewriter extends SqlNodePreprocess {

  private SqlNode getCountArgs(SqlNode node) {
    if (node instanceof SqlBasicCall call) {
      if (call.getOperator().equals(SqlStdOperatorTable.COUNT)
              && call.getFunctionQuantifier() == null
              && call.operandCount() > 1) {
        return node;
      } else {
        for (SqlNode arg : call.getOperandList()) {
          SqlNode count = getCountArgs(arg);
          if (count != null) return count;
        }
      }
    }
    return null;
  }

  private boolean needsHandle(SqlNodeList selectList) {
    for (SqlNode item : selectList) {
      if (getCountArgs(item) != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public SqlNode preprocess(SqlNode node) {
    if (node instanceof SqlSelect select) {
      SqlNodeList list = select.getSelectList();
      if (needsHandle(list)) {
        for (int i = 0; i < list.size(); i++) {
          list.set(i, replaceCountArgs(list.get(i)));
        }
      }
      // recursion
      select.setFrom(preprocess(select.getFrom()));
    } else if (node instanceof SqlBasicCall call) {
      for (int i = 0; i < call.operandCount(); i++) {
        call.setOperand(i, preprocess(call.operand(i)));
      }
    }
    return node;
  }

  private SqlNode replaceCountArgs(SqlNode node) {
    if (node instanceof SqlBasicCall call) {
      if (call.getOperator().equals(SqlStdOperatorTable.COUNT)
              && call.getFunctionQuantifier() == null
              && call.operandCount() > 1) {
        return convertCountArgs(call);
      } else {
        for (int i = 0; i < call.operandCount(); i++) {
          call.setOperand(i, replaceCountArgs(call.operand(i)));
        }
      }
    }
    return node;
  }

  // preserve the first arg
  // move other args into CASE WHEN
  private SqlNode convertCountArgs(SqlBasicCall count) {
    // COUNT(CASE WHEN ... THEN arg1 ELSE NULL END)
    SqlNode when = SqlStdOperatorTable.IS_NOT_NULL.createCall(SqlParserPos.ZERO, (SqlNode) count.operand(1));
    SqlNode then = count.operand(0);
    SqlNode els = SqlLiteral.createNull(SqlParserPos.ZERO);
    for (int i = 2; i < count.operandCount(); i++) {
      SqlNode notnull = SqlStdOperatorTable.IS_NOT_NULL.createCall(SqlParserPos.ZERO, (SqlNode) count.operand(i));
      when = SqlStdOperatorTable.AND.createCall(SqlParserPos.ZERO, when, notnull);
    }
    SqlNodeList whens = new SqlNodeList(SqlParserPos.ZERO);
    whens.add(when);
    SqlNodeList thens = new SqlNodeList(SqlParserPos.ZERO);
    thens.add(then);
    SqlNode newArg = new SqlCase(SqlParserPos.ZERO, null, whens, thens, els);
    return SqlStdOperatorTable.COUNT.createCall(SqlParserPos.ZERO, newArg);
  }

}
