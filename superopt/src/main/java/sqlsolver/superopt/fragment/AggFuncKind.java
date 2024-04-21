package sqlsolver.superopt.fragment;

import java.util.List;

public enum AggFuncKind {
  SUM("sum"),
  AVERAGE("average"),
  COUNT("count"),
  MAX("max"),
  MIN("min"),
  UNKNOWN("unknown");

  private final String text;

  AggFuncKind(String text) {
    this.text = text;
  }

  public String text() {
    return text;
  }

  public static List<AggFuncKind> dedupAggFuncKinds = List.of(COUNT);

  public static List<AggFuncKind> commonAggFuncKinds = List.of(SUM, AVERAGE, COUNT, MAX, MIN);
}
