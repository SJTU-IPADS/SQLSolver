package sqlsolver.common.utils;

import java.util.function.Consumer;

public interface IConsumer<T> extends Consumer<T> {
  default T apply(T t) {
    accept(t);
    return t;
  }

  default Runnable bind(T t) {
    return () -> this.accept(t);
  }
}
