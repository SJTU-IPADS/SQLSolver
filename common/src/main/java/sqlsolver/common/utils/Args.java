package sqlsolver.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.ClassUtils.isPrimitiveWrapper;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;

/**
 * Arguments parser.
 */
public class Args {
  private final Map<String, String> named = new LinkedHashMap<>();
  private final List<String> positional = new ArrayList<>();

  public static Args parse(String[] args, int begin) {
    final Args ret = new Args();

    for (int i = begin; i < args.length; ) {
      final String arg = args[i++];

      if (arg.startsWith("-")) {
        final int splitIndex = arg.indexOf('=');
        if (splitIndex == -1) {
          if (i >= args.length) ret.add(arg.substring(1), null);
          else {
            final String next = args[i];
            if (next.startsWith("-")) ret.add(arg.substring(1), null);
            else ret.add(arg.substring(1), args[i++]);
          }

        } else {
          ret.add(arg.substring(1, splitIndex), arg.substring(splitIndex + 1));
        }
      } else {
        ret.add(null, arg);
      }
    }

    return ret;
  }

  public <T> T getRequired(String key, Class<T> cls) {
    final String v = get(key);
    if (v != null) return convertTo(v, cls);
    else if (cls == boolean.class || cls == Boolean.class) {
      return (T) Boolean.valueOf(named.containsKey(key));
    } else {
      throw new IllegalArgumentException("missing required argument: " + key);
    }
  }

  public <T> T getPositional(int index, Class<T> cls) {
    final String value = ListSupport.elemAt(positional, index);
    if (value == null) throw new IllegalArgumentException("missing positional argument");
    return convertTo(value, cls);
  }

  public <T> T getOptional(String key, Class<T> cls, T defaultVal) {
    key = bareKey(key);
    final String value = named.get(key);
    if (value != null) return convertTo(value, cls);
    else if ((cls == boolean.class || cls == Boolean.class) && named.containsKey(key))
      return (T) Boolean.TRUE;
    else return defaultVal;
  }

  public <T> T getOptional(String key0, String key1, Class<T> cls, T defaultVal) {
    final T v0 = getOptional(key0, cls, null);
    if (v0 != null) return v0;
    final T v1 = getOptional(key1, cls, null);
    if (v1 != null) return v1;
    return defaultVal;
  }

  private static <T> T convertTo(String value, Class<T> cls) {
    Class<?> destClass = cls.isPrimitive() ? primitiveToWrapper(cls) : cls;
    if (destClass == String.class) {
      return (T) value;

    } else if (isPrimitiveWrapper(destClass)) {
      final Method convertMethod;
      try {
        convertMethod = destClass.getMethod("valueOf", String.class);
        return (T) convertMethod.invoke(null, value);

      } catch (NoSuchMethodException e) {
        return Commons.assertFalse();

      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }

    } else {
      throw new IllegalArgumentException("unsupported type: " + cls);
    }
  }

  private String bareKey(String key) {
    if (key.startsWith("--")) return key.substring(2);
    if (key.startsWith("-")) return key.substring(1);
    return key;
  }

  private String get(String key) {
    return named.get(bareKey(key));
  }

  private void add(String key, String value) {
    if (key != null) named.put(key, value);
    else positional.add(value);
  }
}
