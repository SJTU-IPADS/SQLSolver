package sqlsolver.sql.ast.constants;

public enum OLAPOption {
  WITH_ROLLUP("WITH ROLLUP"),
  WITH_CUBE("WITH CUBE");
  private final String text;

  OLAPOption(String text) {
    this.text = text;
  }

  public String text() {
    return text;
  }
}
