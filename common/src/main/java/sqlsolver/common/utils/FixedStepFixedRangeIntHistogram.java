package sqlsolver.common.utils;

import java.util.Arrays;

class FixedStepFixedRangeIntHistogram implements IntHistogram {
  private final int from, to, step;
  private final int[] ranges;
  private int min, max, numSamples;

  private FixedStepFixedRangeIntHistogram(int from, int to, int step) {
    assert step != 0 || from == to;
    final long span = (long) to - from;
    final int numRanges =
        (int) ((step == 0 ? 0 : span / step) + 2) + Integer.min((int) span % step, 1);

    this.from = from;
    this.to = to;
    this.step = step;
    this.ranges = new int[numRanges];
    reset();
  }

  public static IntHistogram mk(int fromInclusive, int toExclusive, int step) {
    if (fromInclusive > toExclusive || step < 0 || (step == 0 && fromInclusive != toExclusive))
      throw new IllegalArgumentException(
          "invalid step. from=%d, to=%d, step=%d".formatted(fromInclusive, toExclusive, step));

    return new FixedStepFixedRangeIntHistogram(fromInclusive, toExclusive, step);
  }

  @Override
  public int numRanges() {
    return ranges.length;
  }

  @Override
  public int min() {
    return min;
  }

  @Override
  public int max() {
    return max;
  }

  @Override
  public int numSamples() {
    return numSamples;
  }

  @Override
  public int beginOfRange(int rangeIndex) {
    return rangeIndex == 0 ? Integer.MIN_VALUE : from + (rangeIndex - 1) * step;
  }

  @Override
  public int endOfRange(int rangeIndex) {
    if (rangeIndex == ranges.length - 1) return Integer.MIN_VALUE;
    if (rangeIndex == ranges.length - 2) return to;
    return from + rangeIndex * step;
  }

  @Override
  public int findCoveringRange(int fromInclusive, int toExclusive) {
    final int rangeIdxOfFrom = rangeIndexOf(fromInclusive);
    final int rangeIdxOfTo = rangeIndexOf(toExclusive - 1);
    if (rangeIdxOfTo != rangeIdxOfFrom) return -1;
    else return rangeIdxOfTo;
  }

  @Override
  public int addSample(int value) {
    final int index = rangeIndexOf(value);
    ++ranges[index];
    ++numSamples;
    if (value > max) max = value;
    else if (value < min) min = value;

    return index;
  }

  @Override
  public void addToRange(int rangeIndex, int increment) {
    ranges[rangeIndex] += increment;
  }

  @Override
  public int populationAt(int rangeIndex) {
    return ranges[rangeIndex];
  }

  @Override
  public void reset() {
    numSamples = 0;
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
    Arrays.fill(ranges, 0);
  }

  @Override
  public IntHistogram copy() {
    final FixedStepFixedRangeIntHistogram other =
        new FixedStepFixedRangeIntHistogram(from, to, step);
    System.arraycopy(ranges, 0, other.ranges, 0, ranges.length);
    return other;
  }

  @Override
  public IntHistogram merge(IntHistogram other) {
    IntHistogram.super.merge(other);
    numSamples += other.numSamples();
    min = Integer.min(min, other.min());
    max = Integer.max(max, other.max());
    return this;
  }

  private int rangeIndexOf(int value) {
    if (value >= to) return ranges.length - 1;
    if (value < from) return 0;
    return (value - from) / step + 1;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("#Samples=").append(numSamples).append(',');
    builder.append("Range=").append('[').append(min).append(',').append(max).append(']');
    builder.append('\n');
    for (int i = 0, bound = ranges.length; i < bound; ++i) {
      appendRange(i, builder);
      builder.append('\n');
    }
    builder.deleteCharAt(builder.length() - 1);
    return builder.toString();
  }

  private void appendRange(int rangeIndex, StringBuilder builder) {
    builder.append('\t');
    if (rangeIndex == 0) builder.append('(').append("-\u221E,");
    else builder.append('[').append(beginOfRange(rangeIndex)).append(',');

    if (rangeIndex == ranges.length - 1) builder.append("+\u221E)");
    else builder.append(endOfRange(rangeIndex)).append(')');

    builder.append(":\t").append(ranges[rangeIndex]);
  }
}
