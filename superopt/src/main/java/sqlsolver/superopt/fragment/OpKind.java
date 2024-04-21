package sqlsolver.superopt.fragment;

public enum OpKind {
  // Replace this by sealed interface after google-java-format plugin support the future.
  INPUT(0, "Input"),
  INNER_JOIN(2, "InnerJoin"),
  CROSS_JOIN(2, "CrossJoin"),
  LEFT_JOIN(2, "LeftJoin"),
  RIGHT_JOIN(2, "RightJoin"),
  FULL_JOIN(2, "FullJoin"),
  SIMPLE_FILTER(1, "Filter"),
  IN_SUB_FILTER(2, "InSubFilter"),
  EXISTS_FILTER(2, "Exists"),
  PROJ(1, "Proj"),
  AGG(1, "Agg"),
  SORT(1, "Sort"),
  LIMIT(1, "Limit"),
  UNION(2, "Union"),
  INTERSECT(2, "Intersect"),
  EXCEPT(2, "Except");

  private final int numPredecessors;
  private final String text;

  OpKind(int numPredecessors, String text) {
    this.numPredecessors = numPredecessors;
    this.text = text;
  }

  public int numPredecessors() {
    return numPredecessors;
  }

  public String text() {
    return text;
  }

  public boolean isValidOutput() {
    return !this.isJoin() && !this.isFilter();
  }

  public boolean isJoin() {
    return this == LEFT_JOIN
        || this == RIGHT_JOIN
        || this == FULL_JOIN
        || this == INNER_JOIN
        || this == CROSS_JOIN;
  }

  public boolean isFilter() {
    return this == SIMPLE_FILTER || this == IN_SUB_FILTER || this == EXISTS_FILTER;
  }

  public boolean isSubquery() {
    return this == IN_SUB_FILTER || this == EXISTS_FILTER;
  }

  public boolean isSetOp() {
    return this == UNION || this == INTERSECT || this == EXCEPT;
  }

  public static OpKind parse(String value) {
    return switch (value) {
      case "LeftJoin" -> LEFT_JOIN;
      case "RightJoin" -> RIGHT_JOIN;
      case "FullJoin" -> FULL_JOIN;
      case "InnerJoin" -> INNER_JOIN;
      case "CrossJoin" -> CROSS_JOIN;
      case "PlainFilter", "SimpleFilter", "Filter" -> SIMPLE_FILTER;
      case "SubqueryFilter", "InSubFilter", "InSub" -> IN_SUB_FILTER;
      case "Exists" -> EXISTS_FILTER;
      case "Input" -> INPUT;
      case "Proj", "Proj*" -> PROJ;
      case "Union", "Union*" -> UNION;
      case "Intersect", "Intersect*" -> INTERSECT;
      case "Except", "Except*" -> EXCEPT;
      case "Agg", "Agg_sum", "Agg_average", "Agg_count", "Agg_max", "Agg_min", "Agg_count*" -> AGG;
      default -> throw new IllegalArgumentException("unknown operator: " + value);
    };
  }
}
