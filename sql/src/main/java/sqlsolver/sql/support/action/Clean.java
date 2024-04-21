package sqlsolver.sql.support.action;

import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;

import java.util.List;

import static sqlsolver.common.tree.TreeSupport.nodeEquals;
import static sqlsolver.common.utils.Commons.joining;
import static sqlsolver.common.utils.FuncSupport.pred;
import static sqlsolver.common.utils.IterableSupport.all;
import static sqlsolver.sql.ast.SqlNodeFields.*;

class Clean {
  static void clean(SqlNode root) {
    SqlNode target;
    boolean flag = true;
    while ((target = LocatorSupport.nodeLocator().accept(Clean::isBoolConstant).find(root)) != null && flag)
      flag = deleteBoolConstant(target);

    for (SqlNode node : LocatorSupport.nodeLocator().accept(Clean::isTextFunc).gather(root))
      inlineTextConstant(node);
  }

  private static boolean isBoolConstant(SqlNode node) {
    final SqlNode parent = node.parent();
    return parent != null
        && (nodeEquals(node, parent.$(QuerySpec_Where))
            || ExprKind.Binary.isInstance(parent) && parent.$(ExprFields.Binary_Op).isLogic())
        && NormalizationSupport.isConstant(node);
  }

  private static boolean deleteBoolConstant(SqlNode node) {
    final SqlNode parent = node.parent();
    final SqlNode grandParent = parent.parent();

    if (nodeEquals(node, parent.$(QuerySpec_Where)) && node.$(ExprFields.Literal_Value)!=null && (Boolean) node.$(ExprFields.Literal_Value)) {
      parent.remove(QuerySpec_Where);
      return true;

    } else if (ExprKind.Binary.isInstance(parent) && parent.$(ExprFields.Binary_Op).isLogic()) {
      final SqlNode lhs = parent.$(ExprFields.Binary_Left), rhs = parent.$(ExprFields.Binary_Right);
      // TRUE <=> (... OR NULL) => TRUE <=> (...)
      if(ExprKind.Binary.isInstance(grandParent)
              && grandParent.$(ExprFields.Binary_Op).precedence() == BinaryOpKind.NULL_SAFE_EQUAL.precedence()) {
        final SqlNode glhs = grandParent.$(ExprFields.Binary_Left), grhs = grandParent.$(ExprFields.Binary_Right);
        if((nodeEquals(glhs, parent) && grhs.$(ExprFields.Literal_Value)!=null && (Boolean) grhs.$(ExprFields.Literal_Value))
           || (nodeEquals(grhs, parent) && glhs.$(ExprFields.Literal_Value)!=null && (Boolean) glhs.$(ExprFields.Literal_Value))) {
          // replacement
          if (nodeEquals(lhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.OR &&
                  lhs.$(Expr_Kind) == ExprKind.Literal && lhs.$(ExprFields.Literal_Value) == null) {
            node.context().displaceNode(parent.nodeId(), rhs.nodeId());
            return true;
          }
          if (nodeEquals(rhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.OR &&
                  rhs.$(Expr_Kind) == ExprKind.Literal && rhs.$(ExprFields.Literal_Value) == null) {
            node.context().displaceNode(parent.nodeId(), lhs.nodeId());
            return true;
          }
        }
      }

      if (nodeEquals(lhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.OR &&
              lhs.$(ExprFields.Literal_Value)!=null && (Boolean) lhs.$(ExprFields.Literal_Value) ) {
        node.context().displaceNode(parent.nodeId(), lhs.nodeId());
        return true;
      }
      if (nodeEquals(lhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.AND &&
              lhs.$(ExprFields.Literal_Value)!=null && !(Boolean) lhs.$(ExprFields.Literal_Value) ) {
        node.context().displaceNode(parent.nodeId(), lhs.nodeId());
        return true;
      }
      if (nodeEquals(rhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.OR &&
              rhs.$(ExprFields.Literal_Value)!=null && (Boolean) rhs.$(ExprFields.Literal_Value) ) {
        node.context().displaceNode(parent.nodeId(), rhs.nodeId());
        return true;
      }
      if (nodeEquals(rhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.AND &&
              rhs.$(ExprFields.Literal_Value)!=null && !(Boolean) rhs.$(ExprFields.Literal_Value) ) {
        node.context().displaceNode(parent.nodeId(), rhs.nodeId());
        return true;
      }
      // TRUE AND NULL -> NULL
      // FALSE AND NULL -> FALSE
      if (nodeEquals(lhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.AND &&
              rhs.$(ExprFields.Literal_Value)!=null && (Boolean) rhs.$(ExprFields.Literal_Value) &&
              lhs.$(Expr_Kind) == ExprKind.Literal && lhs.$(ExprFields.Literal_Value) == null) {
        node.context().displaceNode(parent.nodeId(), lhs.nodeId());
        return true;
      }
      if (nodeEquals(rhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.AND &&
              lhs.$(ExprFields.Literal_Value)!=null && (Boolean) lhs.$(ExprFields.Literal_Value) &&
              rhs.$(Expr_Kind) == ExprKind.Literal && rhs.$(ExprFields.Literal_Value) == null) {
        node.context().displaceNode(parent.nodeId(), rhs.nodeId());
        return true;
      }
      if (nodeEquals(lhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.AND &&
              rhs.$(ExprFields.Literal_Value)!=null && !(Boolean) rhs.$(ExprFields.Literal_Value) &&
              lhs.$(Expr_Kind) == ExprKind.Literal && lhs.$(ExprFields.Literal_Value) == null) {
        node.context().displaceNode(parent.nodeId(), rhs.nodeId());
        return true;
      }
      if (nodeEquals(rhs, node) && parent.$(ExprFields.Binary_Op) == BinaryOpKind.AND &&
              lhs.$(ExprFields.Literal_Value)!=null && !(Boolean) lhs.$(ExprFields.Literal_Value) &&
              rhs.$(Expr_Kind) == ExprKind.Literal && rhs.$(ExprFields.Literal_Value) == null) {
        node.context().displaceNode(parent.nodeId(), lhs.nodeId());
        return true;
      }
//      if (nodeEquals(lhs, node)) {
//        node.context().displaceNode(parent.nodeId(), rhs.nodeId());
//        return true;
//      }
//      else if (nodeEquals(rhs, node)) {
//        node.context().displaceNode(parent.nodeId(), lhs.nodeId());
//        return true;
//      }
    }
    return false;
  }

  private static boolean isTextFunc(SqlNode node) {
    final SqlNode name = node.$(ExprFields.FuncCall_Name);
    if (name == null || name.$(Name2_0) != null) return false; // UDF

    final String funcName = name.$(Name2_1).toLowerCase();
    final List<SqlNode> args = node.$(ExprFields.FuncCall_Args);

    switch (funcName) {
      case "concat":
        return all(args, pred(ExprKind.Literal::isInstance).or(Clean::isTextFunc));
      case "lower":
      case "upper":
        return args.size() == 1 && (ExprKind.Literal.isInstance(args.get(0)) || isTextFunc(args.get(0)));
      default:
        return false;
    }
  }

  private static void inlineTextConstant(SqlNode funcCall) {
    final SqlNode literal = SqlSupport.mkLiteral(funcCall.context(), LiteralKind.TEXT, stringify(funcCall));
    funcCall.context().displaceNode(funcCall.nodeId(), literal.nodeId());
  }

  private static String stringify(SqlNode node) {
    if (ExprKind.Literal.isInstance(node)) return String.valueOf(node.$(ExprFields.Literal_Value));
    assert ExprKind.FuncCall.isInstance(node);

    final String funcName = node.$(ExprFields.FuncCall_Name).$(Name2_1).toLowerCase();
    final SqlNodes args = node.$(ExprFields.FuncCall_Args);

    switch (funcName) {
      case "concat":
        return joining("", args, Clean::stringify);
      case "upper":
        return stringify(args.get(0)).toUpperCase();
      case "lower":
        return stringify(args.get(0)).toLowerCase();
      default:
        assert false;
        return "";
    }
  }
}
