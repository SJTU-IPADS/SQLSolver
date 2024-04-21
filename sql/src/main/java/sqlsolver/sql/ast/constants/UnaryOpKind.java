package sqlsolver.sql.ast.constants;

public enum UnaryOpKind {
  NOT("NOT", 4, true),
  SQRT_ROOT("|/", 6, true),
  CUBE_ROOT("||/", 6, true),
  FACTORIAL("!!", 6, true),
  ABSOLUTE_VALUE("@", 6, true),
  BINARY("BINARY", 13, true),
  UNARY_PLUS("+", 12, true),
  UNARY_MINUS("-", 12, true),
  UNARY_FLIP("~", 12, true);

  private final String text;
  private final int precedence;
  private final boolean atLeft;

  UnaryOpKind(String text, int precedence, boolean atLeft) {
    this.text = text;
    this.precedence = precedence;
    this.atLeft = atLeft;
  }

  public String text() {
    return text;
  }

  public static UnaryOpKind ofOp(String text) {
    if (text.equals("!")) return FACTORIAL;
    for (UnaryOpKind value : values()) if (value.text().equalsIgnoreCase(text)) return value;
    return null;
  }

  public int precedence() {
    return precedence;
  }

  public boolean isLogic() {
    return this == NOT;
  }
}
