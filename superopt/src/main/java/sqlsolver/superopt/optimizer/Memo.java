package sqlsolver.superopt.optimizer;

import sqlsolver.common.utils.BaseCongruence;
import sqlsolver.common.utils.BaseCongruentClass;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanKind;

import static sqlsolver.sql.plan.PlanSupport.stringifyNode;

class Memo extends BaseCongruence<String, SubPlan> {
  boolean isRegistered(SubPlan node) {
    return classes.containsKey(extractKey(node));
  }

  boolean isRegistered(PlanContext plan, int nodeId) {
    return classes.containsKey(extractKey(new SubPlan(plan, nodeId)));
  }

  @Override
  protected String extractKey(SubPlan subPlan) {
    if (subPlan.rootKind() != PlanKind.Input) return subPlan.toString();
    else return stringifyNode(subPlan.plan(), subPlan.nodeId());
  }

  @Override
  protected BaseCongruentClass<SubPlan> mkCongruentClass() {
    return new OptGroup(this);
  }
}
