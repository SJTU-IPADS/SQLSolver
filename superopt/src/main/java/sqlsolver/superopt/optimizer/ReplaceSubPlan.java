package sqlsolver.superopt.optimizer;

import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.plan.*;

import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;

class ReplaceSubPlan {
  private final PlanContext replacementPlan;
  private final PlanContext replacedPlan;

  ReplaceSubPlan(PlanContext replacedPlan, PlanContext replacementPlan) {
    this.replacementPlan = replacementPlan;
    this.replacedPlan = replacedPlan;
  }

  int replace(int replacedSubPlan, int replacementSubPlan) {
    final Values fromValues = replacementPlan.valuesReg().valuesOf(replacementSubPlan);
    final Values toValues = replacedPlan.valuesReg().valuesOf(replacedSubPlan);
    if (fromValues.size() != toValues.size()) return NO_SUCH_NODE;

    final int toRoot = replacedPlan.root();
    final int newSubPlan = copyNode(replacementSubPlan);
    final int parent = replacedPlan.parentOf(replacedSubPlan);
    final int childIdx = indexOfChild(replacedPlan, replacedSubPlan);
    replacedPlan.detachNode(replacedSubPlan);
    replacedPlan.setChild(parent, childIdx, newSubPlan);

    final ValueRefReBinder reBinder = new ValueRefReBinder(replacedPlan);
    if (!reBinder.rebindToRoot(newSubPlan)) return NO_SUCH_NODE;

    final PlanNode newSubPlanNode = replacedPlan.nodeAt(newSubPlan);
    replacedPlan.deleteDetached(toRoot);
    replacedPlan.compact();

    return replacedPlan.nodeIdOf(newSubPlanNode);
  }

  private int copyNode(int node) {
    switch (replacementPlan.kindOf(node)) {
      case Input:
        return copyInput(node);
      case Filter:
        return copyFilter(node);
      case InSub:
        return copyInSub(node);
      case Join:
        return copyJoin(node);
      case Proj:
        return copyProj(node);
      case Agg:
        return copyAgg(node);
      case SetOp:
        return copySetOp(node);
      case Sort:
        return copySort(node);
      case Limit:
        return copyLimit(node);
      case Exists:
        return copyExists(node);
      default:
        throw new IllegalArgumentException("unsupported node");
    }
  }

  private int copyInput(int fromNode) {
    final InputNode inputNode = (InputNode) replacementPlan.nodeAt(fromNode);
    final int toNode = replacedPlan.bindNode(inputNode);
    replacedPlan.valuesReg().bindValues(toNode, replacementPlan.valuesReg().valuesOf(fromNode));
    return toNode;
  }

  private int copyFilter(int fromNode) {
    final int child = copyNode(replacementPlan.childOf(fromNode, 0));
    final SimpleFilterNode filterNode = (SimpleFilterNode) replacementPlan.nodeAt(fromNode);
    final Expression predicate = filterNode.predicate();
    final Values refs = replacementPlan.valuesReg().valueRefsOf(predicate);

    final int toNode = replacedPlan.bindNode(filterNode);
    replacedPlan.setChild(toNode, 0, child);
    replacedPlan.valuesReg().bindValueRefs(predicate, refs);

    return toNode;
  }

  private int copyInSub(int fromNode) {
    final int lhs = copyNode(replacementPlan.childOf(fromNode, 0));
    final int rhs = copyNode(replacementPlan.childOf(fromNode, 1));

    final InSubNode filterNode = (InSubNode) replacementPlan.nodeAt(fromNode);
    final Expression expr = filterNode.expr();
    final ValuesRegistry replacementValuesReg = replacementPlan.valuesReg();
    final Values refs = replacementValuesReg.valueRefsOf(expr);
    final Expression subqueryExpr = replacementPlan.infoCache().getSubqueryExprOf(fromNode);
    final Values subqueryRefs = replacementValuesReg.valueRefsOf(subqueryExpr);

    final int toNode = replacedPlan.bindNode(filterNode);
    replacedPlan.setChild(toNode, 0, lhs);
    replacedPlan.setChild(toNode, 1, rhs);
    replacedPlan.valuesReg().bindValueRefs(expr, refs);
    replacedPlan.valuesReg().bindValueRefs(subqueryExpr, subqueryRefs);
    replacedPlan.infoCache().putSubqueryExprOf(toNode, subqueryExpr);

    return toNode;
  }

  private int copyExists(int fromNode) {
    final int lhs = copyNode(replacementPlan.childOf(fromNode, 0));
    final int rhs = copyNode(replacementPlan.childOf(fromNode, 1));

    final ExistsNode filterNode = (ExistsNode) replacementPlan.nodeAt(fromNode);
    final ValuesRegistry replacementValuesReg = replacementPlan.valuesReg();
    final Expression subqueryExpr = replacementPlan.infoCache().getSubqueryExprOf(fromNode);
    final Values subqueryRefs = replacementValuesReg.valueRefsOf(subqueryExpr);

    final int toNode = replacedPlan.bindNode(filterNode);
    replacedPlan.setChild(toNode, 0, lhs);
    replacedPlan.setChild(toNode, 1, rhs);
    replacedPlan.valuesReg().bindValueRefs(subqueryExpr, subqueryRefs);
    replacedPlan.infoCache().putSubqueryExprOf(toNode, subqueryExpr);

    return toNode;
  }

