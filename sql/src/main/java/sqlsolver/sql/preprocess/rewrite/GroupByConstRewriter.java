package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The rewriter class is for deleting consts in select's groupBy expr.
 */
public class GroupByConstRewriter extends RecursiveRewriter {

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlSelect select && select.getGroup() != null) {
      final List<SqlNode> groupList = select.getGroup().getList();
      final List<SqlNode> newGroupList = new ArrayList<>();
      for (final SqlNode groupNode : groupList) {
        if (groupNode instanceof SqlCharStringLiteral) continue;
        newGroupList.add(groupNode);
      }
      if (newGroupList.isEmpty()) select.setGroupBy(SqlNodeList.EMPTY);
      select.setGroupBy(newGroupList.isEmpty() ? SqlNodeList.EMPTY : new SqlNodeList(newGroupList, SqlParserPos.ZERO));
    }
    return node;
  }
}
