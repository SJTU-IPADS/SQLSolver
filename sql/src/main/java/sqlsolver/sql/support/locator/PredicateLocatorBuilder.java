package sqlsolver.sql.support.locator;

import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;

public class PredicateLocatorBuilder {
  private boolean scoped;
  private boolean bottomUp;
  private boolean primitive;
  private boolean conjunctionOnly;
  private boolean breakdownExpr;

  public PredicateLocatorBuilder scoped() {
    this.scoped = true;
    return this;
  }

  public PredicateLocatorBuilder bottomUp() {
    this.bottomUp = true;
    return this;
  }

  public PredicateLocatorBuilder primitive() {
    this.primitive = true;
    return this;
  }

  public PredicateLocatorBuilder conjunctive() {
    this.conjunctionOnly = true;
    return this;
  }

  public PredicateLocatorBuilder breakdownExpr() {
    this.breakdownExpr = true;
    return this;
  }

  public SqlFinder finder() {
    return mkLocator(1);
  }

  public SqlGatherer gatherer() {
    return mkLocator(-1);
  }

  public SqlNode find(SqlNode node) {
    return finder().findNode(node);
  }

  public SqlNodes gather(SqlNode node) {
    return gatherer().gatherNodes(node);
  }

  private PredicateLocator mkLocator(int expectedNodes) {
    return new PredicateLocator(
        scoped, bottomUp, primitive, conjunctionOnly, breakdownExpr, expectedNodes);
  }
}
