package sqlsolver.sql.plan;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.SqlNodeFields;
import sqlsolver.sql.ast.SqlNode;

import java.util.Collections;
import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.utils.Commons.dumpException;

class ValueRefBinder {
  private final PlanContext plan;
  private final ValuesRegistry valuesReg;

  private String error;

  ValueRefBinder(PlanContext plan) {
    this.plan = plan;
    this.valuesReg = plan.valuesReg();
  }

  boolean bind() {
    try {
      return bind0(plan.root(), Collections.emptyList());
    } catch (RuntimeException ex) {
      error = dumpException(ex);
      return false;
    }
  }

  String lastError() {
    return error;
  }

  private boolean bind0(int nodeId, List<Value> secondaryLookup) {
    if (nodeId == NO_SUCH_NODE) return true;

    return switch (plan.kindOf(nodeId)) {
      case Limit -> bind0(plan.childOf(nodeId, 0), secondaryLookup);
      case Sort -> bind0(plan.childOf(nodeId, 0), secondaryLookup) && bindSort(nodeId);
      case SetOp -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
          && bind0(plan.childOf(nodeId, 1), secondaryLookup);
      case Join -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
          && bind0(plan.childOf(nodeId, 1), secondaryLookup)
          && bindJoin(nodeId);
      case Agg -> bind0(plan.childOf(nodeId, 0), secondaryLookup) && bindAgg(nodeId);
      case Proj -> bind0(plan.childOf(nodeId, 0), secondaryLookup) && bindProj(nodeId);
      case Filter -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
          && bindFilter(nodeId, secondaryLookup);
      case Exists -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
          && bindExists(nodeId, secondaryLookup);
      case InSub -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
          && bindInSub(nodeId, secondaryLookup);
      case Input -> true;
    };
  }

