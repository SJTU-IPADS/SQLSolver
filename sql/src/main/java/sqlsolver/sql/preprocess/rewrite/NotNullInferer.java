package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.List;

// WHERE ... AND A NOT LIKE B AND ...
// ->
// WHERE ... AND A NOT LIKE B AND A IS NOT NULL AND ...
public class NotNullInferer extends SqlNodePreprocess {

  private SqlNode handleWhere(SqlNode where) {
    if (where instanceof SqlBasicCall call) {
      switch (call.getKind()) {
        case AND -> {
          call.setOperand(0, handleWhere(call.operand(0)));
          call.setOperand(1, handleWhere(call.operand(1)));
        }
        case NOT -> {
          SqlNode sub = call.operand(0);
          if (sub instanceof SqlBasicCall subCall
                  && subCall.getKind() == SqlKind.LIKE) {
            // A NOT LIKE B
            // append (A IS NOT NULL)
            SqlNodeList a = new SqlNodeList(SqlParserPos.ZERO);
            a.add(subCall.operand(0));
            SqlNode notNull = SqlStdOperatorTable.IS_NOT_NULL.createCall(a);
            return SqlStdOperatorTable.AND.createCall(SqlParserPos.ZERO,
                    call, notNull);
          }
        }
        case LIKE -> {
          if (call.getOperator().equals(SqlStdOperatorTable.NOT_LIKE)) {
            // A NOT LIKE B
            // append (A IS NOT NULL)
            SqlNodeList a = new SqlNodeList(SqlParserPos.ZERO);
            a.add(call.operand(0));
            SqlNode notNull = SqlStdOperatorTable.IS_NOT_NULL.createCall(a);
            return SqlStdOperatorTable.AND.createCall(SqlParserPos.ZERO,
                    call, notNull);
          }
        }
      }
    }
    return where;
  }

  @Override
  public SqlNode preprocess(SqlNode node) {
    if (node instanceof SqlSelect select) {
      // handle SELECT list
      SqlNodeList items = select.getSelectList();
      for (int i = 0; i < items.size(); i++) {
        SqlNode item = items.get(i);
        SqlNode newItem = preprocess(item);
        if (newItem != item) {
          items.set(i, newItem);
        }
      }
      // handle FROM
      select.setFrom(preprocess(select.getFrom()));
      // handle WHERE
      select.setWhere(handleWhere(select.getWhere()));
    } else if (node instanceof SqlOrderBy orderBy) {
      // SqlOrderBy do not support setOperand
      // rebuild SqlOrderBy
      return new SqlOrderBy(SqlParserPos.ZERO, preprocess(orderBy.query),
              orderBy.orderList, orderBy.offset, orderBy.fetch);
    } else if (node instanceof SqlBasicCall call) {
      // normal recursion
      List<SqlNode> args = call.getOperandList();
      for (int i = 0; i < args.size(); i++) {
        call.setOperand(i, preprocess(args.get(i)));
      }
    } else if (node instanceof SqlJoin join) {
      // SqlJoin
      return new SqlJoin(SqlParserPos.ZERO, preprocess(join.getLeft()),
              join.isNaturalNode(), join.getJoinTypeNode(),
              preprocess(join.getRight()), join.getConditionTypeNode(),
              preprocess(join.getCondition()));
    }
    return node;
  }

}
