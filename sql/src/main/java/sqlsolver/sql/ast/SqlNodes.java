package sqlsolver.sql.ast;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.tree.LabeledTreeNodes;

import java.util.List;

public interface SqlNodes extends LabeledTreeNodes<SqlKind, SqlContext, SqlNode> {
  static SqlNodes mkEmpty() {
    return new SqlNodesImpl(null, null);
  }

  static SqlNodes mk(SqlContext context, List<SqlNode> nodes) {
    final TIntList nodeIds = new TIntArrayList(nodes.size());
    for (SqlNode node : nodes) nodeIds.add(node.nodeId());
    return mk(context, nodeIds);
  }

  static SqlNodes mk(SqlContext context, TIntList nodeIds) {
    return new SqlNodesImpl(context, nodeIds);
  }
}
