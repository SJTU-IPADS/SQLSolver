package sqlsolver.superopt.optimizer;

import sqlsolver.sql.plan.*;

import java.util.ArrayList;
import java.util.List;

import static sqlsolver.common.utils.ListSupport.join;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.sql.plan.PlanSupport.*;

class ValueRefReBinder {
  private final PlanContext plan;

  ValueRefReBinder(PlanContext plan) {
    this.plan = plan;
  }

  boolean rebindToRoot(int leafNode) {
    int cursor = plan.parentOf(leafNode);
    final ValueRefReBinder reBinder = new ValueRefReBinder(plan);
    while (plan.isPresent(cursor)) {
      if (!reBinder.rebind(cursor)) return false;
      cursor = plan.parentOf(cursor);
    }
    return true;
  }

  boolean rebind(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    return switch (kind) {
      case Agg -> rebindAgg(nodeId);
      case Sort -> rebindSort(nodeId);
      case Proj -> rebindProj(nodeId);
      case Filter -> rebindFilter(nodeId);
      case InSub -> rebindInSub(nodeId);
      case Join -> rebindJoin(nodeId);
      default -> true;
    };
  }

  List<Value> rebindRefs(List<Value> refs, List<Value> inValues) {
    // Handle subquery elimination.
    // e.g., Select sub.a From (Select t.a, t.b From t) As sub
    //       -> Select sub.a From t
    // But "sub.a" is actually not present in the out-values of "(Select t.a, t.b From t) As sub".
    // We have to trace the ref-chain of "sub.a", and find that "t.a" is present.
    // Finally, we replace "sub.a" by "t.a".
    List<Value> adaptedValues = null;
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final Value adapted = rebindRef(refs.get(i), inValues);
      if (adapted == null) return null;
      if (adapted != refs.get(i)) {
        if (adaptedValues == null) adaptedValues = new ArrayList<>(refs);
        adaptedValues.set(i, adapted);
      }
    }
    return adaptedValues == null ? refs : adaptedValues;
  }

  private Value rebindRef(Value oldRef, List<Value> lookup) {
    // common case
    Value ref = oldRef;
    while (ref != null) {
      if (lookup.contains(ref)) return ref;
      ref = deRef(plan, ref);
    }
    // rare case
    final Value baseRef = traceRef(plan, oldRef);
    for (Value value : lookup) {
      if (baseRef.equals(traceRef(plan, value))) return value;
    }
    return null;
  }

  private boolean rebindAgg(int nodeId) {
    final int child = plan.childOf(nodeId, 0);
    if (plan.kindOf(child) != PlanKind.Proj) return false;

    final AggNode aggNode = (AggNode) plan.nodeAt(nodeId);
    final ProjNode projNode = (ProjNode) plan.nodeAt(child);
    final ValuesRegistry valuesReg = plan.valuesReg();
    final List<Expression> secondaryExprs = projNode.attrExprs();
    final List<Value> secondaryRefs = map(secondaryExprs, it -> valuesReg.valueRefsOf(it).get(0));
    final Values primaryRefs = valuesReg.valuesOf(nodeId);

    int globalIndex = 0;
    for (Expression attrExpr : aggNode.attrExprs())
      globalIndex = rebindAggExpr(attrExpr, primaryRefs, secondaryRefs, globalIndex);
    for (Expression groupByExpr : aggNode.groupByExprs())
      globalIndex = rebindAggExpr(groupByExpr, primaryRefs, secondaryRefs, globalIndex);
    if (aggNode.havingExpr() != null)
      rebindAggExpr(aggNode.havingExpr(), primaryRefs, secondaryRefs, globalIndex);

    return true;
  }

  private boolean rebindSort(int nodeId) {
    final List<Value> lookup = getRefBindingLookup(plan, nodeId);
    final SortNode sortNode = (SortNode) plan.nodeAt(nodeId);
    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values inValues = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final int[] indexedRefs = sortNode.indexedRefs();

    int offset = 0;
    for (Expression sortSpec : sortNode.sortSpec()){
      if (rebindExpr(sortSpec, lookup)) continue;

      final List<Value> oldRefs = valuesReg.valueRefsOf(sortSpec);
      final List<Value> newRefs = new ArrayList<>(oldRefs.size());
      for (int i = 0, bound = oldRefs.size(); i < bound; ++i, ++offset) {
        final Value ref = rebindRef(oldRefs.get(i), lookup);
        if (ref != null) newRefs.add(ref);
        else if (indexedRefs[offset] < 0) return false;
        else newRefs.add(inValues.get(indexedRefs[offset]));
      }
      valuesReg.bindValueRefs(sortSpec, newRefs);
    }
    return true;
  }

  private boolean rebindProj(int nodeId) {
    final ProjNode projNode = (ProjNode) plan.nodeAt(nodeId);
    final List<Value> lookup = getRefBindingLookup(plan, nodeId);
    for (Expression attrExpr : projNode.attrExprs())
      if (!rebindExpr(attrExpr, lookup)) return false;
    return true;
  }

  private boolean rebindFilter(int nodeId) {
    final Expression predicate = ((SimpleFilterNode) plan.nodeAt(nodeId)).predicate();
    final List<Value> lookup0 = getRefBindingLookup(plan, nodeId);
    final List<Value> lookup1 = getRefBindingForeignLookup(plan, nodeId);
    return rebindExpr(predicate, join(lookup0, lookup1));
  }

  private boolean rebindInSub(int nodeId) {
    final Expression expr = ((InSubNode) plan.nodeAt(nodeId)).expr();
    final List<Value> lookup0 = getRefBindingLookup(plan, nodeId);
    final List<Value> lookup1 = getRefBindingForeignLookup(plan, nodeId);
    return rebindExpr(expr, join(lookup0, lookup1));
  }

  private boolean rebindJoin(int nodeId) {
    final Expression joinCond = ((JoinNode) plan.nodeAt(nodeId)).joinCond();
    if (joinCond == null) return true;

    final List<Value> lookup = getRefBindingLookup(plan, nodeId);
    if (!rebindExpr(joinCond, lookup)) return false;

    setupJoinKeyOf(plan, nodeId);
    return true;
  }

  private boolean rebindExpr(Expression expr, List<Value> lookup) {
    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values oldRefs = valuesReg.valueRefsOf(expr);
    final List<Value> newRefs = rebindRefs(oldRefs, lookup);
    if (newRefs == null) return false;
    if (oldRefs == newRefs) return true;
    valuesReg.bindValueRefs(expr, newRefs);
    return true;
  }

  private int rebindAggExpr(
      Expression expr, List<Value> primaryRefs, List<Value> secondaryRefs, int fromIndex) {
    final ValuesRegistry valuesReg = plan.valuesReg();
    final List<Value> oldRefs = valuesReg.valueRefsOf(expr);
    List<Value> newRefs = null;

    for (int i = 0, bound = oldRefs.size(); i < bound; ++i) {
      final Value oldRef = oldRefs.get(i);
      if (primaryRefs.contains(oldRef)) continue;

      final Value newRef = secondaryRefs.get(fromIndex);
      if (!oldRef.equals(newRef)) {
        if (newRefs == null) newRefs = new ArrayList<>(oldRefs);
        newRefs.set(i, newRef);
      }

      ++fromIndex;
    }
    if (newRefs != null) valuesReg.bindValueRefs(expr, newRefs);

    return fromIndex;
  }
}
