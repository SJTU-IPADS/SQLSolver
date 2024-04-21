package sqlsolver.common.utils;

public class LongMetric implements Metric<Long, LongMetric> {
  private final String name;
  private final long initValue;
  private long value;

  public LongMetric(String name) {
    this(name, 0);
  }

  public LongMetric(String name, long initValue) {
    this.name = name;
    this.initValue = initValue;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Long value() {
    return value;
  }

  public void add(long increment) {
    value += increment;
  }

  public Timer timeIt() {
    return new Timer();
  }

  @Override
  public void reset() {
    value = initValue;
  }

  @Override
  public void assign(LongMetric other) {
    value = other.value;
  }

  @Override
  public void accumulate(LongMetric other) {
    value += other.value;
  }

  @Override
  public String toString() {
    return name + "=" + value;
  }

  public class Timer implements AutoCloseable {
    private final long begin = System.currentTimeMillis();

    @Override
    public void close() {
      add(System.currentTimeMillis() - begin);
    }
  }
}
