package sqlsolver.common.utils;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.Integer.min;

public abstract class ListSupport {
  private static final TIntList EMPTY_INT_LIST = new TIntArrayList(0);

  private ListSupport() {}

  public static <T> List<T> generate(int total, IntFunction<? extends T> func) {
    final List<T> list = new ArrayList<>(total);
    for (int i = 0; i < total; ++i) list.add(func.apply(i));
    return list;
  }

  public static <Y> List<Y> map(int[] xs, IntFunction<Y> func) {
    final List<Y> ys = new ArrayList<>(xs.length);
    for (int x : xs) ys.add(func.apply(x));
    return ys;
  }

  public static <X, Y> List<Y> map(
      Collection<? extends X> xs, Function<? super X, ? extends Y> func) {
    final List<Y> ys = new ArrayList<>(xs.size());
    for (X x : xs) ys.add(func.apply(x));
    return ys;
  }

  public static <X, Y> List<Y> map(
      Iterable<? extends X> xs, Function<? super X, ? extends Y> func) {
    if (xs instanceof Collection) return map((Collection<X>) xs, func);
    return StreamSupport.stream(xs.spliterator(), false).map(func).collect(Collectors.toList());
  }

  public static <T> List<T> join(List<T> xs, List<T> ys) {
    if (xs.isEmpty()) return ys;
    else if (ys.isEmpty()) return xs;
    else return new BinaryJoinedList<>(xs, ys);
  }

  @SafeVarargs
  public static <T> List<T> join(
      List<? extends T> xs, List<? extends T> ys, List<? extends T>... ts) {
    final List<List<? extends T>> lists = new ArrayList<>(ts.length + 2);
    if (!xs.isEmpty()) lists.add(xs);
    if (!ys.isEmpty()) lists.add(ys);
    for (List<? extends T> t : ts) if (!t.isEmpty()) lists.add(t);
    return new JoinedList<>(lists);
  }

  public static <T> List<T> concat(List<? extends T> ts0, List<? extends T> ts1) {
    final List<T> ts = new ArrayList<>(ts0.size() + ts1.size());
    ts.addAll(ts0);
    ts.addAll(ts1);
    return ts;
  }

  public static <T> T pop(List<? extends T> xs) {
    if (xs.isEmpty()) return null;
    return xs.remove(xs.size() - 1);
  }

  public static <T> List<T> filter(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
    return IterableSupport.stream(iterable).filter(predicate).collect(Collectors.toList());
  }

  public static <T, R> List<R> flatMap(
      Iterable<? extends T> os, Function<? super T, ? extends Iterable<R>> func) {
    return IterableSupport.stream(os)
        .map(func)
        .flatMap(IterableSupport::stream)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  public static <T, R> List<R> linkedListFlatMap(
      Iterable<? extends T> os, Function<? super T, ? extends Iterable<R>> func) {
    return IterableSupport.stream(os)
        .map(func)
        .flatMap(IterableSupport::stream)
        .collect(Collectors.toCollection(LinkedList::new));
  }

  public static <P0, P1, R> List<R> zipMap(
      Iterable<? extends P0> l0,
      Iterable<? extends P1> l1,
      BiFunction<? super P0, ? super P1, ? extends R> func) {
    final List<R> ret;
    if (l0 instanceof Collection && l1 instanceof Collection) {
      final int size0 = ((Collection) l0).size();
      final int size1 = ((Collection) l1).size();
      ret = new ArrayList<>(min(size0, size1));
    } else {
      ret = new ArrayList<>();
    }

    final Iterator<? extends P0> iter0 = l0.iterator();
    final Iterator<? extends P1> iter1 = l1.iterator();
    while (iter0.hasNext() && iter1.hasNext()) {
      final P0 x = iter0.next();
      final P1 y = iter1.next();
      ret.add(func.apply(x, y));
    }
    return ret;
  }

  public static <T> T elemAt(List<T> xs, int idx) {
    if (idx >= xs.size() || idx <= -xs.size() - 1) return null;
    if (idx >= 0) return xs.get(idx);
    else return xs.get(xs.size() + idx);
  }

  public static <T> T head(List<T> xs) {
    if (xs.isEmpty()) return null;
    else return xs.get(0);
  }

  public static <T> T tail(List<T> xs) {
    if (xs.isEmpty()) return null;
    else return xs.get(xs.size() - 1);
  }

  public static <T> void push(List<T> xs, T x) {
    xs.add(x);
  }

  public static TIntList emptyIntList() {
    return EMPTY_INT_LIST;
  }

  public static <T> void union(List<T> xs, T x) {
    if (!xs.contains(x)) xs.add(x);
  }

  public static <T> void union(List<T> xs, List<T> ys) {
    for (T y : ys) {
      union(xs, y);
    }
  }
}
