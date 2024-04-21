package sqlsolver.sql.support.locator;

import gnu.trove.list.TIntList;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;

public interface SqlGatherer {
  TIntList gather(SqlNode root);

  default SqlNodes gatherNodes(SqlNode root) {
    return SqlNodes.mk(root.context(), gather(root));
  }

  TIntList nodeIds();
}
