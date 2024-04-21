package sqlsolver.superopt.liastar.translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import sqlsolver.superopt.uexpr.UVar;

/** Bound var matching. */
public class BVM {
  final List<Set<UVar>> matching;

  public BVM(List<Set<UVar>> matching) {
    this.matching = matching;
  }

  /**
   * Get the depth-th layer of summation bound vars.
   *
   * @param depth the layer index (ascending; the outermost layer has index 0)
   * @return a set representing bound vars at the specified layer
   */
  public Set<UVar> get(int depth) {
    return matching.get(depth);
  }

  /**
   * Swap the specified pair of bound vars in this BVM. Note that this method does not change the
   * object.
   *
   * @return the resulting BVM after swap
   */
  public BVM swapVars(UVar v1, UVar v2) {
    final List<Set<UVar>> newMatching = new ArrayList<>();
    final BVM newBvm = new BVM(newMatching);
    // swap vars in each layer
    for (Set<UVar> layer : matching) {
      newMatching.add(
          layer.stream()
              .map(v -> v.equals(v1) ? v2 : (v.equals(v2) ? v1 : v))
              .collect(Collectors.toSet()));
    }
    return newBvm;
  }

  @Override
  public int hashCode() {
    return matching.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BVM bvm && matching.equals(bvm.matching);
  }

  @Override
  public String toString() {
    return matching.toString();
  }
}
