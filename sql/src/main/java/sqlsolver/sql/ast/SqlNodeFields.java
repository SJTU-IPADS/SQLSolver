package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.constants.*;
import sqlsolver.sql.ast.constants.*;

import java.util.EnumSet;
import java.util.List;

public interface SqlNodeFields { // also serves as a marker interface
  //// TableName
  FieldKey<String> TableName_Schema = SqlKind.TableName.textField("Schema");
  FieldKey<String> TableName_Table = SqlKind.TableName.textField("Table");

  //// ColumnName
  FieldKey<String> ColName_Schema = SqlKind.ColName.textField("Schema");
  FieldKey<String> ColName_Table = SqlKind.ColName.textField("Table");
  FieldKey<String> ColName_Col = SqlKind.ColName.textField("Column");

  //// CommonName2
  FieldKey<String> Name2_0 = SqlKind.Name2.textField("Part0");
  FieldKey<String> Name2_1 = SqlKind.Name2.textField("Part1");

  //// CommonName3
  FieldKey<String> Name3_0 = SqlKind.Name3.textField("Part0");
  FieldKey<String> Name3_1 = SqlKind.Name3.textField("Part1");
  FieldKey<String> Name3_2 = SqlKind.Name3.textField("Part2");

  //// CreateTable
  FieldKey<SqlNode> CreateTable_Name = SqlKind.CreateTable.nodeField("Name"); // TableName
  FieldKey<SqlNodes> CreateTable_Cols = SqlKind.CreateTable.nodesField("Columns"); // ColDef
  FieldKey<SqlNodes> CreateTable_Cons = SqlKind.CreateTable.nodesField("Constraints"); // IndexDef
  FieldKey<String> CreateTable_Engine = SqlKind.CreateTable.textField("Engine");

  //// ColumnDef
  FieldKey<SqlNode> ColDef_Name = SqlKind.ColDef.nodeField("Name"); // ColName
  FieldKey<String> ColDef_RawType = SqlKind.ColDef.textField("TypeRaw");
  FieldKey<SqlDataType> ColDef_DataType = SqlKind.ColDef.field("DataType", SqlDataType.class);
  FieldKey<EnumSet<ConstraintKind>> ColDef_Cons = SqlKind.ColDef.field("Constraint", EnumSet.class);
  FieldKey<SqlNode> ColDef_Ref = SqlKind.ColDef.nodeField("References"); // References
  FieldKey<Boolean> ColDef_Generated = SqlKind.ColDef.boolField("Genearted");
  FieldKey<Boolean> ColDef_Default = SqlKind.ColDef.boolField("Default");
  FieldKey<Boolean> ColDef_AutoInc = SqlKind.ColDef.boolField("AutoInc");

  //// References
  FieldKey<SqlNode> Reference_Table = SqlKind.Reference.nodeField("Table"); // TableName
  FieldKey<SqlNodes> Reference_Cols = SqlKind.Reference.nodesField("Columns"); // ColName

  //// IndexDef
  FieldKey<String> IndexDef_Name = SqlKind.IndexDef.textField("Name");
  FieldKey<SqlNode> IndexDef_Table = SqlKind.IndexDef.nodeField("Table"); // TableName
  FieldKey<IndexKind> IndexDef_Kind = SqlKind.IndexDef.field("Kind", IndexKind.class);
  FieldKey<ConstraintKind> IndexDef_Cons =
      SqlKind.IndexDef.field("Constraint", ConstraintKind.class);
  FieldKey<SqlNodes> IndexDef_Keys = SqlKind.IndexDef.nodesField("Keys"); // KeyPart
  FieldKey<SqlNode> IndexDef_Refs = SqlKind.IndexDef.nodeField("References"); // References

  //// KeyPart
  FieldKey<String> KeyPart_Col = SqlKind.KeyPart.textField("Column");
  FieldKey<Integer> KeyPart_Len = SqlKind.KeyPart.field("Length", Integer.class);
  FieldKey<SqlNode> KeyPart_Expr = SqlKind.KeyPart.nodeField("Expr"); // Expr
  FieldKey<KeyDirection> KeyPart_Direction = SqlKind.KeyPart.field("Direction", KeyDirection.class);

  //// Union
  FieldKey<SqlNode> SetOp_Left = SqlKind.SetOp.nodeField("Left"); // Query
  FieldKey<SqlNode> SetOp_Right = SqlKind.SetOp.nodeField("Right"); // Query
  FieldKey<SetOpKind> SetOp_Kind = SqlKind.SetOp.field("Kind", SetOpKind.class);
  FieldKey<SetOpOption> SetOp_Option = SqlKind.SetOp.field("Option", SetOpOption.class);

  //// Query
  FieldKey<SqlNode> Query_Body = SqlKind.Query.nodeField("Body"); // QuerySpec
  FieldKey<SqlNodes> Query_OrderBy = SqlKind.Query.nodesField("OrderBy"); // OrderItem
  FieldKey<SqlNode> Query_Limit = SqlKind.Query.nodeField("Limit"); // Offset
  FieldKey<SqlNode> Query_Offset = SqlKind.Query.nodeField("Offset"); // Offset

