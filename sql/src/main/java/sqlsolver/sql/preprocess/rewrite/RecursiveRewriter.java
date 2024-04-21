package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.List;

/**
 * <p>
 * A rewriter that explores and replaces sub-nodes
 * (e.g. sub-queries, sub-expressions)
 * of a query node recursively.
 * </p>
 * <p>
 * The default recursion strategy is to stop immediately
 * and not to further explore sub-nodes
 * once a rewrite has been done.
 * </p>
 */
public abstract class RecursiveRewriter extends SqlNodePreprocess {

  private boolean allowsMultipleApplications = false;

  /**
   * Set whether the rewriter further explore sub-nodes recursively
   * once a rewrite has been done.
   * It is <code>false</code> by default.
   */
  public void setAllowsMultipleApplications(boolean b) {
    allowsMultipleApplications = b;
  }

  /**
   * Handle the sub-node in the current level.
   * It returns a new sub-node for substitution only when necessary;
   * otherwise it can return directly the same sub-node.
   * Recursion is handled by this class.
   * @param node the sub-node to be handled
   * @return the substituted node, which may be the same as input
   */
  public abstract SqlNode handleNode(SqlNode node);

  /**
   * Prepare before preprocessing and decide whether to preprocess.
   * The default implementation always returns <code>true</code>.
   * @param node the SqlNode to be preprocessed
   * @return whether preprocessing is performed then
   */
  protected boolean prepare(SqlNode node) {
    return true;
  }

  @Override
  public final SqlNode preprocess(SqlNode node) {
    if (node != null && prepare(node)) return preprocess0(node);
    return node;
  }

  public SqlNode preprocess0(SqlNode node) {
    if (node == null) return null;
    // current-level replacement
    SqlNode newExpr = handleNode(node);
    if (newExpr != node && !allowsMultipleApplications) {
      // the desired rewrite has been performed
      return newExpr;
    }
    // recursion
    if (newExpr instanceof SqlSelect select) {
      // handle SELECT list
      SqlNodeList items = select.getSelectList();
      for (int i = 0; i < items.size(); i++) {
        SqlNode item = items.get(i);
        SqlNode newItem = preprocess0(item);
        if (newItem != item) {
          items.set(i, newItem);
        }
      }
      // handle FROM
      select.setFrom(preprocess0(select.getFrom()));
      // handle WHERE
      select.setWhere(preprocess0(select.getWhere()));
      // handle groupBy
      SqlNodeList groups = select.getGroup();
      if (groups != null) {
        for (int i = 0; i < groups.size(); i++) {
          SqlNode group = groups.get(i);
          SqlNode newGroup = preprocess0(group);
          if (newGroup != group) {
            groups.set(i, newGroup);
          }
        }
      }
      // handle have
      select.setHaving(preprocess0(select.getHaving()));
    } else if (newExpr instanceof SqlOrderBy orderBy) {
      final SqlNodeList orderList = orderBy.orderList;
      for (int i = 0; i < orderList.size(); i++) {
        SqlNode item = orderList.get(i);
        SqlNode newItem = preprocess0(item);
        if (newItem != item) {
          orderList.set(i, newItem);
        }
      }

      SqlNode offset = orderBy.offset;
      if (offset != null) {
        offset = preprocess(offset);
      }

      SqlNode fetch = orderBy.fetch;
      if (fetch != null) {
        fetch = preprocess(fetch);
      }

      // rebuild SqlOrderBy
      return new SqlOrderBy(SqlParserPos.ZERO, preprocess0(orderBy.query), orderList, offset, fetch);
    } else if (newExpr instanceof SqlBasicCall call) {
      // normal recursion
      List<SqlNode> args = call.getOperandList();
      for (int i = 0; i < args.size(); i++) {
        call.setOperand(i, preprocess0(args.get(i)));
      }
    } else if (newExpr instanceof SqlCase cas) {
      SqlNodeList whens = cas.getWhenOperands();
      for (int i = 0; i < whens.size(); i++) {
        whens.set(i, preprocess0(whens.get(i)));
      }
      SqlNodeList thens = cas.getThenOperands();
      for (int i = 0; i < thens.size(); i++) {
        thens.set(i, preprocess0(thens.get(i)));
      }
      cas.setOperand(3, cas.getElseOperand());
    } else if (newExpr instanceof SqlJoin join) {
      // SqlJoin
      return new SqlJoin(SqlParserPos.ZERO, preprocess0(join.getLeft()),
              join.isNaturalNode(), join.getJoinTypeNode(),
              preprocess0(join.getRight()), join.getConditionTypeNode(),
              preprocess0(join.getCondition()));
    }
    return newExpr;
  }

}
