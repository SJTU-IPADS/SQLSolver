package sqlsolver.superopt.liastar.translator;

import static sqlsolver.superopt.liastar.translator.BvmEvaluation.similarityScore;

import java.util.*;
import sqlsolver.superopt.uexpr.UTerm;
import sqlsolver.superopt.uexpr.UVar;

/**
 * A heuristic BVM enumerator enumerates BVMs in the following order: first, a "seed" is chosen by
 * algorithm; second, apply breadth-first search starting with that seed; the enqueueing order is
 * the descending order of "similarity score".
 *
 * @see BvmEvaluation#similarityScore(BVM, UTerm, UTerm)
 */
public class HeuristicBvmEnumerator extends BvmEnumerator {

  private static final int DEFAULT_LIMIT = 10;
  private final USumTree sumTree1, sumTree2;

  // traversal state
  private final Queue<BVM> bvmQueue;
  private final Set<BVM> enqueuedBvms;

  public HeuristicBvmEnumerator(UTerm uexp1, UTerm uexp2, UVar outVar, int limit) {
    super(uexp1, uexp2, limit);
    // compute and cache the structure of both U-expressions
    sumTree1 = new USumTree(uexp1);
    sumTree2 = new USumTree(uexp2);
    // initialize the traversal state
    bvmQueue =
        new PriorityQueue<>(Comparator.comparingInt(bvm -> similarityScore(bvm, uexp1, uexp2)));
    bvmQueue.add(new HeuristicSeedGenerator().generateSeed(uexp1, uexp2, outVar));
    enqueuedBvms = new HashSet<>();
    enqueuedBvms.add(bvmQueue.peek());
  }

  /**
   * Construct an enumerator with a default limit.
   *
   * @param uexp1 the first U-expression
   * @param uexp2 the second U-expression
   */
  public HeuristicBvmEnumerator(UTerm uexp1, UTerm uexp2, UVar outVar) {
    this(uexp1, uexp2, outVar, DEFAULT_LIMIT);
  }

  @Override
  protected BVM next(UTerm uexp1, UTerm uexp2) {
    // try to fetch the head of queue
    final BVM currentBvm = bvmQueue.poll();
    if (currentBvm == null) {
      return null;
    }
    // append mutations of the BVM being visited at the end of queue
    Collection<BVM> mutatedBvms = mutate(currentBvm);
    for (BVM bvm : mutatedBvms) {
      if (!enqueuedBvms.contains(bvm)) {
        bvmQueue.add(bvm);
        enqueuedBvms.add(bvm);
      }
      // BVMs already visited are not enqueued
    }
    // return the BVM being visited
    return currentBvm;
  }

  /**
   * Try all possible mutations on the BVM (i.e. try swapping every pair of bound vars within the
   * same node of either sum tree).
   */
  private Collection<BVM> mutate(BVM bvm) {
    final List<Set<BoundVar>> nodes = sumTree1.getAllNodes();
    nodes.addAll(sumTree2.getAllNodes());
    final Collection<BVM> result = new HashSet<>();
    for (Set<BoundVar> node : nodes) {
      result.addAll(mutateWithinSet(bvm, node));
    }
    return result;
  }

  /** Try swapping every pair of bound vars within the specified set. */
  private Collection<BVM> mutateWithinSet(BVM bvm, Set<BoundVar> vars) {
    final List<BoundVar> varList = new ArrayList<>(vars);
    final Collection<BVM> result = new HashSet<>();
    final int bound = varList.size();
    for (int i = 0; i < bound; i++) {
      for (int j = i + 1; j < bound; j++) {
        final BoundVar bv1 = varList.get(i);
        final BoundVar bv2 = varList.get(j);
        // only swap vars within the same table or no table
        if (Objects.equals(bv1.table(), bv2.table())) {
          result.add(bvm.swapVars(bv1.var(), bv2.var()));
        }
      }
    }
    return result;
  }
}
