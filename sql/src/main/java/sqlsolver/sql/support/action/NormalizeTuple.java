package sqlsolver.sql.support.action;

import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.ast.*;

import static java.util.Collections.singletonList;
import static sqlsolver.common.tree.TreeSupport.nodeEquals;

class NormalizeTuple {
  static void normalize(SqlNode node) {
    for (SqlNode target : LocatorSupport.nodeLocator().accept(NormalizeTuple::isTuple).gather(node))
      normalizeTuple(target);
  }

  private static boolean isTuple(SqlNode node) {
    final SqlNode parent = node.parent();
    if (parent == null) return false;

    final BinaryOpKind op = parent.$(ExprFields.Binary_Op);
    final SqlNode rhs = parent.$(ExprFields.Binary_Right);
    return (op == BinaryOpKind.ARRAY_CONTAINS || op == BinaryOpKind.IN_LIST) && nodeEquals(rhs, node);
  }

  private static void normalizeTuple(SqlNode node) {
    final SqlContext ctx = node.context();
    final SqlNodes elements = SqlNodes.mk(ctx, singletonList(SqlNode.mk(ctx, ExprKind.Param)));
    if (ExprKind.Array.isInstance(node)) node.$(ExprFields.Array_Elements, elements);
    else if (ExprKind.Tuple.isInstance(node)) node.setField(ExprFields.Tuple_Exprs, elements);
    else assert false;
  }
}
