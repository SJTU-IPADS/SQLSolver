package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.UnaryOpKind;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.*;

import static sqlsolver.sql.support.locator.LocatorSupport.gatherColRefs;
import static sqlsolver.sql.support.locator.LocatorSupport.nodeLocator;

class JoinGraphBuilder {
  static JoinGraph build(SqlContext ctx) {
    return build(SqlNode.mk(ctx, ctx.root()));
  }

  static JoinGraph build(SqlNode root) {
    final JoinGraph graph = new JoinGraphImpl();

    final SqlNodes tables = nodeLocator().accept(TableSourceKind.SimpleSource).gather(root);
    for (SqlNode table : tables) graph.addTable(ResolutionSupport.getEnclosingRelation(table));

    final SqlNodes joinConds = nodeLocator().accept(SqlSupport::isColRefEq).gather(root);
    for (SqlNode joinCond : joinConds) addJoinCondition(graph, joinCond);

    final SqlNodes inSubExprs = nodeLocator().accept(JoinGraphBuilder::isInSubExpr).gather(root);
    for (SqlNode inSubExpr : inSubExprs) addInSubquery(graph, inSubExpr);

    return graph;
  }

  private static void addJoinCondition(JoinGraph graph, SqlNode joinCond) {
    if (isNegated(joinCond)) return;

    final Attribute lhsKey = ResolutionSupport.resolveAttribute(joinCond.$(ExprFields.Binary_Left));
    final Attribute rhsKey = ResolutionSupport.resolveAttribute(joinCond.$(ExprFields.Binary_Right));
    if (lhsKey == null || rhsKey == null) return;

    final Column lhsCol = ResolutionSupport.traceRef(lhsKey).column(), rhsCol = ResolutionSupport.traceRef(rhsKey).column();
    if (lhsCol == null || rhsCol == null) return;

    graph.addJoin(ResolutionSupport.traceRef(lhsKey).owner(), lhsCol, ResolutionSupport.traceRef(rhsKey).owner(), rhsCol);
  }

  private static void addInSubquery(JoinGraph graph, SqlNode inSub) {
    if (isNegated(inSub)) return;

    final SqlNodes lhsColRefs = gatherColRefs(inSub.$(ExprFields.Binary_Left));
    if (lhsColRefs.isEmpty()) return;

    final Attribute lhsKey = ResolutionSupport.resolveAttribute(lhsColRefs.get(0));
    if (lhsKey == null) return;

    final Attribute lhsBaseKey = ResolutionSupport.traceRef(lhsKey);
    final Column lhsCol = lhsBaseKey.column();
    if (lhsCol == null) return;

    final Relation subqueryRel = ResolutionSupport.getEnclosingRelation(inSub.$(ExprFields.Binary_Right).$(ExprFields.QueryExpr_Query));
    final Attribute rhsBaseKey = ResolutionSupport.traceRef(subqueryRel.attributes().get(0));
    final Column rhsCol = rhsBaseKey.column();
    if (rhsCol == null) return;

    graph.addJoin(lhsBaseKey.owner(), lhsCol, rhsBaseKey.owner(), rhsCol);
  }

  private static boolean isInSubExpr(SqlNode node) {
    return node.$(ExprFields.Binary_Op) == BinaryOpKind.IN_SUBQUERY;
  }

  private static boolean isNegated(SqlNode node) {
    boolean negated = false;
    while (SqlKind.Expr.isInstance(node)) {
      if (node.$(ExprFields.Unary_Op) == UnaryOpKind.NOT) negated = !negated;
      node = node.parent();
    }
    return negated;
  }
}
