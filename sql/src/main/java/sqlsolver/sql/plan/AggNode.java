package sqlsolver.sql.plan;

import java.util.List;

public interface AggNode extends Exporter, PlanNode {
  List<Expression> groupByExprs();

  Expression havingExpr();

  @Override
  default PlanKind kind() {
    return PlanKind.Agg;
  }

  static AggNode mk(
      boolean deduplicated,
      List<String> attrNames,
      List<Expression> attrExprs,
      List<Expression> groupByExprs,
      Expression havingExpr) {
    return new AggNodeImpl(deduplicated, attrNames, attrExprs, groupByExprs, havingExpr);
  }
}
