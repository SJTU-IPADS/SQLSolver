package sqlsolver.common.utils;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface FuncSupport {
  static <T, R> IFunction<T, R> deaf(Supplier<R> supplier) {
    return t -> supplier.get();
  }

  static <T> IConsumer<T> dumb(Function<T, ?> func) {
    return func::apply;
  }

  static <P> IConsumer<P> consumer(IConsumer<P> consumer) {
    return consumer;
  }

  static <P0, P1> IBiConsumer<P0, P1> consumer2(IBiConsumer<P0, P1> consumer) {
    return consumer;
  }

  static <P, R> IFunction<P, R> func(IFunction<P, R> func) {
    return func;
  }

  static <P0, P1, R> IBiFunction<P0, P1, R> func2(IBiFunction<P0, P1, R> func2) {
    return func2;
  }

  static <R> ISupplier<R> supplier(ISupplier<R> supplier) {
    return supplier;
  }

  static <P> Predicate<P> pred(Predicate<P> pred) {
    return pred;
  }
}
