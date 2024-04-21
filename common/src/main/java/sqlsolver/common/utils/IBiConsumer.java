package sqlsolver.common.utils;

import java.util.function.BiConsumer;

public interface IBiConsumer<P0, P1> extends BiConsumer<P0, P1> {
  default IConsumer<P1> bind0(P0 p0) {
    return p1 -> this.accept(p0, p1);
  }

  default IConsumer<P0> bind1(P1 p1) {
    return p0 -> this.accept(p0, p1);
  }
}
