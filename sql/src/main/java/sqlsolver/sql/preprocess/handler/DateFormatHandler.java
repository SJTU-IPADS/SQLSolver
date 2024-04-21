package sqlsolver.sql.preprocess.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * date function transformer.
 * <p/>
 * <code>date('yyyy-MM-DD +xx')
 * <br/>-><br/>
 * date('yyyy-MM-DD')</code>
 * <p/>
 * <code>DATE 'yyyy-MM-DD'
 * <br/>-><br/>
 * date('yyyy-MM-DD')</code>
 */
public class DateFormatHandler extends SqlHandler {

  /**
   * date('yyyy-MM-DD +xx') -> date 'yyyy-MM-DD'
   */
  private static String dateFormatHandler(String query) {
    StringBuilder sb = new StringBuilder();
    Pattern pattern = Pattern.compile("date\\('[0-9]{4}-[0-9]{2}-[0-9]{2} +\\+[0-9]+'\\)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    int lastEnd = 0;
    while (matcher.find()) {
      int start = matcher.start(), end = matcher.end();
      // unmatched part remains unchanged
      sb.append(query, lastEnd, start);
      // matched part is to be modified
      String sub = matcher.group()
              .replaceAll(" *\\+[0-9]+'", "'");
      sb.append(sub);
      // next loop
      lastEnd = end;
    }
    sb.append(query.substring(lastEnd));
    return sb.toString();
  }

  /**
   * DATE 'yyyy-MM-DD' -> date 'yyyy-MM-DD'
   */
  private static String dateConstHandler(String query) {
    StringBuilder sb = new StringBuilder();
    Pattern pattern = Pattern.compile("DATE\\s+'[0-9]{4}-[0-9]{2}-[0-9]{2}'", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    int lastEnd = 0;
    while (matcher.find()) {
      int start = matcher.start(), end = matcher.end();
      // unmatched part remains unchanged
      sb.append(query, lastEnd, start);
      // matched part is to be modified
      String sub = matcher.group()
              .replaceAll("\\s+'", "('") + ")";
      sb.append(sub);
      // next loop
      lastEnd = end;
    }
    sb.append(query.substring(lastEnd));
    return sb.toString();
  }

  @Override
  public String handle(String sql) {
    try {
      sql = dateConstHandler(sql);
      return dateFormatHandler(sql);
    } catch (Throwable e) {
      return sql;
    }
  }
}
