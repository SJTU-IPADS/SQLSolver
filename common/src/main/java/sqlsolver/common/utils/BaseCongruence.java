package sqlsolver.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

public abstract class BaseCongruence<K, T> implements Congruence<K, T> {
  protected final Map<K, BaseCongruentClass<T>> classes;

  protected BaseCongruence() {
    classes = new HashMap<>();
  }

  boolean bind(T t, BaseCongruentClass<T> cls) {
    // returns: true: the `key` is bound to `group`
    //          false: the `key` is not bound, because there are another group already.
    //                 in this case, the two group is merged
    if (cls == null) throw new IllegalArgumentException();

    final K k = extractKey(t);
    final BaseCongruentClass<T> existing = classes.putIfAbsent(k, cls);
    if (existing == null) return true;
    if (existing.equals(cls)) return false;

    cls.merge(existing);
    assert cls.equals(existing);

    return false;
  }

  BaseCongruentClass<T> getClass0(T x) {
    return classes.get(extractKey(x));
  }

  @Override
  public Set<K> keys() {
    return classes.keySet();
  }

  @Override
  public Set<T> mkEqClass(T x) {
    requireNonNull(x);

    BaseCongruentClass<T> congruentClass = classes.get(extractKey(x));
    if (congruentClass != null) return congruentClass;

    congruentClass = mkCongruentClass();
    congruentClass.add(x);
    return congruentClass;
  }

  @Override
  public Set<T> eqClassAt(K k) {
    return classes.get(k);
  }

  @Override
  public Set<T> eqClassOf(T x) {
    final BaseCongruentClass<T> cls = classes.get(extractKey(x));
    return cls != null ? cls : singleton(x);
  }

  @Override
  public void putCongruent(T x, T y) {
    mkEqClass(x).add(y);
  }

  @Override
  public boolean isCongruent(T x, T y) {
    if (Objects.equals(x, y)) return true; // reflexivity
    if (x == null || y == null) return false;

    final CongruentClass<T> gx = classes.get(extractKey(x)), gy = classes.get(extractKey(y));
    return gx != null && gx.equals(gy);
  }

  protected abstract K extractKey(T t);

  protected BaseCongruentClass<T> mkCongruentClass() {
    return new BaseCongruentClass<>(this);
  }
}
