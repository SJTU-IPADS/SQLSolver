package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import sqlsolver.sql.Rewriter;

public class AggGroupRewriter extends RecursiveRewriter {

  @Override
  public SqlNode handleNode(SqlNode node) {
    // SqlNode -> SQL
    String sql = node.toString().replace("\n", " ").replace("`", ""), newSql;
    if (!sql.contains("GROUP BY")) return node;
    // handle
    final Rewriter rewriter = new Rewriter(sql);
    try {
      newSql = rewriter.transform(rewriter.getSqlNode(sql), Rewriter.rewriteType.AGGGROUPBY);
    } catch (Exception e) {
      return node;
    }
    if (sql.equals(newSql)) return node;
    // SQL -> SqlNode
    FrameworkConfig config = Frameworks.newConfigBuilder().build();
    Planner planner = Frameworks.getPlanner(config);
    try {
      return planner.parse(newSql);
    } catch (Exception e) {
      // stay unchanged upon exception
      return node;
    }
  }

}
