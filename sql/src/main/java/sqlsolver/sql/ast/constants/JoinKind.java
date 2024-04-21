package sqlsolver.sql.ast.constants;

public enum JoinKind {
  CROSS_JOIN("CROSS JOIN"),
  INNER_JOIN("INNER JOIN"),
  STRAIGHT_JOIN("STRAIGHT_JOIN"),
  LEFT_JOIN("LEFT JOIN"),
  RIGHT_JOIN("RIGHT JOIN"),
  FULL_JOIN("FULL JOIN"),
  NATURAL_INNER_JOIN("NATURAL JOIN"),
  NATURAL_LEFT_JOIN("NATURAL LEFT JOIN"),
  NATURAL_RIGHT_JOIN("NATURAL RIGHT JOIN");

  private final String text;

  JoinKind(String text) {
    this.text = text;
  }

  public String text() {
    return text;
  }

  public boolean isInner() {
    return this == CROSS_JOIN
        || this == INNER_JOIN
        || this == STRAIGHT_JOIN
        || this == NATURAL_INNER_JOIN;
  }

  public boolean isNatural() {
    return this == NATURAL_INNER_JOIN || this == NATURAL_LEFT_JOIN || this == NATURAL_RIGHT_JOIN;
  }

  public boolean isOuter() {
    return this == LEFT_JOIN
        || this == RIGHT_JOIN
        || this == FULL_JOIN
        || this == NATURAL_LEFT_JOIN
        || this == NATURAL_RIGHT_JOIN;
  }

  public boolean isRight() {
    return this == NATURAL_RIGHT_JOIN || this == RIGHT_JOIN;
  }
}
