package sqlsolver.sql.ast;


import sqlsolver.common.field.FieldKey;

interface SqlVisitorDriver {
  static boolean enter(SqlNode n, SqlVisitor v) {
    if (n == null) return false;
    if (!v.enter(n)) return false;

    return switch (n.kind()) {
      case Expr -> enterExpr(n, v);
      case TableSource -> enterTableSource(n, v);
      case TableName -> v.enterTableName(n);
      case ColName -> v.enterColumnName(n);
      case CreateTable -> v.enterCreateTable(n);
      case ColDef -> v.enterColumnDef(n);
      case Reference -> v.enterReferences(n);
      case IndexDef -> v.enterIndexDef(n);
      case KeyPart -> v.enterKeyPart(n);
      case WindowSpec -> v.enterWindowSpec(n);
      case WindowFrame -> v.enterWindowFrame(n);
      case FrameBound -> v.enterFrameBound(n);
      case GroupItem -> v.enterGroupItem(n);
      case OrderItem -> v.enterOrderItem(n);
      case SelectItem -> v.enterSelectItem(n);
      case IndexHint -> v.enterIndexHint(n);
      case QuerySpec -> v.enterQuerySpec(n);
      case Query -> v.enterQuery(n);
      case SetOp -> v.enterSetOp(n);
      case Name2 -> v.enterName2(n);
      case Name3 -> v.enterCommonName(n);
      default -> false;
    };

  }

  static void visitChildren(SqlNode n, SqlVisitor v) {
    switch (n.kind()) {
      case Expr -> visitExprChildren(n, v);
      case TableSource -> visitTableSourceChildren(n, v);
      case CreateTable -> {
        safeVisitChild(SqlNodeFields.CreateTable_Name, n, v);
        safeVisitList(SqlNodeFields.CreateTable_Cols, n, v);
        safeVisitList(SqlNodeFields.CreateTable_Cons, n, v);
      }
      case ColDef -> {
        safeVisitChild(SqlNodeFields.ColDef_Name, n, v);
        safeVisitChild(SqlNodeFields.ColDef_Ref, n, v);
      }
      case Reference -> {
        safeVisitChild(SqlNodeFields.Reference_Table, n, v);
        safeVisitList(SqlNodeFields.Reference_Cols, n, v);
      }
      case IndexDef -> {
        safeVisitList(SqlNodeFields.IndexDef_Keys, n, v);
        safeVisitChild(SqlNodeFields.IndexDef_Refs, n, v);
      }
      case WindowSpec -> {
        safeVisitList(SqlNodeFields.WindowSpec_Part, n, v);
        safeVisitList(SqlNodeFields.WindowSpec_Order, n, v);
        safeVisitChild(SqlNodeFields.WindowSpec_Frame, n, v);
      }
      case WindowFrame -> {
        safeVisitChild(SqlNodeFields.WindowFrame_Start, n, v);
        safeVisitChild(SqlNodeFields.WindowFrame_End, n, v);
      }
      case FrameBound -> safeVisitChild(SqlNodeFields.FrameBound_Expr, n, v);
      case OrderItem -> safeVisitChild(SqlNodeFields.OrderItem_Expr, n, v);
      case GroupItem -> safeVisitChild(SqlNodeFields.GroupItem_Expr, n, v);
      case SelectItem -> safeVisitChild(SqlNodeFields.SelectItem_Expr, n, v);
      case QuerySpec -> {
        safeVisitList(SqlNodeFields.QuerySpec_DistinctOn, n, v);
        safeVisitList(SqlNodeFields.QuerySpec_SelectItems, n, v);
        safeVisitChild(SqlNodeFields.QuerySpec_From, n, v);
        safeVisitChild(SqlNodeFields.QuerySpec_Where, n, v);
        safeVisitList(SqlNodeFields.QuerySpec_GroupBy, n, v);
        safeVisitChild(SqlNodeFields.QuerySpec_Having, n, v);
        safeVisitList(SqlNodeFields.QuerySpec_Windows, n, v);
      }
      case Query -> {
        safeVisitChild(SqlNodeFields.Query_Body, n, v);
        safeVisitList(SqlNodeFields.Query_OrderBy, n, v);
        safeVisitChild(SqlNodeFields.Query_Offset, n, v);
        safeVisitChild(SqlNodeFields.Query_Limit, n, v);
      }
      case SetOp -> {
        safeVisitChild(SqlNodeFields.SetOp_Left, n, v);
        safeVisitChild(SqlNodeFields.SetOp_Right, n, v);
      }
    }
  }

