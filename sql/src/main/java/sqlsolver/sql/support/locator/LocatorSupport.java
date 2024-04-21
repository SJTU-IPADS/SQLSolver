package sqlsolver.sql.support.locator;

import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.*;

public interface LocatorSupport {
  static NodeLocatorBuilder nodeLocator() {
    return new NodeLocatorBuilder();
  }

  static ClauseLocatorBuilder clauseLocator() {
    return new ClauseLocatorBuilder();
  }

  static PredicateLocatorBuilder predicateLocator() {
    return new PredicateLocatorBuilder();
  }

  static SqlNodes gatherColRefs(SqlNode root) {
    return gatherColRefs(root, true);
  }

  static SqlNodes gatherColRefs(SqlNode root, boolean scoped) {
    return nodeLocator().accept(ExprKind.ColRef).scoped(scoped).gather(root);
  }

  static SqlNodes gatherColRefs(Iterable<SqlNode> roots) {
    return gatherColRefs(roots, true);
  }

  static SqlNodes gatherColRefs(Iterable<SqlNode> roots, boolean scoped) {
    final SqlGatherer gatherer = nodeLocator().accept(ExprKind.ColRef).scoped(scoped).gatherer();
    SqlContext ctx = null;
    for (SqlNode root : roots) {
      if (ctx == null) ctx = root.context();
      gatherer.gather(root);
    }
    return ctx == null ? SqlNodes.mkEmpty() : SqlNodes.mk(ctx, gatherer.nodeIds());
  }

  static SqlNodes gatherSimpleSources(SqlNode root) {
    return gatherSimpleSources(root, true);
  }

  static SqlNodes gatherSimpleSources(SqlNode root, boolean scoped) {
    return nodeLocator().accept(TableSourceKind.SimpleSource).scoped(scoped).gather(root);
  }
}
