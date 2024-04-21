package sqlsolver.sql.support.locator;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.ast.SqlVisitor;
import sqlsolver.sql.ast.SqlKind;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;

public abstract class ClauseLocator implements SqlVisitor, SqlFinder, SqlGatherer {
  private final TIntList nodes;
  private final boolean scoped;
  private final boolean bottomUp;
  private int exemptQueryNode;

  protected ClauseLocator(boolean scoped, boolean bottomUp, int expectedNumNodes) {
    this.nodes = expectedNumNodes >= 0 ? new TIntArrayList(expectedNumNodes) : new TIntArrayList();
    this.bottomUp = bottomUp;
    this.scoped = scoped;
  }

  @Override
  public TIntList nodeIds() {
    return nodes;
  }

  @Override
  public int find(SqlNode root) {
    exemptQueryNode = SqlKind.Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
    root.accept(this);
    return nodes.isEmpty() ? NO_SUCH_NODE : nodes.get(0);
  }

  @Override
  public TIntList gather(SqlNode root) {
    exemptQueryNode = SqlKind.Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
    root.accept(this);
    return nodes;
  }

  @Override
  public boolean enterQuery(SqlNode query) {
    return !scoped || query.nodeId() == exemptQueryNode;
  }

  @Override
  public boolean enterChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (child == null) return false;
    if (!bottomUp && shouldAccept(key)) nodes.add(child.nodeId());
    return true;
  }

  @Override
  public boolean enterChildren(SqlNode parent, FieldKey<SqlNodes> key, SqlNodes child) {
    if (!bottomUp && shouldAccept(key)) nodes.addAll(child.nodeIds());
    return true;
  }

  @Override
  public void leaveChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (bottomUp && shouldAccept(key)) nodes.add(child.nodeId());
  }

  @Override
  public void leaveChildren(SqlNode parent, FieldKey<SqlNodes> key, SqlNodes child) {
    if (bottomUp && shouldAccept(key)) nodes.addAll(child.nodeIds());
  }

  protected abstract boolean shouldAccept(FieldKey<?> clause);
}
