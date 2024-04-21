package sqlsolver.common.utils;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Commons {
  static String dumpException(Throwable ex) {
    try (final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(stream)) {
      ex.printStackTrace(out);
      out.flush();
      return stream.toString(Charset.defaultCharset());
    } catch (IOException e) {
      return "cannot dump exception";
    }
  }

  /** if str[0] == '"' and str[-1] == '"', return str[1:-2] */
  static String unquoted(String str) {
    return unquoted(str, '"');
  }

  /** if str[0] == quota and str[-1] == quota, return str[1:-2] */
  static String unquoted(String str, char quota) {
    if (str == null) return null;
    final int length = str.length();
    if (length <= 1) return str;

    final char c0 = str.charAt(0);
    final char ce = str.charAt(length - 1);
    return quota == c0 && quota == ce ? str.substring(1, length - 1) : str;
  }

  /** Helper to make compiler happy. */
  static <T> T assertFalse() {
    assert false;
    return null;
  }

  static StringBuilder trimTrailing(StringBuilder sb, int i) {
    return sb.delete(sb.length() - i, sb.length());
  }

  @Contract("!null, _ -> param1; null, !null -> param2; null, null -> null")
  static <T> T coalesce(T val0, T val1) {
    return val0 != null ? val0 : val1;
  }

  @SafeVarargs
  static <T> T coalesce(T... vals) {
    for (T val : vals) if (val != null) return val;
    return null;
  }

  static <T> T coalesce(T val, Supplier<T> other) {
    return val == null ? other.get() : val;
  }

  static <T> T[] sorted(T[] arr, Comparator<? super T> comparator) {
    Arrays.sort(arr, comparator);
    return arr;
  }

  static int countOccurrences(String str, String target) {
    int index = -1, occurrences = 0;
    while ((index = str.indexOf(target, index + 1)) != -1) {
      ++occurrences;
    }
    return occurrences;
  }

  static int compareStringLengthFirst(String x, String y) {
    if (x.length() < y.length()) return -1;
    if (x.length() > y.length()) return 1;
    return x.compareTo(y);
  }

  static <T> Set<T> newIdentitySet() {
    return Collections.newSetFromMap(new IdentityHashMap<>());
  }

  static <T> Set<T> newIdentitySet(int expectedSize) {
    return Collections.newSetFromMap(new IdentityHashMap<>(expectedSize));
  }

  static <T> Set<T> newIdentitySet(Collection<T> xs) {
    final Set<T> set = Collections.newSetFromMap(new IdentityHashMap<>(xs.size()));
    set.addAll(xs);
    return set;
  }

  static TIntList newIntList(int expectedSize) {
    return new TIntArrayList(expectedSize);
  }

  static boolean isNullOrEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  static String joining(String sep, Iterable<?> objs) {
    return joining(sep, objs, new StringBuilder()).toString();
  }

  static <T> String joining(String sep, Iterable<T> objs, Function<T, String> func) {
    return joining(sep, objs, new StringBuilder(), func).toString();
  }

  static String joining(
      String headOrPrefix, String sep, String tailOrSuffix, boolean asFixture, Iterable<?> objs) {
    return joining(headOrPrefix, sep, tailOrSuffix, asFixture, objs, new StringBuilder())
        .toString();
  }

  static <T> String joining(
      String headOrPrefix,
      String sep,
      String tailOrSuffix,
      boolean asFixture,
      Iterable<T> objs,
      Function<T, String> func) {
    return joining(headOrPrefix, sep, tailOrSuffix, asFixture, objs, new StringBuilder(), func)
        .toString();
  }

  static String joining(
      String head, String prefix, String sep, String suffix, String tail, Iterable<?> objs) {
    return joining(head, prefix, sep, suffix, tail, objs, new StringBuilder()).toString();
  }

  static <T> String joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<T> objs,
      Function<T, String> func) {
    return joining(head, prefix, sep, suffix, tail, objs, new StringBuilder(), func).toString();
  }

  static StringBuilder joining(String sep, Iterable<?> objs, StringBuilder dest) {
    return joining("", "", sep, "", "", objs, dest);
  }

  static <T> StringBuilder joining(
      String sep, Iterable<T> objs, StringBuilder dest, Function<T, String> func) {
    return joining("", "", sep, "", "", objs, dest, func);
  }

  static <T> StringBuilder joining(
      String sep, Iterable<T> objs, StringBuilder dest, BiConsumer<T, StringBuilder> func) {
    return joining("", "", sep, "", "", objs, dest, func);
  }

  static StringBuilder joining(
      String headOrPrefix,
      String sep,
      String tailOrSuffix,
      boolean asFixture,
      Iterable<?> objs,
      StringBuilder dest) {
    return joining(headOrPrefix, sep, tailOrSuffix, asFixture, objs, dest, Objects::toString);
  }

  static <T> StringBuilder joining(
      String headOrPrefix,
      String sep,
      String tailOrSuffix,
      boolean asFixture,
      Iterable<T> objs,
      StringBuilder dest,
      Function<T, String> func) {
    if (asFixture) return joining("", headOrPrefix, sep, tailOrSuffix, "", objs, dest, func);
    else return joining(headOrPrefix, "", sep, "", tailOrSuffix, objs, dest, func);
  }

  static StringBuilder joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<?> objs,
      StringBuilder dest) {
    return joining(head, prefix, sep, suffix, tail, objs, dest, it -> Objects.toString(it));
  }

  static <T> StringBuilder joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<T> objs,
      StringBuilder dest,
      Function<T, String> func) {
    final StringBuilder builder = dest != null ? dest : new StringBuilder();
    builder.append(head);
    boolean isFirst = true;
    for (T obj : objs) {
      if (!isFirst) builder.append(sep);
      isFirst = false;
      builder.append(prefix);
      builder.append(func.apply(obj));
      builder.append(suffix);
    }
    builder.append(tail);
    return builder;
  }

  static <T> StringBuilder joining(
      String head,
      String prefix,
      String sep,
      String suffix,
      String tail,
      Iterable<T> objs,
      StringBuilder dest,
      BiConsumer<T, StringBuilder> func) {
    final StringBuilder builder = dest != null ? dest : new StringBuilder();
    builder.append(head);
    boolean isFirst = true;
    for (T obj : objs) {
      if (!isFirst) builder.append(sep);
      isFirst = false;
      builder.append(prefix);
      func.accept(obj, builder);
      builder.append(suffix);
    }
    builder.append(tail);
    return builder;
  }
}
