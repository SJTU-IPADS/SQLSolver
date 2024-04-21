package sqlsolver.superopt.optimizer;

import sqlsolver.common.tree.TreeSupport;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.ast.constants.TernaryOp;
import sqlsolver.sql.plan.*;

import java.util.ArrayList;
import java.util.List;

import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.sql.ast.ExprFields.*;
import static sqlsolver.sql.ast.constants.BinaryOpKind.*;
import static sqlsolver.sql.ast.constants.JoinKind.*;
import static sqlsolver.sql.ast.constants.LiteralKind.*;
import static sqlsolver.sql.plan.PlanKind.*;
import static sqlsolver.sql.plan.PlanSupport.joinKindOf;

class InnerJoinInference {
  private final PlanContext plan;
  private boolean modified;

  InnerJoinInference(PlanContext plan) {
    this.plan = plan;
    this.modified = false;
  }

  void inferenceAndEnforce(int rootId) {
    inferenceAndEnforce0(rootId);

    for (int i = 0, bound = plan.kindOf(rootId).numChildren(); i < bound; ++i)
      inferenceAndEnforce(plan.childOf(rootId, i));
  }

  boolean isModified() {
    return modified;
  }

  private void inferenceAndEnforce0(int evidenceNode) {
    final ValuesRegistry valuesReg = plan.valuesReg();
    final PlanKind kind = plan.kindOf(evidenceNode);
    final PlanNode node = plan.nodeAt(evidenceNode);
    List<Value> enforcedNonNullRefs = null;

    if (kind == Filter) {
      enforcedNonNullRefs = gatherEnforcedNonNullRefs(((SimpleFilterNode) node).predicate());
    } else if (kind == InSub) {
      enforcedNonNullRefs = valuesReg.valueRefsOf(((InSubNode) node).expr());
    } else if (kind == Join) {
      if (joinKindOf(plan, evidenceNode).isInner())
        enforcedNonNullRefs = valuesReg.valueRefsOf(((JoinNode) node).joinCond());
    }

    if (enforcedNonNullRefs != null && !enforcedNonNullRefs.isEmpty())
      for (Value ref : enforcedNonNullRefs) enforceInnerJoin(ref, evidenceNode);
  }

  private void enforceInnerJoin(Value ref, int surface) {
    final int initiator = plan.valuesReg().initiatorOf(ref);
    if (!TreeSupport.isDescendant(plan, surface, initiator)) return;

    int cursor = initiator;
    while (cursor != surface) {
      final int parent = plan.parentOf(cursor);
      if (isLeftJoin(parent) && indexOfChild(plan, cursor) == 1) {
        plan.infoCache().putJoinKindOf(parent, INNER_JOIN);
        modified = true;
      }
      if (isRightJoin(parent) && indexOfChild(plan, cursor) == 0) {
        plan.infoCache().putJoinKindOf(parent, INNER_JOIN);
        modified = true;
      }
      if (isFullJoin(parent)) {
        final int index = indexOfChild(plan, cursor);
        if (index == 0) {
          plan.infoCache().putJoinKindOf(parent, LEFT_JOIN);
        } else {
          plan.infoCache().putJoinKindOf(parent, RIGHT_JOIN);
        }
        modified = true;
      }
      cursor = parent;
    }
  }

  private List<Value> gatherEnforcedNonNullRefs(Expression expr) {
    final Values valueRefs = plan.valuesReg().valueRefsOf(expr);
    final List<SqlNode> colRefs = expr.internalRefs();
    final List<Value> nonNullRefs = new ArrayList<>(valueRefs.size());
    assert valueRefs.size() == colRefs.size();
    for (int i = 0, bound = colRefs.size(); i < bound; i++)
      if (isEnforcedNonNull(colRefs.get(i))) {
        nonNullRefs.add(valueRefs.get(i));
      }
    return nonNullRefs;
  }

  private static boolean isEnforcedNonNull(SqlNode colRef) {
    final SqlNode parent = colRef.parent();
    final BinaryOpKind binOp = parent.$(Binary_Op);
    if (binOp != null) {
      final LiteralKind literalKind = parent.$(Binary_Right).$(Literal_Kind);
      return binOp != IS || (literalKind != NULL && literalKind != UNKNOWN);
    }

    final TernaryOp ternaryOp = parent.$(Ternary_Op);
    if (ternaryOp != null) return colRef == parent.$(Ternary_Left);

    return false;
  }

  private boolean isLeftJoin(int node) {
    return plan.kindOf(node) == Join && joinKindOf(plan, node) == LEFT_JOIN;
  }

  private boolean isRightJoin(int node) {
    return plan.kindOf(node) == Join && joinKindOf(plan, node) == RIGHT_JOIN;
  }

  private boolean isFullJoin(int node) {
    return plan.kindOf(node) == Join && joinKindOf(plan, node) == FULL_JOIN;
  }
}
