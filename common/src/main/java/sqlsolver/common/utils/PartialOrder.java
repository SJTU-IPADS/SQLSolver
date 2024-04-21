package sqlsolver.common.utils;

public enum PartialOrder {
  SAME,
  LESS_THAN,
  GREATER_THAN,
  INCOMPARABLE;

  public boolean lessOrSame() {
    return this == LESS_THAN || this == SAME;
  }

  public boolean greaterOrSame() {
    return this == GREATER_THAN || this == SAME;
  }
}
