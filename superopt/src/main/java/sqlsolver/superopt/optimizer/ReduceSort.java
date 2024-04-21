package sqlsolver.superopt.optimizer;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import sqlsolver.common.utils.Lazy;
import sqlsolver.sql.plan.AggNode;
import sqlsolver.sql.plan.Expression;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanKind;

import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.common.tree.TreeSupport.rootOf;
import static sqlsolver.sql.ast.ExprFields.Aggregate_Name;
import static sqlsolver.sql.plan.PlanKind.*;

class ReduceSort {
  private final PlanContext plan;

  private final Lazy<TIntSet> sortNodes;
  private final Lazy<TIntSet> enforcers;

  private boolean isReduced;

  ReduceSort(PlanContext plan) {
    this.plan = plan;
    this.sortNodes = Lazy.mk(TIntHashSet::new);
    this.enforcers = Lazy.mk(TIntHashSet::new);
    this.isReduced = false;
  }

  int reduce(int planRoot) {
    final int anchor = getAnchor(planRoot);
    gatherSortEnforcer(resolveSortChain(planRoot));

    if (!sortNodes.isInitialized()) return planRoot;

    final TIntSet toRemove = sortNodes.get();
    if (enforcers.isInitialized()) toRemove.removeAll(enforcers.get());
    if (toRemove.isEmpty()) return planRoot;

    isReduced = true;
    toRemove.forEach(this::removeSort);

    if (!toRemove.contains(planRoot)) return planRoot;

    final int newRoot = rootOf(plan, anchor);
    plan.setRoot(newRoot);
    return newRoot;
  }

  public boolean isReduced() {
    return isReduced;
  }

  private void addSortNode(int sortNode) {
    assert plan.kindOf(sortNode) == PlanKind.Sort;
    sortNodes.get().add(sortNode);
  }

  private void addEnforcer(int sortNode) {
    assert plan.kindOf(sortNode) == PlanKind.Sort;
    enforcers.get().add(sortNode);
  }

  private SortSpec resolveSortChain(int node) {
    final PlanKind kind = plan.kindOf(node);
    if (kind == Input) return null;

    final SortSpec sort0 = resolveSortChain(plan.childOf(node, 0));
    final SortSpec sort1 = kind.numChildren() >= 2 ? resolveSortChain(plan.childOf(node, 1)) : null;
    if (kind == Filter) return sort0;
    if (kind.isSubqueryFilter()) {
      resolveSortChain(plan.childOf(node, 1));
      return sort0;
    }

    if (kind == Join || kind == SetOp) {
      final boolean preserve0 = sort0 != null && sort0.limited;
      final boolean preserve1 = sort1 != null && sort1.limited;

      final SortSpec[] basedOn;
      if (preserve0 && preserve1) basedOn = new SortSpec[] {sort0, sort1};
      else if (preserve0) basedOn = new SortSpec[] {sort0};
      else if (preserve1) basedOn = new SortSpec[] {sort1};
      else return null;

      return new SortSpec(NO_SUCH_NODE, false, basedOn);
    }

    if (kind == Proj)
      return sort0 != null
          ? sort0.limited ? sort0 : new SortSpec(NO_SUCH_NODE, false, sort0.basedOn)
          : null;

    if (kind == Limit)
      return sort0 == null ? null : new SortSpec(sort0.enforcer, true, sort0.basedOn);

    if (kind == Agg)
      if (sort0 == null) return null;
      else if (isCountAgg(node)) return new SortSpec(NO_SUCH_NODE, false, sort0.basedOn);
      else return sort0;

    if (kind == Sort) {
      addSortNode(node);

      if (sort0 == null) return new SortSpec(node, false, null);
      else if (sort0.limited) return new SortSpec(node, false, new SortSpec[] {sort0});
      else return new SortSpec(node, false, sort0.basedOn);
    }

    assert false;
    return null;
  }

  private void gatherSortEnforcer(SortSpec sort) {
    if (sort == null) return;
    if (sort.enforcer != NO_SUCH_NODE) addEnforcer(sort.enforcer);
    if (sort.basedOn != null) for (SortSpec base : sort.basedOn) gatherSortEnforcer(base);
  }

  private boolean isCountAgg(int nodeId) {
    assert plan.kindOf(nodeId) == Agg;
    final List<Expression> exprs = ((AggNode) plan.nodeAt(nodeId)).attrExprs();
    for (Expression expr : exprs) {
      if ("count".equalsIgnoreCase(expr.template().$(Aggregate_Name))) {
        return true;
      }
    }
    return false;
  }

  private boolean removeSort(int nodeId) {
    assert plan.kindOf(nodeId) == Sort;
    final int parent = plan.parentOf(nodeId);
    final int replacement = plan.childOf(nodeId, 0);
    final int childIdx = parent == NO_SUCH_NODE ? -1 : indexOfChild(plan, nodeId);

    plan.detachNode(replacement);
    plan.detachNode(nodeId);
    if (parent != NO_SUCH_NODE) plan.setChild(parent, childIdx, replacement);

    return true;
  }

  private int getAnchor(int node) {
    while (plan.kindOf(node) == Sort) node = plan.childOf(node, 0);
    return node;
  }

  private static class SortSpec {
    private final int enforcer;
    private final boolean limited;
    private final SortSpec[] basedOn; // at most of the length 2

    private SortSpec(int enforcer, boolean limited, SortSpec[] basedOn) {
      this.enforcer = enforcer;
      this.limited = limited;
      this.basedOn = basedOn;
      assert basedOn == null || basedOn.length <= 2;
    }
  }
}
