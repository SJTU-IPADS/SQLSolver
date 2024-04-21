package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Rearrange GROUP BY / COUNT DISTINCT columns in a consistent order.
 */
public class ColumnReorderRewriter extends RecursiveRewriter {
  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlBasicCall call && isCountDistinctMultipleColumn(call)) {
      final List<SqlNode> ops = new ArrayList<>(call.getOperandList());
      ops.sort(this::compareSqlNode);
      return new SqlBasicCall(call.getOperator(), ops, SqlParserPos.ZERO, call.getFunctionQuantifier());
    } else if (node instanceof SqlSelect select) {
      final SqlNodeList group = select.getGroup();
      if (group == null || group.size() <= 1) return node;
      group.sort(this::compareSqlNode);
    }
    return node;
  }

  @Override
  protected boolean prepare(SqlNode node) {
    setAllowsMultipleApplications(true);
    return true;
  }

  private boolean isCountDistinctMultipleColumn(SqlBasicCall call) {
    return call.getOperator().toString().equalsIgnoreCase("count")
            && call.getFunctionQuantifier() != null
            && SqlSelectKeyword.DISTINCT.equals(call.getFunctionQuantifier().getValue())
            && call.operandCount() > 1;
  }

  private int compareSqlNode(SqlNode o1, SqlNode o2) {
    return o1.toString().compareToIgnoreCase(o2.toString());
  }
}
