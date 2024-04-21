package sqlsolver.sql.plan;

public interface LimitNode extends PlanNode {
  Expression limit();

  Expression offset();

  @Override
  default PlanKind kind() {
    return PlanKind.Limit;
  }

  static LimitNode mk(Expression limit, Expression offset) {
    return new LimitNodeImpl(limit, offset);
  }
}