  static void leave(SqlNode n, SqlVisitor v) {
    if (n == null) return;
    switch (n.kind()) {
      case Invalid:
        break;

      case Expr:
        leaveExpr(n, v);
        break;

      case TableSource:
        leaveTableSource(n, v);

      case TableName:
        v.leaveTableName(n);
        break;

      case ColName:
        v.leaveColumnName(n);
        break;

      case Name2:
        v.leaveName2(n);
        break;

      case Name3:
        v.leaveCommonName(n);
        return;

      case CreateTable:
        v.leaveCreateTable(n);
        break;

      case ColDef:
        v.leaveColumnDef(n);
        break;

      case Reference:
        v.leaveReferences(n);
        break;

      case IndexDef:
        v.leaveIndexDef(n);
        break;

      case KeyPart:
        v.leaveKeyPart(n);
        break;

      case WindowSpec:
        v.leaveWindowSpec(n);
        break;

      case WindowFrame:
        v.leaveWindowFrame(n);
        break;

      case FrameBound:
        v.leaveFrameBound(n);
        break;

      case OrderItem:
        v.leaveOrderItem(n);
        break;

      case GroupItem:
        v.leaveGroupItem(n);
        break;

      case SelectItem:
        v.leaveSelectItem(n);
        break;

      case IndexHint:
        v.leaveIndexHint(n);
        break;

      case QuerySpec:
        v.leaveQuerySpec(n);
        break;

      case Query:
        v.leaveQuery(n);
        break;

      case SetOp:
        v.leaveSetOp(n);
        break;
    }

    v.leave(n);
  }

  private static void safeAccept(SqlNode n, SqlVisitor v) {
    if (n != null) n.accept(v);
  }

  private static void safeVisitChild(FieldKey<SqlNode> key, SqlNode n, SqlVisitor v) {
    final SqlNode child = n.$(key);
    if (v.enterChild(n, key, child)) safeAccept(child, v);
    v.leaveChild(n, key, child);
  }

  private static void safeVisitList(FieldKey<SqlNodes> key, SqlNode n, SqlVisitor v) {
    final SqlNodes children = n.$(key);
    if (v.enterChildren(n, key, children))
      if (children != null) for (SqlNode child : children) safeAccept(child, v);
    v.leaveChildren(n, key, children);
  }

  private static boolean enterExpr(SqlNode n, SqlVisitor v) {
    assert SqlKind.Expr.isInstance(n);

    switch (n.$(SqlNodeFields.Expr_Kind)) {
      case Variable:
        return v.enterVariable(n);
      case ColRef:
        return v.enterColumnRef(n);
      case Literal:
        return v.enterLiteral(n);
      case FuncCall:
        return v.enterFuncCall(n);
      case Collate:
        return v.enterCollation(n);
      case Param:
        return v.enterParamMarker(n);
      case Unary:
        return v.enterUnary(n);
      case GroupingOp:
        return v.enterGroupingOp(n);
      case Tuple:
        return v.enterTuple(n);
      case Match:
        return v.enterMatch(n);
      case Cast:
        return v.enterCast(n);
      case Symbol:
        return v.enterSymbol(n);
      case Default:
        return v.enterDefault(n);
      case Values:
        return v.enterValues(n);
      case Interval:
        return v.enterInterval(n);
      case Exists:
        return v.enterExists(n);
      case QueryExpr:
        return v.enterQueryExpr(n);
      case Wildcard:
        return v.enterWildcard(n);
      case Aggregate:
        return v.enterAggregate(n);
      case ConvertUsing:
        return v.enterConvertUsing(n);
      case Case:
        return v.enterCase(n);
      case When:
        return v.enterWhen(n);
      case Binary:
        return v.enterBinary(n);
      case Ternary:
        return v.enterTernary(n);
      case Indirection:
        return v.enterIndirection(n);
      case IndirectionComp:
        return v.enterIndirectionComp(n);
      case Array:
        return v.enterArray(n);
      case Unknown:
    }

    return false;
  }

  private static boolean enterTableSource(SqlNode n, SqlVisitor v) {
    assert SqlKind.TableSource.isInstance(n);

    return switch (n.$(SqlNodeFields.TableSource_Kind)) {
      case SimpleSource -> v.enterSimpleTableSource(n);
      case JoinedSource -> v.enterJoinedTableSource(n);
      case DerivedSource -> v.enterDerivedTableSource(n);
    };

  }

