package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.constants.*;
import sqlsolver.sql.ast.constants.*;

import static sqlsolver.sql.ast.SqlKind.Expr;
import static sqlsolver.sql.ast.SqlNodeFields.Expr_Kind;


public interface ExprFields {
  // Variable
  FieldKey<VariableScope> Variable_Scope = ExprKind.Variable.field("Scope", VariableScope.class);
  FieldKey<String> Variable_Name = ExprKind.Variable.textField("Name");
  FieldKey<SqlNode> Variable_Assignment = ExprKind.Variable.nodeField("Assignment"); // Expr
  // Col Ref
  FieldKey<SqlNode> ColRef_ColName = ExprKind.ColRef.nodeField("Column"); // ColName
  // Func Call
  FieldKey<SqlNode> FuncCall_Name = ExprKind.FuncCall.nodeField("Name"); // Name2
  FieldKey<SqlNodes> FuncCall_Args = ExprKind.FuncCall.nodesField("Args"); // Expr
  // Collate
  FieldKey<SqlNode> Collate_Expr = ExprKind.Collate.nodeField("Expr"); // Expr
  FieldKey<SqlNode> Collate_Collation = ExprKind.Collate.nodeField("Collation"); // Symbol
  // Interval
  FieldKey<SqlNode> Interval_Expr = ExprKind.Interval.nodeField("Expr");// Expr
  FieldKey<IntervalUnit> Interval_Unit = ExprKind.Interval.field("Unit", IntervalUnit.class);
  // Symbol
  FieldKey<String> Symbol_Text = ExprKind.Symbol.textField("Text");
  // Literal
  FieldKey<LiteralKind> Literal_Kind = ExprKind.Literal.field("Kind", LiteralKind.class);
  FieldKey<Object> Literal_Value = ExprKind.Literal.field("Value", Object.class);
  FieldKey<String> Literal_Unit = ExprKind.Literal.textField("Unit");
  // Aggregate
  FieldKey<String> Aggregate_Name = ExprKind.Aggregate.textField("Name");
  FieldKey<Boolean> Aggregate_Distinct = ExprKind.Aggregate.boolField("Distinct");
  FieldKey<SqlNodes> Aggregate_Args = ExprKind.Aggregate.nodesField("Args"); // Expr
  FieldKey<String> Aggregate_WindowName = ExprKind.Aggregate.textField("WindowName");
  FieldKey<SqlNode> Aggregate_WindowSpec = ExprKind.Aggregate.nodeField("WindowSpec"); // WindowSpec
  FieldKey<SqlNode> Aggregate_Filter = ExprKind.Aggregate.nodeField("Filter"); // Expr
  FieldKey<SqlNodes> Aggregate_WithinGroupOrder = ExprKind.Aggregate.nodesField("WithinGroupOrder"); // Grouping
  FieldKey<SqlNodes> Aggregate_Order = ExprKind.Aggregate.nodesField("Order");
  FieldKey<String> Aggregate_Sep = ExprKind.Aggregate.textField("Sep");
  // Wildcard
  FieldKey<SqlNode> Wildcard_Table = ExprKind.Wildcard.nodeField("Table"); // TableName
  // Grouping
  FieldKey<SqlNodes> GroupingOp_Exprs = ExprKind.GroupingOp.nodesField("Exprs"); // Expr
  // Unary
  FieldKey<UnaryOpKind> Unary_Op = ExprKind.Unary.field("Op", UnaryOpKind.class);
  FieldKey<SqlNode> Unary_Expr = ExprKind.Unary.nodeField("Expr"); // Expr
  // Binary
  FieldKey<BinaryOpKind> Binary_Op = ExprKind.Binary.field("Op", BinaryOpKind.class);
  FieldKey<SqlNode> Binary_Left = ExprKind.Binary.nodeField("Left");
  FieldKey<SqlNode> Binary_Right = ExprKind.Binary.nodeField("Right");
  FieldKey<SubqueryOption> Binary_SubqueryOption =
      ExprKind.Binary.field("SubqueryOption", SubqueryOption.class);
  // Ternary
  FieldKey<TernaryOp> Ternary_Op = ExprKind.Ternary.field("Op", TernaryOp.class);
  FieldKey<SqlNode> Ternary_Left = ExprKind.Ternary.nodeField("Left"); // Expr
  FieldKey<SqlNode> Ternary_Middle = ExprKind.Ternary.nodeField("Middle"); // Expr
  FieldKey<SqlNode> Ternary_Right = ExprKind.Ternary.nodeField("Right"); // Expr
  // Tuple
  FieldKey<SqlNodes> Tuple_Exprs = ExprKind.Tuple.nodesField("Exprs"); // Expr
  FieldKey<Boolean> Tuple_AsRow = ExprKind.Tuple.boolField("AsRow");
  // Exists
  FieldKey<SqlNode> Exists_Subquery = ExprKind.Exists.nodeField("Subquery"); // QueryExpr
  // MatchAgainst
  FieldKey<SqlNodes> Match_Cols = ExprKind.Match.nodesField("Columns"); // ColRef
  FieldKey<SqlNode> Match_Expr = ExprKind.Match.nodeField("Expr"); // Expr
  FieldKey<MatchOption> Match_Option = ExprKind.Match.field("Option", MatchOption.class);
  // Cast
  FieldKey<SqlNode> Cast_Expr = ExprKind.Cast.nodeField("Expr"); // Expr
  FieldKey<SqlDataType> Cast_Type = ExprKind.Cast.field("Type", SqlDataType.class);
  FieldKey<Boolean> Cast_IsArray = ExprKind.Cast.boolField("IsArray");
  // Case
  FieldKey<SqlNode> Case_Cond = ExprKind.Case.nodeField("Condition"); // Expr
  FieldKey<SqlNodes> Case_Whens = ExprKind.Case.nodesField("When"); // When
  FieldKey<SqlNode> Case_Else = ExprKind.Case.nodeField("Else"); // Expr
  // When
  FieldKey<SqlNode> When_Cond = ExprKind.When.nodeField("Condition"); // Expr
  FieldKey<SqlNode> When_Expr = ExprKind.When.nodeField("Expr"); // Expr
  // ConvertUsing
  FieldKey<SqlNode> ConvertUsing_Expr = ExprKind.ConvertUsing.nodeField("Expr"); // Expr
  FieldKey<SqlNode> ConvertUsing_Charset = ExprKind.ConvertUsing.nodeField("Charset"); // Symbol
  // Default
  FieldKey<SqlNode> Default_Col = ExprKind.Default.nodeField("Col"); // Expr
  // Values
  FieldKey<SqlNode> Values_Expr = ExprKind.Values.nodeField("Expr"); // Expr
  // QueryExpr
  FieldKey<SqlNode> QueryExpr_Query = ExprKind.QueryExpr.nodeField("Query"); // Query
  // Indirection
  FieldKey<SqlNode> Indirection_Expr = ExprKind.Indirection.nodeField("Expr"); // Expr
  FieldKey<SqlNodes> Indirection_Comps = ExprKind.Indirection.nodesField("Comps"); // IndirectionComp
  // IndirectionComp
  FieldKey<Boolean> IndirectionComp_Subscript = ExprKind.IndirectionComp.boolField("Subscript");
  FieldKey<SqlNode> IndirectionComp_Start = ExprKind.IndirectionComp.nodeField("Start"); // Expr
  FieldKey<SqlNode> IndirectionComp_End = ExprKind.IndirectionComp.nodeField("End"); // Expr
  // Param
  FieldKey<Integer> Param_Number = ExprKind.Param.field("Number", Integer.class);
  FieldKey<Boolean> Param_ForceQuestion = ExprKind.Param.boolField("ForceQuestion");
  // ComparisonMod
  FieldKey<SubqueryOption> ComparisonMod_Option =
      ExprKind.ComparisonMod.field("Option", SubqueryOption.class);
  FieldKey<SqlNode> ComparisonMod_Expr = ExprKind.ComparisonMod.nodeField("Expr"); // Expr
  // Array
  FieldKey<SqlNodes> Array_Elements = ExprKind.Array.nodesField("Elements"); // Expr
  // TypeCoercion
  FieldKey<SqlDataType> TypeCoercion_Type = ExprKind.TypeCoercion.field("Type", SqlDataType.class);
  FieldKey<String> TypeCoercion_String = ExprKind.TypeCoercion.textField("RawType");
  // DataTimeOverlap
  FieldKey<SqlNode> DateTimeOverlap_LeftStart = ExprKind.DateTimeOverlap.nodeField("LeftStart"); // Expr
  FieldKey<SqlNode> DateTimeOverlap_LeftEnd = ExprKind.DateTimeOverlap.nodeField("LeftEnd"); // Expr
  FieldKey<SqlNode> DateTimeOverlap_RightStart = ExprKind.DateTimeOverlap.nodeField("RightStart"); // Expr
  FieldKey<SqlNode> DateTimeOverlap_RightEnd = ExprKind.DateTimeOverlap.nodeField("RightEnd"); // Expr

  static int getOperatorPrecedence(SqlNode node) {
    if (!Expr.isInstance(node)) return -1;
    final ExprKind exprKind = node.$(Expr_Kind);
    return switch (exprKind) {
      case Unary -> node.$(Unary_Op).precedence();
      case Binary -> node.$(Binary_Op).precedence();
      case Ternary -> node.$(Ternary_Op).precedence();
      case Case, When -> 5;
      case Collate -> 13;
      case Interval -> 14;
      default -> -1;
    };
  }
}
