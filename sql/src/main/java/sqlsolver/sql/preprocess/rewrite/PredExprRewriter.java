package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlInOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

/**
 * <code>
 * SELECT t.x, p(t) FROM t
 * <br/>-><br/>
 * SELECT t.x, TRUE FROM t WHERE p(t)<br/>
 * UNION ALL<br/>
 * SELECT t.x, FALSE FROM t WHERE NOT p(t)<br/>
 * UNION ALL<br/>
 * SELECT t.x, NULL FROM t WHERE p(t) IS NULL
 * </code>
 */
public class PredExprRewriter extends SqlNodePreprocess {

  private static final int TYPE_OTHER = 0;
  private static final int TYPE_COLUMN = 1;
  private static final int TYPE_IN_EXPR = 2;
  private static final int TYPE_EXISTS_EXPR = 3;
  private static final SqlLiteral LITERAL_TRUE = SqlLiteral.createBoolean(true, SqlParserPos.ZERO);
  private static final SqlLiteral LITERAL_FALSE = SqlLiteral.createBoolean(false, SqlParserPos.ZERO);
  private static final SqlLiteral LITERAL_NULL = SqlLiteral.createNull(SqlParserPos.ZERO);
  private int type;

  @Override
  public SqlNode preprocess(SqlNode node) {
    type = TYPE_OTHER;
    if (node instanceof SqlSelect select) {
      // only handle specific kinds of SELECT
      // 1. no WHERE
      if (select.getWhere() != null) return node;
      // 2. no DISTINCT, GROUP BY
      if (select.isDistinct() || select.getGroup() != null) return node;
      // 3. output several column references and exactly one predExpr
      SqlBasicCall predItem = null;
      for (SqlNode item : select.getSelectList()) {
        type = getItemType(item);
        if (type == TYPE_OTHER) return node;
        if (type == TYPE_IN_EXPR || type == TYPE_EXISTS_EXPR) {
          if (predItem != null) return node;
          // IN/EXISTS or AS
          predItem = (SqlBasicCall) item;
        }
      }
      if (predItem == null) return node;
      // handle
      return handle(select, predItem);
    }
    return node;
  }

  private int getItemType(SqlNode item) {
    if (item instanceof SqlBasicCall call
            && call.getOperator() instanceof SqlAsOperator) {
      item = call.operand(0);
    }
    if (item instanceof SqlIdentifier) return TYPE_COLUMN;
    if (item instanceof SqlBasicCall call)
      if (call.getOperator() instanceof SqlInOperator)
        return TYPE_IN_EXPR;
      else if (call.getOperator().getKind() == SqlKind.EXISTS)
        return TYPE_EXISTS_EXPR;
    return TYPE_OTHER;
  }

  private SqlNode unionAll(SqlNode n1, SqlNode n2) {
    return SqlStdOperatorTable.UNION_ALL.createCall(SqlParserPos.ZERO, n1, n2);
  }

  // SELECT ..., pred [AS a] ,...
  private SqlNode handle(SqlSelect select, SqlBasicCall predItem) {
    SqlNode node = unionAll(
            handlePredExpr(select, predItem, LITERAL_TRUE),
            handlePredExpr(select, predItem, LITERAL_FALSE));
    // EXISTS(...) must not be NULL
    if (type == TYPE_IN_EXPR)
      node = unionAll(node,
              handlePredExpr(select, predItem, LITERAL_NULL));
    return node;
  }

  private SqlNode filter(SqlNode inner, SqlLiteral literal) {
    if (literal == LITERAL_TRUE)
      return inner;
    if (literal == LITERAL_FALSE)
      return SqlStdOperatorTable.NOT.createCall(SqlParserPos.ZERO, inner);
    if (literal == LITERAL_NULL)
      return SqlStdOperatorTable.IS_NULL.createCall(SqlParserPos.ZERO, inner);
    assert false;
    return null;
  }

  // SELECT ... <predExpr> [AS a] ... FROM ...
  // ->
  // SELECT ... <literal> [AS a] ... FROM ... WHERE <filter(predExpr)>
  private SqlSelect handlePredExpr(SqlSelect select, SqlBasicCall predItem, SqlLiteral literal) {
    SqlSelect newSelect = (SqlSelect) select.clone(SqlParserPos.ZERO);
    SqlNodeList list = newSelect.getSelectList().clone(SqlParserPos.ZERO);
    SqlBasicCall predExpr = null;
    for (int i = 0; i < list.size(); i++) {
      // match <predItem>
      SqlNode item = list.get(i);
      if (item == predItem) {
        if (predItem.getOperator() instanceof SqlAsOperator as) {
          // (... IN ...) AS ...
          predExpr = predItem.operand(0);
          list.set(i, as.createCall(SqlParserPos.ZERO, literal, predItem.operand(1)));
        } else {
          // ... IN ...
          predExpr = predItem;
          list.set(i, literal);
        }
        break;
      }
    }
    newSelect.setSelectList(list);
    newSelect.setWhere(filter(predExpr, literal));
    return newSelect;
  }

}
