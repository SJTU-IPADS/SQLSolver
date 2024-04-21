package sqlsolver.common.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ISupplier<R> extends Supplier<R> {
  default <T> ISupplier<T> andThen(Function<R, T> func) {
    return () -> func.apply(get());
  }

  default List<R> repeat(int n) {
    final List<R> xs = new ArrayList<>(n);
    for (int i = 0; i < n; i++) xs.add(get());
    return xs;
  }

  @SuppressWarnings("unchecked")
  default R[] repeat(int n, Class<R> cls) {
    final R[] xs = (R[]) Array.newInstance(cls, n);
    for (int i = 0; i < n; i++) xs[i] = get();
    return xs;
  }
}
