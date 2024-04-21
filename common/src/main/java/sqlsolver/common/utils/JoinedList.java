package sqlsolver.common.utils;

import java.util.AbstractList;
import java.util.List;

/**
 * A view of concatenated lists. If underlying list is modified, `get` and `size` will reflect the
 * modification accordingly, while iterator becomes undefined.
 */
class JoinedList<E> extends AbstractList<E> {
  private final List<List<? extends E>> lists;

  JoinedList(List<List<? extends E>> lists) {
    this.lists = lists;
  }

  @Override
  public E get(int index) {
    for (List<? extends E> list : lists) {
      if (index >= list.size()) index -= list.size();
      else return list.get(index);
    }
    throw new IndexOutOfBoundsException(index);
  }

  @Override
  public int size() {
    return lists.stream().mapToInt(List::size).sum();
  }
}
