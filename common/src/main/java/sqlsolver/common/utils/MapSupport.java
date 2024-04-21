package sqlsolver.common.utils;

import java.util.Map;
import java.util.function.Function;

public interface MapSupport {
  static <K, V> Map<K, V> mkLate() {
    return new LateMap<>();
  }

  static <K, V> Map<K, V> mkLazy(Function<K, V> initializer) {
    return new LazyMap<>(initializer);
  }
}
