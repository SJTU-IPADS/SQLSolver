package sqlsolver.sql.copreprocess;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry of co-preprocessing SqlNodes (Calcite),
 * and the parent class of all co-rewriters that
 * co-rewrite SqlNodes.
 */
public abstract class SqlNodeCoPreprocess {

  private static List<SqlNodeCoPreprocess> rewriters = null;

  /** HINT: Add new SqlNode co-preprocessors HERE. */
  private static void registerCoPreprocessors() {
    rewriters.add(new ColumnAliasCoRewriter());
  }

  /**
   * Apply all the registered co-preprocessors one by one
   * to preprocess the given query pair.
   * @param sql0 the first SQL query to be preprocessed
   * @param sql1 the second SQL query to be preprocessed
   * @return the query pair after co-preprocessed by all registered co-preprocessors
   */
  public static String[] coPreprocessAll(String sql0, String sql1) {
    FrameworkConfig config = Frameworks.newConfigBuilder().build();
    Planner planner = Frameworks.getPlanner(config);
    try {
      // string -> sqlnode
      SqlNode node0 = planner.parse(sql0);
      planner.close();
      SqlNode node1 = planner.parse(sql1);
      String strOld0 = node0.toString();
      String strOld1 = node1.toString();
      // preprocess sqlnode
      SqlNode[] nodes = coPreprocessAll(new SqlNode[]{node0, node1}); // noexcept
      node0 = nodes[0];
      node1 = nodes[1];
      // sqlnode -> string
      String strNew0 = node0.toString();
      String strNew1 = node1.toString();
      if (strNew0.equals(strOld0) && strNew1.equals(strOld1)) {
        // unchanged
        return new String[]{sql0, sql1};
      }
      strNew0 = strNew0.replace("\n", " ").replace("\r", " ").replace("`", "");
      strNew1 = strNew1.replace("\n", " ").replace("\r", " ").replace("`", "");
      return new String[]{strNew0, strNew1};
    } catch (Exception e) {
      // stay unchanged upon exception
      return new String[]{sql0, sql1};
    }
  }

  private static synchronized void init() {
    if (rewriters == null) {
      rewriters = new ArrayList<>();
      registerCoPreprocessors();
    }
  }

  /**
   * Call all the registered co-preprocessors one by one
   * to co-preprocess the given SqlNode pair.
   * @param nodes the SqlNode pair to be co-preprocessed
   * @return the SqlNode pair after co-preprocessed by all registered co-preprocessors
   */
  public static SqlNode[] coPreprocessAll(SqlNode[] nodes) {
    init();
    try {
      for (SqlNodeCoPreprocess rewriter : rewriters) {
        nodes = rewriter.coPreprocess(nodes);
      }
    } catch (Exception e) {
      // keep the last result upon exception
//      e.printStackTrace();
    }
    return nodes;
  }

  /**
   * A template method which should co-preprocess given SqlNodes.
   * @param nodes the SqlNodes to be co-preprocessed
   * @return the SqlNodes after preprocessing
   */
  public abstract SqlNode[] coPreprocess(SqlNode[] nodes);
}
