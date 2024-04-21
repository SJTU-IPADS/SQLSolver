package sqlsolver.sql.support.locator;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlVisitor;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.sql.ast.SqlKind.Query;

abstract class NodeLocator implements SqlVisitor, SqlGatherer, SqlFinder {
  private final TIntList nodes;
  private final boolean scoped;
  private final boolean bottomUp;
  private int exemptQueryNode;

  protected NodeLocator(boolean scoped, boolean bottomUp, int expectedNumNodes) {
    this.scoped = scoped;
    this.bottomUp = bottomUp;
    this.nodes = expectedNumNodes >= 0 ? new TIntArrayList(expectedNumNodes) : new TIntArrayList();
  }

  @Override
  public TIntList nodeIds() {
    return nodes;
  }

  @Override
  public TIntList gather(SqlNode root) {
    exemptQueryNode = Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
    root.accept(this);
    return nodes;
  }

  @Override
  public int find(SqlNode root) {
    exemptQueryNode = Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
    root.accept(this);
    return nodes.isEmpty() ? NO_SUCH_NODE : nodes.get(0);
  }

  @Override
  public boolean enter(SqlNode node) {
    if (scoped && Query.isInstance(node) && node.nodeId() != exemptQueryNode) return false;
    if (!bottomUp && shouldAccept(node)) nodes.add(node.nodeId());
    return !shouldStop(node);
  }

  @Override
  public void leave(SqlNode node) {
    if (bottomUp && shouldAccept(node)) nodes.add(node.nodeId());
  }

  protected abstract boolean shouldStop(SqlNode node);

  protected abstract boolean shouldAccept(SqlNode node);
}
