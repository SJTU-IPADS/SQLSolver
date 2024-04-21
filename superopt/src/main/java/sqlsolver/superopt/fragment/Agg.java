package sqlsolver.superopt.fragment;

public interface Agg extends Op {
  // Agg <grpAttrs aggAttrs aggOutputAttrs aggFunc schema havingPred>
  Symbol groupByAttrs();

  Symbol aggregateAttrs();

  Symbol aggregateOutputAttrs();

  Symbol aggFunc();

  AggFuncKind aggFuncKind();

  void setAggFuncKind(AggFuncKind kind);

  Symbol schema();

  Symbol havingPred();

  boolean deduplicated();

  void setDeduplicated(boolean flag);

  @Override
  default OpKind kind() {
    return OpKind.AGG;
  }
}
