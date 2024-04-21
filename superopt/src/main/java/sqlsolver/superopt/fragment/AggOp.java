package sqlsolver.superopt.fragment;

import static sqlsolver.superopt.fragment.AggFuncKind.*;

class AggOp extends BaseOp implements Agg {
  private AggFuncKind aggFuncKind;
  private boolean deduplicated;

  AggOp() {
    // By default config
    this(UNKNOWN, false);
  }

  AggOp(AggFuncKind aggFuncKind, boolean deduplicated) {
    this.aggFuncKind = aggFuncKind;
    this.deduplicated = aggFuncKind == COUNT ? deduplicated : false;
  }

  @Override
  protected Op copy0() {
    return new AggOp(aggFuncKind, deduplicated);
  }

  @Override
  public Symbol groupByAttrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 0);
  }

  @Override
  public Symbol aggregateAttrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 1);
  }

  @Override
  public Symbol aggregateOutputAttrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 2);
  }

  @Override
  public Symbol aggFunc() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.FUNC, 0);
  }

  @Override
  public AggFuncKind aggFuncKind() {
    return aggFuncKind;
  }

  @Override
  public void setAggFuncKind(AggFuncKind kind) {
    this.aggFuncKind = kind;
  }

  @Override
  public Symbol schema() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.SCHEMA, 0);
  }

  @Override
  public Symbol havingPred() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.PRED, 0);
  }

  @Override
  public boolean deduplicated() {
    return deduplicated;
  }

  @Override
  public void setDeduplicated(boolean flag) {
    this.deduplicated = flag;
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterAgg(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveAgg(this);
  }
}
