package sqlsolver.superopt.util;

import java.util.function.Consumer;

/** Hole represents a position can be filled in in some structure. */
public interface Hole<T> {
  boolean fill(T t);

  void unFill();

  /**
   * Build a instance of Hole from a callback.
   *
   * <p>`fill` invokes callback(t) `unfill` invokes callback(null)
   *
   * @param callback a callback used to fill the hole.
   */
  static <T> Hole<T> ofSetter(Consumer<T> callback) {
    return new Hole<>() {
      private boolean filled = false;

      @Override
      public boolean fill(T t) {
        if (filled) return false;
        callback.accept(t);
        filled = true;
        return true;
      }

      @Override
      public void unFill() {
        if (!filled) return;
        callback.accept(null);
        filled = false;
      }
    };
  }
}
