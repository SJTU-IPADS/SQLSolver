package sqlsolver.common.utils;

import java.util.List;

public interface Metrics<T extends Metrics<T>> {
  List<Metric> metrics();

  default void reset() {
    for (Metric metric : metrics()) metric.reset();
  }

  default void accumulate(T other) {
    final List<Metric> metrics = metrics();
    final List<Metric> otherMetrics = other.metrics();
    assert metrics.size() == otherMetrics.size();
    for (int i = 0, bound = metrics.size(); i < bound; i++) {
      metrics.get(i).accumulate(otherMetrics.get(i));
    }
  }

  default void assign(T other) {
    final List<Metric> metrics = metrics();
    final List<Metric> otherMetrics = other.metrics();
    assert metrics.size() == otherMetrics.size();
    for (int i = 0, bound = metrics.size(); i < bound; i++) {
      metrics.get(i).assign(otherMetrics.get(i));
    }
  }

  default StringBuilder stringify(StringBuilder builder) {
    return Commons.joining("\n", metrics(), builder);
  }
}
