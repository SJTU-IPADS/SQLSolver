package sqlsolver.sql.plan;

import java.util.List;

public interface ProjNode extends Exporter, PlanNode {
  void setDeduplicated(boolean deduplicated);

  @Override
  default PlanKind kind() {
    return PlanKind.Proj;
  }

  static ProjNode mk(boolean deduplicated, List<String> attrNames, List<Expression> attrExprs) {
    return new ProjNodeImpl(deduplicated, attrNames, attrExprs);
  }
}
