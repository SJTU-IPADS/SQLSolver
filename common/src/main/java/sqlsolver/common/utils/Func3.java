package sqlsolver.common.utils;

@FunctionalInterface
public interface Func3<R, P1, P2, P3> {
  R apply(P1 p1, P2 p2, P3 p3);
}
