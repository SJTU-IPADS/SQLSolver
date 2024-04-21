package sqlsolver.sql.support.resolution;

import com.google.common.collect.Lists;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.Category;
import sqlsolver.sql.ast.constants.UnaryOpKind;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.support.locator.LocatorSupport;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;
import static sqlsolver.common.tree.TreeSupport.nodeEquals;
import static sqlsolver.sql.ast.constants.UnaryOpKind.NOT;
import static sqlsolver.sql.ast.constants.UnaryOpKind.UNARY_MINUS;
import static sqlsolver.sql.support.resolution.ParamModifier.Type.*;
import static sqlsolver.sql.support.resolution.ParamModifier.modifier;
import static sqlsolver.sql.support.resolution.ResolutionSupport.resolveAttribute;

class ResolveParam {
  private boolean negated;
  private LinkedList<ParamModifier> stack;

  List<ParamDesc> resolve(SqlNode expr) {
    if (!SqlSupport.isPrimitivePredicate(expr))
      throw new IllegalArgumentException("only accept primitive predicate");

    final List<SqlNode> params =
        LocatorSupport.nodeLocator().accept(ResolveParam::isParam).stopIfNot(SqlKind.Expr).gather(expr);
    if (params.isEmpty()) return emptyList();

    return ListSupport.map(params, it -> resolve(expr, it));
  }

  private ParamDesc resolve(SqlNode expr, SqlNode paramNode) {
    // determine if the expr is negated
    boolean negated = false;
    SqlNode parent = expr.parent();
    while (SqlKind.Expr.isInstance(parent)) { // trace back to expr root
      if (parent.get(ExprFields.Unary_Op) == NOT) negated = !negated;
      parent = parent.parent();
    }

    this.negated = negated;
    this.stack = new LinkedList<>();

    SqlNode cursor = paramNode;
    do {
      if (!deduce(cursor)) {
        return null;
      }
      cursor = cursor.parent();
    } while (!nodeEquals(cursor, expr));

    if (stack.getFirst().type() == GUESS) {
      stack.removeFirst();
      if (ExprKind.Literal.isInstance(paramNode)) {
        stack.offerFirst(modifier(DIRECT_VALUE, paramNode.$(ExprFields.Literal_Value)));
      } else {
        stack.offerFirst(modifier(DIRECT_VALUE, "UNKNOWN"));
      }
    }

    return new ParamDescImpl(expr, paramNode, stack);
  }

  private boolean deduce(SqlNode target) {
    final SqlNode parent = target.parent();
    final ExprKind exprKind = parent.$(SqlNodeFields.Expr_Kind);
    final boolean negated = this.negated;

    switch (exprKind) {
      case Unary:
        {
          final UnaryOpKind op = parent.$(ExprFields.Unary_Op);
          if (op == UNARY_MINUS) stack.offerFirst(modifier(INVERSE));
          return op != UnaryOpKind.BINARY;
        }

      case Binary:
        {
          final SqlNode otherSide = SqlSupport.getAnotherSide(parent, target);
          assert otherSide != null;
          // `swapped` indicates whether the param is of the right side.
          // e.g. For "age > ?" ? should be LESS than age.
          //      For "? > age" ? should be GREATER than age.
          final boolean swapped = nodeEquals(parent.$(ExprFields.Binary_Right), target);
          final BinaryOpKind op = parent.$(ExprFields.Binary_Op);

          final ParamModifier modifier = ParamModifier.fromBinaryOp(op, target, swapped, negated);
          if (modifier == null) return false;
          if (modifier.type() != KEEP) stack.offerFirst(modifier);

          return induce(otherSide);
        }

      case Tuple:
        stack.offerFirst(modifier(TUPLE_ELEMENT));
        return true;

      case Array:
        stack.offerFirst(modifier(ARRAY_ELEMENT));
        return true;

      case Ternary:
        if (nodeEquals(parent.$(ExprFields.Ternary_Middle), target))
          stack.offerFirst(modifier(negated ? INCREASE : DECREASE));
        else if (nodeEquals(parent.$(ExprFields.Ternary_Right), target))
          stack.offerFirst(modifier(negated ? DECREASE : INCREASE));
        else return false;
        return induce(parent.$(ExprFields.Ternary_Left));

      case Match:
        {
          final List<SqlNode> cols = parent.$(ExprFields.Match_Cols);
          if (cols.size() > 1) return false;
          stack.offerFirst(modifier(MATCHING));
          return induce(parent.$(ExprFields.Match_Cols).get(0));
        }

      case FuncCall:
        {
          final String funcName = parent.$(ExprFields.FuncCall_Name).$(SqlNodeFields.Name2_1).toLowerCase();
          if ("to_days".equals(funcName)) return true;
        }

      default:
        return false;
    }
  }

