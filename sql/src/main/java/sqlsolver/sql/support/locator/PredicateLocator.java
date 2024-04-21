package sqlsolver.sql.support.locator;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.*;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;

class PredicateLocator implements SqlVisitor, SqlGatherer, SqlFinder {
  private final TIntList nodes;
  private final boolean scoped;
  private final boolean bottomUp;
  private final boolean primitive;
  private final boolean conjunctionOnly;
  private final boolean breakdownExpr;
  private int exemptQueryNode;

  protected PredicateLocator(
      boolean scoped,
      boolean bottomUp,
      boolean primitive,
      boolean conjunctionOnly,
      boolean breakdownExpr,
      int expectedNumNodes) {
    this.nodes = expectedNumNodes >= 0 ? new TIntArrayList(expectedNumNodes) : new TIntArrayList();
    this.bottomUp = bottomUp;
    this.scoped = scoped;
    this.primitive = primitive;
    this.breakdownExpr = breakdownExpr;
    this.conjunctionOnly = conjunctionOnly;
  }

  @Override
  public int find(SqlNode root) {
    if (breakdownExpr && SqlKind.Expr.isInstance(root)) {
      traversePredicate(root);
    } else {
      exemptQueryNode = SqlKind.Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
      root.accept(this);
    }
    return nodes.isEmpty() ? NO_SUCH_NODE : nodes.get(0);
  }

  @Override
  public TIntList gather(SqlNode root) {
    if (breakdownExpr && SqlKind.Expr.isInstance(root)) {
      traversePredicate(root);
    } else {
      exemptQueryNode = SqlKind.Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
      root.accept(this);
    }

    return nodes;
  }

  @Override
  public TIntList nodeIds() {
    return nodes;
  }

  @Override
  public boolean enterQuery(SqlNode query) {
    return !scoped || query.nodeId() == exemptQueryNode;
  }

  @Override
  public boolean enterCase(SqlNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.$(ExprFields.Case_Cond) == null;
  }

  @Override
  public boolean enterWhen(SqlNode when) {
    if (!bottomUp && when != null) traversePredicate(when.$(ExprFields.When_Cond));
    return false;
  }

  @Override
  public boolean enterChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (!bottomUp
        && child != null
        && (key == TableSourceFields.Joined_On || key == SqlNodeFields.QuerySpec_Where || key == SqlNodeFields.QuerySpec_Having)) {
      traversePredicate(child);
    }
    return true;
  }

  @Override
  public void leaveChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (bottomUp
        && child != null
        && (key == TableSourceFields.Joined_On || key == SqlNodeFields.QuerySpec_Where || key == SqlNodeFields.QuerySpec_Having)) {
      traversePredicate(child);
    }
  }

  @Override
  public void leaveWhen(SqlNode when) {
    if (bottomUp && when != null) nodes.add(when.$(ExprFields.When_Cond).nodeId());
  }

  private void traversePredicate(SqlNode expr) {
    assert SqlKind.Expr.isInstance(expr);
    // `expr` must be evaluated as boolean

    if (ExprKind.Binary.isInstance(expr) && expr.$(ExprFields.Binary_Op).isLogic()) {
      if (conjunctionOnly) {
        final BinaryOpKind op = expr.$(ExprFields.Binary_Op);
        if (op == BinaryOpKind.AND) {
          if (!primitive && !bottomUp) nodes.add(expr.nodeId());
          traversePredicate(expr.$(ExprFields.Binary_Left));
          traversePredicate(expr.$(ExprFields.Binary_Right));
          if (!primitive && bottomUp) nodes.add(expr.nodeId());
        } else {
          nodes.add(expr.nodeId());
        }
      } else {
        if (!primitive && !bottomUp) nodes.add(expr.nodeId());
        traversePredicate(expr.$(ExprFields.Binary_Left));
        traversePredicate(expr.$(ExprFields.Binary_Right));
        if (!primitive && bottomUp) nodes.add(expr.nodeId());
      }

    } else if (ExprKind.Unary.isInstance(expr) && expr.$(ExprFields.Unary_Op).isLogic()) {
      if (!primitive && !bottomUp) nodes.add(expr.nodeId());
      traversePredicate(expr.$(ExprFields.Unary_Expr));
      if (!primitive && bottomUp) nodes.add(expr.nodeId());

    } else {
      nodes.add(expr.nodeId());
    }
  }
}
