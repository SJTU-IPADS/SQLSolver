package sqlsolver.common.utils;

import java.util.function.Function;

public final class COW<T> {
  private T obj;
  private Function<T, T> copier;

  public COW(T obj, Function<T, T> copier) {
    this.obj = obj;
    this.copier = copier;
  }

  public T forRead() {
    return obj;
  }

  public T forWrite() {
    if (copier != null) {
      obj = copier.apply(obj);
      copier = null;
    }
    return obj;
  }
}
