package sqlsolver.sql;

public class SqlException extends RuntimeException {
  public SqlException() {}

  public SqlException(String message) {
    super(message);
  }

  public SqlException(String message, Throwable cause) {
    super(message, cause);
  }

  public SqlException(Throwable cause) {
    super(cause);
  }

  public SqlException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
