package sqlsolver.sql.support.locator;

import sqlsolver.sql.ast.FieldDomain;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static sqlsolver.common.utils.FuncSupport.pred;

public class NodeLocatorBuilder {
  private boolean scoped;
  private boolean bottomUp;
  private Predicate<SqlNode> criteria;
  private Predicate<SqlNode> brake;

  public NodeLocatorBuilder scoped(boolean scoped) {
    this.scoped = scoped;
    return this;
  }

  public NodeLocatorBuilder bottomUp(boolean bottomUp) {
    this.bottomUp = bottomUp;
    return this;
  }

  public NodeLocatorBuilder accept(FieldDomain domain) {
    requireNonNull(domain);
    final Predicate<SqlNode> criterion = domain::isInstance;
    if (this.criteria == null) criteria = criterion;
    else criteria = criteria.and(criterion);
    return this;
  }

  public NodeLocatorBuilder accept(Predicate<SqlNode> criterion) {
    requireNonNull(criterion);
    if (criteria == null) criteria = criterion;
    else criteria = criteria.and(criterion);
    return this;
  }

  public NodeLocatorBuilder stopIf(Predicate<SqlNode> stopAt) {
    requireNonNull(stopAt);
    if (brake == null) brake = stopAt;
    else brake = brake.and(stopAt);
    return this;
  }

  public NodeLocatorBuilder stopIfNot(FieldDomain domain) {
    requireNonNull(domain);
    if (brake == null) brake = pred(domain::isInstance).negate();
    else brake = brake.and(pred(domain::isInstance).negate());
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

  private NodeLocator mkLocator(int expectedNodes) {
    final Predicate<SqlNode> criteria = this.criteria;
    final Predicate<SqlNode> brake = this.brake;

    return new NodeLocator(scoped, bottomUp, expectedNodes) {
      @Override
      protected boolean shouldStop(SqlNode node) {
        return brake != null && brake.test(node);
      }

      @Override
      protected boolean shouldAccept(SqlNode node) {
        return criteria == null || criteria.test(node);
      }
    };
  }
}
