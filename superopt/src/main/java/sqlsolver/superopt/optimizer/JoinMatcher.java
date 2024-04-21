package sqlsolver.superopt.optimizer;

import sqlsolver.sql.plan.PlanContext;
import sqlsolver.superopt.fragment.Join;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;

class JoinMatcher {
  private final Join op;
  private final LinearJoinTree joinTree;

  JoinMatcher(Join op, PlanContext ctx, int joinTreeRoot) {
    this.op = op;
    this.joinTree = LinearJoinTree.mk(ctx, joinTreeRoot);
  }

  List<Match> matchBasedOn(Match baseMatch) {
    if (joinTree == null) return emptyList();

    final List<Match> matches = new LinkedList<>();
    for (int i = joinTree.numJoiners() - 1; i >= -1; --i) {
      final Match match = tryMatchAt(i, baseMatch);
      if (match != null) matches.add(match);
    }

    return matches;
  }

  private Match tryMatchAt(int rootJoineeIdx, Match baseMatch) {
    if (rootJoineeIdx < 0 && !allowFlip()) return null;
    if (!joinTree.isEligibleRoot(rootJoineeIdx)) return null;

    final PlanContext newPlan = joinTree.mkRootedBy(rootJoineeIdx);
    final Match derived = baseMatch.derive().setSourcePlan(newPlan);
    final int joiner = joinTree.joinerOf(rootJoineeIdx);

    if (baseMatch.matchRootNode() == joinTree.rootJoiner()) derived.setMatchRootNode(joiner);

    if (derived.matchOne(op, joiner)) return derived;
    else return null;
  }

  private boolean allowFlip() {
    return (OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_DISABLE_JOIN_FLIP) == 0;
  }
}
