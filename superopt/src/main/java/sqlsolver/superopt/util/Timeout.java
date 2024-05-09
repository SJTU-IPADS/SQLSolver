package sqlsolver.superopt.util;

public class Timeout {
  private static final String TIMEOUT_MSG = "Verification timeout";

  public static void checkTimeout() {
    // INTERRUPT CHECKPOINT
    if (Thread.interrupted()) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(TIMEOUT_MSG);
    }
  }

  public static boolean isTimeout(Throwable e) {
    return e instanceof RuntimeException && TIMEOUT_MSG.equals(e.getMessage());
  }

  /**
   * Given a {@code Throwable} object, if it indicates verification timeout,
   * throw it directly.
   */
  public static void bypassTimeout(Throwable e) {
    // bypass timeout as-is
    if (Timeout.isTimeout(e)) {
      throw (RuntimeException) e;
    }
  }
}
