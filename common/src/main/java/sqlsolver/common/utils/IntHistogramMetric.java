package sqlsolver.common.utils;

public class IntHistogramMetric implements Metric<IntHistogram, IntHistogramMetric> {
  private final String name;
  private IntHistogram histogram;

  public IntHistogramMetric(String name, int fromInclusive, int toExclusive, int step) {
    this.name = name;
    this.histogram = IntHistogram.mkFixed(fromInclusive, toExclusive, step);
  }

  public IntHistogramMetric(String name, int fromInclusive, int step) {
    this.name = name;
    this.histogram = IntHistogram.mkDynamic(fromInclusive, step);
  }

  public void addSample(int sample) {
    histogram.addSample(sample);
  }

  public IntHistogram histogram() {
    return histogram;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public IntHistogram value() {
    return histogram;
  }

  @Override
  public void reset() {
    histogram.reset();
  }

  @Override
  public void assign(IntHistogramMetric other) {
    this.histogram = other.histogram.copy();
  }

  @Override
  public void accumulate(IntHistogramMetric other) {
    histogram.merge(other.histogram);
  }
}
