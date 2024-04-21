package sqlsolver.sql.plan;

import sqlsolver.sql.schema.Column;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.constants.JoinKind;

class NotNullInference {
  private final PlanContext ctx;

  NotNullInference(PlanContext ctx) {
    this.ctx = ctx;
  }

  boolean isNotNullAt(Value toCheck, int surfaceId) {
    if (!ctx.valuesReg().valuesOf(surfaceId).contains(toCheck)) return false;

    final PlanKind kind = ctx.kindOf(surfaceId);
    if (kind == PlanKind.Input) return onInput(toCheck);
    else if (kind == PlanKind.Join) return onJoin(toCheck, surfaceId);
    else if (kind == PlanKind.Filter) return onFilter(toCheck, surfaceId);
    else if (kind == PlanKind.Proj) return onProj(toCheck, surfaceId);
    else if (kind == PlanKind.Agg) return onAgg(toCheck, surfaceId);
    else return isNotNullAt(toCheck, ctx.childOf(surfaceId, 0));
  }

  private boolean onInput(Value toCheck) {
    // Input: check if the attrs are one of the NOT_NULL keys
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final Column column = valuesReg.columnOf(toCheck);
    return column != null && column.isFlag(Column.Flag.NOT_NULL);
  }

  private boolean onAgg(Value toCheck, int surfaceId) {
    final Value ref = PlanSupport.deRef(ctx, toCheck);
    return ref != null && isNotNullAt(ref, ctx.childOf(ctx.childOf(surfaceId, 0), 0));
  }

  private boolean onProj(Value toCheck, int surfaceId) {
    final Value ref = PlanSupport.deRef(ctx, toCheck);
    return ref != null && isNotNullAt(ref, ctx.childOf(surfaceId, 0));
  }

  private boolean onJoin(Value toCheck, int surfaceId) {
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final JoinNode join = (JoinNode) ctx.nodeAt(surfaceId);
    final JoinKind joinKind = PlanSupport.joinKindOf(ctx, surfaceId);
    final Values lhsValues = valuesReg.valuesOf(ctx.childOf(surfaceId, 0));

    if (joinKind.isInner()) {
      // For InnerJoin, if `toCheck` is the join key, it must be NOT_NULL.
      // Otherwise, recursively check in the LHS or RHS
      final Values joinKeys = valuesReg.valueRefsOf(join.joinCond());
      if (joinKeys.contains(toCheck)) return true;
      else {
        final boolean fromLhs = lhsValues.contains(toCheck);
        return (fromLhs && isNotNullAt(toCheck, ctx.childOf(surfaceId, 0)))
            || (!fromLhs && isNotNullAt(toCheck, ctx.childOf(surfaceId, 1)));
      }
    }

    // Otherwise, only check from the master side.
    // e.g. From t Left Join s On ..., even if s.c is defined NOT_NULL,
    // it still can be NULL after Left-Join.
    if (joinKind == JoinKind.LEFT_JOIN) {
      return lhsValues.contains(toCheck) && isNotNullAt(toCheck, ctx.childOf(surfaceId, 0));
    } else if (joinKind == JoinKind.RIGHT_JOIN) {
      return !lhsValues.contains(toCheck) && isNotNullAt(toCheck, ctx.childOf(surfaceId, 1));
    } else {
      return false;
    }
  }

  private boolean onFilter(Value toCheck, int surfaceId) {
    // Filter: check if the attrs are NOT_NULL at Filter's input
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final Expression predicate = ((SimpleFilterNode) ctx.nodeAt(surfaceId)).predicate();
    if (SqlSupport.isEquiConstPredicate(predicate.template())) {
      // If the filter is of "col_ref = const_value", then `col_ref` must be NOT_NULL afterwards.
      final Values refs = valuesReg.valueRefsOf(predicate);
      assert refs.size() == 1;
      if (refs.get(0) == toCheck) return true;
    }
    return isNotNullAt(toCheck, ctx.childOf(surfaceId, 0));
  }
}
