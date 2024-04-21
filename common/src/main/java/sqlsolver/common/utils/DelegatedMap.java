package sqlsolver.common.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class DelegatedMap<K, V> implements Map<K, V> {
  protected abstract Map<K, V> delegation();

  @Override
  public int size() {
    return delegation().size();
  }

  @Override
  public boolean isEmpty() {
    return delegation().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return delegation().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return delegation().containsValue(value);
  }

  @Override
  public V get(Object key) {
    return delegation().get(key);
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    return delegation().put(key, value);
  }

  @Override
  public V remove(Object key) {
    return delegation().remove(key);
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    delegation().putAll(m);
  }

  @Override
  public void clear() {
    delegation().clear();
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return delegation().keySet();
  }

  @NotNull
  @Override
  public Collection<V> values() {
    return delegation().values();
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return delegation().entrySet();
  }
}
