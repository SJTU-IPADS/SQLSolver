package sqlsolver.sql.copreprocess;

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
public abstract class RecursiveCoRewriter extends SqlNodeCoPreprocess {

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
   * Prepare before co-preprocessing and decide whether to co-preprocess.
   * The default implementation always returns <code>true</code>.
   * @param nodes the SqlNode pair to be co-preprocessed
   * @return whether co-preprocessing is performed then
   */
  protected boolean prepare(SqlNode[] nodes) {
    return true;
  }

  @Override
  public final SqlNode[] coPreprocess(SqlNode[] nodes) {
    if (prepare(nodes)) {
      return new SqlNode[]{preprocess0(nodes[0]), preprocess0(nodes[1])};
    }
    return nodes;
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
    } else if (newExpr instanceof SqlOrderBy orderBy) {
      // SqlOrderBy do not support setOperand
      // rebuild SqlOrderBy
      return new SqlOrderBy(SqlParserPos.ZERO, preprocess0(orderBy.query),
              orderBy.orderList, orderBy.offset, orderBy.fetch);
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
