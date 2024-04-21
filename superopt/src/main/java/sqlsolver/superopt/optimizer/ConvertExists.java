package sqlsolver.superopt.optimizer;

import gnu.trove.list.TIntList;
import sqlsolver.sql.plan.*;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.sql.SqlSupport.isColRefEq;
import static sqlsolver.sql.plan.PlanSupport.mkColRefExpr;

class ConvertExists {
  private final PlanContext plan;
  private boolean isConverted;

  ConvertExists(PlanContext plan) {
    this.plan = plan;
  }

  int convert(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) convert(plan.childOf(nodeId, i));

    if (plan.kindOf(nodeId) != PlanKind.Exists) return nodeId;

    final TIntList depNodes = plan.infoCache().getDependentNodesIn(nodeId);
    if (depNodes.size() != 1) return nodeId;

    final int depNode = depNodes.get(0);
    final int subqueryRoot = plan.childOf(nodeId, 1);
    if (plan.kindOf(depNode) != PlanKind.Filter) return nodeId;
    if (!isInSameScope(subqueryRoot, depNode)) return nodeId;

    final Expression predicate = ((SimpleFilterNode) plan.nodeAt(depNode)).predicate();
    if (!isColRefEq(predicate.template())) return nodeId;

    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values refs = valuesReg.valueRefsOf(predicate);
    assert refs.size() == 2;

    final Values outerValues = valuesReg.valuesOf(nodeId);
    final boolean isOuter0 = outerValues.contains(refs.get(0));
    final boolean isOuter1 = outerValues.contains(refs.get(1));
    if (isOuter0 == isOuter1) return nodeId;

    final Value lhsRef = refs.get(isOuter0 ? 0 : 1), rhsRef = refs.get(isOuter0 ? 1 : 0);
    final Values rhsValues = valuesReg.valuesOf(plan.childOf(subqueryRoot, 0));
    if (!rhsValues.contains(rhsRef)) return nodeId;

    final List<String> projAttrNames = singletonList(rhsRef.name());
    final List<Expression> projExprs = singletonList(PlanSupport.mkColRefExpr(rhsRef));
    final ProjNode projNode = ProjNode.mk(false, projAttrNames, projExprs);
    final int projNodeId = plan.bindNode(projNode);

    final Expression inSubExpr = mkColRefExpr(lhsRef);
    final InSubNode inSubNode = InSubNode.mk(inSubExpr);
    final int inSubNodeId = plan.bindNode(inSubNode);

    final int c0 = plan.childOf(depNode, 0);
    final int p0 = plan.parentOf(depNode);
    plan.detachNode(c0);
    plan.setChild(p0, indexOfChild(plan, depNode), c0);

    final int c1 = plan.childOf(nodeId, 0);
    final int p1 = plan.parentOf(nodeId);
    plan.detachNode(c1);
    plan.setChild(inSubNodeId, 0, c1);
    plan.setChild(p1, indexOfChild(plan, nodeId), inSubNodeId);

    final int rhsInput = plan.childOf(subqueryRoot, 0);
    plan.detachNode(rhsInput);
    plan.setChild(projNodeId, 0, rhsInput);
    plan.setChild(inSubNodeId, 1, projNodeId);

    valuesReg.bindValueRefs(projExprs.get(0), newArrayList(rhsRef));
    valuesReg.bindValueRefs(inSubExpr, newArrayList(lhsRef));

    PlanSupport.setupSubqueryExprOf(plan, inSubNodeId);

    isConverted = true;
    return inSubNodeId;
  }

  boolean isConverted() {
    return isConverted;
  }

  private boolean isInSameScope(int parent, int child) {
    while (child != parent) {
      if (!plan.isPresent(child)) return false;
      if (plan.kindOf(child) == PlanKind.Proj) return false;
      child = plan.parentOf(child);
    }
    return true;
  }
}
