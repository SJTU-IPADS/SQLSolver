package sqlsolver.common.utils;

import java.util.Iterator;
import java.util.function.Predicate;

class FilteredIterator<E> implements Iterator<E> {
  private final Iterator<? extends E> iter;
  private final Predicate<? super E> predicate;
  private E next;
  private boolean hasNext;

  FilteredIterator(Iterator<? extends E> iter, Predicate<? super E> predicate) {
    this.iter = iter;
    this.predicate = predicate;

    forward();
  }

  private void forward() {
    while (iter.hasNext()) {
      final E next = iter.next();
      if (predicate.test(next)) {
        this.next = next;
        this.hasNext = true;
        return;
      }
    }

    this.hasNext = false;
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public E next() {
    final E ret = next;
    forward();
    return ret;
  }
}
