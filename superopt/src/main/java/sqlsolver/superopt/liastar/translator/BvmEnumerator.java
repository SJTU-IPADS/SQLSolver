package sqlsolver.superopt.liastar.translator;

import sqlsolver.superopt.uexpr.UTerm;

/**
 * A BvmEnumerator enumerates possible bound var matching in some order. "Bvm" stands for "bound var
 * matching". Implementation and input decide the enumeration order.
 */
public abstract class BvmEnumerator {
  private final UTerm uexp1, uexp2;
  private BVM cachedNext;
  private final int limit;
  // how many BVMs have been returned by "next" (from an external perspective)
  // seeNext does not change this
  private int count;

  /**
   * Initialize the enumerator with the U-expressions to match and parameters. Note that if
   * implementation of <code>next</code> does not enumerate all possible matching, <code>limit
   * </code> should be set positive.
   *
   * @param uexp1 the first U-expression
   * @param uexp2 the second U-expression
   * @param limit the allowed maximum number of invoking "next", or a non-positive number to allow
   *     arbitrarily many invocations
   */
  protected BvmEnumerator(UTerm uexp1, UTerm uexp2, int limit) {
    this.uexp1 = uexp1;
    this.uexp2 = uexp2;
    cachedNext = null;
    count = 0;
    this.limit = limit;
  }

  /**
   * Given two U-expressions, get the next BVM. Its implementation may update both U-expressions.
   */
  protected abstract BVM next(UTerm uexp1, UTerm uexp2);

  /**
   * Get the next BVM. The allowed maximum number of invoking this method is <code>limit</code>,
   * which is set during initialization.
   *
   * @return the next BVM if there is one and the number of invocations does not exceed the limit;
   *     otherwise <code>null</code>
   */
  public BVM next() {
    // see the cached "next", or fetch "next" if it is not cached
    final BVM result = seeNext();
    // clear cache and update counter
    cachedNext = null;
    count++;
    return result;
  }

  /**
   * Get the next BVM. The allowed maximum number of invoking this method is <code>limit</code>,
   * which is set during initialization. Note that this method is idempotent (i.e. it does not "take
   * away" the next BVM).
   *
   * @return the next BVM if there is one and the number of invocations does not exceed the limit;
   *     otherwise <code>null</code>
   */
  public BVM seeNext() {
    return seeNext(limit);
  }

  /**
   * Whether the enumerator has next BVM without the preset limit.
   *
   * @return whether "next" BVM is non-null if limit is set to zero
   */
  public boolean hasNextBeyondLimit() {
    return seeNext(0) != null;
  }

  /**
   * "Next" BVMs are always fetched via this method.
   *
   * @param limit restrict visibility of "next"; only first <code>limit</code> BVMs are visible;
   *     non-positive <code>limit</code> poses no restriction
   */
  private BVM seeNext(int limit) {
    if (limit > 0 && count >= limit) {
      return null;
    }
    if (cachedNext == null) {
      // fetch "next" when cachedNext is null; it is correct in both cases
      // case 1: "next" is not cached, just fetch and cache it
      // case 2: "next" is already null, and fetching it always returns null
      cachedNext = next(uexp1.copy(), uexp2.copy());
    }
    return cachedNext;
  }
}
