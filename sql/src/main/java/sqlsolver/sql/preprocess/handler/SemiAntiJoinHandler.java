package sqlsolver.sql.preprocess.handler;

import sqlsolver.sql.preprocess.rewrite.SemiAntiJoinRewriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This co-works with SemiAntiJoinRewriter.
 * It is the first step of semi/anti-join rewrites.
 * <p/>
 * <code>LEFT SEMI JOIN (R) A ON P
 * <br/>-><br/>
 * LEFT JOIN (R) A ON SEMI_JOIN() AND P</code>
 */
public class SemiAntiJoinHandler extends SqlHandler {
  // What if P is "B OR C"?
  //   it should choose the "left-most" condition
  //  regardless of precedence of ops

  private static String sqlUpper = null;

  // the position of the " ON " corresponding to the join at <start>
  private static int findJoinCondition(String sql, int start) {
    int firstLeftParen = sql.indexOf("(", start);
    int firstOn = sql.indexOf(" ON ", start);
    // "ON" must be present in <sql> after <start>
    assert firstOn >= start;
    if (firstLeftParen == -1 || firstLeftParen > firstOn) {
      // no paren between JOIN and ON; the first ON is the desired one
      return firstOn;
    }
    // skip closed parens & find ON
    start = firstLeftParen + 1;
    for (int count = 1; count > 0; start++) {
      switch (sql.charAt(start)) {
        case '(' -> count++;
        case ')' -> count--;
      }
    }
    return sql.indexOf(" ON ", start);
  }

  // joinType is semi/anti
  // not support recursive semi/anti joins (e.g. "semi join ( ... semi join ...)")
  private static String handleOfType(String sql, String joinType) {
    StringBuilder sb = new StringBuilder();
    String patternStr = " " + joinType + " JOIN ";
    Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(sql);
    int lastEnd = 0;
    while (matcher.find()) {
      // matcher matches " <joinType> JOIN "
      // we want to match " ON " corresponding to that JOIN
      int start = matcher.start();
      start = findJoinCondition(sqlUpper, start);
      int end = start + 4;
      // unmatched part remains unchanged
      sb.append(sql, lastEnd, start);
      // matched part is to be modified
      sb.append(" ON ").append(joinType).append("_JOIN() AND ");
      // next loop
      lastEnd = end;
    }
    sb.append(sql.substring(lastEnd));
    return sb.toString().replaceAll("(?i)" + patternStr, " JOIN ");
  }

  // sql should be upper case
  private static boolean needsHandle(String sql) {
    return sql.contains("LEFT SEMI JOIN") || sql.contains("LEFT ANTI JOIN");
  }

  // sql should be upper case
  private static boolean initRewriter(String sql) {
    SemiAntiJoinRewriter.setEnabled(true);
    // prevent duplicate function names
    if (sql.contains("SEMI_JOIN()") || sql.contains("ANTI_JOIN()")) {
      SemiAntiJoinRewriter.setEnabled(false);
      return false;
    }
    return true;
  }

  @Override
  public String handle(String sql) {
    sqlUpper = sql.toUpperCase();
    try {
      if (!initRewriter(sqlUpper)) return sql;
      if (!needsHandle(sqlUpper)) return sql;
      sql = handleOfType(sql, "SEMI");
      sqlUpper = sql.toUpperCase();
      return handleOfType(sql, "ANTI");
    } catch (Throwable e) {
      return sql;
    }
  }

}
