package sqlsolver.superopt.optimizer;

import sqlsolver.sql.plan.*;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.common.utils.IterableSupport.zip;
import static sqlsolver.sql.plan.PlanKind.*;
import static sqlsolver.sql.plan.PlanSupport.mkColRefExpr;

class NormalizeProj {
  private final PlanContext plan;

  NormalizeProj(PlanContext plan) {
    this.plan = plan;
  }

  int normalizeTree(int rootId) {
    final PlanKind kind = plan.kindOf(rootId);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      if (normalizeTree(plan.childOf(rootId, i)) == NO_SUCH_NODE) return NO_SUCH_NODE;
    }

    if (kind == Proj && shouldReduceProj(rootId)) return reduceProj(rootId);
    if (shouldInsertProjBefore(rootId)) return insertProjBefore(rootId);
    return rootId;
  }

  int insertProjBefore(int position) {
    final int parent = plan.parentOf(position);
    assert parent != NO_SUCH_NODE;

    final ValuesRegistry valuesReg = plan.valuesReg();
    final List<Value> inputs = valuesReg.valuesOf(position);

    final List<String> outputNames = new ArrayList<>(inputs.size());
    final List<Expression> outputExprs = new ArrayList<>(inputs.size());
    for (final Value value : inputs) {
      outputNames.add(value.name());
      outputExprs.add(mkColRefExpr(value));
    }

    final ProjNode proj = ProjNode.mk(false, outputNames, outputExprs);
    final int projNode = plan.bindNode(proj);
    final int childIdx = indexOfChild(plan, position);
    plan.setChild(parent, childIdx, projNode);
    plan.setChild(projNode, 0, position);
    zip(outputExprs, inputs, (e, ref) -> valuesReg.bindValueRefs(e, newArrayList(ref)));

    final ValueRefReBinder reBinder = new ValueRefReBinder(plan);
    if (!reBinder.rebindToRoot(projNode)) return NO_SUCH_NODE;

    PlanSupport.disambiguateQualification(plan);

    return projNode;
  }

  int reduceProj(int proj) {
    assert plan.kindOf(proj) == Proj;

    final int parent = plan.parentOf(proj);
    assert parent != NO_SUCH_NODE;

    final int childIdx = indexOfChild(plan, proj);
    final int replacement = plan.childOf(proj, 0);
    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values oldRefs = valuesReg.valuesOf(proj);
    final Values newRefs = valuesReg.valuesOf(replacement);
    assert oldRefs.size() == newRefs.size();

    plan.detachNode(replacement);
    plan.setChild(parent, childIdx, replacement);

    final ValueRefReBinder reBinder = new ValueRefReBinder(plan);
    if (!reBinder.rebindToRoot(replacement)) return NO_SUCH_NODE;

    return replacement;
  }

  boolean shouldInsertProjBefore(int node) {
    final int parent = plan.parentOf(node);
    final PlanKind nodeKind = plan.kindOf(node);
    if (nodeKind.isFilter()) return parent == NO_SUCH_NODE || plan.kindOf(parent) == Join;
    if (nodeKind == Join) return parent == NO_SUCH_NODE;
    return false;
  }

  boolean shouldReduceProj(int node) {
    final PlanKind nodeKind = plan.kindOf(node);
    if (nodeKind != Proj) return false;

    final int parent = plan.parentOf(node);
    final int child = plan.childOf(node, 0);

    if (parent == NO_SUCH_NODE) return false;

    final PlanKind childKind = plan.kindOf(child);
    final PlanKind parentKind = plan.kindOf(parent);
    if (childKind.isFilter()) {
      if (!parentKind.isFilter()) return false;
      if (parentKind.isSubqueryFilter() && indexOfChild(plan, node) != 0) return false;

    } else if (childKind == Input) {
      if (!parentKind.isFilter() && parentKind != Join) return false;
      if (parentKind.isSubqueryFilter() && indexOfChild(plan, node) != 0) return false;

    } else return false;

    final Values inputs = plan.valuesReg().valuesOf(child);
    final Values outputs = plan.valuesReg().valuesOf(node);
    if (inputs.size() != outputs.size()) return false;

    for (int i = 0, bound = inputs.size(); i < bound; i++) {
      if (PlanSupport.deRef(plan, outputs.get(i)) != inputs.get(i)) return false;
    }

    return true;
  }
}
