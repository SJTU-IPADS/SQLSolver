package sqlsolver.sql.ast.constants;

public enum FrameBoundDirection {
  PRECEDING,
  FOLLOWING;

  public static FrameBoundDirection ofText(String text) {
    if (text.equalsIgnoreCase(PRECEDING.name())) return PRECEDING;
    else if (text.equalsIgnoreCase(FOLLOWING.name())) return FOLLOWING;
    else return null;
  }
}
