package sqlsolver.sql.support.locator;

import sqlsolver.sql.ast.SqlNode;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;

public interface SqlFinder {
  int find(SqlNode root);

  default SqlNode findNode(SqlNode root) {
    final int found = find(root);
    return found == NO_SUCH_NODE ? null : SqlNode.mk(root.context(), found);
  }
}
