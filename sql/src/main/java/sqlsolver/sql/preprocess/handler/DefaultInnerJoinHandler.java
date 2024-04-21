package sqlsolver.sql.preprocess.handler;

/**
 * When there is no "ON", "USING" and "NATURAL",
 * replace all occurrences of " INNER JOIN " with " CROSS JOIN ".
 */
public class DefaultInnerJoinHandler extends SqlHandler {
  @Override
  public String handle(String sql) {
    final String upperSql = sql.toUpperCase();
    if (!upperSql.contains("ON") && !upperSql.contains("USING") && !upperSql.contains("NATURAL")) {
      return sql.replaceAll("(?i) INNER JOIN ", " CROSS JOIN ");
    }
    return sql;
  }
}
