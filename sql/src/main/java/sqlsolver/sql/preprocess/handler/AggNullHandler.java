package sqlsolver.sql.preprocess.handler;

/**
 * Replace NULL with NULL in Aggregation.
 * <code>AGG(NULL)/AGG(CAST(NULL AS ...))
 * <br/>-><br/>
 * NULL</code>
 */
public class AggNullHandler extends SqlHandler {
  private static String replaceSumCastNull(String str, int start) {
    int end = str.indexOf("(", start);
    assert end > 0;
    int flag = 1;
    end++;
    for (; flag != 0; ++end) {
      char curChar = str.charAt(end);
      if (curChar == '(')
        flag++;
      else if (curChar == ')')
        flag--;
    }
    return str.replace(str.substring(start, end), "NULL");
  }

  private static String aggNullHandler(String str) {
    str = str.replaceAll("(?i)SUM\\(NULL\\)", "NULL");
    int start = str.toUpperCase().indexOf("SUM(CAST(NULL AS");
    while (start >= 0) {
      str = replaceSumCastNull(str, start);
      start = str.toUpperCase().indexOf("SUM(CAST(NULL AS");
    }
    return str;
  }

  @Override
  public String handle(String sql) {
    try {
      return aggNullHandler(sql);
    } catch (Throwable e) {
      return sql;
    }
  }
}
