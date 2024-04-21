package sqlsolver.superopt.optimizer;

import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.SetOpKind;
import sqlsolver.sql.plan.*;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.fragment.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.common.utils.ListSupport.flatMap;
import static sqlsolver.common.utils.ListSupport.linkedListFlatMap;
import static sqlsolver.sql.ast.ExprKind.Aggregate;
import static sqlsolver.sql.ast.ExprKind.ColRef;
import static sqlsolver.sql.ast.SqlNodeFields.GroupItem_Expr;
import static sqlsolver.sql.plan.PlanSupport.joinKindOf;
import static sqlsolver.sql.plan.PlanSupport.locateNode;

class Match {
  private final Substitution rule;
  private final Model model;

  private PlanContext sourcePlan;
  private int matchRootNode;
  private int matchStartNode;

  private int lastMatchedNode;
  private Op lastMatchedOp;

  private PlanContext modifiedPlan;
  private int modifiedRootNode;

  Match(Substitution rule) {
    this.rule = rule;
    this.model = new Model(rule.constraints());
    this.matchRootNode = NO_SUCH_NODE;
    this.matchStartNode = NO_SUCH_NODE;
    this.lastMatchedNode = NO_SUCH_NODE;
    this.modifiedRootNode = NO_SUCH_NODE;
  }

  Match(Match other) {
    this.rule = other.rule;
    this.model = other.model.derive();
    this.sourcePlan = other.sourcePlan;
    this.matchRootNode = other.matchRootNode;
    this.matchStartNode = other.matchStartNode;
    this.lastMatchedNode = other.lastMatchedNode;
    this.lastMatchedOp = other.lastMatchedOp;
    this.modifiedPlan = other.modifiedPlan;
    this.modifiedRootNode = other.modifiedRootNode;
  }

  Match setSourcePlan(PlanContext sourcePlan) {
    this.sourcePlan = sourcePlan;
    this.model.setPlan(sourcePlan);
    return this;
  }

  Match setMatchRootNode(int nodeId) {
    this.matchRootNode = nodeId;
    return this;
  }

  Match setLastMatchPoint(int lastMatchedNode, Op lastMatchOp) {
    this.lastMatchedNode = lastMatchedNode;
    this.lastMatchedOp = lastMatchOp;
    if (matchStartNode == NO_SUCH_NODE) matchStartNode = lastMatchedNode;
    return this;
  }

  Model model() {
    return model;
  }

  PlanContext sourcePlan() {
    return sourcePlan;
  }

  int matchRootNode() {
    return matchRootNode;
  }

  int modifiedRootNode() {
    return modifiedRootNode;
  }

  PlanContext modifiedPlan() {
    return modifiedPlan;
  }

  Match derive() {
    return new Match(this);
  }

  boolean assembleModifiedPlan() {
    final Instantiation instantiation = new Instantiation(rule, model);
    final int modifiedPoint = instantiation.instantiate();
    if (modifiedPoint <= 0) {
      OptimizerSupport.setLastError(instantiation.lastError());
      return false;
    }

    modifiedPlan = instantiation.instantiatedPlan();
    final int parent0 = sourcePlan.parentOf(matchStartNode);
    if (parent0 != NO_SUCH_NODE)
      modifiedPlan.setChild(parent0, indexOfChild(sourcePlan, matchStartNode), modifiedPoint);

    final ValueRefReBinder reBinder = new ValueRefReBinder(modifiedPlan);
    if (!reBinder.rebindToRoot(modifiedPoint)) return false;

    final int parent1 = sourcePlan.parentOf(matchRootNode);
    final int newRootId;
    if (parent1 == NO_SUCH_NODE)
      if (matchRootNode == matchStartNode) newRootId = modifiedPoint;
      else newRootId = matchRootNode;
    else newRootId = sourcePlan.root();

    modifiedRootNode = matchRootNode == matchStartNode ? modifiedPoint : matchRootNode;
    final PlanNode modifiedRoot = modifiedPlan.nodeAt(modifiedRootNode);
    modifiedPlan.setRoot(newRootId);
    modifiedPlan.deleteDetached(newRootId);
    modifiedPlan.compact();
    modifiedRootNode = modifiedPlan.nodeIdOf(modifiedRoot);
    return true;
  }

  boolean matchOne(Op op, int nodeId) {
    boolean result;
    switch (op.kind()) {
      case INPUT:
        result = matchInput((Input) op, nodeId);
        break;
      case INNER_JOIN:
      case LEFT_JOIN:
        result = matchJoin((Join) op, nodeId);
        break;
      case SIMPLE_FILTER:
        result = matchFilter((SimpleFilter) op, nodeId);
        break;
      case IN_SUB_FILTER:
        result = matchInSub((InSubFilter) op, nodeId);
        break;
      case PROJ:
        result = matchProj((Proj) op, nodeId);
        break;
      case UNION:
        result = matchSetOp((Union) op, nodeId);
        break;
      case AGG:
        result = matchAgg((Agg) op, nodeId);
        break;
      default:
        throw new IllegalArgumentException("unknown operator: " + op.kind());
    }

    if (result) setLastMatchPoint(nodeId, op);
    return result;
  }

