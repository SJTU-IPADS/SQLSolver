package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The rewriter class is for deleting consts in sort node.
 */
public class OrderByConstRewriter extends RecursiveRewriter {

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlOrderBy orderBy) {
      final List<SqlNode> orderList = orderBy.orderList.getList();
      final List<SqlNode> newOrderList = new ArrayList<>();
      for (final SqlNode orderNode : orderList) {
        if (orderNode instanceof SqlCharStringLiteral) continue;
        newOrderList.add(orderNode);
      }
      if (newOrderList.isEmpty() && orderBy.offset == null && orderBy.fetch == null) return orderBy.getOperandList().get(0);
      return new SqlOrderBy(SqlParserPos.ZERO,
              orderBy.getOperandList().get(0),
              newOrderList.isEmpty() ? SqlNodeList.EMPTY : new SqlNodeList(newOrderList, SqlParserPos.ZERO),
              orderBy.offset,
              orderBy.fetch);
    }
    return node;
  }
}
