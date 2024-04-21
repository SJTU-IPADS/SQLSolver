package sqlsolver.common.utils;

public interface Metric<V, M extends Metric<V, M>> {
  String name();

  V value();

  void reset();

  void assign(M other);

  void accumulate(M other);
}
