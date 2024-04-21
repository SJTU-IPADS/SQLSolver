package sqlsolver.sql.support.action;

import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.support.resolution.Attribute;
import sqlsolver.sql.support.resolution.Relation;
import sqlsolver.sql.support.resolution.ResolutionSupport;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.ast.*;

import static sqlsolver.common.tree.TreeSupport.rootOf;
import static sqlsolver.common.utils.IterableSupport.all;

class InlineLiteralTable {
  static void normalize(SqlNode node) {
    final SqlNodes literalTables = LocatorSupport.nodeLocator().accept(InlineLiteralTable::canInline).gather(node);
    for (SqlNode literalTable : literalTables) inlineLiteralTable(literalTable);
  }

  private static boolean canInline(SqlNode node) {
    return TableSourceKind.DerivedSource.isInstance(node)
        && TableSourceKind.JoinedSource.isInstance(node.parent())
        && isLiteralTable(node);
  }

  private static boolean isLiteralTable(SqlNode derived) {
    final SqlNode subquery = derived.$(TableSourceFields.Derived_Subquery);
    final SqlNode body = subquery.$(SqlNodeFields.Query_Body);
    return !SqlKind.SetOp.isInstance(body)
        && body.$(SqlNodeFields.QuerySpec_From) == null
        && all(body.$(SqlNodeFields.QuerySpec_SelectItems), it -> ExprKind.Literal.isInstance(it.$(SqlNodeFields.SelectItem_Expr)));
  }

  private static void inlineLiteralTable(SqlNode table) {
    final SqlContext ctx = table.context();
    inlineExprs(SqlNode.mk(ctx, rootOf(ctx, table.nodeId())), table);
    reduceTable(table);
  }

  private static void inlineExprs(SqlNode rootQuery, SqlNode tableSource) {
    assert TableSourceKind.DerivedSource.isInstance(tableSource);
    final Relation targetRelation = ResolutionSupport.getEnclosingRelation(tableSource.$(TableSourceFields.Derived_Subquery));
    final SqlNodes colRefs = LocatorSupport.nodeLocator().accept(ExprKind.ColRef).gather(rootQuery);
    final SqlContext ctx = rootQuery.context();

    for (SqlNode colRef : colRefs) {
      final Attribute attr = ResolutionSupport.resolveAttribute(colRef);
      if (attr == null) continue;

      final Attribute baseRef = ResolutionSupport.traceRef(attr);
      if (baseRef == null || baseRef.owner() != targetRelation) continue;
      assert ExprKind.Literal.isInstance(baseRef.expr());

      // If the expr is an ORDER BY item then just remove it.
      // Consider "SELECT .. FROM (SELECT 1 AS o) t ORDER BY t.o"
      // "t.o" shouldn't be replaced as "1" because "ORDER BY 1"
      // means "order by the 1st output column".
      // It can be just removed since constant value won't affect
      // the ordering
      final SqlNode parent = colRef.parent();

      if (SqlKind.OrderItem.isInstance(parent)) {
        final SqlNode q = parent.parent();
        ctx.detachNode(parent.nodeId());
        if (SqlKind.Query.isInstance(q) && q.$(SqlNodeFields.Query_OrderBy).isEmpty()) q.remove(SqlNodeFields.Query_OrderBy);

      } else {
        final SqlNode copied = SqlSupport.copyAst(baseRef.expr()).go();
        ctx.displaceNode(colRef.nodeId(), copied.nodeId());
      }
    }
  }

  private static void reduceTable(SqlNode deriveTableSource) {
    final SqlContext ctx = deriveTableSource.context();
    final SqlNode body = ResolutionSupport.getEnclosingRelation(deriveTableSource).rootNode().$(SqlNodeFields.Query_Body);
    final SqlNode joinNode = deriveTableSource.parent();
    final SqlNode lhs = joinNode.$(TableSourceFields.Joined_Left);
    final SqlNode rhs = joinNode.$(TableSourceFields.Joined_Right);
    final SqlNode cond = joinNode.$(TableSourceFields.Joined_On);
    if (lhs == deriveTableSource) ctx.displaceNode(joinNode.nodeId(), rhs.nodeId());
    else ctx.displaceNode(joinNode.nodeId(), lhs.nodeId());
    assert SqlKind.QuerySpec.isInstance(body);
    NormalizationSupport.conjunctExprTo(body, SqlNodeFields.QuerySpec_Where, cond);
  }
}