  //// QuerySpec
  FieldKey<Boolean> QuerySpec_Distinct = SqlKind.QuerySpec.boolField("Distinct");
  FieldKey<SqlNodes> QuerySpec_DistinctOn = SqlKind.QuerySpec.nodesField("DistinctOn"); // Expr
  FieldKey<SqlNodes> QuerySpec_SelectItems =
      SqlKind.QuerySpec.nodesField("SelectItem"); // SelectItem
  FieldKey<SqlNode> QuerySpec_From = SqlKind.QuerySpec.nodeField("From"); // TableSource
  FieldKey<SqlNode> QuerySpec_Where = SqlKind.QuerySpec.nodeField("Where"); // Expr
  FieldKey<SqlNodes> QuerySpec_GroupBy = SqlKind.QuerySpec.nodesField("GroupBy"); // GroupItem
  FieldKey<OLAPOption> QuerySpec_OlapOption =
      SqlKind.QuerySpec.field("OlapOption", OLAPOption.class);
  FieldKey<SqlNode> QuerySpec_Having = SqlKind.QuerySpec.nodeField("Having"); // Expr
  FieldKey<SqlNodes> QuerySpec_Windows = SqlKind.QuerySpec.nodesField("Windows"); // WindowSpec

  //// SelectItem
  FieldKey<SqlNode> SelectItem_Expr = SqlKind.SelectItem.nodeField("Expr"); // Expr
  FieldKey<String> SelectItem_Alias = SqlKind.SelectItem.textField("Alias");

  //// OrderItem
  FieldKey<SqlNode> OrderItem_Expr = SqlKind.OrderItem.nodeField("Expr"); // Expr
  FieldKey<KeyDirection> OrderItem_Direction =
      SqlKind.OrderItem.field("Direction", KeyDirection.class);

  //// GroupItem
  FieldKey<SqlNode> GroupItem_Expr = SqlKind.GroupItem.nodeField("Expr"); // Expr

  //// WindowSpec
  FieldKey<String> WindowSpec_Alias = SqlKind.WindowSpec.textField("Alias");
  FieldKey<String> WindowSpec_Name = SqlKind.WindowSpec.textField("Name");
  FieldKey<SqlNodes> WindowSpec_Part = SqlKind.WindowSpec.nodesField("Partition"); // WindowSpec
  FieldKey<SqlNodes> WindowSpec_Order = SqlKind.WindowSpec.nodesField("Order"); // OrderItem
  FieldKey<SqlNode> WindowSpec_Frame = SqlKind.WindowSpec.nodeField("Frame"); // WindowFrame

  //// WindowFrame
  FieldKey<WindowUnit> WindowFrame_Unit = SqlKind.WindowFrame.field("Unit", WindowUnit.class);
  FieldKey<SqlNode> WindowFrame_Start = SqlKind.WindowFrame.nodeField("Start"); // FrameBound
  FieldKey<SqlNode> WindowFrame_End = SqlKind.WindowFrame.nodeField("End"); // FrameBound
  FieldKey<WindowExclusion> WindowFrame_Exclusion =
      SqlKind.WindowFrame.field("Exclusion", WindowExclusion.class);

  //// FrameBound
  FieldKey<SqlNode> FrameBound_Expr = SqlKind.FrameBound.nodeField("Expr"); // Expr
  FieldKey<FrameBoundDirection> FrameBound_Direction =
      SqlKind.FrameBound.field("Direction", FrameBoundDirection.class);

  //// IndexHint
  FieldKey<IndexHintType> IndexHint_Kind = SqlKind.IndexHint.field("Kind", IndexHintType.class);
  FieldKey<IndexHintTarget> IndexHint_Target =
      SqlKind.IndexHint.field("Target", IndexHintTarget.class);
  FieldKey<List<String>> IndexHint_Names = SqlKind.IndexHint.field("Names", List.class);

  //// Statement
  FieldKey<StmtType> Statement_Kind = SqlKind.Statement.field("Kind", StmtType.class);
  FieldKey<SqlNode> Statement_Body = SqlKind.Statement.nodeField("Body"); // Any

  //// AlterSequence
  FieldKey<SqlNode> AlterSeq_Name = SqlKind.AlterSeq.nodeField("Name"); // Name2
  FieldKey<String> AlterSeq_Op = SqlKind.AlterSeq.textField("Op");
  FieldKey<Object> AlterSeq_Payload = SqlKind.AlterSeq.field("Payload", Object.class);

  //// AlterTable
  FieldKey<SqlNode> AlterTable_Name = SqlKind.AlterTable.nodeField("Name");
  FieldKey<SqlNodes> AlterTable_Actions = SqlKind.AlterTable.nodesField("Actions");

  //// AlterTableAction
  FieldKey<String> AlterTableAction_Name = SqlKind.AlterTableAction.textField("Name");
  FieldKey<Object> AlterTableAction_Payload =
      SqlKind.AlterTableAction.field("Payload", Object.class);

  //// Expr
  FieldKey<ExprKind> Expr_Kind = SqlKind.Expr.field("Kind", ExprKind.class);
  // for named argument in PG
  FieldKey<String> Expr_ArgName = SqlKind.Expr.textField("ArgName");
  FieldKey<Boolean> Expr_FuncArgVariadic = SqlKind.Expr.boolField("Variadic");

  //// TableSource
  FieldKey<TableSourceKind> TableSource_Kind =
      SqlKind.TableSource.field("Kind", TableSourceKind.class);
}
