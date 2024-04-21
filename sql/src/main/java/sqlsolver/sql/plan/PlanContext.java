package sqlsolver.sql.plan;

import sqlsolver.common.tree.UniformTreeContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.schema.Schema;

public interface PlanContext extends UniformTreeContext<PlanKind> {
  Schema schema();

  PlanNode nodeAt(int id);

  int nodeIdOf(PlanNode node);

  int bindNode(PlanNode node);

  ValuesRegistry valuesReg();

  InfoCache infoCache();

  void setSubQueryPlanRootId(int sqlNodeId, int rootId);

  int getSubQueryPlanRootId(int sqlNodeId);

  void setSubQueryPlanRootIdBySqlNode(SqlNode sqlNode, int rootId);

  int getSubQueryPlanRootIdBySqlNode(SqlNode sqlNode);

  PlanContext copy();

  @Override
  int root();

  PlanContext setRoot(int rootId);

  default Values valuesOf(PlanNode node) {
    return valuesReg().valuesOf(nodeIdOf(node));
  }

  default PlanNode planRoot() {
    return nodeAt(root());
  }

  static PlanContext mk(Schema schema, int root) {
    return new PlanContextImpl(root, 16, schema);
  }

  default void myDeleteNode(int nodeId) {}
}