  private boolean matchInput(Input input, int nodeId) {
    return model.assign(input.table(), nodeId) && model.checkConstraints();
  }

  private boolean matchJoin(Join joinOp, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.Join) return false;

    final OpKind opKind = joinOp.kind();
    final InfoCache infoCache = sourcePlan.infoCache();
    final JoinKind joinKind = joinKindOf(sourcePlan, nodeId);
    if (opKind == OpKind.LEFT_JOIN && joinKind != JoinKind.LEFT_JOIN) return false;
    if (opKind == OpKind.INNER_JOIN && joinKind != JoinKind.INNER_JOIN) return false;
    if (!infoCache.isEquiJoin(nodeId)) return false;

    final var keys = infoCache.getJoinKeyOf(nodeId);
    return model.assign(joinOp.lhsAttrs(), keys.getLeft())
        && model.assign(joinOp.rhsAttrs(), keys.getRight())
        && model.checkConstraints();
  }

  private boolean matchFilter(SimpleFilter filter, int nodeId) {
    final PlanKind nodeKind = sourcePlan.kindOf(nodeId);
    if (!nodeKind.isFilter()) return false;

    final Expression predicate;
    if (nodeKind.isSubqueryFilter()) predicate = sourcePlan.infoCache().getSubqueryExprOf(nodeId);
    else predicate = ((SimpleFilterNode) sourcePlan.nodeAt(nodeId)).predicate();

    final Values attrs = sourcePlan.valuesReg().valueRefsOf(predicate);

    return model.assign(filter.predicate(), predicate)
        && model.assign(filter.attrs(), attrs)
        && model.checkConstraints();
  }

  private boolean matchInSub(InSubFilter inSub, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.InSub) return false;

    final InSubNode inSubNode = (InSubNode) sourcePlan.nodeAt(nodeId);
    if (!inSubNode.isPlain()) return false;

    final Expression expr = inSubNode.expr();
    final Values attrs = sourcePlan.valuesReg().valueRefsOf(expr);

    return model.assign(inSub.attrs(), attrs) && model.checkConstraints();
  }

  private boolean matchProj(Proj proj, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.Proj) return false;

    final ProjNode projNode = (ProjNode) sourcePlan.nodeAt(nodeId);
    if (proj.deduplicated() ^ PlanSupport.isDedup(sourcePlan, nodeId)) return false;

    final ValuesRegistry valuesReg = sourcePlan.valuesReg();
    final List<Value> outValues = valuesReg.valuesOf(nodeId);
    final List<Value> inValues = flatMap(projNode.attrExprs(), valuesReg::valueRefsOf);

    return model.assign(proj.attrs(), inValues)
        && model.assign(proj.schema(), outValues)
        && model.checkConstraints();
  }

  private boolean matchSetOp(Union union, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.SetOp) return false;

    final SetOpNode setOpNode = (SetOpNode) sourcePlan.nodeAt(nodeId);
    return setOpNode.opKind() == SetOpKind.UNION
        && union.deduplicated() == setOpNode.deduplicated();
  }

  private boolean matchAgg(Agg agg, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.Agg) return false;

    final ValuesRegistry valuesReg = sourcePlan.valuesReg();
    final AggNode aggNode = (AggNode) sourcePlan.nodeAt(nodeId);
    // if (aggNode.deduplicated()) return false;
    List<Expression> attrs = aggNode.attrExprs();
    if(attrs == null || attrs.size() < 1)
      return false;


    boolean sameAggFunc = false;
    for(Expression exp : attrs) {
      if (exp.toString().toUpperCase().indexOf(agg.aggFuncKind().text().toUpperCase() + "(") >= 0) {
        sameAggFunc = true;
        break;
      }
    }
    if(sameAggFunc == false) return false;


    final List<Expression> aggFuncs = new ArrayList<>(3);
    final List<Value> aggRefs = new ArrayList<>(3);

    collectAggregates(aggNode, aggFuncs, aggRefs);

    if (aggFuncs.isEmpty()) return false;

    final Expression havingPredExpr = aggNode.havingExpr();
    if (havingPredExpr != null && !valuesReg.valueRefsOf(havingPredExpr).equals(aggRefs))
      return false;

    if (any(aggNode.groupByExprs(), it -> !ColRef.isInstance(it.template().$(GroupItem_Expr))))
      return false;

    final List<Value> groupRefs = flatMap(aggNode.groupByExprs(), valuesReg::valueRefsOf);

    if (!isClosedAggregates(aggNode, aggRefs, groupRefs)) return false;
    final List<Value> schema = valuesReg.valuesOf(nodeId);

    return model.assign(agg.schema(), schema)
        && agg.deduplicated() == aggNode.deduplicated()
        && model.assign(agg.aggFunc(), aggFuncs)
        && model.assign(agg.aggregateAttrs(), aggRefs)
        && model.assign(agg.groupByAttrs(), groupRefs)
        && (havingPredExpr == null || model.assign(agg.havingPred(), havingPredExpr));
  }

  private void collectAggregates(AggNode aggNode, List<Expression> aggFuncs, List<Value> aggRefs) {
    final ValuesRegistry valuesReg = sourcePlan.valuesReg();
    for (Expression attrExpr : aggNode.attrExprs()) {
      if (Aggregate.isInstance(attrExpr.template())) {
        aggFuncs.add(attrExpr);
        aggRefs.addAll(valuesReg.valueRefsOf(attrExpr));
      }
    }
  }

  private boolean isClosedAggregates(AggNode aggNode, List<Value> aggRefs, List<Value> groupRefs) {
    final ValuesRegistry valuesReg = sourcePlan.valuesReg();
    for (Expression attrExpr : aggNode.attrExprs()) {
      if (!Aggregate.isInstance(attrExpr.template())) continue;
      final Values refs = valuesReg.valueRefsOf(attrExpr);
      if (!aggRefs.containsAll(refs) && !groupRefs.containsAll(refs)) return false;
    }
    return true;
  }

  private Op nextOp(int childIdx) {
    return lastMatchedOp.predecessors()[childIdx];
  }

  private int nextNode(int childIdx) {
    return sourcePlan.childOf(lastMatchedNode, childIdx);
  }

  static List<Match> match(Match match, Op op, int nodeId) {
    if (op.kind() == OpKind.INPUT) {
      if (match.matchOne(op, nodeId)) return singletonList(match);
      else return emptyList();
    }

    final PlanContext plan = match.sourcePlan();
    if (op.kind() == OpKind.PROJ) {
      if (match.matchOne(op, nodeId))
        return match(match, op.predecessors()[0], plan.childOf(nodeId, 0));
      else return emptyList();
    }

    if (op.kind().isJoin()) {
      if (plan.kindOf(nodeId) != PlanKind.Join) return emptyList();

      final JoinMatcher matcher = new JoinMatcher((Join) op, plan, nodeId);
      final List<Match> localMatches = matcher.matchBasedOn(match);
      List<Match> matches = new LinkedList<>();
      for (Match localMatch : localMatches) {
        final Op nextOp0 = localMatch.nextOp(0), nextOp1 = localMatch.nextOp(1);
        final int nextNode0 = localMatch.nextNode(0), nextNode1 = localMatch.nextNode(1);
        final List<Match> partialMatches = match(localMatch, nextOp0, nextNode0);
        matches.addAll(linkedListFlatMap(partialMatches, m -> match(m, nextOp1, nextNode1)));
      }

      if (shouldPermuteJoinTree() || matches.size() <= 1) return matches;
      else return new ArrayList<>(matches.subList(0, 1));
    }

    if (op.kind().isFilter()) {
      // Matching filter is holistic: the whole filter chain are matched as a whole. Thus the
      // matching happens only at the chain's head.
      assert op.successor() == null || !op.successor().kind().isFilter();
      if (!plan.kindOf(nodeId).isFilter()) return emptyList();

      final FilterMatcher matcher = new FilterMatcher((Filter) op, plan, nodeId);
      final List<Match> matches = matcher.matchBasedOn(match);
      return linkedListFlatMap(matches, m -> match(m, m.nextOp(0), m.nextNode(0)));
    }

    if (op.kind() == OpKind.AGG) {
      if (!match.matchOne(op, nodeId)) return emptyList();
      return match(match, op.predecessors()[0], locateNode(plan, nodeId, 0, 0));
    }

    if (op.kind() == OpKind.UNION) {
      if (!match.matchOne(op, nodeId)) return emptyList();
      final Op nextOp0 = match.nextOp(0), nextOp1 = match.nextOp(1);
      final int nextNode0 = match.nextNode(0), nextNode1 = match.nextNode(1);

      return linkedListFlatMap(match(match, nextOp0, nextNode0), m -> match(m, nextOp1, nextNode1));
    }

    OptimizerSupport.setLastError(OptimizerSupport.FAILURE_UNKNOWN_OP + op.kind());
    return emptyList();
  }

  private static boolean shouldPermuteJoinTree() {
    return (OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_PERMUTE_JOIN_TREE) != 0;
  }
}
