package sqlsolver.sql.util;

import org.apache.calcite.rel.RelNode;
import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.Category;
import sqlsolver.sql.ast.constants.DataTypeName;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.plan.*;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Table;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.plan.*;

import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;

public class CastRemover {
  /** Remove useless CASTs of <code>plan</code> in place. */
  public static void removeUselessCast(PlanContext plan) {
    new CastRemover().removeUselessCast(plan, plan.root());
  }

  public static String removeUselessCastNull(String sql) {
    sql = sql.replaceAll("CAST\\(NULL AS (SIGNED|CHAR\\(\\d+\\)|VARCHAR\\(\\d+\\)|TIMESTAMP\\(\\d+\\)|BOOLEAN)\\)", "NULL");
    return sql;
  }

  private boolean removeUselessCast(PlanContext plan, int nodeId) {
    if (nodeId == NO_SUCH_NODE) {
      return true;
    }
    PlanNode node = plan.nodeAt(nodeId);
    if (node instanceof Exporter exporter) {
      List<Expression> attrExprs = exporter.attrExprs();
      // iterate over all attribute expressions
      for (Expression expr : attrExprs) {
        removeUselessCast(plan, expr);
      }
    } else if (node instanceof JoinNode join) {
      removeUselessCast(plan, join.joinCond());
    } else if (node instanceof SimpleFilterNode filter) {
      removeUselessCast(plan, filter.predicate());
    }
    // recursion
    int numChildren = plan.nodeAt(nodeId).numChildren(plan);
    int[] children = plan.childrenOf(nodeId);
    assert numChildren <= children.length;
    for (int i = 0; i < numChildren; ++i) {
      if (children[i] != NO_SUCH_NODE) {
        if (!removeUselessCast(plan, children[i])) {
          return false;
        }
      }
    }
    return true;
  }

  /** It removes CASTs from an expression. */
  private void removeUselessCast(PlanContext plan, Expression expr) {
    if (expr == null) return;
    // it updates "template" of expr in place
    SqlNode node = expr.template();
    SqlNode node0 = removeUselessCast(plan, expr, node);
    if (node != node0) {
      expr.setTemplate(node0);
    }
  }

  private void removeCastOfField(PlanContext plan, Expression expr, SqlNode node, FieldKey<SqlNode> key) {
    SqlNode oldValue = node.field(key);
    if (oldValue == null) return;
    SqlNode value = removeUselessCast(plan, expr, oldValue);
    // detach to prevent exception during setField check
    value.context().setParentOf(value.nodeId(), NO_SUCH_NODE);
    if (value != oldValue)
      node.setField(key, value);
  }

  private SqlNode removeUselessCast(PlanContext plan, Expression expr, SqlNode node) {
    // TODO: this is a partial impl;
    //   it only handles aggregates & removes outermost CASTs;
    //   it does not handle CASTs contained by CASTs.
    // node stands for an expression to be processed.
    // If the type of expression to be casted is the same
    //   as the target type, remove CAST.
    ExprKind kind = node.field(SqlNodeFields.Expr_Kind);
    if (ExprKind.Cast.equals(kind)) {
      SqlNode innerNode = node.field(ExprFields.Cast_Expr);
      SqlDataType castType = node.field(ExprFields.Cast_Type);
      boolean useless = checkType(plan, expr, innerNode, castType);
      if (useless)
        return innerNode;
    } else if (ExprKind.Aggregate.equals(kind)) {
      SqlNodes args = node.field(ExprFields.Aggregate_Args);
      if (args != null) {
        args.set(0, removeUselessCast(plan, expr, args.get(0)));
      }
      return node;
    } else if (ExprKind.Case.equals(kind)) {
      SqlNodes whens = node.field(ExprFields.Case_Whens);
      for (SqlNode when : whens) {
        removeCastOfField(plan, expr, when, ExprFields.When_Cond);
        removeCastOfField(plan, expr, when, ExprFields.When_Expr);
      }
      removeCastOfField(plan, expr, node, ExprFields.Case_Else);
      return node;
    } else if (ExprKind.Binary.equals(kind)) {
      removeCastOfField(plan, expr, node, ExprFields.Binary_Left);
      removeCastOfField(plan, expr, node, ExprFields.Binary_Right);
      return node;
    }
    // other cases are currently by-passed
    return node;
  }

  // To track expr's type through sub-queries:
  // 1. values = plan.valuesReg.exprRefs[expr]
  //      where values[i] corresponds to expr.internalRefs[i]
  //      and internalRefs (ColRef.Column) appear in the template
  // 2. subexpr (like t6.DEPTNO) = plan.valuesReg.valueExprs[values[i]]
  // ...
  // until subexpr is a constant or column of input (like EMP.DEPTNO)

