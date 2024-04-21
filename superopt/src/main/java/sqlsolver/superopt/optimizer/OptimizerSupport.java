package sqlsolver.superopt.optimizer;

import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanKind;

import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.sql.plan.PlanSupport.setupSubqueryExprOf;
import static sqlsolver.sql.plan.PlanSupport.stringifyTree;

public abstract class OptimizerSupport {
  public static final String FAILURE_INCOMPLETE_MODEL = "incomplete model ";
  public static final String FAILURE_MISMATCHED_JOIN_KEYS = "mismatched join key ";
  public static final String FAILURE_FOREIGN_VALUE = "foreign value ";
  public static final String FAILURE_MALFORMED_SUBQUERY = "malformed subquery ";
  public static final String FAILURE_MALFORMED_AGG = "malformed aggregation ";
  public static final String FAILURE_MISMATCHED_REFS = "mismatched refs ";
  public static final String FAILURE_UNKNOWN_OP = "unknown op ";
  public static final String FAILURE_ABUSED_SUBQUERY = "abused subquery ";

  public static final int TWEAK_DISABLE_JOIN_FLIP = 1;
  public static final int TWEAK_ENABLE_EXTENSIONS = 2;
  public static final int TWEAK_KEEP_ORIGINAL_PLAN = 4;
  public static final int TWEAK_SORT_FILTERS_DURING_REWRITE = 8;
  public static final int TWEAK_SORT_FILTERS_BEFORE_OUTPUT = 16;
  public static final int TWEAK_PERMUTE_JOIN_TREE = 32;
  public static final int TWEAK_ENABLE_QUERY_AS_EQ_INPUT = 64;
  static int optimizerTweaks = 0;

  private static final ThreadLocal<String> LAST_ERROR = new ThreadLocal<>();

  public static void setOptimizerTweaks(int optimizerTweaks) {
    OptimizerSupport.optimizerTweaks = optimizerTweaks;
  }

  public static void addOptimizerTweaks(int optimizerTweaks) {
    OptimizerSupport.optimizerTweaks |= optimizerTweaks;
  }

  static void setLastError(String error) {
    LAST_ERROR.set(error);
  }

  static int normalizeJoin(PlanContext plan, int rootId) {
    return new NormalizeJoin(plan).normalizeTree(rootId);
  }

  static int normalizeProj(PlanContext plan, int rootId) {
    return new NormalizeProj(plan).normalizeTree(rootId);
  }

  static int normalizeFilter(PlanContext plan, int rootId) {
    if ((optimizerTweaks & TWEAK_SORT_FILTERS_DURING_REWRITE) != 0) {
      return new NormalizeFilter(plan).normalizeTree(rootId);
    } else {
      return rootId;
    }
  }

  static int normalizePlan(PlanContext plan, int rootId) {
    if ((rootId = normalizeJoin(plan, rootId)) == NO_SUCH_NODE) return NO_SUCH_NODE;
    if ((rootId = normalizeProj(plan, rootId)) == NO_SUCH_NODE) return NO_SUCH_NODE;
    if ((rootId = normalizeFilter(plan, rootId)) == NO_SUCH_NODE) return NO_SUCH_NODE;
    if (!compensateSubqueryExpr(plan, rootId)) return NO_SUCH_NODE;
    return rootId;
  }

  private static boolean compensateSubqueryExpr(PlanContext plan, int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    if (kind.isSubqueryFilter() && !setupSubqueryExprOf(plan, nodeId)) {
      LAST_ERROR.set(FAILURE_MALFORMED_SUBQUERY + nodeId + " in " + plan);
      return false;
    }

    for (int i = 0; i < kind.numChildren(); ++i)
      if (!compensateSubqueryExpr(plan, plan.childOf(nodeId, i))) {
        return false;
      }

    return true;
  }

  public static void dumpTrace(Optimizer optimizer, PlanContext result) {
    System.out.println("=== begin dump trace ===");
    final List<OptimizationStep> steps = optimizer.traceOf(result);
    if (!steps.isEmpty()) {
      final PlanContext startPoint = steps.get(0).source();
      System.out.println(stringifyTree(startPoint, startPoint.root(), false, true));
      for (OptimizationStep step : steps) {
        final PlanContext target = step.target();
        final String ruleString;
        if (step.rule() != null) ruleString = step.rule().toString();
        else ruleString = PreprocessRule.getDescByRuleId(step.extra());

        System.out.println("~~ " + ruleString);
        System.out.println("==> " + stringifyTree(target, target.root(), true, true));
      }
    }
    System.out.println("=== end dump trace ===");
  }

  public static String getLastError() {
    return LAST_ERROR.get();
  }
}
