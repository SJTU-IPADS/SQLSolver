package sqlsolver.common.utils;

public class IntStatistic {
  private int numSamples, min, max;
  private double avg, powAvg;
  private double p50, p90;

  private IntStatistic() {
    reset();
  }

  public static IntStatistic mk() {
    return new IntStatistic();
  }

  public void addSample(int value) {
    ++numSamples;
    if (value > max) max = value;
    if (value < min) min = value;

    avg = (avg * (numSamples - 1) + value) / numSamples;
    powAvg = (powAvg * (numSamples - 1) + (value * value)) / numSamples;

    if (Double.isNaN(p50)) p50 = value;
    else p50 = estimatePercentile(p50, value, 0.5);
    if (Double.isNaN(p90)) p90 = value;
    else p90 = estimatePercentile(p90, value, 0.9);
  }

  public int numSamples() {
    return numSamples;
  }

  public int max() {
    return max;
  }

  public int min() {
    return min;
  }

  public double estimatedStdDev() {
    return Math.sqrt(powAvg - (avg * avg));
  }

  public double estimatedP50() {
    return p50;
  }

  public double estimatedP90() {
    return p90;
  }

  public void reset() {
    numSamples = 0;
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
    avg = 0.0;
    p50 = Double.NaN;
    p90 = Double.NaN;
  }

  private double estimatePercentile(double currentEst, int newObservation, double percentile) {
    if (currentEst == newObservation) return currentEst;
    if (newObservation < currentEst) return currentEst - (estimatedStdDev() / percentile) * 0.001;
    return currentEst + (estimatedStdDev() / (1 - percentile)) * 0.001;
  }

  @Override
  public String toString() {
    if (numSamples == 0) return "<No Statistics>";

    final StringBuilder builder = new StringBuilder();
    builder.append("#Samples: ").append(numSamples).append('\n');
    builder.append("Range:\t[").append(min).append(',').append(max).append("]\n");
    builder.append("Avg:\t").append(avg).append('\n');
    builder.append("StdDev:\t").append(estimatedStdDev()).append('\n');
    builder.append("P50:\t").append(estimatedP50()).append('\n');
    builder.append("P90:\t").append(estimatedP90()).append('\n');
    return builder.toString();
  }
}
