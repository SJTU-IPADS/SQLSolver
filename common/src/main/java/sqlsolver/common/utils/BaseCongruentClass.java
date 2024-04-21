package sqlsolver.common.utils;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import static java.util.Objects.requireNonNull;

public class BaseCongruentClass<T> extends AbstractSet<T> implements CongruentClass<T> {
  protected final BaseCongruence<?, T> congruence;
  protected Collection<T> elements;

  protected BaseCongruentClass(BaseCongruence<?, T> congruence) {
    this.congruence = congruence;
    this.elements = mkCollection();
  }

  protected void merge(BaseCongruentClass<T> other) {
    assert other.elements != this.elements;
    assert other.congruence == this.congruence;
    // add all plan from `group`
    elements.addAll(other);
    // share same collection to sync automatically
    for (T t : other) congruence.getClass0(t).elements = elements;
  }

  @Override
  public boolean add(T t) {
    requireNonNull(t);

    if (!elements.contains(t) && congruence.bind(t, this)) return elements.add(t);
    return false;
  }

  @Override
  public Iterator<T> iterator() {
    return elements.iterator();
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseCongruentClass)) return false;
    final BaseCongruentClass<?> that = (BaseCongruentClass<?>) o;
    return elements == that.elements;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(elements);
  }

  protected Collection<T> mkCollection() {
    return new HashSet<>();
  }
}
