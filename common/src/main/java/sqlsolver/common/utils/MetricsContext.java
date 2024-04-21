package sqlsolver.common.utils;

public interface MetricsContext<T extends Metrics<T>> {
  String name();

  T local(boolean reset);

  T global();

  void updateGlobal();
}
