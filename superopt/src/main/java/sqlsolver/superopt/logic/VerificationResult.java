package sqlsolver.superopt.logic;

public enum VerificationResult {
  EQ,
  NEQ,
  UNKNOWN,
  TIMEOUT;

  public String toString() {
    return switch (this) {
      case EQ -> "EQ";
      case NEQ -> "NEQ";
      case UNKNOWN -> "UNKNOWN";
      case TIMEOUT -> "TIMEOUT";
    };
  }
}
