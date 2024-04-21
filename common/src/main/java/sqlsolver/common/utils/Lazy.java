package sqlsolver.common.utils;

import java.util.function.Supplier;

public final class Lazy<T> {
  private final Supplier<T> supplier;
  private T val;

  private Lazy(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  private Lazy(T val) {
    this.supplier = null;
    this.val = val;
  }

  public static <T> Lazy<T> mk(Supplier<T> supplier) {
    return new Lazy<>(supplier);
  }

  public static <T> Lazy<T> mk(T val) {
    return new Lazy<>(val);
  }

  public T get() {
    if (val == null) val = supplier.get();
    return val;
  }

  public void set(T val) {
    this.val = val;
  }

  public boolean isInitialized() {
    return val != null;
  }
}
