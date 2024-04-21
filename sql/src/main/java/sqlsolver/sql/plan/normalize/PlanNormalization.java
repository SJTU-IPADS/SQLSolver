package sqlsolver.sql.plan.normalize;

import sqlsolver.sql.plan.PlanContext;

import static sqlsolver.sql.plan.normalize.action.NormalizeBool.normalizeFilter;

public class PlanNormalization {

  private PlanNormalization(PlanContext plan) {
  }

  public static void normalizePlan(PlanContext plan) {
    normalizeNode(plan.root(), plan);
  }

  private static void normalizeNode(int nodeId, PlanContext plan) {
    if (nodeId == 0) return;
    switch (plan.kindOf(nodeId)) {
      case Filter -> {
        normalizeNode(plan.childOf(nodeId, 0), plan);
        normalizeFilter(nodeId, plan);
      }
      default -> normalizeNode(plan.childOf(nodeId, 0), plan);
    }
  }
}
