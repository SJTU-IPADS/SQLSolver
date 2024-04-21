package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.Arrays;
import java.util.List;

// ROLLUP(x,y) -> GROUPING SETS ((x,y), (x), ())
public class RollupRewriter extends SqlNodePreprocess {

  private boolean needsHandle(SqlSelect select) {
    SqlNodeList group = select.getGroup();
    if (group == null) return false;
    if (group.size() == 1) {
      SqlNode node = group.get(0);
      return node instanceof SqlBasicCall call
              && call.getOperator().equals(SqlStdOperatorTable.ROLLUP);
    }
    return false;
  }

  @Override
  public SqlNode preprocess(SqlNode node) {
    if (node instanceof SqlSelect select) {
      // check if node needs to be handled
      if (needsHandle(select)) {
        // replace ROLLUP
        SqlBasicCall rollup = (SqlBasicCall) select.getGroup().get(0);
        SqlNodeList newGroup = new SqlNodeList(SqlParserPos.ZERO);
        newGroup.add(convertRollup(rollup.getOperandList()));
        select.setGroupBy(newGroup);
      } else {
        // recursion
        select.setFrom(preprocess(select.getFrom()));
      }
    } else if (node instanceof SqlBasicCall call) {
      // recursion
      for (int i = 0; i < call.getOperandList().size(); i++) {
        SqlNode child = call.operand(i);
        SqlNode childNew = preprocess(child);
        if (childNew != child) {
          call.setOperand(i, childNew);
        }
      }
    }
    return node;
  }

  // ROLLUP(x,y) -> GROUPING SETS ((x,y), (x), ())
  private SqlNode convertRollup(List<SqlNode> operands) {
    SqlNodeList gsets = new SqlNodeList(SqlParserPos.ZERO);
    for (int n = operands.size(); n >= 0; n--) {
      SqlNode gset;
      if (n == 0) {
        gset = new SqlNodeList(SqlParserPos.ZERO);
      } else if (n == 1) {
        gset = operands.get(0);
      } else {
        gset = SqlStdOperatorTable.ROW.createCall(SqlParserPos.ZERO, operands.subList(0, n));
      }
      gsets.add(gset);
    }
    return SqlStdOperatorTable.GROUPING_SETS.createCall(gsets);
  }

}
