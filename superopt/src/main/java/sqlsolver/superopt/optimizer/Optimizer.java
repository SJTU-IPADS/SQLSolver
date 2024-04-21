package sqlsolver.superopt.optimizer;

import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanSupport;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.substitution.SubstitutionBank;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

public interface Optimizer {
  Set<PlanContext> optimize(PlanContext plan);

  Set<PlanContext> optimizePartial(PlanContext plan, int root);

  void setTimeout(long timeout);

  void setTracing(boolean flag);

  void setExtended(boolean extension);

  void setVerbose(boolean flag);

  void setKeepOriginal(boolean keepOriginal);

  List<OptimizationStep> traceOf(PlanContext plan);

  static Optimizer mk(SubstitutionBank bank) {
    return new BottomUpOptimizer(bank);
  }

  default Set<PlanContext> optimize(SqlNode sql) {
    return optimize(sql, sql.context().schema());
  }

  default Set<PlanContext> optimize(SqlNode sql, Schema schema) {
    final PlanContext plan = PlanSupport.assemblePlan(sql, schema);
    if (plan != null) return optimize(plan);
    else return emptySet();
  }
}
