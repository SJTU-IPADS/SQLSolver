package sqlsolver.common.utils;

public interface ITriConsumer<P0, P1, P2> {
  void accept(P0 p0, P1 p1, P2 p2);

  default IBiConsumer<P1, P2> bind0(P0 p0) {
    return (p1, p2) -> this.accept(p0, p1, p2);
  }

  default IBiConsumer<P0, P2> bind1(P1 p1) {
    return (p0, p2) -> this.accept(p0, p1, p2);
  }

  default IBiConsumer<P0, P1> bind2(P2 p2) {
    return (p0, p1) -> this.accept(p0, p1, p2);
  }
}
