package sqlsolver.sql.parser;

import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.*;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.*;

import java.util.List;

public interface AstBuilderMixin {
  SqlContext ast();

  default SqlNode mkVoid() {
    return mkNode(SqlKind.Void);
  }

  default SqlNode mkNode(SqlKind kind) {
    return SqlNode.mk(ast(), ast().mkNode(kind));
  }

  default SqlNode mkName2(String piece0, String piece1) {
    final SqlNode name = mkNode(SqlKind.Name2);
    name.$(SqlNodeFields.Name2_0, piece0);
    name.$(SqlNodeFields.Name2_1, piece1);
    return name;
  }

  default SqlNode mkName3(String piece0, String piece1, String piece2) {
    final SqlNode name = mkNode(SqlKind.Name3);
    name.$(SqlNodeFields.Name3_0, piece0);
    name.$(SqlNodeFields.Name3_1, piece1);
    name.$(SqlNodeFields.Name3_2, piece2);
    return name;
  }

  default SqlNode mkTableName(String schemaName, String tableName) {
    final SqlNode name = mkNode(SqlKind.TableName);
    if (schemaName != null) name.$(SqlNodeFields.TableName_Schema, schemaName);
    name.$(SqlNodeFields.TableName_Table, tableName);
    return name;
  }

  default SqlNode mkColName(String schemaName, String tableName, String colName) {
    final SqlNode name = mkNode(SqlKind.ColName);
    if (schemaName != null) name.$(SqlNodeFields.ColName_Schema, schemaName);
    if (tableName != null) name.$(SqlNodeFields.ColName_Table, tableName);
    name.$(SqlNodeFields.ColName_Col, colName);
    return name;
  }

  default SqlNode mkExpr(ExprKind kind) {
    final SqlNode node = mkNode(SqlKind.Expr);
    node.setField(SqlNodeFields.Expr_Kind, kind);
    return node;
  }

  default SqlNode mkColRef(String schemaName, String tableName, String colName) {
    final SqlNode name = mkColName(schemaName, tableName, colName);
    final SqlNode ref = mkExpr(ExprKind.ColRef);
    ref.$(ExprFields.ColRef_ColName, name);
    return ref;
  }

  default SqlNode mkColRef(SqlNode colName) {
    final SqlNode colRef = mkExpr(ExprKind.ColRef);
    colRef.$(ExprFields.ColRef_ColName, colName);
    return colRef;
  }

  default SqlNode mkLiteral(LiteralKind kind, Object value) {
    final SqlNode literal = mkExpr(ExprKind.Literal);
    literal.$(ExprFields.Literal_Kind, kind);
    literal.$(ExprFields.Literal_Value, value);
    return literal;
  }

  default SqlNode mkSymbol(String text) {
    final SqlNode symbol = mkExpr(ExprKind.Symbol);
    symbol.$(ExprFields.Symbol_Text, text);
    return symbol;
  }

  default SqlNode mkWildcard(SqlNode tableName) {
    final SqlNode wildcard = mkExpr(ExprKind.Wildcard);
    wildcard.$(ExprFields.Wildcard_Table, tableName);
    return wildcard;
  }

  default SqlNode mkParam() {
    return mkExpr(ExprKind.Param);
  }

  default SqlNode mkParam(int index) {
    final SqlNode param = mkParam();
    param.$(ExprFields.Param_Number, index);
    return param;
  }

  default SqlNode mkUnary(SqlNode expr, UnaryOpKind op) {
    final SqlNode unary = mkExpr(ExprKind.Unary);
    unary.$(ExprFields.Unary_Expr, expr);
    unary.$(ExprFields.Unary_Op, op);
    return unary;
  }

  default SqlNode mkBinary(SqlNode left, SqlNode right, BinaryOpKind op) {
    final SqlNode binary = mkExpr(ExprKind.Binary);
    binary.$(ExprFields.Binary_Left, left);
    binary.$(ExprFields.Binary_Right, right);
    binary.$(ExprFields.Binary_Op, op);
    return binary;
  }

  default SqlNode mkInterval(SqlNode expr, IntervalUnit unit) {
    final SqlNode interval = mkExpr(ExprKind.Interval);
    interval.$(ExprFields.Interval_Expr, expr);
    interval.$(ExprFields.Interval_Unit, unit);
    return interval;
  }

  default SqlNode mkIndirection(SqlNode expr, SqlNodes indirections) {
    final SqlNode indirection = mkExpr(ExprKind.Indirection);
    indirection.$(ExprFields.Indirection_Expr, expr);
    indirection.$(ExprFields.Indirection_Comps, indirections);
    return indirection;
  }

  default SqlNode mkSelectItem(SqlNode expr, String alias) {
    final SqlNode selectItem = mkNode(SqlKind.SelectItem);
    selectItem.$(SqlNodeFields.SelectItem_Expr, expr);
    selectItem.$(SqlNodeFields.SelectItem_Alias, alias);
    return selectItem;
  }

  default SqlNode mkGroupItem(SqlNode expr) {
    final SqlNode groupItem = mkNode(SqlKind.GroupItem);
    groupItem.$(SqlNodeFields.GroupItem_Expr, expr);
    return groupItem;
  }

  default SqlNode mkTableSource(TableSourceKind kind) {
    final SqlNode node = mkNode(SqlKind.TableSource);
    node.$(SqlNodeFields.TableSource_Kind, kind);
    return node;
  }

  default SqlNode mkJoined(SqlNode left, SqlNode right, JoinKind kind) {
    final SqlNode joined = mkTableSource(TableSourceKind.JoinedSource);
    joined.$(TableSourceFields.Joined_Left, left);
    joined.$(TableSourceFields.Joined_Right, right);
    joined.$(TableSourceFields.Joined_Kind, kind);
    return joined;
  }

  default SqlNodes mkNodes(List<SqlNode> nodes) {
    return SqlNodes.mk(ast(), nodes);
  }

  default SqlNode wrapAsQuery(SqlNode node) {
    final SqlKind kind = node.kind();
    if (kind == SqlKind.QuerySpec || kind == SqlKind.SetOp) {
      final SqlNode queryNode = mkNode(SqlKind.Query);
      queryNode.$(SqlNodeFields.Query_Body, node);
      return queryNode;
    } else {
      return node;
    }
  }

  default SqlNode wrapAsQueryExpr(SqlNode node) {
    assert SqlKind.Query.isInstance(node);
    final SqlNode queryExpr = mkExpr(ExprKind.QueryExpr);
    queryExpr.setField(ExprFields.QueryExpr_Query, node);
    return queryExpr;
  }
}
