package sqlsolver.common.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static sqlsolver.common.utils.Commons.coalesce;

class LateMap<K, V> implements Map<K, V> {
  private Map<K, V> map;

  private Map<K, V> getOrEmpty() {
    return coalesce(map, emptyMap());
  }

  private Map<K, V> getOrCreate() {
    if (map == null) map = new HashMap<>();
    return map;
  }

  @Override
  public int size() {
    return getOrEmpty().size();
  }

  @Override
  public boolean isEmpty() {
    return getOrEmpty().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return getOrEmpty().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return getOrEmpty().containsValue(value);
  }

  @Override
  public V get(Object key) {
    return getOrEmpty().get(key);
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    return getOrCreate().put(key, value);
  }

  @Override
  public V remove(Object key) {
    return getOrEmpty().remove(key);
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    getOrCreate().putAll(m);
  }

  @Override
  public void clear() {
    getOrEmpty().clear();
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return getOrEmpty().keySet();
  }

  @NotNull
  @Override
  public Collection<V> values() {
    return getOrEmpty().values();
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return getOrEmpty().entrySet();
  }
}
