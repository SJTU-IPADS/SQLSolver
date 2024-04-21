package sqlsolver.sql.ast.constants;

public enum TernaryOp {
  BETWEEN_AND("BETWEEN", "AND", 4);

  private final String text0;
  private final String text1;
  private final int precedence;

  TernaryOp(String text0, String text1, int precedence) {
    this.text0 = text0;
    this.text1 = text1;
    this.precedence = precedence;
  }

  public String text0() {
    return text0;
  }

  public String text1() {
    return text1;
  }

  public int precedence() {
    return precedence;
  }
}
