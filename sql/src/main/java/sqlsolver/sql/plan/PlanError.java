package sqlsolver.sql.plan;

import sqlsolver.sql.ast.ExprFields;

import java.util.List;

import static sqlsolver.sql.ast.SqlNodeFields.GroupItem_Expr;

public class PlanError {
  private final PlanContext plan0, plan1;

  public PlanError(PlanContext plan0, PlanContext plan1) {
    this.plan0 = plan0;
    this.plan1 = plan1;
  }

  boolean isErrorTrees() {
    return isErrorTrees(plan0.root(), plan1.root());
  }

  boolean isErrorTrees(int root0, int root1) {
    // both semantic error imply EQ
    return isSemanticError(root0, this.plan0) && isSemanticError(root1, this.plan1);
  }

  private boolean isSemanticError(int node, PlanContext plan) {
    final PlanKind kind = plan.kindOf(node);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      if (isSemanticError(plan0.childOf(node, i), plan)) return true;
    }


    return switch (kind) {
      case SetOp -> isErrorSetOp(node, plan);
      case Sort -> isErrorSort(node, plan);
      case Limit -> isErrorLimit(node, plan);
      case Filter -> isErrorFilter(node, plan);
      case Agg -> isErrorAgg(node, plan);
      case Proj -> isErrorProj(node, plan);
      case InSub -> isErrorInSub(node, plan);
      case Exists -> isErrorExists(node, plan);
      case Join -> isErrorJoin(node, plan);
      case Input -> isErrorInput(node, plan);
    };
  }

  private boolean isErrorInput(int node, PlanContext plan) { return false; }

  private boolean isErrorJoin(int node, PlanContext plan) { return false; }

  private boolean isErrorExists(int node, PlanContext plan) { return false; }

  private boolean isErrorInSub(int node, PlanContext plan) { return false; }

  private boolean isErrorProj(int node, PlanContext plan) { return false; }

  // SELECT ... GROUP BY CONSTANT, when CONSTANT is larger than selectlist size -> ERROR
  private boolean isErrorAgg(int node, PlanContext plan) {
    final AggNode agg = (AggNode) plan.nodeAt(node);

    final List<Expression> exprs = agg.attrExprs();

    final List<Expression> groups = agg.groupByExprs();

    for(Expression group : groups) {
      if(group.template().$(GroupItem_Expr).$(ExprFields.Literal_Value) != null
              && group.template().$(GroupItem_Expr).$(ExprFields.Literal_Value) instanceof Integer
              && (Integer) group.template().$(GroupItem_Expr).$(ExprFields.Literal_Value) > exprs.size())
        return true;
    }
    return false;
  }

  private boolean isErrorFilter(int node, PlanContext plan) { return false; }

  private boolean isErrorLimit(int node, PlanContext plan) { return false; }

  private boolean isErrorSort(int node, PlanContext plan) { return false; }

  private boolean isErrorSetOp(int node, PlanContext plan) { return false; }
}
