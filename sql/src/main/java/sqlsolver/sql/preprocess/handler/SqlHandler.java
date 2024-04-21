package sqlsolver.sql.preprocess.handler;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry of handle SQLs,
 * and parent class of all handlers
 * that rewrite SQL by analyzing string
 */
public abstract class SqlHandler {

  private static List<SqlHandler> handlers = null;

  /**
   * HINT: Add new SQL handlers HERE.
   */
  private static void registerHandlers() {
    handlers.add(new DateFormatHandler());
    handlers.add(new SemiAntiJoinHandler());
    handlers.add(new DefaultInnerJoinHandler());
  }

  private static synchronized void init() {
    if (handlers == null) {
      handlers = new ArrayList<>();
      registerHandlers();
    }
  }

  /**
   * Call all the registered handlers one by one
   * to handle the given SQL.
   *
   * @param sql the SQL to be handled.
   * @return the SQL after handled by all registered preprocessors.
   */
  public static String handleAll(String sql) {
    init();
    for (SqlHandler handler : handlers) {
      try {
        sql = handler.handle(sql);
      } catch (Exception e) {
        // keep the last result upon exception
//        e.printStackTrace();
      }
    }
    return sql;
  }

  /**
   * A template method which should handle a given SQL string.
   *
   * @param sql the sql to be handled.
   * @return the sql after handled.
   */
  public abstract String handle(String sql);
}
