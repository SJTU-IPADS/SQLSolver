package sqlsolver.superopt.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;


public class Bag<T> implements Collection<T> {
  private final Map<T, Integer> countMap;

  public static <T> Bag<T> minus(Bag<T> bag1, Bag<T> bag2) {
    Bag<T> diffBag = new Bag<>();
    for (T key : bag1.countMap.keySet()) {
      diffBag.add(key, Math.max(bag1.count(key) - bag2.count(key), 0));
    }
    return diffBag;
  }

  public Bag() {
    countMap = new HashMap<>();
  }

  public Bag(Collection<T> collection) {
    this();
    addAll(collection);
  }

  public boolean add(T e, int count) {
    if (countMap.containsKey(e)) countMap.compute(e, (k, v) -> v + count);
    else countMap.put(e, count);
    return true;
  }

  public int count(Object e) {
    return countMap.getOrDefault(e, 0);
  }

  @Override
  public boolean contains(Object o) {
    return count(o) > 0;
  }

  @Override
  public boolean add(T e) {
    return add(e, 1);
  }

  @Override
  public boolean remove(Object o) {
    final Integer count = countMap.get(o);
    if (count != null && count > 0) {
      if (count == 1) countMap.remove(o);
      else countMap.put((T) o, count - 1);
      return true;
    } else return false;
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    final Bag<T> that = new Bag<>();
    for (Object o : c) {
      try {
        that.add((T) o);
      } catch (ClassCastException e) {
        return false;
      }
    }
    for (T key : that.countMap.keySet()) {
      if (count(key) < that.count(key)) return false;
    }
    return true;
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends T> c) {
    boolean result = false;
    for (T t : c) {
      result = add(t) || result;
    }
    return result;
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    boolean result = false;
    for (Object o : c) {
      result = remove(o) || result;
    }
    return result;
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    final Bag<T> that = new Bag<>();
    for (Object o : c) {
      try {
        that.add((T) o);
      } catch (ClassCastException ignored) {}
    }
    boolean result = false;
    for (T key : new HashSet<>(countMap.keySet())) {
      final int limit = that.count(key);
      if (count(key) > limit) {
        countMap.put(key, limit);
        result = true;
      }
    }
    return result;
  }

  @Override
  public void clear() {
    countMap.clear();
  }

  @Override
  public int size() {
    return countMap.values().stream().mapToInt(Integer::intValue).sum();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return new Itr();
  }

  private class Itr implements Iterator<T> {
    final Iterator<Map.Entry<T, Integer>> entryIterator;
    // which key is the next element; null indicates the end
    Map.Entry<T, Integer> next;
    int count;

    Itr() {
      entryIterator = countMap.entrySet().iterator();
      if (entryIterator.hasNext()) next = entryIterator.next();
      else next = null;
      count = 0;
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public T next() {
      if (next == null) throw new NoSuchElementException();
      final T result = next.getKey();
      if (++count >= next.getValue()) {
        if (entryIterator.hasNext()) next = entryIterator.next();
        else next = null;
        count = 0;
      }
      return result;
    }
  }

  @NotNull
  @Override
  public Object @NotNull [] toArray() {
    return toArray(new Object[0]);
  }

  @NotNull
  @Override
  public <T1> T1[] toArray(@NotNull T1[] a) {
    final T1[] target;
    final int requiredSize = size();
    if (a.length >= requiredSize) target = a;
    else target = (T1[]) Array.newInstance(a.getClass(), requiredSize);
    int index = 0;
    for (T key : countMap.keySet()) {
      for (int i = 0, bound = countMap.get(key); i < bound; i++) {
        // hack: duplicate instead of copy
        target[index++] = (T1) key;
      }
    }
    return target;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Bag<?> bag)) return false;
    Bag<T> that = new Bag<>();
    for (Object key : bag.countMap.keySet()) {
      try {
        that.add((T) key, bag.count(key));
      } catch (ClassCastException e) {
        return false;
      }
    }
    return minus(this, that).isEmpty() && minus(that, this).isEmpty();
  }
}