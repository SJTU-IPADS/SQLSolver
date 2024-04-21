package sqlsolver.sql.plan.normalize.action;

import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.plan.Expression;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanSupport;
import sqlsolver.sql.plan.SimpleFilterNode;
import sqlsolver.sql.plan.Value;

import static sqlsolver.sql.ast.SqlNodeFields.Expr_Kind;
import static sqlsolver.sql.util.TypeConverter.isConvertibleStringToInt;

public class NormalizeBool {

  public static void normalizeFilter(int nodeId, PlanContext plan) {
    SimpleFilterNode filter = (SimpleFilterNode) plan.nodeAt(nodeId);
    final Expression predExpr = filter.predicate();
    final SqlNode predNode = predExpr.template();

    ExprKind exprKind = predNode.$(Expr_Kind);
    switch (exprKind) {
      case Binary -> {
        final BinaryOpKind binaryOpKind = predNode.$(ExprFields.Binary_Op);
        final SqlNode lhs = predNode.$(ExprFields.Binary_Left);
        final SqlNode rhs = predNode.$(ExprFields.Binary_Right);
        if (binaryOpKind.isComparison()) {
          /* for comparison filter,  a {=,<>,<,>,<=,>=} CONST, when a's type is different from CONST => FALSE
          e.g a = 'test' and a's type is INTEGER => FALSE
          */

          final SqlNode cstNode = ExprKind.Literal.isInstance(rhs) ? rhs : ExprKind.Literal.isInstance(lhs) ? lhs : null;
          final SqlNode refNode = ExprKind.ColRef.isInstance(rhs) ? rhs : ExprKind.ColRef.isInstance(lhs) ? lhs : null;

          if (cstNode != null && refNode != null) {
            if (cstNode.$(ExprFields.Literal_Kind) == LiteralKind.NULL) break;
            final int index = predExpr.internalRefs().indexOf(refNode);
            if (index < 0) break;
            final Value param = plan.valuesReg().valueRefsOf(predExpr).get(index);
            final Column refColumn = PlanSupport.tryResolveColumn(plan, param);
            if (refColumn != null)
              switch (refColumn.dataType().category()) {
                case INTEGRAL -> {
                  if (cstNode.$(ExprFields.Literal_Kind) != LiteralKind.INTEGER
                          && !isConvertibleStringToInt((String) cstNode.$(ExprFields.Literal_Value))) {
                    replaceFilterWithFalse(cstNode, filter, plan);
                  }
                }
                default -> {
                }
              }
          }
        }
      }
    }
  }

  private static void replaceFilterWithFalse(SqlNode cstNode, SimpleFilterNode filter, PlanContext plan) {
    final SqlNode literal = SqlNode.mk(cstNode.context(), ExprKind.Literal);
    literal.$(ExprFields.Literal_Kind, LiteralKind.BOOL);
    literal.$(ExprFields.Literal_Value, false);
    filter.setPredicate(Expression.mk(literal));
    PlanSupport.resolvePlan(plan);
  }
}
