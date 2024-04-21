package sqlsolver.superopt.optimizer;

import sqlsolver.sql.plan.PlanContext;
import sqlsolver.superopt.substitution.Substitution;

public record OptimizationStep(PlanContext source,
                               PlanContext target,
                               Substitution rule,
                               int extra) {
  public int ruleId() {
    return rule == null ? -extra : rule.id();
  }
}
