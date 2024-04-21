package sqlsolver.superopt.optimizer;

import sqlsolver.common.tree.TreeContext;
import sqlsolver.common.tree.TreeSupport;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanNode;

import java.util.AbstractList;
import java.util.List;

class FilterChain extends AbstractList<PlanNode> implements List<PlanNode> {
  private PlanContext plan;
  private final int chainParent, chainChild, chainPosition;
  private final int[] filterIds;

  FilterChain(
      PlanContext plan, int chainParent, int chainChild, int chainPosition, int[] filterIds) {
    this.plan = plan;
    this.chainParent = chainParent;
    this.chainChild = chainChild;
    this.chainPosition = chainPosition;
    this.filterIds = filterIds;
  }

  static FilterChain mk(PlanContext plan, int chainHead) {
    int cursor = chainHead, length = 0;
    while (plan.kindOf(cursor).isFilter()) {
      ++length;
      cursor = plan.childOf(cursor, 0);
    }

    // Note the order:
    // p AND q AND r
    // <=> Filter<r>(Filter<q>(Filter<p>(..))
    // <=> [p,q,r]
    final int chainChild = cursor;
    final int[] filterIds = new int[length];
    while ((--length) >= 0) {
      cursor = plan.parentOf(cursor);
      filterIds[length] = cursor;
    }

    final int chainParent = plan.parentOf(chainHead);
    final int childIdx = TreeSupport.indexOfChild(plan, chainHead);

    return new FilterChain(plan, chainParent, chainChild, childIdx, filterIds);
  }

  @Override
  public PlanNode get(int index) {
    return plan.nodeAt(filterIds[index]);
  }

  @Override
  public int size() {
    return filterIds.length;
  }

  @Override
  public PlanNode set(int index, PlanNode element) {
    final int oldNodeId = filterIds[index];
    filterIds[index] = plan.nodeIdOf(element);
    return plan.nodeAt(oldNodeId);
  }

  FilterChain setPlan(PlanContext plan) {
    this.plan = plan;
    return this;
  }

  PlanContext plan() {
    return plan;
  }

  int chainPosition() {
    return chainPosition;
  }

  int chainChild() {
    return chainChild;
  }

  int chainParent() {
    return chainParent;
  }

  int at(int idx) {
    return filterIds[idx];
  }

  PlanContext assemble() {
    for (int filterId : filterIds) {
      if (plan.parentOf(filterId) != TreeContext.NO_SUCH_NODE) plan.detachNode(filterId);
    }
    plan.detachNode(chainChild);

    plan.setChild(filterIds[size() - 1], 0, chainChild);
    for (int i = 1, bound = filterIds.length; i < bound; ++i)
      plan.setChild(filterIds[i - 1], 0, filterIds[i]);
    plan.setChild(chainParent, chainPosition, filterIds[0]);

    return plan;
  }

  FilterChain derive(int[] filters) {
    return new FilterChain(plan, chainParent, chainChild, chainPosition, filters);
  }
}