  // Induce an expression's value and express as modifier.
  // e.g. `x` + 1 (where `x` is a column name),
  // the resultant modifiers is [Value("x"), DirectValue(1), Plus()]
  private boolean induce(SqlNode target) {
    final ExprKind exprKind = target.$(SqlNodeFields.Expr_Kind);

    switch (exprKind) {
      case ColRef:
        {
          final Attribute reference = ResolutionSupport.traceRef(ResolutionSupport.resolveAttribute(target));
          final Column column = reference == null ? null : reference.column();
          if (reference == null || column == null) {
            stack.offerFirst(modifier(GUESS));

          } else {
            final Relation relation = reference.owner();
            stack.offerFirst(modifier(COLUMN_VALUE, relation, column));
          }
          return true;
        }

      case FuncCall:
        {
          final List<SqlNode> args = target.$(ExprFields.FuncCall_Args);
          final String funcName = target.$(ExprFields.FuncCall_Name).toString().toLowerCase();
          stack.offerFirst(modifier(INVOKE_FUNC, funcName, args.size()));
          for (SqlNode arg : Lists.reverse(args)) if (!induce(arg)) return false;
          return true;
        }

      case Binary:
        {
          switch (target.$(ExprFields.Binary_Op)) {
            case PLUS:
              stack.offerFirst(modifier(ADD));
              break;
            case MINUS:
              stack.offerFirst(modifier(SUBTRACT));
              break;
            case MULT:
              stack.offerFirst(modifier(TIMES));
              break;
            case DIV:
              stack.offerFirst(modifier(DIVIDE));
              break;
            default:
              return false;
          }
          return induce(target.$(ExprFields.Binary_Right)) && induce(target.$(ExprFields.Binary_Left));
        }

      case Aggregate:
        stack.offerFirst(modifier(INVOKE_AGG, target.get(ExprFields.Aggregate_Name)));
        return true;

      case Cast:
        return target.$(ExprFields.Cast_Type).category() != Category.INTERVAL && induce(target.$(ExprFields.Cast_Expr));

      case Literal:
        stack.offerFirst(modifier(DIRECT_VALUE, target.get(ExprFields.Literal_Value)));
        return true;

      case Tuple:
        {
          final List<SqlNode> exprs = target.$(ExprFields.Tuple_Exprs);
          stack.offerFirst(modifier(MAKE_TUPLE, exprs.size()));
          for (SqlNode elements : Lists.reverse(exprs)) if (!induce(elements)) return false;
          return true;
        }

      case Symbol:
        stack.offerFirst(modifier(DIRECT_VALUE, target.get(ExprFields.Symbol_Text)));
        return true;

      case QueryExpr:
        stack.offerFirst(modifier(GUESS));
        return true;

      default:
        return false;
    }
  }

  private static boolean isParam(SqlNode expr) {
    return (ExprKind.Literal.isInstance(expr) && !ExprKind.FuncCall.isInstance(expr.parent()))
        || ExprKind.Param.isInstance(expr)
        || ExprKind.FuncCall.isInstance(expr) && "now".equalsIgnoreCase(expr.$(ExprFields.FuncCall_Name).$(SqlNodeFields.Name2_1));
  }
}
