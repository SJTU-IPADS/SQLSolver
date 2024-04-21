package sqlsolver.sql.ast;

import sqlsolver.common.tree.LabeledTreeNodeBase;

class SqlNodeImpl extends LabeledTreeNodeBase<SqlKind, SqlContext, SqlNode> implements SqlNode {
  SqlNodeImpl(SqlContext context, int nodeId) {
    super(context, nodeId);
  }

  @Override
  public void accept(SqlVisitor visitor) {
    if (SqlVisitorDriver.enter(this, visitor)) SqlVisitorDriver.visitChildren(this, visitor);
    SqlVisitorDriver.leave(this, visitor);
  }

  @Override
  public String toString() {
    return toString(true);
  }

  @Override
  public String toString(boolean oneLine) {
    final SqlFormatter formatter = new SqlFormatter(oneLine);
    accept(formatter);
    return formatter.toString();
  }

  @Override
  public String dbType() {
    return context().dbType();
  }

  @Override
  protected SqlNode mk(SqlContext context, int nodeId) {
    return new SqlNodeImpl(context, nodeId);
  }
}
