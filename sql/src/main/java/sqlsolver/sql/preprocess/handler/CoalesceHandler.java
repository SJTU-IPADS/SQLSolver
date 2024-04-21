package sqlsolver.sql.preprocess.handler;


/**
 * Handle the SQL with coalesce.
 * COALESCE(x, y) -> if x IS NULL return y, otherwise x.
 * <code>COALESCE(x, y)
 * <br/>-><br/>
 * CASE WHEN x IS NOT NULL THEN x ELSE y END</code>
 */
public class CoalesceHandler extends SqlHandler {

  private static String aggNullToZero(String[] params) {
    if (params.length == 2 && params[1].equals("0") && params[0].indexOf("(") != 0)
      return params[0];
    else
      return null;
  }

  private static int coalesceEndIndex(int startIndex, String query) {
    int counter = 1;
    for (int i = startIndex + 1; i < query.length(); ++i) {
      if (query.charAt(i) == '(')
        counter++;
      else if (query.charAt(i) == ')')
        counter--;
      if (counter == 0)
        return i;
    }
    assert false;
    return 0;
  }

  private static String coalesceHandler(String query, int coalesceBegin) {
    int startIndex = -1;
    int endIndex = -1;
    startIndex = query.toUpperCase().indexOf("COALESCE(", coalesceBegin);
    if (startIndex == -1)
      return query;
    else
      startIndex = startIndex + 9;
    endIndex = coalesceEndIndex(startIndex, query);
    assert endIndex > 0;
    String paramString = query.substring(startIndex, endIndex);
    String[] params = paramString.split(",");
    for (int i = 0; i < params.length; ++i) {
      params[i] = params[i].replace(" ", "");
    }
    String agg = aggNullToZero(params);
    if (agg != null) {
      query = query.replace(query.substring(startIndex - 9, endIndex + 1), agg);
      return coalesceHandler(query, endIndex);
    }

    String caseWhen = "CASE WHEN ";
    for (int i = 0; i < params.length; ++i) {
      String param = params[i];
      if (i == params.length - 1) {
        caseWhen = caseWhen + "ELSE " + param + " ";
      } else {
        caseWhen = caseWhen + param + " IS NOT NULL ";
        caseWhen = caseWhen + "THEN " + param + " ";
      }
    }
    caseWhen += " END";
    query = query.replace(query.substring(startIndex - 9, endIndex + 1), caseWhen);
    return coalesceHandler(query, endIndex);
  }

  @Override
  public String handle(String sql) {
    try {
      return coalesceHandler(sql, 0);
    } catch (Throwable e) {
      return sql;
    }
  }
}
