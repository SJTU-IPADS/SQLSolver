package sqlsolver.common.utils;

import java.util.concurrent.atomic.AtomicReference;

public abstract class MetricsContextBase<T extends Metrics<T>> implements MetricsContext<T> {
  private final String name;
  private final ThreadLocal<T> localMetrics;
  private final AtomicReference<T> globalMetric;

  protected MetricsContextBase(String name) {
    this.name = name;
    this.localMetrics = ThreadLocal.withInitial(this::newMetric);
    this.globalMetric = new AtomicReference<>(newMetric());
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public T local(boolean reset) {
    final T local = localMetrics.get();
    if (reset) local.reset();
    return local;
  }

  @Override
  public T global() {
    return globalMetric.get();
  }

  @Override
  public void updateGlobal() {
    final T local = local(false);
    T updated = newMetric(), global;
    do {
      global = globalMetric.get();
      updated.assign(local);
      updated.accumulate(global);
    } while (!globalMetric.weakCompareAndSetVolatile(global, updated));
  }

  protected abstract T newMetric();
}
