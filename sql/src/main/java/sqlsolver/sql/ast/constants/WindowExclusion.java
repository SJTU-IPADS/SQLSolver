package sqlsolver.sql.ast.constants;

public enum WindowExclusion {
  CURRENT_ROW("CURRENT ROW"),
  GROUP("GROUP"),
  TIES("TIES"),
  NO_OTHERS("NO OTHERS");

  private final String text;

  WindowExclusion(String text) {
    this.text = text;
  }

  public String text() {
    return text;
  }
}
