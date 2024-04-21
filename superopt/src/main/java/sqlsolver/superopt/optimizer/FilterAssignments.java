package sqlsolver.superopt.optimizer;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.plan.*;

import java.util.ArrayList;
import java.util.List;

import static sqlsolver.sql.SqlSupport.*;
import static sqlsolver.sql.ast.constants.BinaryOpKind.AND;
import static sqlsolver.sql.plan.PlanKind.Filter;

class FilterAssignments {
  private final FilterChain filterChain;
  private final int[][] assignments;
  private final boolean[] used;
  private int numUsed;

  FilterAssignments(FilterChain filters, int numOps) {
    this.filterChain = filters;
    this.assignments = new int[numOps][];
    this.used = new boolean[filters.size()];
  }

  void setExact(int opIdx, int filterIdx) {
    assignments[opIdx] = new int[] {filterIdx};
    used[filterIdx] = true;
    ++numUsed;
  }

  void setCombined(int opIdx, int... filterIndices) {
    if (filterIndices.length == 1) {
      setExact(opIdx, filterIndices[0]);
      return;
    }

    assert filterIndices.length >= 2;
    assignments[opIdx] = filterIndices;
    for (int filterIndex : filterIndices) used[filterIndex] = true;
    numUsed += filterIndices.length;
  }

  void unset(int opIdx) {
    final int[] assignment = assignments[opIdx];
    if (assignment == null) return;

    assignments[opIdx] = null;
    for (int i : assignment) used[i] = false;
    numUsed -= assignment.length;
  }

  boolean isUsed(int filterIdx) {
    return used[filterIdx];
  }

  public int numUsed() {
    return numUsed;
  }

  public int numUnused() {
    return filterChain.size() - numUsed;
  }

  FilterChain mkChain(boolean appendUnused) {
    final PlanContext plan = filterChain.plan().copy();
    final TIntList filterNodes = new TIntArrayList(filterChain.size());

    if (!appendUnused) addUnused(filterNodes);

    for (final int[] assignment : assignments) {
      assert assignment != null && assignment.length > 0;
      if (assignment.length == 1) filterNodes.add(filterChain.at(assignment[0]));
      else filterNodes.add(mkMerged(plan, assignment));
    }

    if (appendUnused) addUnused(filterNodes);

    return filterChain.derive(filterNodes.toArray()).setPlan(plan);
  }

  private void addUnused(TIntList dest) {
    for (int i = 0, bound = used.length; i < bound; i++) if (!used[i]) dest.add(filterChain.at(i));
  }

  private int mkMerged(PlanContext plan, int... indices) {
    final int[] mergedFilters = new int[indices.length];
    final Expression[] exprs = new Expression[indices.length];

    for (int i = 0, bound = indices.length; i < bound; i++) {
      final int filterNode = filterChain.at(indices[bound - i - 1]);
      mergedFilters[i] = filterNode;
      exprs[i] = exprOf(plan, filterNode);
    }

    final ValuesRegistry valuesReg = plan.valuesReg();
    final SqlContext tempCtx = SqlContext.mk(16);
    final TIntList colRefs = new TIntArrayList();
    final List<Value> valueRefs = new ArrayList<>();
    SqlNode mergedAst = null;
    for (Expression expr : exprs) {
      final int[] colRefIds = idsOf(expr.internalRefs());
      final SqlNode copiedAst = copyAst(expr.template()).track(colRefIds).to(tempCtx).go();
      if (mergedAst == null) mergedAst = copiedAst;
      else mergedAst = mkBinary(tempCtx, AND, mergedAst, copiedAst);
      colRefs.addAll(colRefIds);
      valueRefs.addAll(valuesReg.valueRefsOf(expr));
    }

    final Expression expr = Expression.mk(mergedAst, SqlNodes.mk(tempCtx, colRefs));
    final SimpleFilterNode filterNode = SimpleFilterNode.mk(expr);
    final int mergedNode = plan.bindNode(filterNode);

    plan.valuesReg().bindValueRefs(expr, valueRefs);
    plan.infoCache().putVirtualExpr(expr, mergedFilters);

    return mergedNode;
  }

  private static Expression exprOf(PlanContext plan, int nodeId) {
    final PlanNode filter = plan.nodeAt(nodeId);
    if (filter.kind() == Filter) return ((SimpleFilterNode) filter).predicate();
    else return plan.infoCache().getSubqueryExprOf(nodeId);
  }
}
