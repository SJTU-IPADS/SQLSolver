package sqlsolver.common.utils;

public class IntMetric implements Metric<Integer, IntMetric> {
  private final String name;
  private final int initValue;
  private int value;

  public IntMetric(String name) {
    this(name, 0);
  }

  public IntMetric(String name, int initValue) {
    this.name = name;
    this.initValue = initValue;
  }

  public void set(int value) {
    this.value = value;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Integer value() {
    return value;
  }

  public void increment() {
    ++value;
  }

  public boolean incrementIf(boolean b) {
    if (b) ++value;
    return b;
  }

  @Override
  public void reset() {
    value = initValue;
  }

  @Override
  public void assign(IntMetric other) {
    value = other.value;
  }

  @Override
  public void accumulate(IntMetric other) {
    value += other.value;
  }

  @Override
  public String toString() {
    return name + "=" + value;
  }
}
