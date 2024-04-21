package sqlsolver.sql.plan;

import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Table;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.function.Predicate.not;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.sql.SqlSupport.isEquiConstPredicate;

class UniquenessInference {
  private final PlanContext ctx;

  UniquenessInference(PlanContext ctx) {
    this.ctx = ctx;
  }

  boolean isUnique(int surfaceId) {
    return isUniqueCoreAt(new HashSet<>(ctx.valuesReg().valuesOf(surfaceId)), surfaceId);
  }

  boolean isUniqueCoreAt(Collection<Value> toCheck, int surfaceId) {
    if (toCheck instanceof Set<Value>) return isUniqueCoreAt((Set<Value>) toCheck, surfaceId);
    else return isUniqueCoreAt(new HashSet<>(toCheck), surfaceId);
  }

  boolean isUniqueCoreAt(Set<Value> toCheck, int surfaceId) {
    final PlanKind kind = ctx.kindOf(surfaceId);
    return switch (kind) {
      case Input -> onInput(toCheck, surfaceId);
      case Join -> onJoin(toCheck, surfaceId);
      case Proj -> onProj(toCheck, surfaceId);
      case Filter -> onFilter(toCheck, surfaceId);
      case Agg -> onAgg(toCheck, surfaceId);
      default -> isUniqueCoreAt(toCheck, ctx.childOf(surfaceId, 0));
    };
  }

  private boolean onInput(Set<Value> toCheck, int surfaceId) {
    // Input: check if the attrs are one of the defined Unique Key
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final Set<Column> columns = SetSupport.map(toCheck, valuesReg::columnOf);
    final Table table = ((InputNode) ctx.nodeAt(surfaceId)).table();
    return any(table.constraints(ConstraintKind.UNIQUE), it -> columns.containsAll(it.columns()));
  }

  private boolean onJoin(Set<Value> toCheck, int surfaceId) {
    // Join: split the `toCheck` by sides, check if both are the unique key
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final Values lhsValues = valuesReg.valuesOf(ctx.childOf(surfaceId, 0));
    final Set<Value> lhsToCheck = SetSupport.filter(toCheck, lhsValues::contains);
    final Set<Value> rhsToCheck = SetSupport.filter(toCheck, not(lhsToCheck::contains));

    final JoinNode joinNode = (JoinNode) ctx.nodeAt(surfaceId);
    final Expression joinCond = joinNode.joinCond();
    if (ctx.infoCache().isEquiJoin(surfaceId)) {
      // For equi-join, the attrs will be expanded symmetrically.
      // e.g. ["t1.k1"] is the unique-core of Join<t1.k1=t2.k2>(t1,t2)
      // as long as ["t1.k1"] and ["t2.k2"] are the unique-core of t1 and t2, respectively.
      final Values joinKeys = valuesReg.valueRefsOf(joinCond);
      for (int i = 0, bound = joinKeys.size() - 1; i < bound; i += 2) {
        final Value key0 = joinKeys.get(i), key1 = joinKeys.get(i + 1);
        if (lhsToCheck.contains(key0)) rhsToCheck.add(key1);
        else if (rhsToCheck.contains(key0)) lhsToCheck.add(key1);
        if (lhsToCheck.contains(key1)) rhsToCheck.add(key0);
        else if (rhsToCheck.contains(key1)) lhsToCheck.add(key0);
      }
    }

    return isUniqueCoreAt(rhsToCheck, ctx.childOf(surfaceId, 1))
        && isUniqueCoreAt(lhsToCheck, ctx.childOf(surfaceId, 0));
  }

  private boolean onProj(Set<Value> toCheck, int surfaceId) {
    // Proj: check if the referenced attrs are the unique-core of input of Proj node.
    // e.g. ["n"] is the unique-core of Proj<t.m AS n>(t)
    // as long as ["t.m"] is the unique-core of t
    if (PlanSupport.isDedup(ctx, surfaceId)) return true;
    final Set<Value> refAttrs = new HashSet<>(toCheck.size());
    for (Value value : toCheck) {
      final Value ref = PlanSupport.deRef(ctx, value);
      if (ref != null) refAttrs.add(ref);
    }
    return isUniqueCoreAt(refAttrs, ctx.childOf(surfaceId, 0));
  }

  private boolean onFilter(Set<Value> toCheck, int surfaceId) {
    // Filter: check if the attrs are the unique core of Filter's input
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final Expression predicate = ((SimpleFilterNode) ctx.nodeAt(surfaceId)).predicate();
    if (isEquiConstPredicate(predicate.template())) {
      // If the filter is in form of "col_ref = const_value", then add "col_ref" to `to_check`
      // e.g. [] is the unique core of Filter<t.a = 1>(t)
      // as long as ["t.a"] is the unique core of t.
      final Values refs = valuesReg.valueRefsOf(predicate);
      assert refs.size() == 1;
      toCheck.add(refs.get(0));
    }
    return isUniqueCoreAt(toCheck, ctx.childOf(surfaceId, 0));
  }

  private boolean onAgg(Set<Value> toCheck, int surfaceId) {
    // Agg: check if all group keys are contained by `toCheck`
    // e.g., ["t.a","t.b"] is the unique core of Agg<group=["t.a"]>(t)
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final List<Expression> groupExprs = ((AggNode) ctx.nodeAt(surfaceId)).groupByExprs();
    final Set<Value> groupAttrs = SetSupport.flatMap(groupExprs, valuesReg::valueRefsOf);

    for (Value value : toCheck) {
      final Value ref = PlanSupport.deRef(ctx, value);
      if (ref == null) return false;
      groupAttrs.remove(ref);
    }
    return groupAttrs.isEmpty();
  }
}
