package sqlsolver.common.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class DelegateList<T> implements List<T> {
  protected abstract List<T> delegation();

  @Override
  public int size() {
    return delegation().size();
  }

  @Override
  public boolean isEmpty() {
    return delegation().isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegation().contains(o);
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return delegation().iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return delegation().toArray();
  }

  @NotNull
  @Override
  public <T1> T1[] toArray(@NotNull T1[] a) {
    return delegation().toArray(a);
  }

  @Override
  public boolean add(T t) {
    return delegation().add(t);
  }

  @Override
  public boolean remove(Object o) {
    return delegation().remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return delegation().containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends T> c) {
    return delegation().addAll(c);
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends T> c) {
    return delegation().addAll(index, c);
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    return delegation().removeAll(c);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return delegation().retainAll(c);
  }

  @Override
  public void clear() {
    delegation().clear();
  }

  @Override
  public T get(int index) {
    return delegation().get(index);
  }

  @Override
  public T set(int index, T element) {
    return delegation().set(index, element);
  }

  @Override
  public void add(int index, T element) {
    delegation().add(index, element);
  }

  @Override
  public T remove(int index) {
    return delegation().remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return delegation().indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return delegation().lastIndexOf(o);
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator() {
    return delegation().listIterator();
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator(int index) {
    return delegation().listIterator(index);
  }

  @NotNull
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return delegation().subList(fromIndex, toIndex);
  }

  @Override
  public boolean equals(Object obj) {
    return delegation().equals(obj);
  }

  @Override
  public int hashCode() {
    return delegation().hashCode();
  }

  @Override
  public String toString() {
    return delegation().toString();
  }
}
