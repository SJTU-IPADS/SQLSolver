package sqlsolver.sql.plan;

import sqlsolver.sql.schema.Column;

import java.util.List;

public interface ValuesRegistry {
  Values valuesOf(int nodeId);

  int initiatorOf(Value value);

  Column columnOf(Value value);

  Expression exprOf(Value value);

  Values valueRefsOf(Expression expr);

  void bindValues(int nodeId, List<Value> values);

  void bindValueRefs(Expression expr, List<Value> valueRefs);

  void bindExpr(Value value, Expression expr);
}
