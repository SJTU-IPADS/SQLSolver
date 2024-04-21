package sqlsolver.sql.ast.constants;

public enum BinaryOpKind {
  BITWISE_XOR("^", 12),
  EXP("^", 12),
  MULT("*", 11),
  DIV("/", 11),
  MOD("%", 11),
  PLUS("+", 10),
  MINUS("-", 10),
  LEFT_SHIFT("<<", 9),
  RIGHT_SHIFT(">>", 9),
  BITWISE_AND("&", 8),
  BITWISE_OR("|", 7),
  BITWISE_XOR_PG("#", 7, BITWISE_XOR),
  EQUAL("=", 6),
  IS("IS", 6),
  NULL_SAFE_EQUAL("<=>", 6),
  GREATER_OR_EQUAL(">=", 6),
  GREATER_THAN(">", 6),
  LESS_OR_EQUAL("<=", 6),
  LESS_THAN("<", 6),
  NOT_EQUAL("<>", 6),
  IN_LIST("IN", 6),
  IN_SUBQUERY("IN", 6),
  LIKE("LIKE", 6),
  ILIKE("ILIKE", 6),
  SIMILAR_TO("SIMILAR TO", 6),
  IS_DISTINCT_FROM("IS DISTINCT FROM", 6),
  ARRAY_CONTAINS("@>", 6),
  ARRAY_CONTAINED_BY("<@", 6),
  REGEXP("REGEXP", 6),
  REGEXP_PG("~", 6, REGEXP),
  REGEXP_I_PG("~*", 6, REGEXP),
  MEMBER_OF("MEMBER OF", 6),
  SOUNDS_LIKE("SOUNDS LIKE", 6),
  AT_TIME_ZONE("AT TIME ZONE", 6),
  CONCAT("||", 6),
  AND("AND", 3),
  XOR_SYMBOL("XOR", 2),
  OR("OR", 1);

  private final String text;
  private final int precedence;
  private final BinaryOpKind standard;

  BinaryOpKind(String text, int precedence) {
    this.text = text.toUpperCase();
    this.precedence = precedence < 0 ? Integer.MAX_VALUE : precedence;
    this.standard = null;
  }

  BinaryOpKind(String text, int precedence, BinaryOpKind standard) {
    this.text = text.toUpperCase();
    this.precedence = precedence < 0 ? Integer.MAX_VALUE : precedence;
    this.standard = standard;
  }

  public String text() {
    return text;
  }

  public static BinaryOpKind ofOp(String opText) {
    opText = opText.toUpperCase();
    if (opText.equals("DIV")) return DIV;
    if (opText.equals("MOD")) return MOD;
    if (opText.equals("!=")) return NOT_EQUAL;
    for (BinaryOpKind op : values()) if (op.text.equals(opText)) return op;
    return null;
  }

  public BinaryOpKind toStandardOp() {
    return standard == null ? this : standard;
  }

  public boolean isArithmetic() {
    return precedence >= BITWISE_OR.precedence;
  }

  public boolean isRelation() {
    return precedence == LIKE.precedence && this != AT_TIME_ZONE && this != CONCAT;
  }

  public boolean isLogic() {
    return precedence <= AND.precedence;
  }

  public boolean isComparison() {
    return this == EQUAL
            || this == IS
            || this == NULL_SAFE_EQUAL
            || this == GREATER_OR_EQUAL
            || this == GREATER_THAN
            || this == LESS_OR_EQUAL
            || this == LESS_THAN
            || this == NOT_EQUAL;

  }

  public int precedence() {
    return precedence;
  }
}