  private boolean bindAgg(int nodeId) {
    // Group By & Having can use the attribute exposed by Aggregation.
    final AggNode aggNode = (AggNode) plan.nodeAt(nodeId);
    final Values secondaryLookup = valuesReg.valuesOf(plan.childOf(plan.childOf(nodeId, 0), 0));
    final List<Value> lookup = PlanSupport.getRefBindingLookup(plan, nodeId);

    for (Expression attr : aggNode.attrExprs()) {
      final List<Value> refs = ListSupport.map(attr.colRefs(), it -> bindRef(it, secondaryLookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(attr, refs);
    }

    for (Expression groupBy : aggNode.groupByExprs()) {
      final List<Value> refs = ListSupport.map(groupBy.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(groupBy, refs);
    }

    final Expression having = aggNode.havingExpr();
    if (having != null) {
      final List<Value> refs = ListSupport.map(having.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(having, refs);
    }

    return true;
  }

  private boolean bindProj(int nodeId) {
    final List<Value> lookup = PlanSupport.getRefBindingLookup(plan, nodeId);
    final ProjNode projNode = (ProjNode) plan.nodeAt(nodeId);

    final int[] childrenIds = plan.childrenOf(nodeId);
    for (int i = 1; i < childrenIds.length; i++) {
      // Bind value for inlined sub-queries
      if (childrenIds[i] == NO_SUCH_NODE) continue;
      if (!bind0(plan.childOf(nodeId, i), lookup)) return false;
    }

    for (Expression attr : projNode.attrExprs()) {
      final List<Value> refs = ListSupport.map(attr.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(attr, refs);
    }

    return true;
  }

  private boolean bindJoin(int nodeId) {
    final List<Value> lookup = PlanSupport.getRefBindingLookup(plan, nodeId);
    final JoinNode joinNode = (JoinNode) plan.nodeAt(nodeId);
    final Expression joinCond = joinNode.joinCond();
    if (joinCond == null) return true;

    final List<Value> valueRefs = ListSupport.map(joinCond.colRefs(), it -> bindRef(it, lookup));
    valuesReg.bindValueRefs(joinCond, valueRefs);
    if (valueRefs.contains(null)) return false;

    PlanSupport.setupJoinKeyOf(plan, nodeId);
    return true;
  }

  private boolean bindFilter(int nodeId, List<Value> secondaryLookup) {
    final List<Value> primaryLookup = PlanSupport.getRefBindingLookup(plan, nodeId);
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final int[] childrenIds = plan.childrenOf(nodeId);
    for (int i = 1; i < childrenIds.length; i++) {
      // Bind value for inlined sub-queries
      if (childrenIds[i] == NO_SUCH_NODE) continue;
      if (!bind0(plan.childOf(nodeId, i), lookup)) return false;
    }
    final Expression predicate = ((SimpleFilterNode) plan.nodeAt(nodeId)).predicate();
    final List<Value> valueRefs = ListSupport.map(predicate.colRefs(), it -> bindRef(it, lookup));
    valuesReg.bindValueRefs(predicate, valueRefs);
    return !valueRefs.contains(null);
  }

  private boolean bindExists(int nodeId, List<Value> secondaryLookup) {
    final List<Value> primaryLookup = PlanSupport.getRefBindingLookup(plan, nodeId);
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    if (!bind0(plan.childOf(nodeId, 1), lookup)) return false;

    if (PlanSupport.setupSubqueryExprOf(plan, nodeId)) return true;
    else return onError(PlanSupport.FAILURE_BAD_SUBQUERY_EXPR);
  }

  private boolean bindInSub(int nodeId, List<Value> secondaryLookup) {
    final List<Value> primaryLookup = PlanSupport.getRefBindingLookup(plan, nodeId);
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final InSubNode inSub = (InSubNode) plan.nodeAt(nodeId);
    final Expression lhsExpr = inSub.expr();
    final List<Value> valueRefs = ListSupport.map(lhsExpr.colRefs(), it -> bindRef(it, lookup));

    valuesReg.bindValueRefs(lhsExpr, valueRefs);
    if (valueRefs.contains(null) || !bind0(plan.childOf(nodeId, 1), lookup)) return false;

    inSub.setPlain(PlanSupport.isColRefs(inSub.expr()));

    if (PlanSupport.setupSubqueryExprOf(plan, nodeId)) return true;
    else return onError(PlanSupport.FAILURE_BAD_SUBQUERY_EXPR + inSub);
  }

  private boolean bindSort(int nodeId) {
    final List<Value> lookup = PlanSupport.getRefBindingLookup(plan, nodeId);
    final SortNode sortNode = (SortNode) plan.nodeAt(nodeId);
    final List<Expression> sortSpecs = sortNode.sortSpec();

    for (Expression sortSpec : sortSpecs) {
      final List<Value> refs = ListSupport.map(sortSpec.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(sortSpec, refs);
    }

    final List<Value> inValues = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final TIntList indexedRefs = new TIntArrayList(4);
    for (Expression sortSpec : sortSpecs) {
      for (Value ref : valuesReg.valueRefsOf(sortSpec)) {
        for (int i = 0, bound = inValues.size(); i < bound; i++) {
          final Value value = inValues.get(i);
          if (ref == value) indexedRefs.add(i);
          else if (PlanSupport.deRef(plan, value) == ref) indexedRefs.add(i);
          else indexedRefs.add(-1);
        }
      }
    }
    sortNode.setIndexedRefs(indexedRefs.toArray());

    return true;
  }

  private Value bindRef(SqlNode colRef, List<Value> lookup) {
    final SqlNode colName = colRef.$(ExprFields.ColRef_ColName);
    final String qualification = colName.$(SqlNodeFields.ColName_Table);
    final String name = colName.$(SqlNodeFields.ColName_Col);

    for (Value value : lookup) {
      if ((qualification == null || qualification.equalsIgnoreCase(value.qualification()))
          && name.equalsIgnoreCase(value.name())) {
        return value;
      }
    }

    onError(PlanSupport.FAILURE_MISSING_REF + colRef);
    return null;
  }

  private boolean onError(String error) {
    this.error = error;
    return false;
  }

}
