package sqlsolver.superopt.constraint;

import sqlsolver.common.utils.MetricsContextBase;

class EnumerationMetricsContext extends MetricsContextBase<EnumerationMetrics> {
  private static final EnumerationMetricsContext INSTANCE = new EnumerationMetricsContext();

  private EnumerationMetricsContext() {
    super("EnumerationMetric");
  }

  static EnumerationMetricsContext instance() {
    return INSTANCE;
  }

  @Override
  protected EnumerationMetrics newMetric() {
    return new EnumerationMetrics();
  }
}
