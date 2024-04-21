package sqlsolver.sql.support.locator;

import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class ClauseLocatorBuilder {
  private boolean scoped;
  private boolean bottomUp;

  private Predicate<FieldKey<?>> criteria;

  public ClauseLocatorBuilder scoped() {
    this.scoped = true;
    return this;
  }

  public ClauseLocatorBuilder bottomUp(boolean bottomUp) {
    this.bottomUp = bottomUp;
    return this;
  }

  public ClauseLocatorBuilder accept(FieldKey<?> domain) {
    requireNonNull(domain);
    final Predicate<FieldKey<?>> criterion = domain::equals;
    if (this.criteria == null) criteria = criterion;
    else criteria = criteria.and(criterion);
    return this;
  }

  public ClauseLocatorBuilder accept(Predicate<FieldKey<?>> criterion) {
    requireNonNull(criterion);
    if (criteria == null) criteria = criterion;
    else criteria = criteria.and(criterion);
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

  private ClauseLocator mkLocator(int expectedNodes) {
    final Predicate<FieldKey<?>> criteria = this.criteria;

    return new ClauseLocator(scoped, bottomUp, expectedNodes) {
      @Override
      protected boolean shouldAccept(FieldKey<?> clause) {
        return criteria == null || criteria.test(clause);
      }
    };
  }
}
