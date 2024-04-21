package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.ExprKind;

import java.util.Arrays;

import static sqlsolver.common.utils.Commons.assertFalse;
import static sqlsolver.sql.support.resolution.ParamModifier.Type.*;

public class ParamModifier {
  public enum Type {
    INVERSE,
    SUBTRACT,
    ADD,
    DIVIDE,
    TIMES,
    TUPLE_ELEMENT,
    ARRAY_ELEMENT,
    DECREASE,
    INCREASE,
    LIKE,
    REGEX,
    CHECK_NULL,
    CHECK_NULL_NOT,
    CHECK_BOOL,
    CHECK_BOOL_NOT,
    NEQ,
    COLUMN_VALUE,
    INVOKE_FUNC,
    INVOKE_AGG,
    DIRECT_VALUE,
    MAKE_TUPLE,
    MATCHING,
    OFFSET_VAL,
    LIMIT_VAL,
    GEN_OFFSET,
    GUESS,
    KEEP
  }

  private final Type type;
  private final Object[] args;

  private ParamModifier(Type type, Object[] args) {
    this.type = type;
    this.args = args;
  }

  public Type type() {
    return type;
  }

  public Object[] args() {
    return args;
  }

  public static ParamModifier modifier(Type type, Object... args) {
    return new ParamModifier(type, args);
  }

  private static ParamModifier fromLike(SqlNode param) {
    if (ExprKind.Literal.isInstance(param)) {
      final String value = param.get(ExprFields.Literal_Value).toString();
      return ParamModifier.modifier(LIKE, value.startsWith("%"), value.endsWith("%"));
    }
    return ParamModifier.modifier(LIKE);
  }

  private static ParamModifier fromIs(SqlNode param, boolean not) {
    if (ExprKind.Literal.isInstance(param)) {
      if (param.get(ExprFields.Literal_Kind) == LiteralKind.NULL)
        return not ? modifier(CHECK_NULL_NOT) : modifier(Type.CHECK_NULL);
      else if (param.get(ExprFields.Literal_Kind) == LiteralKind.BOOL)
        return not ? modifier(Type.CHECK_BOOL_NOT) : modifier(Type.CHECK_BOOL);
      else return assertFalse();

    } else return assertFalse();
  }

  private static final ParamModifier KEEP_STILL = modifier(Type.KEEP);

  public static ParamModifier fromBinaryOp(
      BinaryOpKind op, SqlNode target, boolean inverse, boolean not) {
    if (op == BinaryOpKind.EQUAL || op == BinaryOpKind.IN_LIST || op == BinaryOpKind.ARRAY_CONTAINS)
      return not ? modifier(NEQ) : KEEP_STILL;

    if (op == BinaryOpKind.NOT_EQUAL) return not ? KEEP_STILL : modifier(NEQ);

    if (op == BinaryOpKind.GREATER_OR_EQUAL || op == BinaryOpKind.GREATER_THAN)
      return modifier(inverse ^ not ? DECREASE : INCREASE);

    if (op == BinaryOpKind.LESS_OR_EQUAL || op == BinaryOpKind.LESS_THAN)
      return modifier(inverse ^ not ? INCREASE : DECREASE);

    if (op == BinaryOpKind.LIKE || op == BinaryOpKind.ILIKE || op == BinaryOpKind.SIMILAR_TO)
      return not ? modifier(NEQ) : fromLike(target);

    if (op == BinaryOpKind.IS) return fromIs(target, not);

    if (op.toStandardOp() == BinaryOpKind.REGEXP) return not ? modifier(NEQ) : modifier(REGEX);

    if (op == BinaryOpKind.PLUS) return modifier(SUBTRACT);
    if (op == BinaryOpKind.MINUS) return modifier(ADD);
    if (op == BinaryOpKind.MULT) return modifier(DIVIDE);
    if (op == BinaryOpKind.DIV) return modifier(TIMES);

    // omit others since not encountered
    return null;
  }

  @Override
  public String toString() {
    return type + Arrays.toString(args);
  }
}
