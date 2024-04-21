package sqlsolver.common.utils;

import java.util.Set;

/**
 * An equivalence relation on a set of {@link T}.
 *
 * <p>Another key type {@link K} is used to determine the equivalence by calling {@link
 * K#equals(Object)}. Implementor should provide a conversion T -> K.
 *
 * <p>Some methods returns an equivalence class, which is a modifiable set. Putting object into it
 * logically put new object into the equivalent class.
 */
public interface Congruence<K, T> extends Copyable<Congruence<K, T>> {
  /** Returns all keys */
  Set<K> keys();

  /** Retrieve the equivalent class of an object */
  Set<T> eqClassOf(T x);

  /** Retrieve the equivalent class of a key */
  Set<T> eqClassAt(K k);

  /**
   * Put `x` and `y` into the same equivalent class.
   *
   * <p>If the two objects are in different equivalent classes before, they are merged.
   */
  void putCongruent(T x, T y);

  /** Check if `x` and `y` is in the same equivalence class. */
  boolean isCongruent(T x, T y);

  /**
   * Explicitly register this object.
   *
   * <p>If `x` is known to the relation before, returns its equivalent class. Otherwise, returns a
   * new equivalent class containing only `x`.
   */
  Set<T> mkEqClass(T x);

  default Congruence<K, T> copy() {
    return this;
  }
}