  private int copyJoin(int fromNode) {
    final int lhs = copyNode(replacementPlan.childOf(fromNode, 0));
    final int rhs = copyNode(replacementPlan.childOf(fromNode, 1));

    final JoinNode joinNode = (JoinNode) replacementPlan.nodeAt(fromNode);
    final Expression joinCond = joinNode.joinCond();
    final Values refs = replacementPlan.valuesReg().valueRefsOf(joinCond);
    final var joinKeys = replacementPlan.infoCache().getJoinKeyOf(fromNode);
    final JoinKind joinKind = replacementPlan.infoCache().getJoinKindOf(fromNode);

    final int toNode = replacedPlan.bindNode(joinNode);
    replacedPlan.setChild(toNode, 0, lhs);
    replacedPlan.setChild(toNode, 1, rhs);
    replacedPlan.valuesReg().bindValueRefs(joinCond, refs);
    if (joinKeys != null) replacedPlan.infoCache().putJoinKindOf(toNode, joinKind);
    if (joinKeys != null)
      replacedPlan.infoCache().putJoinKeyOf(toNode, joinKeys.getLeft(), joinKeys.getRight());

    return toNode;
  }

  private int copyProj(int fromNode) {
    final int child = copyNode(replacementPlan.childOf(fromNode, 0));

    final ProjNode projNode = (ProjNode) replacementPlan.nodeAt(fromNode);
    final ValuesRegistry fromValuesReg = replacementPlan.valuesReg();
    final ValuesRegistry toValuesReg = replacedPlan.valuesReg();
    final List<Expression> attrExprs = projNode.attrExprs();
    final Values values = fromValuesReg.valuesOf(fromNode);
    assert values.size() == attrExprs.size();

    final int toNode = replacedPlan.bindNode(projNode);
    replacedPlan.setChild(toNode, 0, child);
    for (int i = 0, bound = values.size(); i < bound; ++i) {
      final Expression attrExpr = attrExprs.get(i);
      toValuesReg.bindValueRefs(attrExpr, fromValuesReg.valueRefsOf(attrExpr));
      toValuesReg.bindExpr(values.get(i), attrExpr);
    }
    toValuesReg.bindValues(toNode, values);

    final Boolean deduplicatedFlag = replacementPlan.infoCache().getDeduplicatedOf(fromNode);
    if (deduplicatedFlag != null)
      replacementPlan.infoCache().putDeduplicatedOf(toNode, deduplicatedFlag);

    return toNode;
  }

  private int copyAgg(int fromNode) {
    final int child = copyNode(replacementPlan.childOf(fromNode, 0));

    final AggNode aggNode = (AggNode) replacementPlan.nodeAt(fromNode);
    final ValuesRegistry fromValuesReg = replacementPlan.valuesReg();
    final ValuesRegistry toValuesReg = replacedPlan.valuesReg();

    final int toNode = replacedPlan.bindNode(aggNode);
    replacedPlan.setChild(toNode, 0, child);

    for (Expression fromExpr : aggNode.attrExprs())
      toValuesReg.bindValueRefs(fromExpr, fromValuesReg.valueRefsOf(fromExpr));

    for (Expression fromExpr : aggNode.groupByExprs())
      toValuesReg.bindValueRefs(fromExpr, fromValuesReg.valueRefsOf(fromExpr));

    final Expression havingExpr = aggNode.havingExpr();
    if (havingExpr != null)
      toValuesReg.bindValueRefs(havingExpr, fromValuesReg.valueRefsOf(havingExpr));

    toValuesReg.bindValues(toNode, fromValuesReg.valuesOf(fromNode));

    return toNode;
  }

  private int copySetOp(int fromNode) {
    final int lhs = copyNode(replacementPlan.childOf(fromNode, 0));
    final int rhs = copyNode(replacementPlan.childOf(fromNode, 1));

    final SetOpNode setOpNode = (SetOpNode) replacementPlan.nodeAt(fromNode);
    final int toNode = replacedPlan.bindNode(setOpNode);
    replacedPlan.setChild(toNode, 0, lhs);
    replacedPlan.setChild(toNode, 1, rhs);

    return toNode;
  }

  private int copySort(int fromNode) {
    final int child = copyNode(replacementPlan.childOf(fromNode, 0));

    final SortNode sortNode = (SortNode) replacementPlan.nodeAt(fromNode);
    final ValuesRegistry fromValuesReg = replacementPlan.valuesReg();
    final ValuesRegistry toValuesReg = replacedPlan.valuesReg();

    final int toNode = replacedPlan.bindNode(sortNode);
    replacedPlan.setChild(toNode, 0, child);
    for (Expression fromExpr : sortNode.sortSpec())
      toValuesReg.bindValueRefs(fromExpr, fromValuesReg.valueRefsOf(fromExpr));

    return toNode;
  }

  private int copyLimit(int fromNode) {
    final int child = copyNode(replacementPlan.childOf(fromNode, 0));

    final LimitNode limitNode = (LimitNode) replacementPlan.nodeAt(fromNode);
    final int toNode = replacedPlan.bindNode(limitNode);
    replacedPlan.setChild(toNode, 0, child);

    return toNode;
  }
}
