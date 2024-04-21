package sqlsolver.sql.ast.constants;

public enum MatchOption {
  BOOLEAN_MODE("in boolean mode"),
  NATURAL_MODE("in natural language mode"),
  NATURAL_MODE_WITH_EXPANSION("in natural language mode with query expansion"),
  WITH_EXPANSION("with query expansion");

  private final String optionText;

  MatchOption(String optionText) {
    this.optionText = optionText.toUpperCase();
  }

  public String optionText() {
    return optionText;
  }
}
