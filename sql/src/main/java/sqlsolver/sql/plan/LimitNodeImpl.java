package sqlsolver.sql.plan;

public class LimitNodeImpl implements LimitNode {
  private final Expression limit;
  private final Expression offset;

  public LimitNodeImpl(Expression limit, Expression offset) {
    this.limit = limit;
    this.offset = offset;
  }

  @Override
  public Expression limit() {
    return limit;
  }

  @Override
  public Expression offset() {
    return offset;
  }
}
