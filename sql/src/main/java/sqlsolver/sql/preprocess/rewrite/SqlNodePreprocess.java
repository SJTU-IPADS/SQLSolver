package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.Planner;
import sqlsolver.sql.schema.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry of preprocessing SqlNodes (Calcite),
 * and the parent class of all rewriters that
 * rewrite SqlNodes.
 */
public abstract class SqlNodePreprocess {

  private static List<SqlNodePreprocess> rewriters = null;
  private static Schema schema;

  /**
   * HINT: Add new SqlNode preprocessors HERE.
   */
  private static void registerPreprocessors() {
    rewriters.add(new DateOpRewriter());
    rewriters.add(new SemiAntiJoinRewriter());
    rewriters.add(new ConstantFoldingRewriter());
    rewriters.add(new UnresolvedFunctionRewriter());
    rewriters.add(new CastRewriter());
    rewriters.add(new AggFilterRewriter());
    rewriters.add(new OrderByConstRewriter());
    rewriters.add(new GroupByConstRewriter());
    rewriters.add(new LimitZeroRewriter());
    rewriters.add(new RollupRewriter());
    rewriters.add(new GroupingSetsRewriter());
    rewriters.add(new ColumnReorderRewriter());
    // TODO: spider cases need this rewrite rule.
//    rewriters.add(new AggSelectLargerGroupRewriter());
  }

  public static void setSchema(Schema s) {
    schema = s;
  }

  public static Schema getSchema() {
    return schema;
  }

  /**
   * Apply all the registered preprocessors one by one
   * to preprocess the given query.
   *
   * @param sql the SQL query to be preprocessed
   * @return the query after preprocessed by all registered preprocessors
   */
  public static String preprocessAll(String sql, Planner planner) {
    try {
      // parse sql string into SqlNode
      sql = sql.replace('\"', '\'').replace(';', ' ');
      SqlNode node = planner.parse(sql);
      // preprocess SqlNode with all rewriters
      node = preprocessAll(node);
      // restore SqlNode to sql string
      String strNew = node.toString();
      strNew = postProcess(strNew);
      return strNew;
    } catch (Exception e) {
      // stay unchanged upon exception
      return sql;
    }
  }

  /**
   * Process SQL text after preprocessing.
   */
  private static String postProcess(String sql) {
    sql = sql.replace("\n", " ").replace("\r", " ").replace("`", "");
    sql = removeRow(sql);
    sql = turnFetchNextToLimit(sql);
    sql = removeROWSInOffset(sql);
    return sql;
  }

  // "ROW(a, b) IN ..." (invalid) -> "(a, b) IN ..." (valid)
  // remove ROW directly
  private static String removeRow(String str) {
    Pattern pattern = Pattern.compile("ROW\\([A-Z0-9]+\\.[A-Z0-9]+(, [A-Z0-9]+\\.[A-Z0-9]+)*\\)");
    Matcher matcher = pattern.matcher(str);
    while (matcher.find()) {
      int pos = matcher.start();
      str = str.substring(0, pos) + str.substring(pos + 3);
      pattern = Pattern.compile("ROW\\([A-Z0-9]+\\.[A-Z0-9]+(, [A-Z0-9]+\\.[A-Z0-9]+)*\\)");
      matcher = pattern.matcher(str);
    }
    return str;
  }

  // "FETCH NEXT n ROWS ONLY" -> "LIMIT n"
  private static String turnFetchNextToLimit(String str) {
    Pattern pattern = Pattern.compile("FETCH NEXT [0-9]+ ROWS ONLY");
    Matcher matcher = pattern.matcher(str);
    while (matcher.find()) {
      int start = matcher.start(), end = matcher.end();
      String num = str.substring(start + 11, end - 10);
      String limit = "LIMIT " + num;
      str = str.substring(0, start) + limit + str.substring(end);
      pattern = Pattern.compile("FETCH NEXT [0-9]+ ROWS ONLY");
      matcher = pattern.matcher(str);
    }
    return str;
  }

  private static String removeROWSInOffset(String str) {
    Pattern pattern = Pattern.compile("OFFSET [0-9]+ ROWS");
    Matcher matcher = pattern.matcher(str);
    while (matcher.find()) {
      int start = matcher.start(), end = matcher.end();
      String num = str.substring(start + 11, end - 10);
      String limit = "OFFSET " + num;
      str = str.substring(0, start) + limit + str.substring(end);
      pattern = Pattern.compile("OFFSET [0-9]+ ROWS");
      matcher = pattern.matcher(str);
    }
    return str;
  }

  private static synchronized void init() {
    if (rewriters == null) {
      rewriters = new ArrayList<>();
      registerPreprocessors();
    }
  }

  /**
   * Call all the registered preprocessors one by one
   * to preprocess the given SqlNode.
   *
   * @param node the SqlNode to be preprocessed.
   * @return the SqlNode after preprocessed by all registered preprocessors.
   */
  public static SqlNode preprocessAll(SqlNode node) {
    init();
    for (SqlNodePreprocess rewriter : rewriters) {
      try {
        node = rewriter.preprocess(node);
      } catch (Exception e) {
        // keep the last result upon exception
//        e.printStackTrace();
      }
    }
    return node;
  }

  /**
   * A template method which should preprocess a given SqlNode.
   *
   * @param node the SqlNode to be preprocessed
   * @return the SqlNode after preprocessing
   */
  public abstract SqlNode preprocess(SqlNode node);
}
