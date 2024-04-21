package sqlsolver.common.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface IterableSupport {
  static <T> T linearFind(Iterable<? extends T> os, Predicate<? super T> pred) {
    for (T o : os) if (pred.test(o)) return o;
    return null;
  }

  static <T> int linearLoc(Iterable<? extends T> os, Predicate<? super T> pred) {
    int i = 0;
    for (T o : os) {
      if (pred.test(o)) return i;
      ++i;
    }
    return -1;
  }

  static <T> Iterable<T> lazyFilter(Iterable<? extends T> os, Predicate<? super T> predicate) {
    return () -> new FilteredIterator<>(os.iterator(), predicate);
  }

  static <T> boolean any(Iterable<? extends T> xs, Predicate<? super T> check) {
    if (xs == null) return false;
    for (T x : xs) if (check.test(x)) return true;
    return false;
  }

  static <T> boolean all(Iterable<? extends T> xs, Predicate<? super T> check) {
    if (xs == null) return true;
    for (T x : xs) if (!check.test(x)) return false;
    return true;
  }

  static <T> boolean none(Iterable<? extends T> xs, Predicate<? super T> check) {
    if (xs == null) return true;
    for (T x : xs) if (check.test(x)) return false;
    return true;
  }

  static int count(Iterable<?> xs) {
    int num = 0;
    for (Object x : xs) ++num;
    return num;
  }

  static <X, Y> Iterable<Pair<X, Y>> zip(Iterable<? extends X> xs, Iterable<? extends Y> ys) {
    return () -> new ZippedIterator<>(xs.iterator(), ys.iterator());
  }

  static <P0, P1> void zip(
      Iterable<? extends P0> l0,
      Iterable<? extends P1> l1,
      BiConsumer<? super P0, ? super P1> func) {
    final Iterator<? extends P0> it0 = l0.iterator();
    final Iterator<? extends P1> it1 = l1.iterator();
    while (it0.hasNext() && it1.hasNext()) func.accept(it0.next(), it1.next());
  }

  static <T> Stream<T> stream(Iterable<T> iterable) {
    if (iterable instanceof Collection) return ((Collection<T>) iterable).stream();
    else return StreamSupport.stream(iterable.spliterator(), false);
  }
}
