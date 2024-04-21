package sqlsolver.common.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BaseNaturalCongruence<T> extends BaseCongruence<T, T> implements NaturalCongruence<T> {
  @Override
  protected T extractKey(T t) {
    return t;
  }

  @Override
  public Congruence<T, T> copy() {
    final Set<T> keys = new HashSet<>(keys());
    final Congruence<T, T> copy = new BaseNaturalCongruence<>();
    while (!keys.isEmpty()) {
      final Iterator<T> iter = keys.iterator();
      final T targetKey = iter.next();
      // First put itself into a class
      copy.putCongruent(targetKey, targetKey);
      iter.remove();
      while (iter.hasNext()) {
        final T key = iter.next();
        if (isCongruent(targetKey, key)) {
          copy.putCongruent(targetKey, key);
          iter.remove();
        }
      }
    }
    return copy;
  }
}
