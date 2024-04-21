package sqlsolver.sql.plan;

public enum PlanKind {
  // Don't change the order, should be consistent with OperatorType
  Input(0),
  Join(2),
  Filter(1),
  InSub(2),
  Exists(2),
  Proj(1),
  Agg(1),
  Sort(1),
  Limit(1),
  SetOp(2);

  private final int numChildren;

  PlanKind(int numChildren) {
    this.numChildren = numChildren;
  }

  public boolean isFilter() {
    return this == Filter || this == InSub || this == Exists;
  }

  public boolean isSubqueryFilter() {
    return this == InSub || this == Exists;
  }

  public int numChildren() {
    return numChildren;
  }
}
