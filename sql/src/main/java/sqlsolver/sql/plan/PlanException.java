package sqlsolver.sql.plan;

import sqlsolver.sql.SqlException;

public class PlanException extends SqlException {
  public PlanException() {}

  public PlanException(String message) {
    super(message);
  }

  public PlanException(String message, Throwable cause) {
    super(message, cause);
  }

  public PlanException(Throwable cause) {
    super(cause);
  }

  public PlanException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
