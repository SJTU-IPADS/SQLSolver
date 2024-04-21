package sqlsolver.sql.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.utils.Lazy;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.support.resolution.ParamDesc;
import sqlsolver.sql.support.resolution.ParamModifier;
import sqlsolver.sql.support.resolution.Params;
import sqlsolver.sql.support.resolution.ResolutionSupport;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.ast.*;

import static sqlsolver.common.utils.ListSupport.elemAt;
import static sqlsolver.common.utils.ListSupport.tail;

public class ParamInterpolator {
  private final SqlNode ast;
  private final Lazy<TIntList> interpolations;

  public ParamInterpolator(SqlNode ast) {
    this.ast = ast;
    this.interpolations = Lazy.mk(TIntArrayList::new);
  }

  public void go() {
    ResolutionSupport.setLimitClauseAsParam(false);
    final Params params = ast.context().getAdditionalInfo(Params.PARAMS);
    final SqlNodes paramNodes = LocatorSupport.nodeLocator().accept(ExprKind.Param).gather(ast);
    for (SqlNode paramNode : paramNodes) interpolateOne(params.paramOf(paramNode));
  }

  public void undo() {
    if (interpolations.isInitialized()) {
      final TIntList nodeIds = interpolations.get();
      for (int i = 0, bound = nodeIds.size(); i < bound; ++i) {
        final SqlNode node = SqlNode.mk(ast.context(), nodeIds.get(i));
        node.$(SqlNodeFields.Expr_Kind, ExprKind.Param);
        node.$(ExprFields.Param_Number, i + 1);
        node.remove(ExprFields.Literal_Kind);
        node.remove(ExprFields.Literal_Value);
      }
    }
  }

  private void interpolateOne(ParamDesc param) {
    final SqlNode paramNode = param.node();
    if (!ExprKind.Param.isInstance(paramNode)) return;

    ParamModifier modifier = tail(param.modifiers());
    if (modifier == null) return;
    if (modifier.type() == ParamModifier.Type.TUPLE_ELEMENT || modifier.type() == ParamModifier.Type.ARRAY_ELEMENT)
      modifier = elemAt(param.modifiers(), -2);
    if (modifier == null || modifier.type() != ParamModifier.Type.COLUMN_VALUE) return;

    final SqlNode valueNode = mkValue(((Column) modifier.args()[1]));
    ast.context().displaceNode(paramNode.nodeId(), valueNode.nodeId());

    interpolations.get().add(paramNode.nodeId());
  }

  private SqlNode mkValue(Column column) {
    final SqlNode value = SqlNode.mk(ast.context(), ExprKind.Literal);
    switch (column.dataType().category()) {
      case INTEGRAL:
        value.$(ExprFields.Literal_Kind, LiteralKind.INTEGER);
        value.$(ExprFields.Literal_Value, 1);
        break;
      case FRACTION:
        value.$(ExprFields.Literal_Kind, LiteralKind.FRACTIONAL);
        value.$(ExprFields.Literal_Value, 1.0);
        break;
      case BOOLEAN:
        value.$(ExprFields.Literal_Kind, LiteralKind.BOOL);
        value.$(ExprFields.Literal_Value, false);
        break;
      case STRING:
        value.$(ExprFields.Literal_Kind, LiteralKind.TEXT);
        value.$(ExprFields.Literal_Value, "00001");
        break;
      case TIME:
        value.$(ExprFields.Literal_Kind, LiteralKind.TEXT);
        value.$(ExprFields.Literal_Value, "2021-01-01 00:00:00.000");
        break;
      default:
        value.$(ExprFields.Literal_Kind, LiteralKind.NULL);
        break;
    }
    return value;
  }
}
