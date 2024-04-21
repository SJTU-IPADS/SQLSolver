package sqlsolver.sql.ast.constants;

public enum VariableScope {
  USER("@"),
  SYSTEM_GLOBAL("@@GLOBAL."),
  SYSTEM_LOCAL("@@LOCAL."),
  SYSTEM_SESSION("@@SESSION.");
  private final String prefix;

  VariableScope(String prefix) {
    this.prefix = prefix;
  }

  public String prefix() {
    return prefix;
  }
}
