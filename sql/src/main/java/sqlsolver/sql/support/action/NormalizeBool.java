package sqlsolver.sql.support.action;

import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.ast.constants.UnaryOpKind;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.*;

class NormalizeBool {
  static void normalize(SqlNode node) {
    for (SqlNode target : LocatorSupport.predicateLocator().gather(node)) normalizeExpr(target);
  }

  private static void normalizeExpr(SqlNode expr) {
    assert SqlKind.Expr.isInstance(expr);
    // `expr` must be evaluated as boolean

    final SqlContext ctx = expr.context();

    if (ExprKind.ColRef.isInstance(expr)) {
      final SqlNode lhs = SqlSupport.copyAst(expr).go();
      final SqlNode rhs = SqlSupport.mkLiteral(ctx, LiteralKind.INTEGER, 1);
      final SqlNode binary = SqlSupport.mkBinary(ctx, BinaryOpKind.EQUAL, lhs, rhs);
      ctx.displaceNode(expr.nodeId(), binary.nodeId());

    } else if (expr.$(ExprFields.Binary_Op) == BinaryOpKind.IS) {
      final SqlNode rhs = expr.$(ExprFields.Binary_Right);

      if (ExprKind.Literal.isInstance(rhs) && rhs.$(ExprFields.Literal_Kind) == LiteralKind.BOOL) {
        expr.$(ExprFields.Binary_Op, BinaryOpKind.EQUAL);

        if (rhs.$(ExprFields.Literal_Value).equals(Boolean.FALSE)) {
          normalizeExpr(expr.$(ExprFields.Binary_Left));

          final SqlNode operand = SqlSupport.copyAst(expr.$(ExprFields.Binary_Left)).go();
          final SqlNode unary = SqlSupport.mkUnary(ctx, UnaryOpKind.NOT, operand);
          ctx.displaceNode(expr.nodeId(), unary.nodeId());
        }
      }

    } else if (ExprKind.Unary.isInstance(expr) && expr.$(ExprFields.Unary_Op).isLogic()) {
      normalizeExpr(expr.$(ExprFields.Unary_Expr));

    } else if (ExprKind.Binary.isInstance(expr) && expr.$(ExprFields.Binary_Op).isLogic()) {
      normalizeExpr(expr.$(ExprFields.Binary_Left));
      normalizeExpr(expr.$(ExprFields.Binary_Right));
    }

    final ExprKind exprKind = expr.$(SqlNodeFields.Expr_Kind);
    assert exprKind == ExprKind.Unary
        || exprKind == ExprKind.Binary
        || exprKind == ExprKind.Ternary
        || exprKind == ExprKind.Case
        || exprKind == ExprKind.Exists
        || exprKind == ExprKind.Match
        || exprKind == ExprKind.ColRef
        || exprKind == ExprKind.Literal
        || exprKind == ExprKind.FuncCall;
  }
}
