package sqlsolver.sql.ast.constants;

public enum IndexHintTarget {
  JOIN("JOIN"),
  ORDER_BY("ORDER BY"),
  GROUP_BY("GROUP BY");
  private final String text;

  IndexHintTarget(String text) {
    this.text = text;
  }

  public String text() {
    return text;
  }
}