  /** acquire the actual column reference (table.name) of node */
  private Value getColRef(PlanContext plan, Expression expr, SqlNode node) {
    assert ExprKind.ColRef.equals(node.field(SqlNodeFields.Expr_Kind));
    SqlNode nodeColumn = node.field(ExprFields.ColRef_ColName);
    // find nodeColumn in internalRefs & get its index
    int index = 0;
    for (SqlNode ref : expr.internalRefs()) {
      SqlNode refColumn = ref.field(ExprFields.ColRef_ColName);
      if (nodeColumn == refColumn) break;
      index++;
    }
    assert index < expr.internalRefs().size();
    // get the actual column reference = exprRefs[expr][index]
    Values refs = plan.valuesReg().valueRefsOf(expr);
    return refs.get(index);
  }

  private Table findSourceTable(PlanContext plan, Value colRef) {
    // direct reference?
    Table table = plan.schema().table(colRef.qualification().toLowerCase());
    if (table != null) return table;
    // aliased reference?
    Column srcColumn = plan.valuesReg().columnOf(colRef);
    if (srcColumn == null) return null; // unknown reference
    return plan.schema().table(srcColumn.tableName().toLowerCase());
  }

  private boolean isConversionSafe(SqlDataType t1, SqlDataType t2) {
    // partial impl
    if (t1.category().equals(t2.category())) {
      if (t1.category().equals(Category.INTEGRAL)) {
        // sound condition, other cases might be OR-ed later
        return t1.storageSize() == t2.storageSize();
      }
    }
    if (t1.category().equals(Category.INTEGRAL)
            && t2.name().equals(DataTypeName.DECIMAL)) {
      // in terms of MySQL, SQL Server & PostgreSQL
      // this conversion does not lose precision in the default case
      // (i.e. INT 4-byte, DECIMAL with default precision & scale)
      // here, width=precision, precision=scale
      int intSize = t1.storageSize();
      int precision = t2.width(), scale = t2.precision();
      // sound condition, other cases might be OR-ed later
      return (precision == -1 && scale == -1 && intSize <= 4);
    }
    return false;
  }

  /** whether node's conversion to type is unnecessary
   *  (i.e. removing CAST does not change
   *  the original query's functionality) */
  private boolean checkType(PlanContext plan, Expression expr, SqlNode node, SqlDataType type) {
    // TODO: check expr type before removing CAST
    ExprKind kind = node.field(SqlNodeFields.Expr_Kind);
    if (ExprKind.Literal.equals(kind)) {
      // NULL can be of any type
      if (LiteralKind.NULL.equals(node.field(ExprFields.Literal_Kind)))
        return true;
    } else if (ExprKind.ColRef.equals(kind)) {
      // acquire the column reference (table.name)
      Value ref = getColRef(plan, expr, node);
      // if it is an indirect reference (e.g. t2.DEPTNO),
      // find its upstream (e.g. what t2 refers to)
      Expression srcExpr = plan.valuesReg().exprOf(ref);
      if (srcExpr != null) {
        return checkType(plan, srcExpr, srcExpr.template(), type);
      } else {
        // if it is in the schema (e.g. EMP.DEPTNO), fetch its type
        Table table = findSourceTable(plan, ref);
        assert table != null;
        return isConversionSafe(table.column(ref.name()).dataType(), type);
      }
    } else if (ExprKind.Aggregate.equals(kind)) {
      String aggName = node.field(ExprFields.Aggregate_Name);
      if ("min".equalsIgnoreCase(aggName) || "sum".equalsIgnoreCase(aggName)) {
        SqlNodes args = node.field(ExprFields.Aggregate_Args);
        assert args.size() == 1;
        // the arg should have the expected type
        return checkType(plan, expr, args.get(0), type);
      } else if ("count".equalsIgnoreCase(aggName)) {
        // "COUNT" must be integral and ignore the inner expression type
        return type.category().equals(Category.INTEGRAL);
      }
    } else if (ExprKind.Case.equals(kind)) {
      // check types of THEN & ELSE
      SqlNodes whens = node.field(ExprFields.Case_Whens);
      for (SqlNode when : whens) {
        SqlNode whenExpr = when.field(ExprFields.When_Expr);
        if (!checkType(plan, expr, whenExpr, type)) return false;
      }
      SqlNode els = node.field(ExprFields.Case_Else);
      return checkType(plan, expr, els, type);
    } else if (ExprKind.Binary.equals(kind)) {
      BinaryOpKind opKind = node.field(ExprFields.Binary_Op);
      if (opKind == BinaryOpKind.IS) {
        return type.category().equals(Category.INTEGRAL);
      } else if (opKind == BinaryOpKind.MULT) {
        if (type.category().equals(Category.INTEGRAL)) {
          SqlNode left = node.field(ExprFields.Binary_Left);
          SqlNode right = node.field(ExprFields.Binary_Right);
          return checkType(plan, expr, left, type) && checkType(plan, expr, right, type);
        }
      }
    }
    // currently does not support other kinds of expressions
    // it is a partial impl
    return false;
  }
}
