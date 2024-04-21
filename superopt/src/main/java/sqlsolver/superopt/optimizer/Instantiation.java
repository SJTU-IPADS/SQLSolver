package sqlsolver.superopt.optimizer;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import sqlsolver.common.utils.Lazy;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.SetOpKind;
import sqlsolver.sql.plan.*;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.fragment.*;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.utils.Commons.dumpException;
import static sqlsolver.common.utils.Commons.newIdentitySet;
import static sqlsolver.common.utils.IterableSupport.zip;
import static sqlsolver.sql.ast.ExprKind.Aggregate;

class Instantiation {
  private final Substitution rule;
  private final Model model;
  private final PlanContext newPlan;
  private final ValueRefReBinder reBinder;
  private final Lazy<TIntSet> usedSubqueryNode;

  private String error;

  Instantiation(Substitution rule, Model model) {
    this.rule = rule;
    this.model = model;
    this.newPlan = model.plan().copy();
    this.reBinder = new ValueRefReBinder(newPlan);
    this.usedSubqueryNode = Lazy.mk(TIntHashSet::new);
  }

  PlanContext instantiatedPlan() {
    return newPlan;
  }

  int instantiate() {
    try {
      return instantiate(rule._1().root());
    } catch (Throwable ex) {
      error = dumpException(ex);
      return NO_SUCH_NODE;
    }
  }

  String lastError() {
    return error;
  }

  private int instantiate(Op op) {
    final OpKind kind = op.kind();
    switch (kind) {
      case INPUT:
        return instantiateInput((Input) op);
      case INNER_JOIN:
      case LEFT_JOIN:
        return instantiateJoin((Join) op);
      case SIMPLE_FILTER:
        return instantiateFilter((SimpleFilter) op);
      case IN_SUB_FILTER:
        return instantiateInSub((InSubFilter) op);
      case UNION:
        return instantiateSetOp((Union) op);
      case PROJ:
        return instantiateProj((Proj) op);
      case AGG:
        return instantiateAgg((Agg) op);
      default:
        return fail(OptimizerSupport.FAILURE_UNKNOWN_OP);
    }
  }

  private int instantiateInput(Input input) {
    final Integer nodeId = model.ofTable(instantiationOf(input.table()));
    if (nodeId == null) return fail(OptimizerSupport.FAILURE_INCOMPLETE_MODEL);

    newPlan.detachNode(nodeId);
    return nodeId;
  }

