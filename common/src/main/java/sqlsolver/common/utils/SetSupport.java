package sqlsolver.common.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface SetSupport {
  static <T> Set<T> filter(Iterable<T> os, Predicate<? super T> func) {
    return IterableSupport.stream(os).filter(func).collect(Collectors.toSet());
  }

  static <X, Y> Set<Y> map(Collection<X> xs, Function<? super X, ? extends Y> func) {
    final Set<Y> ys = new HashSet<>(xs.size());
    for (X x : xs) ys.add(func.apply(x));
    return ys;
  }

  static <X, Y> Set<Y> map(Iterable<X> xs, Function<? super X, ? extends Y> func) {
    if (xs instanceof Collection) return map((Collection<X>) xs, func);
    return StreamSupport.stream(xs.spliterator(), false).map(func).collect(Collectors.toSet());
  }

  static <T, R> Set<R> flatMap(Iterable<T> os, Function<? super T, ? extends Iterable<R>> func) {
    return IterableSupport.stream(os).map(func).flatMap(IterableSupport::stream).collect(Collectors.toSet());
  }

  static <T> Set<T> minus(Set<T> s1, Set<T> s2) {
    Set<T> copy = new HashSet<>(s1);
    copy.removeAll(s2);
    return copy;
  }

  static <T> Set<T> intersect(Set<T> s1, Set<T> s2) {
    Set<T> copy = new HashSet<>(s1);
    copy.retainAll(s2);
    return copy;
  }

  static <T> boolean intersects(Set<T> s1, Set<T> s2) {
    return !intersect(s1, s2).isEmpty();
  }

  static <T> boolean isSubSet(Set<T> s1, Set<T> s2) {
    return minus(s1, s2).isEmpty();
  }

  static <T> Set<T> union(Set<T> s1, Set<T> s2) {
    Set<T> result = new HashSet<>(s1);
    result.addAll(s2);
    return result;
  }
}
