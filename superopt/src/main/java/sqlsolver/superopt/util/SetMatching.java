package sqlsolver.superopt.util;

import java.util.*;

import static sqlsolver.common.utils.SetSupport.intersect;

/**
 * Record matching between sets.
 * (e.g. {1,2}~{4,5}, {3}~{6})
 * <br/>
 * Matching sets always have the same size.
 * (e.g. {1,2} and {3} cannot match)
 * <br/>
 * Sets on either side (left/right) are always disjoint.
 * (e.g. {1}~{2}, {1}~{3} is not allowed)
 */
public class SetMatching<T> implements Iterable<Set<T>[]> {

  private final List<Set<T>[]> pairs = new ArrayList<>();

  private void addMatching(Set<T> s1, Set<T> s2) {
    if (!s1.isEmpty())
      pairs.add(new Set[]{s1, s2});
  }

  // Assumption: a1,b1 have the same size; a2,b2 have the same size
  //    A1 ~ B1 /\ A2 ~ B2
  // -> A1A2 ~ B1B2 /\ A1-A1A2 ~ B1-B1B2 /\ A2-A1A2 ~ B2-B1B2
  // return false if sets of different sizes become matched
  private boolean resolveOverlapping(Set<T> a1, Set<T> b1, Set<T> a2, Set<T> b2) {
    // a=a1a2, b=b1b2
    Set<T> a = intersect(a1, a2);
    Set<T> b = intersect(b1, b2);
    if (a.size() != b.size()) return false;
    // from now on, size of matching sets must be the same
    // a ~ b
    addMatching(a, b);
    // a1-a ~ b1-b
    a1.removeAll(a);
    b1.removeAll(b);
    // a2-a ~ b2-b
    a2.removeAll(a);
    b2.removeAll(b);
    return true;
  }

  private void removeEmptyPairs() {
    List<Set<T>[]> toRemove = new ArrayList<>();
    for (Set<T>[] pair : pairs) {
      if (pair[0].isEmpty()) toRemove.add(pair);
    }
    pairs.removeAll(toRemove);
  }

  public boolean match(Set<T> s1, Set<T> s2) {
    if (s1.size() != s2.size()) return false;
    s1 = new HashSet<>(s1);
    s2 = new HashSet<>(s2);
    for (Set<T>[] pair : new ArrayList<>(pairs)) {
      if (!resolveOverlapping(pair[0], pair[1], s1, s2)) return false;
    }
    addMatching(s1, s2);
    removeEmptyPairs();
    return true;
  }

  public boolean match(T t1, T t2) {
    Set<T> s1 = new HashSet<>(), s2 = new HashSet<>();
    s1.add(t1);
    s2.add(t2);
    return match(s1, s2);
  }

  public int size() {
    return pairs.size();
  }

  public Set<T>[] get(int index) {
    return pairs.get(index);
  }

  @Override
  public Iterator<Set<T>[]> iterator() {
    return pairs.iterator();
  }
}