  private static void visitExprChildren(SqlNode n, SqlVisitor v) {
    assert SqlKind.Expr.isInstance(n);
    switch (n.$(SqlNodeFields.Expr_Kind)) {
      case Variable -> safeVisitChild(ExprFields.Variable_Assignment, n, v);
      case ColRef -> safeVisitChild(ExprFields.ColRef_ColName, n, v);
      case FuncCall -> safeVisitList(ExprFields.FuncCall_Args, n, v);
      case Collate -> safeVisitChild(ExprFields.Collate_Expr, n, v);
      case Unary -> safeVisitChild(ExprFields.Unary_Expr, n, v);
      case GroupingOp -> safeVisitList(ExprFields.GroupingOp_Exprs, n, v);
      case Tuple -> safeVisitList(ExprFields.Tuple_Exprs, n, v);
      case Match -> {
        safeVisitList(ExprFields.Match_Cols, n, v);
        safeVisitChild(ExprFields.Match_Expr, n, v);
      }
      case Cast -> safeVisitChild(ExprFields.Cast_Expr, n, v);
      case Default -> safeVisitChild(ExprFields.Default_Col, n, v);
      case Values -> safeVisitChild(ExprFields.Values_Expr, n, v);
      case Interval -> safeVisitChild(ExprFields.Interval_Expr, n, v);
      case Exists -> safeVisitChild(ExprFields.Exists_Subquery, n, v);
      case QueryExpr -> safeVisitChild(ExprFields.QueryExpr_Query, n, v);
      case Aggregate -> {
        safeVisitList(ExprFields.Aggregate_Args, n, v);
        safeVisitList(ExprFields.Aggregate_Order, n, v);
        safeVisitChild(ExprFields.Aggregate_WindowSpec, n, v);
      }
      case ConvertUsing -> safeVisitChild(ExprFields.ConvertUsing_Expr, n, v);
      case Case -> {
        safeVisitChild(ExprFields.Case_Cond, n, v);
        safeVisitList(ExprFields.Case_Whens, n, v);
        safeVisitChild(ExprFields.Case_Else, n, v);
      }
      case When -> {
        safeVisitChild(ExprFields.When_Cond, n, v);
        safeVisitChild(ExprFields.When_Expr, n, v);
      }
      case Binary -> {
        safeVisitChild(ExprFields.Binary_Left, n, v);
        safeVisitChild(ExprFields.Binary_Right, n, v);
      }
      case Ternary -> {
        safeVisitChild(ExprFields.Ternary_Left, n, v);
        safeVisitChild(ExprFields.Ternary_Middle, n, v);
        safeVisitChild(ExprFields.Ternary_Right, n, v);
      }
      case Wildcard -> safeVisitChild(ExprFields.Wildcard_Table, n, v);
      case Indirection -> {
        safeVisitChild(ExprFields.Interval_Expr, n, v);
        safeVisitList(ExprFields.Indirection_Comps, n, v);
      }
      case IndirectionComp -> {
        safeVisitChild(ExprFields.IndirectionComp_Start, n, v);
        safeVisitChild(ExprFields.IndirectionComp_End, n, v);
      }
      case Array -> safeVisitList(ExprFields.Array_Elements, n, v);
    }
  }

  private static void visitTableSourceChildren(SqlNode n, SqlVisitor v) {
    assert SqlKind.TableSource.isInstance(n);

    switch (n.$(SqlNodeFields.TableSource_Kind)) {
      case SimpleSource -> {
        safeVisitChild(TableSourceFields.Simple_Table, n, v);
        safeVisitList(TableSourceFields.Simple_Hints, n, v);
      }
      case JoinedSource -> {
        safeVisitChild(TableSourceFields.Joined_Left, n, v);
        safeVisitChild(TableSourceFields.Joined_Right, n, v);
        safeVisitChild(TableSourceFields.Joined_On, n, v);
      }
      case DerivedSource -> safeVisitChild(TableSourceFields.Derived_Subquery, n, v);
    }
  }

  private static void leaveExpr(SqlNode n, SqlVisitor v) {
    assert SqlKind.Expr.isInstance(n);

    switch (n.$(SqlNodeFields.Expr_Kind)) {
      case Variable:
        v.leaveColumnDef(n);
        return;

      case ColRef:
        v.leaveColumnRef(n);
        return;

      case FuncCall:
        v.leaveFuncCall(n);

      case Literal:
        v.leaveLiteral(n);
        return;

      case Collate:
        v.leaveCollation(n);
        return;

      case Param:
        v.leaveParamMarker(n);
        return;

      case Unary:
        v.leaveUnary(n);
        return;

      case GroupingOp:
        v.leaveGroupingOp(n);
        return;

      case Tuple:
        v.leaveTuple(n);
        return;

      case Match:
        v.leaveMatch(n);
        return;

      case Cast:
        v.leaveCast(n);
        return;

      case Symbol:
        v.leaveSymbol(n);
        return;

      case Default:
        v.leaveDefault(n);
        return;

      case Values:
        v.leaveValues(n);
        return;

      case Interval:
        v.leaveInterval(n);
        return;

      case Exists:
        v.leaveExists(n);
        return;

      case QueryExpr:
        v.leaveQueryExpr(n);
        return;

      case Wildcard:
        v.leaveWildcard(n);
        return;

      case Aggregate:
        v.leaveAggregate(n);
        return;

      case ConvertUsing:
        v.leaveConvertUsing(n);
        return;

      case Case:
        v.leaveCase(n);
        return;

      case When:
        v.leaveWhen(n);
        return;

      case Binary:
        v.leaveBinary(n);
        return;

      case Ternary:
        v.leaveTernary(n);
        return;

      case Indirection:
        v.leaveIndirection(n);
        return;

      case IndirectionComp:
        v.leaveIndirectionComp(n);
        return;

      case Array:
        v.leaveArray(n);
        return;

      case Unknown:
    }
  }

  private static void leaveTableSource(SqlNode n, SqlVisitor v) {
    assert SqlKind.TableSource.isInstance(n);

    switch (n.$(SqlNodeFields.TableSource_Kind)) {
      case SimpleSource -> v.leaveSimpleTableSource(n);
      case JoinedSource -> v.leaveJoinedTableSource(n);
      case DerivedSource -> v.leaveDerivedTableSource(n);
    }
  }
}
