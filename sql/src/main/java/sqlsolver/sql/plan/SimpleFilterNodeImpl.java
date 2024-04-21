package sqlsolver.sql.plan;

class SimpleFilterNodeImpl implements SimpleFilterNode {
  private Expression predicate;

  SimpleFilterNodeImpl(Expression predicate) {
    this.predicate = predicate;
  }

  @Override
  public Expression predicate() {
    return predicate;
  }

  @Override
  public void setPredicate(Expression predicate) {
    this.predicate = predicate;
  }
}
