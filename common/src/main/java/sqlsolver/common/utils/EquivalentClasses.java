package sqlsolver.common.utils;

import java.util.*;

import static sqlsolver.common.utils.Commons.newIdentitySet;

public class EquivalentClasses<T> {
  private final Map<T, EquivalentClass> classes = new IdentityHashMap<>();

  public Set<T> makeClass(T x) {
    EquivalentClass cls = classes.get(x);
    if (cls != null) return cls;

    cls = new EquivalentClass();
    cls.add(x);
    return cls;
  }

  public Set<T> get(T x) {
    return classes.get(x);
  }

  private boolean bind(T x, EquivalentClass cls) {
    final EquivalentClass existing = classes.putIfAbsent(x, cls);
    if (existing == null) return true;
    if (existing.equals(cls)) return false;

    cls.merge(existing);
    assert cls.equals(existing);
    return false;
  }

  private class EquivalentClass extends AbstractSet<T> {
    private Set<T> elements = Commons.newIdentitySet();

    @Override
    public boolean add(T t) {
      if (bind(t, this)) return elements.add(t);
      return true;
    }

    public void merge(EquivalentClass other) {
      this.elements.addAll(other.elements);
      other.elements = this.elements;
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
      if (o == null || getClass() != o.getClass()) return false;
      final EquivalentClass that = (EquivalentClass) o;
      return this.elements == that.elements;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(elements);
    }
  }
}
