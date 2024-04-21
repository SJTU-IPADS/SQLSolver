package sqlsolver.superopt.optimizer;

import sqlsolver.common.utils.ArraySupport;
import sqlsolver.common.utils.Lazy;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.plan.*;

import java.util.Collections;
import java.util.List;

import static java.lang.Integer.max;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.sql.plan.PlanSupport.joinKindOf;

/**
 * A data structure representing a left-deep join tree.
 *
 * <pre>
 * Join{A.z=C.w}(Join{A.x=B.y}(Input{A}, Input{B}), Input{C})
 * joiners[0]: Join{A.x=B.y}, [1]: Join{A.z=C.w}
 * joinees[-1]: Input{A}, [0]: Input{B}, [1]: Input{C}
 * </pre>
 *
 * Invariant: i-th joinee is the RHS of i-th joiner.
 *
 * <p>invariant: joinees.length == joiners.length + 1
 */
final class LinearJoinTree {
  private final PlanContext plan;
  private final int treeParent;
  private final int[] joiners; // JoinNodes.
  private final int[] joinees; // joined PlanNodes.
  // dependency[i] == j: i-th joinee's LHS join keys come from j-th joinee.
  // e.g., A Join B On p(A,B) Join C On p(B,C), dependencies=[-1,0,1]
  private final Lazy<int[]> dependencies;

  private LinearJoinTree(PlanContext plan, int treeParent, int[] joiners, int[] joinees) {
    this.plan = plan;
    this.treeParent = treeParent;
    this.joiners = joiners;
    this.joinees = joinees;
    this.dependencies = Lazy.mk(this::calcDependencies);
  }

  static LinearJoinTree mk(PlanContext plan, int treeRoot) {
    final InfoCache infoCache = plan.infoCache();

    int depth = 0, cursor = treeRoot;
    while (plan.kindOf(cursor) == PlanKind.Join) { // && infoCache.isEquiJoin(cursor)) {
      ++depth;
      cursor = plan.childOf(cursor, 0);
    }

    if (depth == 0) return null;

    final int[] joiners = new int[depth], joinees = new int[depth + 1];
    cursor = treeRoot;
    while (depth > 0) {
      --depth;
      joiners[depth] = cursor;
      joinees[depth + 1] = plan.childOf(cursor, 1);
      cursor = plan.childOf(cursor, 0);
    }
    joinees[0] = cursor;

    return new LinearJoinTree(plan, plan.parentOf(treeRoot), joiners, joinees);
  }

  int treeParent() {
    return treeParent;
  }

  int numJoiners() {
    return joiners.length;
  }

  int joinerAt(int joinerIdx) {
    return joiners[joinerIdx];
  }

  int joineeAt(int joineeIdx) {
    return joinees[joineeIdx + 1];
  }

  int joinerOf(int joineeIdx) {
    return joiners[max(0, joineeIdx)];
  }

  int rootJoiner() {
    return joiners[joiners.length - 1];
  }

  boolean isEligibleRoot(int joineeIndex) {
    if (joineeIndex >= joinees.length - 2) return true;
    final int[] dependencies = this.dependencies.get();
    if (ArraySupport.linearFind(dependencies, joineeIndex, max(2, joineeIndex + 2)) != -1)
      return false;

    return joineeIndex >= 0 || joinKindOf(plan, rootJoiner()).isInner();
  }

  PlanContext mkRootedBy(int joineeIdx) {
    if (joineeIdx >= joinees.length - 2) return plan;

    final PlanContext newPlan = plan.copy();
    final int oldRootJoiner = rootJoiner();
    final int treeIndex = indexOfChild(newPlan, oldRootJoiner);
    final int newRootIdx = max(0, joineeIdx);
    final int newRootJoiner = joiners[newRootIdx];
    final int cutParent = newPlan.parentOf(newRootJoiner);
    final int cutChild0 = newPlan.childOf(newRootJoiner, 0);
    final int cutChild1 = newPlan.childOf(newRootJoiner, 1);

    if (joineeIdx >= 0) {
      // Join0(Join1(A,B),C) -> Join1(Join0(A,C),B)
      // oldRootJoiner = Join0, newRootJoiner = Join1
      // cutParent = Join0, cutChild0 = A
      // (Note that `oldRootJoiner`'s RHS and `newRootJoiner`'s RHS are unchanged)
      assert newRootJoiner != oldRootJoiner;
      newPlan.detachNode(oldRootJoiner);
      newPlan.detachNode(newRootJoiner);
      newPlan.detachNode(cutChild0);
      newPlan.setChild(treeParent, treeIndex, newRootJoiner);
      newPlan.setChild(newRootJoiner, 0, oldRootJoiner);
      newPlan.setChild(cutParent, 0, cutChild0);

    } else {
      // case1: Join0(A,B) -> Join0(B,A)
      //   oldRootJoiner = Join0, newRootJoiner = Join0
      //   cutParent = Join0.parent, cutChild0 = A, cutChild = B
      // case2: Join0(Join1(A,B),C) -> Join1(Join0(B,C),A)
      //   oldRootJoiner = Join0, newRootJoiner = Join1
      //   cutParent = Join0, cutChild0 = A, cutChild1 = B
      // (Note that in both cases, `cutChild0` becomes `newRootJoiner`'s RHS)
      assert joineeIdx == -1;
      assert newRootJoiner == oldRootJoiner || cutParent != treeParent;

      newPlan.detachNode(cutChild0);
      newPlan.detachNode(cutChild1);
      newPlan.setChild(newRootJoiner, 1, cutChild0);
      if (newRootJoiner == oldRootJoiner) newPlan.setChild(newRootJoiner, 0, cutChild1);
      else {
        newPlan.detachNode(newRootJoiner);
        newPlan.setChild(treeParent, treeIndex, newRootJoiner);
        newPlan.setChild(newRootJoiner, 0, oldRootJoiner);
        newPlan.setChild(cutParent, 0, cutChild1);
      }

      final InfoCache infoCache = newPlan.infoCache();
      final var keys = infoCache.getJoinKeyOf(newRootJoiner);
      if (keys != null) infoCache.putJoinKeyOf(newRootJoiner, keys.getRight(), keys.getLeft());
    }

    return newPlan;
  }

  private int[] calcDependencies() {
    final int[] dependencies = new int[joinees.length];
    final ValuesRegistry valuesReg = plan.valuesReg();

    dependencies[0] = -2;
    dependencies[1] = -1;
    for (int i = 2, bound = joinees.length; i < bound; ++i) {
      final List<Value> lhsKeys = getLhsRefs(joiners[i - 1]);
      for (int j = i - 1; j >= 0; --j)
        if (any(lhsKeys, valuesReg.valuesOf(joinees[j])::contains)) {
          dependencies[i] = j - 1;
          break;
        }
    }

    return dependencies;
  }

  private List<Value> getLhsRefs(int joinerNode) {
    final Expression joinCond = ((JoinNode) plan.nodeAt(joinerNode)).joinCond();
    if (joinCond == null) return Collections.emptyList();
    final Values rhsValues = plan.valuesReg().valuesOf(plan.childOf(joinerNode, 1));
    final Values refs = plan.valuesReg().valueRefsOf(joinCond);
    return ListSupport.filter(refs, ref -> !rhsValues.contains(ref));
  }
}
