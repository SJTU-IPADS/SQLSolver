package sqlsolver.sql.plan;

import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.constants.LiteralKind;

import java.util.List;

public interface Expression {
  /** The template of the expression. All the col-refs are replaced by placeholders. */
  SqlNode template();

  /**
   * The used col-refs in the original expression. Should only be used during value-ref solution.
   */
  List<SqlNode> colRefs();

  /** The used col-refs in the `template()`. */
  List<SqlNode> internalRefs();

  /** BE CAREFUL TO USE. This updates itself with a new template. */
  Expression setTemplate(SqlNode ast);

  /** Interpolate names to placeholders. */
  SqlNode interpolate(SqlContext ctx, Values values);

  Expression copy();

  static Expression mk(SqlNode ast) {
    return new ExpressionImpl(ast);
  }

  /** USE THIS WITH CAUTION!!! Make sure `colRefs` belongs to `ast` */
  static Expression mk(SqlNode ast, List<SqlNode> colRefs) {
    return new ExpressionImpl(ast, colRefs);
  }

  Expression EXPRESSION_TRUE = mkBooleanLiteral(true);
  Expression EXPRESSION_FALSE = mkBooleanLiteral(false);

  static Expression mkBooleanLiteral(boolean val){
    final SqlNode node = SqlNode.mk(SqlContext.mk(4), ExprKind.Literal);
    node.$(ExprFields.Literal_Kind, LiteralKind.BOOL);
    node.$(ExprFields.Literal_Value, val);
    node.$(ExprFields.Literal_Unit, null);
    return Expression.mk(node);
  }
}
