package sqlsolver.sql.support.action;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.UnaryOpKind;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.support.resolution.Attribute;
import sqlsolver.sql.support.resolution.Relation;
import sqlsolver.sql.support.resolution.ResolutionSupport;
import sqlsolver.sql.util.RenumberListener;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.*;

import java.util.List;

import static sqlsolver.sql.SqlSupport.linearizeConjunction;
import static sqlsolver.sql.util.RenumberListener.watch;

class NormalizeJoinCond {
  static void normalize(SqlNode root) {
    for (SqlNode querySpec : LocatorSupport.nodeLocator().accept(SqlKind.QuerySpec).bottomUp(true).gather(root)) {
      process(querySpec);
    }
  }

  private static void process(SqlNode querySpec) {
    final TIntList plainConds = collectPlainCondition(querySpec);
    try (final var exprs = RenumberListener.watch(querySpec.context(), plainConds)) {
      for (SqlNode expr : exprs) {
        NormalizationSupport.detachExpr(expr);
        NormalizationSupport.conjunctExprTo(querySpec, SqlNodeFields.QuerySpec_Where, expr);
      }
    }

    final TIntList joinConds = collectJoinCondition(querySpec);
    try (final var exprs = RenumberListener.watch(querySpec.context(), joinConds)) {
      for (SqlNode expr : exprs) {
        final SqlNode targetJoin = locateJoinSource(expr);
        if (targetJoin != null) {
          NormalizationSupport.detachExpr(expr);
          NormalizationSupport.conjunctExprTo(targetJoin, TableSourceFields.Joined_On, expr);
        }
      }
    }
  }

  private static TIntList collectPlainCondition(SqlNode querySpec) {
    final SqlNodes onConditions = LocatorSupport.clauseLocator().accept(TableSourceFields.Joined_On).scoped().gather(querySpec);
    if (onConditions.isEmpty()) return new TIntArrayList(0);

    final TIntList plainConditions = new TIntArrayList(onConditions.size());
    for (SqlNode onCond : onConditions) {
      if (!onCond.parent().$(TableSourceFields.Joined_Kind).isInner()) continue;

      final List<SqlNode> terms = SqlSupport.linearizeConjunction(onCond);
      for (SqlNode term : terms) if (isPlainCondition(term)) plainConditions.add(term.nodeId());
    }

    return plainConditions;
  }

  private static TIntList collectJoinCondition(SqlNode querySpec) {
    final SqlNode whereClause = querySpec.$(SqlNodeFields.QuerySpec_Where);
    if (whereClause == null) return new TIntArrayList(0);

    return LocatorSupport.nodeLocator()
        .accept(SqlSupport::isColRefEq)
        .stopIfNot(SqlKind.Expr)
        .stopIf(n -> BinaryOpKind.AND != n.$(ExprFields.Binary_Op))
        .gatherer()
        .gather(whereClause);
  }

  private static boolean isPlainCondition(SqlNode expr) {
    if (ExprKind.Unary.isInstance(expr))
      return expr.$(ExprFields.Unary_Op) == UnaryOpKind.NOT && isPlainCondition(expr.$(ExprFields.Unary_Expr));

    if (ExprKind.Binary.isInstance(expr))
      return !expr.$(ExprFields.Binary_Op).isLogic()
          && (!ExprKind.ColRef.isInstance(expr.$(ExprFields.Binary_Left)) || !ExprKind.ColRef.isInstance(expr.$(ExprFields.Binary_Right)));

    return false;
  }

  private static SqlNode locateJoinSource(SqlNode joinCond) {
    assert SqlSupport.isColRefEq(joinCond);

    final Attribute lhs = ResolutionSupport.resolveAttribute(joinCond.$(ExprFields.Binary_Left));
    final Attribute rhs = ResolutionSupport.resolveAttribute(joinCond.$(ExprFields.Binary_Right));
    Relation enclosingRelation = ResolutionSupport.getEnclosingRelation(joinCond);
    if (enclosingRelation.toString().contains("LEFT JOIN")) {
      return null;
    }
    final List<Relation> inputs = enclosingRelation.inputs();
    for (int i = 1, bound = inputs.size(); i < bound; ++i) {
      final List<Relation> visibleInputs = inputs.subList(0, i + 1);
      if (isAttributePresent(lhs, visibleInputs) && isAttributePresent(rhs, visibleInputs)) {
        return ResolutionSupport.tableSourceOf(inputs.get(i)).parent();
      }
    }

    return null;
  }

  private static boolean isAttributePresent(Attribute attr, List<Relation> relations) {
    for (Relation relation : relations) if (relation.attributes().contains(attr)) return true;
    return false;
  }
}
