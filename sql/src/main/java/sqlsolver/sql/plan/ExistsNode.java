package sqlsolver.sql.plan;

public interface ExistsNode extends PlanNode {
  @Override
  default PlanKind kind() {
    return PlanKind.Exists;
  }

  static ExistsNode mk() {
    return new ExistsNodeImpl();
  }
}
