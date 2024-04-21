package sqlsolver.common.utils;

import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class LazyMap<K, V> extends DelegatedMap<K, V> implements Map<K, V> {
  private final Map<K, V> map;
  private final Function<K, V> initializer;

  LazyMap(Function<K, V> initializer) {
    this(MapSupport.mkLate(), initializer);
  }

  LazyMap(Map<K, V> map, Function<K, V> initializer) {
    this.map = map;
    this.initializer = requireNonNull(initializer);
  }

  @Override
  protected Map<K, V> delegation() {
    return map;
  }

  @Override
  public V get(Object key) {
    final V existed = super.get(key);
    if (existed != null) return existed;

    final V newValue = initializer.apply((K) key);
    map.put((K) key, newValue);
    return newValue;
  }
}
