package sqlsolver.common.utils;

import java.util.AbstractList;
import java.util.List;

public class BinaryJoinedList<E> extends AbstractList<E> {
  private final List<? extends E> left, right;

  public BinaryJoinedList(List<? extends E> left, List<? extends E> right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public E get(int index) {
    final int boundary = left.size();
    if (index < boundary) return left.get(index);
    else return right.get(index - boundary);
  }

  @Override
  public int size() {
    return left.size() + right.size();
  }
}
