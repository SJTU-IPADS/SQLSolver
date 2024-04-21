package sqlsolver.sql.support.action;

import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.*;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.tree.TreeSupport.nodeEquals;
import static sqlsolver.common.utils.IterableSupport.all;
import static sqlsolver.sql.SqlSupport.copyAst;

public abstract class NormalizationSupport {
  private NormalizationSupport() {}

  public static void normalizeAst(SqlNode node) {
    InlineLiteralTable.normalize(node);
    //NormalizeRightJoin.normalize(node);
    Clean.clean(node);
    NormalizeGrouping.normalize(node);
    NormalizeBool.normalize(node);
    NormalizeJoinCond.normalize(node);
    //NormalizeTuple.normalize(node);
    node.context().clearAdditionalInfo();
  }

  public static void installParamMarkers(SqlNode node) {
    InstallParamMarker.normalize(node);
  }

  static void detachExpr(SqlNode node) {
    final SqlContext ctx = node.context();
    final SqlNode parent = node.parent();
    if (SqlKind.QuerySpec.isInstance(parent)) {
      if (nodeEquals(node, parent.$(SqlNodeFields.QuerySpec_Where))) parent.remove(SqlNodeFields.QuerySpec_Where);
      else if (nodeEquals(node, parent.$(SqlNodeFields.QuerySpec_Having))) parent.remove(SqlNodeFields.QuerySpec_Having);
      else throw new IllegalArgumentException();
      ctx.setParentOf(node.nodeId(), NO_SUCH_NODE);
      return;
    }
    if (TableSourceKind.JoinedSource.isInstance(parent)) {
      parent.remove(TableSourceFields.Joined_On);
      ctx.setParentOf(node.nodeId(), NO_SUCH_NODE);
    }
    if (!ExprKind.Binary.isInstance(parent)) return;

    final SqlNode lhs = parent.$(ExprFields.Binary_Left), rhs = parent.$(ExprFields.Binary_Right);
    final SqlNode otherSide = nodeEquals(lhs, node) ? rhs : lhs;
    ctx.displaceNode(parent.nodeId(), otherSide.nodeId());
  }

  static void conjunctExprTo(SqlNode parent, FieldKey<SqlNode> clause, SqlNode expr) {
    final SqlNode cond = parent.$(clause);
    if (cond == null) parent.$(clause, expr);
    else {
      final SqlContext ctx = parent.context();
      final SqlNode lhs = SqlSupport.copyAst(cond).go();
      final SqlNode newCond = SqlSupport.mkBinary(ctx, BinaryOpKind.AND, lhs, expr);
      ctx.displaceNode(cond.nodeId(), newCond.nodeId());
    }
  }

  static boolean isConstant(SqlNode node) {
    final ExprKind exprKind = node.$(SqlNodeFields.Expr_Kind);
    if (exprKind == null) return false;
    switch (exprKind) {
      case Literal:
      case Symbol:
        return true;
      case Cast:
        return isConstant(node.$(ExprFields.Cast_Expr));
      case Collate:
        return isConstant(node.$(ExprFields.Collate_Expr));
      case Interval:
        return isConstant(node.$(ExprFields.Interval_Expr));
      case ConvertUsing:
        return isConstant(node.$(ExprFields.ConvertUsing_Expr));
      case Default:
        return isConstant(node.$(ExprFields.Default_Col));
      case Values:
        return isConstant(node.$(ExprFields.Values_Expr));
      case Unary:
        return isConstant(node.$(ExprFields.Unary_Expr));
      case Binary:
        return isConstant(node.$(ExprFields.Binary_Left)) && isConstant(node.$(ExprFields.Binary_Right));
      case Ternary:
        return isConstant(node.$(ExprFields.Ternary_Left))
            && isConstant(node.$(ExprFields.Ternary_Middle))
            && isConstant(node.$(ExprFields.Ternary_Right));
      case Tuple:
        return all(node.$(ExprFields.Tuple_Exprs), NormalizationSupport::isConstant);
      case FuncCall:
        return node.$(ExprFields.FuncCall_Name) != null
            && !node.$(ExprFields.FuncCall_Name).$(SqlNodeFields.Name2_1).contains("rand")
            && all(node.$(ExprFields.FuncCall_Args), NormalizationSupport::isConstant);
      case Match:
        return isConstant(node.$(ExprFields.Match_Expr))
            && all(node.$(ExprFields.Match_Cols), NormalizationSupport::isConstant);
      case Case:
        final SqlNode cond = node.$(ExprFields.Case_Cond);
        final SqlNode else_ = node.$(ExprFields.Case_Else);
        return (cond == null || isConstant(cond))
            && (else_ == null || isConstant(else_))
            && all(node.$(ExprFields.Case_Whens), NormalizationSupport::isConstant);
      case When:
        return isConstant(node.$(ExprFields.When_Cond)) && isConstant(node.$(ExprFields.When_Expr));
    }
    return false;
  }
}
