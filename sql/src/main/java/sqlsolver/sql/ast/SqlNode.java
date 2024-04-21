package sqlsolver.sql.ast;

import sqlsolver.common.tree.LabeledTreeNode;

public interface SqlNode extends LabeledTreeNode<SqlKind, SqlContext, SqlNode> {

  void accept(SqlVisitor visitor);

  String toString(boolean oneLine);

  default String dbType() {
    return context().dbType();
  }

  static SqlNode mk(SqlContext ctx, int nodeId) {
    return new SqlNodeImpl(ctx, nodeId);
  }

  static SqlNode mk(SqlContext ctx, SqlKind kind) {
    return mk(ctx, ctx.mkNode(kind));
  }

  static SqlNode mk(SqlContext ctx, ExprKind kind) {
    final SqlNode expr = mk(ctx, ctx.mkNode(SqlKind.Expr));
    expr.$(SqlNodeFields.Expr_Kind, kind);
    return expr;
  }

  static SqlNode mk(SqlContext ctx, TableSourceKind kind) {
    final SqlNode tableSource = mk(ctx, SqlKind.TableSource);
    tableSource.$(SqlNodeFields.TableSource_Kind, kind);
    return tableSource;
  }
}
