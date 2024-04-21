package sqlsolver.common.utils;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface IBiFunction<P1, P2, R> extends BiFunction<P1, P2, R> {
  default BiConsumer<P1, P2> then(Consumer<R> c) {
    return (p1, p2) -> c.accept(apply(p1, p2));
  }

  default IFunction<P2, R> bind0(P1 p1) {
    return p2 -> apply(p1, p2);
  }

  default IFunction<P1, R> bind1(P2 p2) {
    return p1 -> apply(p1, p2);
  }
}
