package sqlsolver.common.utils;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

class FixedStepDynamicRangeIntHistogram implements IntHistogram {
  private final int step, from;
  private final TIntList indicesMap;
  private final TIntList counts;
  private int min, max, numSamples;

  private FixedStepDynamicRangeIntHistogram(int from, int step) {
    this.step = step;
    this.from = from;
    indicesMap = new TIntArrayList();
    counts = new TIntArrayList();
    reset();
  }

  private FixedStepDynamicRangeIntHistogram(
      int step, int from, TIntList indices, TIntList numbers) {
    this.step = step;
    this.from = from;
    this.indicesMap = indices;
    this.counts = numbers;
    reset();
  }

  static IntHistogram mk(int from, int step) {
    if (step <= 0) throw new IllegalArgumentException();
    return new FixedStepDynamicRangeIntHistogram(from, step);
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
  public int numRanges() {
    if (indicesMap.isEmpty()) return 0;
    return indicesMap.get(indicesMap.size() - 1) + 1;
  }

  @Override
  public int beginOfRange(int rangeIndex) {
    return rangeIndex == 0 ? Integer.MIN_VALUE : from + (rangeIndex - 1) * step;
  }

  @Override
  public int endOfRange(int rangeIndex) {
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
    final int logicIdx = rangeIndexOf(value);
    final int physIdx = getOrCreatePhysicalRange(logicIdx);
    counts.set(physIdx, counts.get(physIdx) + 1);
    ++numSamples;
    if (value > max) max = value;
    else if (value < min) min = value;
    return logicIdx;
  }

  @Override
  public void addToRange(int rangeIndex, int increment) {
    final int physIdx = getOrCreatePhysicalRange(rangeIndex);
    counts.set(physIdx, counts.get(physIdx) + increment);
  }

  @Override
  public int populationAt(int rangeIndex) {
    final int physIdx = getPhysicalRange(rangeIndex);
    if (physIdx < 0) return 0;
    else return counts.get(physIdx);
  }

  @Override
  public void reset() {
    numSamples = 0;
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
    indicesMap.clear();
    counts.clear();
  }

  @Override
  public IntHistogram merge(IntHistogram other) {
    IntHistogram.super.merge(other);
    numSamples += other.numSamples();
    min = Integer.min(min, other.min());
    max = Integer.max(max, other.max());
    return this;
  }

  @Override
  public IntHistogram copy() {
    return new FixedStepDynamicRangeIntHistogram(
        step, from, new TIntArrayList(indicesMap), new TIntArrayList(counts));
  }

  private int rangeIndexOf(int value) {
    if (value < from) return 0;
    return (value - from) / step + 1;
  }

  private int getPhysicalRange(int logicRangeIndex) {
    return indicesMap.binarySearch(logicRangeIndex);
  }

  private int getOrCreatePhysicalRange(int logicRangeIndex) {
    int physIdx = indicesMap.binarySearch(logicRangeIndex);
    if (physIdx < 0) {
      physIdx = -physIdx - 1;
      indicesMap.insert(physIdx, logicRangeIndex);
      counts.insert(physIdx, 0);
    }
    return physIdx;
  }

  @Override
  public String toString() {
    if (indicesMap.isEmpty()) {
      return "#Samples=0\n(-\u221E,%d):\t0\n(%d,+\u221E):\t0\n".formatted(from, from);
    }

    final StringBuilder builder = new StringBuilder();

    builder.append("#Samples=").append(numSamples).append(',');
    builder.append("Range=").append('[').append(min).append(',').append(max).append(']');
    builder.append('\n');

    int expectedLogicIdx = 0;
    for (int i = 0, bound = indicesMap.size(); i < bound; ++i) {
      final int logicIndex = indicesMap.get(i);
      if (logicIndex != expectedLogicIdx) appendAbsentRange(expectedLogicIdx, logicIndex, builder);
      appendPresentRange(i, builder);
      expectedLogicIdx = logicIndex + 1;
    }

    builder.append('\t').append('[').append(beginOfRange(expectedLogicIdx)).append(',');
    builder.append("+\u221E):\t0");
    return builder.toString();
  }

  private void appendPresentRange(int physRangeIndex, StringBuilder builder) {
    final int logicRangeIndex = indicesMap.get(physRangeIndex);
    builder.append('\t');
    if (logicRangeIndex == 0) {
      builder.append('(').append(step >= 0 ? '-' : '+').append("\u221E,");
      builder.append(from).append("):\t").append(counts.get(physRangeIndex));
    } else {
      builder.append('[').append(beginOfRange(logicRangeIndex)).append(',');
      builder.append(endOfRange(logicRangeIndex)).append("):\t").append(counts.get(physRangeIndex));
    }
    builder.append('\n');
  }

  private void appendAbsentRange(int fromLogicIdx, int toLogicIdx, StringBuilder builder) {
    assert toLogicIdx > fromLogicIdx;
    builder.append('\t');
    if (fromLogicIdx == 0) builder.append('(').append(step >= 0 ? '-' : '+').append("\u221E,");
    else builder.append('[').append(beginOfRange(fromLogicIdx)).append(',');
    builder.append(beginOfRange(toLogicIdx)).append("):\t0\n");
  }
}