  private int instantiateJoin(Join join) {
    final int lhs = instantiate(join.predecessors()[0]);
    final int rhs = instantiate(join.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    List<Value> lhsKeys = interpretAttrs(join.lhsAttrs());
    List<Value> rhsKeys = interpretAttrs(join.rhsAttrs());

    if (lhsKeys == null || rhsKeys == null) return fail(OptimizerSupport.FAILURE_INCOMPLETE_MODEL);
    if (lhsKeys.size() != rhsKeys.size() || lhsKeys.isEmpty())
      return fail(OptimizerSupport.FAILURE_MISMATCHED_JOIN_KEYS);

    lhsKeys = reBinder.rebindRefs(lhsKeys, outValuesOf(lhs));
    rhsKeys = reBinder.rebindRefs(rhsKeys, outValuesOf(rhs));
    if (lhsKeys == null || rhsKeys == null) return fail(OptimizerSupport.FAILURE_FOREIGN_VALUE);

    final JoinKind joinKind = join.kind() == OpKind.INNER_JOIN ? JoinKind.INNER_JOIN : JoinKind.LEFT_JOIN;
    final Expression joinCond = PlanSupport.mkJoinCond(lhsKeys.size());
    final JoinNode joinNode = JoinNode.mk(joinKind, joinCond);
    final int joinNodeId = newPlan.bindNode(joinNode);

    newPlan.setChild(joinNodeId, 0, lhs);
    newPlan.setChild(joinNodeId, 1, rhs);
    newPlan.valuesReg().bindValueRefs(joinCond, interleaveJoinKeys(lhsKeys, rhsKeys));
    newPlan.infoCache().putJoinKeyOf(joinNodeId, lhsKeys, rhsKeys);

    return joinNodeId;
  }

  private List<Value> interpretAttrs(Symbol attrs) {
    final List<Value> nominalInterpretation = model.ofAttrs(instantiationOf(attrs));
    if (nominalInterpretation == null) return null;

    final Symbol actualSrcSym = rule.constraints().sourceOf(attrs);
    final Symbol nominalSrcSym = rule.constraints().sourceOf(instantiationOf(attrs));
    if (actualSrcSym == null || actualSrcSym.kind() != Symbol.Kind.TABLE || nominalSrcSym.kind() != Symbol.Kind.TABLE)
      return nominalInterpretation;

    final int actualSource = model.ofTable(actualSrcSym);
    final int nominalSource = model.ofTable(nominalSrcSym);

    final ValuesRegistry valuesReg = newPlan.valuesReg();
    final Values actualValues = valuesReg.valuesOf(actualSource);
    final Values nominalValues = valuesReg.valuesOf(nominalSource);
    if (actualValues.size() != nominalValues.size()) return null;

    final List<Value> actualInterpretation = new ArrayList<>(nominalInterpretation.size());
    for (Value nominalValue : nominalInterpretation) {
      final int index = nominalValues.indexOf(nominalValue);
      if (index < 0) return null;
      actualInterpretation.add(actualValues.get(index));
    }

    return actualInterpretation;
  }

  private int instantiateFilter(SimpleFilter filter) {
    final int child = instantiate(filter.predecessors()[0]);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final Expression predicate = model.ofPred(instantiationOf(filter.predicate()));
    List<Value> values = interpretAttrs(filter.attrs());
    if (predicate == null || values == null || values.size() != predicate.colRefs().size())
      return fail(OptimizerSupport.FAILURE_INCOMPLETE_MODEL);

    values = reBinder.rebindRefs(values, outValuesOf(child));
    if (values == null) return fail(OptimizerSupport.FAILURE_FOREIGN_VALUE);

    return mkFilterNode(predicate, values, child);
  }

  private int instantiateInSub(InSubFilter inSub) {
    final int lhs = instantiate(inSub.predecessors()[0]);
    final int rhs = instantiate(inSub.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    List<Value> values = interpretAttrs(inSub.attrs());
    if (values == null || values.isEmpty()) return fail(OptimizerSupport.FAILURE_INCOMPLETE_MODEL);

    values = reBinder.rebindRefs(values, outValuesOf(lhs));
    if (values == null) return fail(OptimizerSupport.FAILURE_FOREIGN_VALUE);

    final Expression expression = PlanSupport.mkColRefsExpr(values.size());
    final InSubNode inSubNode = InSubNode.mk(expression);
    final int inSubNodeId = newPlan.bindNode(inSubNode);

    newPlan.setChild(inSubNodeId, 0, lhs);
    newPlan.setChild(inSubNodeId, 1, rhs);
    newPlan.valuesReg().bindValueRefs(expression, values);
    return inSubNodeId;
  }

  private int instantiateProj(Proj proj) {
    final int child = instantiate(proj.predecessors()[0]);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> outAttrs = model.ofSchema(instantiationOf(proj.schema()));
    if (outAttrs == null) return fail(OptimizerSupport.FAILURE_INCOMPLETE_MODEL);

    List<Value> inAttrs = interpretAttrs(proj.attrs());
    if (inAttrs == null) return fail(OptimizerSupport.FAILURE_INCOMPLETE_MODEL);

    inAttrs = reBinder.rebindRefs(inAttrs, outValuesOf(child));
    if (inAttrs == null) return fail(OptimizerSupport.FAILURE_FOREIGN_VALUE);

    final ValuesRegistry valuesReg = newPlan.valuesReg();
    final List<String> names = ListSupport.map(outAttrs, Value::name);
    final List<Expression> exprs = ListSupport.map(outAttrs, valuesReg::exprOf);

    if (!bindRefs(exprs, inAttrs)) return fail(OptimizerSupport.FAILURE_MISMATCHED_REFS);

    final ProjNode projNode = ProjNode.mk(proj.deduplicated(), names, exprs);
    projNode.setQualification(qualificationOf(outAttrs));

    final int projNodeId = newPlan.bindNode(projNode);
    newPlan.setChild(projNodeId, 0, child);

    valuesReg.bindValues(projNodeId, outAttrs);

    return projNodeId;
  }

  private int instantiateSetOp(Union union) {
    final int lhs = instantiate(union.predecessors()[0]);
    final int rhs = instantiate(union.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    final SetOpNode unionNode = SetOpNode.mk(union.deduplicated(), SetOpKind.UNION);
    final int unionNodeId = newPlan.bindNode(unionNode);
    newPlan.setChild(unionNodeId, 0, lhs);
    newPlan.setChild(unionNodeId, 1, rhs);

    return unionNodeId;
  }

  private int instantiateAgg(Agg agg) {
    final int child = instantiate(agg.predecessors()[0]);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> outVals = interpretAttrs(agg.schema());
    List<Value> aggRefs = interpretAttrs(agg.aggregateAttrs());
    List<Value> groupRefs = interpretAttrs(agg.groupByAttrs());
    final List<Expression> aggFuncs = model.ofFunctions(instantiationOf(agg.aggFunc()));
    final Expression havingPred = model.ofPred(instantiationOf(agg.havingPred()));

    if (outVals == null || aggRefs == null || groupRefs == null || aggFuncs == null)
      return fail(OptimizerSupport.FAILURE_INCOMPLETE_MODEL);

    final List<Value> inValues = outValuesOf(child);
    aggRefs = reBinder.rebindRefs(aggRefs, inValues);
    if (aggRefs == null) return fail(OptimizerSupport.FAILURE_FOREIGN_VALUE);

    final List<Value> oldGroupRefs = newArrayList(groupRefs);
    groupRefs = reBinder.rebindRefs(groupRefs, ListSupport.join(outVals, inValues));
    if (groupRefs == null) return fail(OptimizerSupport.FAILURE_FOREIGN_VALUE);

    final ValuesRegistry reg = newPlan.valuesReg();
    final List<Expression> aggExprs = ListSupport.map(outVals, reg::exprOf);
    final Map<Value, Value> mapping = buildAggRefMapping(aggExprs, aggRefs);
    if (mapping == null) return fail(OptimizerSupport.FAILURE_MALFORMED_AGG);
    zip(oldGroupRefs, groupRefs, mapping::put);

    for (int i = 0, bound = aggExprs.size(), exprIdx = 0; i < bound; ++i) {
      Expression aggExpr = aggExprs.get(i);

      if (Aggregate.isInstance(aggExpr.template())) {
        aggExpr = aggFuncs.get(exprIdx++);
        aggExprs.set(i, aggExpr);
        reg.bindExpr(outVals.get(i), aggExpr);
      }

      if (!rebindAggExpr(aggExpr, mapping)) return fail(OptimizerSupport.FAILURE_MALFORMED_AGG);
    }

    if (havingPred != null && havingPred.colRefs().size() != groupRefs.size())
      return fail(OptimizerSupport.FAILURE_MALFORMED_AGG);

    newPlan.valuesReg().bindValueRefs(havingPred, groupRefs);

    final List<String> names = ListSupport.map(outVals, Value::name);
    final List<Expression> groupExprs = ListSupport.map(groupRefs, PlanSupport::mkColRefExpr);
    bindRefs(groupExprs, groupRefs);

    final AggNode aggNode = AggNode.mk(false, names, aggExprs, groupExprs, havingPred);
    aggNode.setQualification(qualificationOf(outVals));

    final int aggNodeId = newPlan.bindNode(aggNode);
    reg.bindValues(aggNodeId, outVals);

    final List<Value> refs = new ArrayList<>();
    for (Expression attrExpr : aggNode.attrExprs()) refs.addAll(reg.valueRefsOf(attrExpr));
    for (Expression groupByExpr : aggNode.groupByExprs()) refs.addAll(reg.valueRefsOf(groupByExpr));
    if (havingPred != null) refs.addAll(reg.valueRefsOf(havingPred));
    refs.removeIf(outVals::contains);

    final List<Expression> projAttrExprs = ListSupport.map(refs, PlanSupport::mkColRefExpr);
    final List<String> projAttrNames = ListSupport.generate(refs.size(), i -> "agg" + i);
    final ProjNode projNode = ProjNode.mk(false, projAttrNames, projAttrExprs);
    final int projNodeId = newPlan.bindNode(projNode);
    bindRefs(projAttrExprs, refs);

    newPlan.setChild(projNodeId, 0, child);
    newPlan.setChild(aggNodeId, 0, projNodeId);

    return aggNodeId;
  }

  private int fail(String reason) {
    error = reason;
    return NO_SUCH_NODE;
  }

  private Symbol instantiationOf(Symbol symbol) {
    return rule.constraints().instantiationOf(symbol);
  }

  private List<Value> outValuesOf(int nodeId) {
    return newPlan.valuesReg().valuesOf(nodeId);
  }

  private boolean bindRefs(List<Expression> exprs, List<Value> refs) {
    final ValuesRegistry reg = newPlan.valuesReg();
    int offset = 0;
    for (Expression expr : exprs) {
      final int numRefs = expr.colRefs().size();
      if (numRefs == 0) continue;
      if (offset + numRefs > refs.size()) return false;
      reg.bindValueRefs(expr, newArrayList(refs.subList(offset, offset + numRefs)));
      offset += numRefs;
    }
    return true;
  }

  private int mkFilterNode(Expression expr, List<Value> refs, int child) {
    final InfoCache infoCache = model.plan().infoCache();

    final int subqueryNode = infoCache.getSubqueryNodeOf(expr);
    if (subqueryNode != NO_SUCH_NODE) {
      if (usedSubqueryNode.get().add(subqueryNode)) {
        newPlan.detachNode(subqueryNode);
        newPlan.setChild(subqueryNode, 0, child);
        rebindFilterExpr(subqueryNode, refs, 0);
        return subqueryNode;
      } else {
        return fail(OptimizerSupport.FAILURE_ABUSED_SUBQUERY);
      }
    }

    final int[] components = infoCache.getVirtualExprComponents(expr);
    if (components != null) {
      int offset = 0;

      newPlan.setChild(components[0], 0, child);
      for (int i = 0, bound = components.length; i < bound; ++i) {
        offset += rebindFilterExpr(components[i], refs, offset);
        if (i > 0) newPlan.setChild(components[i], 0, components[i - 1]);
        newPlan.detachNode(components[i]);
      }

      return components[components.length - 1];
    }

    final SimpleFilterNode filterNode = SimpleFilterNode.mk(expr);
    final int filterNodeId = newPlan.bindNode(filterNode);

    newPlan.setChild(filterNodeId, 0, child);
    newPlan.valuesReg().bindValueRefs(expr, refs);
    return filterNodeId;
  }

  private int rebindFilterExpr(int nodeId, List<Value> refs, int offset) {
    final PlanKind kind = newPlan.kindOf(nodeId);
    final PlanNode node = newPlan.nodeAt(nodeId);
    final ValuesRegistry valuesReg = newPlan.valuesReg();

    final int numRefs;
    if (kind == PlanKind.Filter) {
      final Expression expr = ((SimpleFilterNode) node).predicate();
      numRefs = expr.colRefs().size();
      valuesReg.bindValueRefs(expr, newArrayList(refs.subList(offset, offset + numRefs)));

    } else if (kind == PlanKind.InSub) {
      final Expression lhsExpr = ((InSubNode) node).expr();
      final Expression subqueryExpr = newPlan.infoCache().getSubqueryExprOf(nodeId);
      final int lhsNumRefs = lhsExpr.colRefs().size();
      numRefs = subqueryExpr.colRefs().size();
      final List<Value> lhsExprRefs = refs.subList(offset, offset + lhsNumRefs);
      final List<Value> subqueryExprRefs = refs.subList(offset, offset + numRefs);
      valuesReg.bindValueRefs(lhsExpr, newArrayList(lhsExprRefs));
      valuesReg.bindValueRefs(subqueryExpr, newArrayList(subqueryExprRefs));

    } else if (kind == PlanKind.Exists) {
      final Expression subqueryExpr = newPlan.infoCache().getSubqueryExprOf(nodeId);
      numRefs = subqueryExpr.colRefs().size();
      valuesReg.bindValueRefs(subqueryExpr, newArrayList(refs.subList(offset, offset + numRefs)));

    } else {
      assert false;
      return -1;
    }

    return numRefs;
  }

  private boolean rebindAggExpr(Expression expr, Map<Value, Value> mapping) {
    final ValuesRegistry valuesReg = newPlan.valuesReg();
    final List<Value> oldRefs = valuesReg.valueRefsOf(expr);
    final List<Value> newRefs = ListSupport.map(oldRefs, mapping::get);
    if (newRefs.contains(null)) return false;
    valuesReg.bindValueRefs(expr, newRefs);
    return true;
  }

  private Map<Value, Value> buildAggRefMapping(List<Expression> exprs, List<Value> aggRefs) {
    final ValuesRegistry valuesReg = newPlan.valuesReg();
    final Map<Value, Value> mapping = new HashMap<>(4);
    int offset = 0;
    for (Expression expr : exprs) {
      if (!Aggregate.isInstance(expr.template())) continue;

      final int numRefs = expr.colRefs().size();
      final List<Value> oldRefs = valuesReg.valueRefsOf(expr);
      final List<Value> newRefs = newArrayList(aggRefs.subList(offset, offset + numRefs));
      if (oldRefs.size() != numRefs) return null;

      offset += numRefs;
      zip(oldRefs, newRefs, mapping::put);
    }

    return mapping;
  }

  private static List<Value> interleaveJoinKeys(List<Value> lhsJoinKeys, List<Value> rhsJoinKeys) {
    final List<Value> joinKeys = new ArrayList<>(lhsJoinKeys.size() << 1);
    for (int i = 0, bound = lhsJoinKeys.size(); i < bound; i++) {
      joinKeys.add(lhsJoinKeys.get(i));
      joinKeys.add(rhsJoinKeys.get(i));
    }
    return joinKeys;
  }

  private static String qualificationOf(List<Value> attrs) {
    String qualification = null;
    for (Value attr : attrs) {
      if (qualification == null) qualification = attr.qualification();
      else if (!qualification.equals(attr.qualification())) return null;
    }
    return qualification;
  }
}
