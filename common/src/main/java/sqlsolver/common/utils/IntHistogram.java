package sqlsolver.common.utils;

import java.util.concurrent.ThreadLocalRandom;

public interface IntHistogram {
  int numRanges();

  int numSamples();

  int max();

  int min();

  int addSample(int value);

  void addToRange(int rangeIndex, int increment);

  int populationAt(int rangeIndex);

  int beginOfRange(int rangeIndex);

  int endOfRange(int rangeIndex);

  int findCoveringRange(int fromInclusive, int toExclusive);

  void reset();

  IntHistogram copy();

  default IntHistogram merge(IntHistogram other) {
    for (int i = 0, bound = other.numRanges(); i < bound; ++i) {
      if (other.populationAt(i) == 0) continue;
      final int rangeIdx = findCoveringRange(other.beginOfRange(i), other.endOfRange(i));
      if (rangeIdx == -1) {
        findCoveringRange(other.beginOfRange(i), other.endOfRange(i));
        throw new IllegalArgumentException("unaligned histogram cannot be merged");
      }
      addToRange(rangeIdx, other.populationAt(i));
    }
    return this;
  }

  default double estimatedPercentile(double percentile) {
    if (percentile < 0.0 || percentile > 1.0) throw new IllegalArgumentException();

    int count = (int) (percentile * numSamples());
    for (int i = 0, bound = numRanges(); i < bound; ++i) {
      final int population = populationAt(i);
      if (count <= population) {
        final int begin = beginOfRange(i), end = endOfRange(i);
        return begin + (count / (double) population) * (end - begin);
      }
      count -= population;
    }

    return 0;
  }

  static IntHistogram mkFixed(int fromInclusive, int toExclusive, int step) {
    return FixedStepFixedRangeIntHistogram.mk(fromInclusive, toExclusive, step);
  }

  static IntHistogram mkDynamic(int fromInclusive, int step) {
    return FixedStepDynamicRangeIntHistogram.mk(fromInclusive, step);
  }

  static void main(String[] args) {
    final IntHistogram hist0 = IntHistogram.mkFixed(0, 100, 30);
    final IntHistogram hist1 = IntHistogram.mkDynamic(0, 10);
    final IntStatistic stat = IntStatistic.mk();
    final ThreadLocalRandom r = ThreadLocalRandom.current();

    for (int i = 0; i < 10000; ++i) hist0.addSample(r.nextInt(99) + 1);
    for (int i = 0; i < 10000; ++i) hist1.addSample(r.nextInt(99) + 1);
    for (int i = 0; i < 100000; ++i) stat.addSample(r.nextInt(99) + 1);

    System.out.println(hist0);
    System.out.println(hist1);
    System.out.println(hist0.merge(hist1));
    System.out.println(stat);
  }
}
