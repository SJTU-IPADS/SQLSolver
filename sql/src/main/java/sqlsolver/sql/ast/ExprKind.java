package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;

import java.util.ArrayList;
import java.util.List;

import static sqlsolver.sql.ast.SqlNodeFields.Expr_Kind;

public enum ExprKind implements FieldDomain {
  Unknown,
  Variable,
  ColRef,
  FuncCall,
  Collate,
  Interval,
  Symbol,
  Literal,
  Param,
  Aggregate,
  Wildcard,
  GroupingOp,
  Unary,
  Binary,
  Ternary,
  Tuple,
  Exists,
  Match,
  Cast,
  Case,
  When,
  ConvertUsing,
  Default,
  Values,
  QueryExpr,
  Indirection,
  IndirectionComp,
  Array,
  TypeCoercion,
  DateTimeOverlap,
  ComparisonMod // actually invalid, just used in parsing process
;

  private final List<FieldKey<?>> fields = new ArrayList<>(5);

  public boolean isInstance(SqlNode node) {
    return node != null && node.$(Expr_Kind) == this;
  }

  @Override
  public List<FieldKey<?>> fields() {
    return fields;
  }

  @Override
  public <T, R extends T> FieldKey<R> field(String name, Class<T> clazz) {
    final FieldKey<R> field = new ExprField<>(name, clazz, this);
    fields.add(field);
    return field;
  }
}
