package sqlsolver.sql.support.action;

import sqlsolver.sql.ast.TableSourceFields;
import sqlsolver.sql.ast.TableSourceKind;
import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.ast.SqlNode;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;

class NormalizeRightJoin {
  static void normalize(SqlNode node) {
    for (SqlNode target : LocatorSupport.nodeLocator().accept(NormalizeRightJoin::isRightJoin).gather(node))
      flipJoin(target);
  }

  private static boolean isRightJoin(SqlNode node) {
    return node.$(TableSourceFields.Joined_Kind) == JoinKind.RIGHT_JOIN
        && TableSourceKind.SimpleSource.isInstance(node.$(TableSourceFields.Joined_Left));
  }

  private static void flipJoin(SqlNode node) {
    final SqlNode left = (SqlNode) node.remove(TableSourceFields.Joined_Left);
    final SqlNode right = (SqlNode) node.remove(TableSourceFields.Joined_Right);
    node.context().setParentOf(left.nodeId(), NO_SUCH_NODE);
    node.context().setParentOf(right.nodeId(), NO_SUCH_NODE);
    node.$(TableSourceFields.Joined_Left, right);
    node.$(TableSourceFields.Joined_Right, left);
    node.$(TableSourceFields.Joined_Kind, JoinKind.LEFT_JOIN);
  }
}
