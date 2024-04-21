package sqlsolver.sql.plan;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.Lazy;
import sqlsolver.sql.ast.SqlNode;

import java.util.LinkedList;
import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.utils.ListSupport.emptyIntList;

class DependentRefInspector {
  private final PlanContext plan;

  private List<Value> outerValues;
  private Lazy<TIntList> depNodes;
  private Lazy<List<Value>> depValRefs;
  private Lazy<List<SqlNode>> depColRefs;

  DependentRefInspector(PlanContext plan) {
    this.plan = plan;
  }

  static Pair<List<Value>, List<SqlNode>> inspectDepRefs(PlanContext plan, int subqueryRoot) {
    final DependentRefInspector inspector = new DependentRefInspector(plan);
    inspector.inspect(subqueryRoot);
    return Pair.of(inspector.dependentValueRefs(), inspector.dependentColRefs());
  }

  void inspect(int subqueryRoot) {
    final int parent = plan.parentOf(subqueryRoot);

    assert parent != NO_SUCH_NODE;
    assert plan.kindOf(parent).isSubqueryFilter();
    assert plan.childOf(parent, 1) == subqueryRoot;

    outerValues = plan.valuesReg().valuesOf(plan.childOf(parent, 0));
    depNodes = Lazy.mk(TIntArrayList::new);
    depValRefs = Lazy.mk(LinkedList::new);
    depColRefs = Lazy.mk(LinkedList::new);
    inspect0(subqueryRoot);
  }

  List<Value> dependentValueRefs() {
    return depValRefs.get();
  }

  List<SqlNode> dependentColRefs() {
    return depColRefs.get();
  }

  TIntList dependentNodes() {
    return depNodes.isInitialized() ? depNodes.get() : emptyIntList();
  }

  private void inspect0(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    boolean isDependentNode = false;
    switch (kind) {
      case Input:
      case Exists:
      case SetOp:
      case Sort:
      case Limit:
      case Agg:
        break;

      case Proj:
        for (Expression expr : ((ProjNode) plan.nodeAt(nodeId)).attrExprs())
          isDependentNode |= inspectExpr(expr);
        break;
      case Filter:
        isDependentNode = inspectExpr(((SimpleFilterNode) plan.nodeAt(nodeId)).predicate());
        break;
      case InSub:
        isDependentNode = inspectExpr(((InSubNode) plan.nodeAt(nodeId)).expr());
        break;
      case Join:
        isDependentNode = inspectExpr(((JoinNode) plan.nodeAt(nodeId)).joinCond());
        break;
    }

    if (isDependentNode) depNodes.get().add(nodeId);

    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      inspect0(plan.childOf(nodeId, i));
    }
  }

  private boolean inspectExpr(Expression expr) {
    if (expr == null) return false;
    final Values refs = plan.valuesReg().valueRefsOf(expr);
    final List<SqlNode> colRefs = expr.colRefs();
    boolean isDependent = false;
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final Value ref = refs.get(i);
      if (outerValues.contains(ref)) {
        isDependent = true;
        depValRefs.get().add(ref);
        depColRefs.get().add(colRefs.get(i));
      }
    }
    return isDependent;
  }
}
