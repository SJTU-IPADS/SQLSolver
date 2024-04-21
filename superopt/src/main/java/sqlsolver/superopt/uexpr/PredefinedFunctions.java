package sqlsolver.superopt.uexpr;

import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;

import java.util.List;
import java.util.function.Function;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.sql.plan.Value.*;
import static sqlsolver.superopt.uexpr.PredefinedFunctions.ValueType.*;

public class PredefinedFunctions {
  public static final String NAME_IS_NULL = UName.FUNC_IS_NULL_NAME;
  public static final String NAME_DIVIDE = "divide";
  public static final String NAME_SQRT = "sqrt";
  public static final String NAME_MINUS = "minus";
  public static final String NAME_UPPER = "upper";
  public static final String NAME_LOWER = "lower";
  public static final String NAME_DATE = "Date";
  public static final String NAME_YEAR = "year";
  public static final String NAME_LIKE = "like";
  public static final String NAME_SUBSTRING = "SUBSTRING";
  public static final String NAME_IN_LIST = "in_list";

  public enum ValueType {
    INT(Context::mkIntSort),
    BOOL(Context::mkBoolSort),
    REAL(Context::mkRealSort),
    STRING(Context::mkStringSort);

    public static ValueType getValueTypeByString(String typeName) {
      switch (typeName) {
        case TYPE_NAT, TYPE_INT, TYPE_BIGINT, TYPE_BOOL -> {
          return ValueType.INT;
        }
        case TYPE_DECIMAL, TYPE_DOUBLE -> {
          return ValueType.REAL;
        }
        case TYPE_CHAR, TYPE_VARCHAR -> {
          return ValueType.STRING;
        }
      }
      throw new UnsupportedOperationException("unknown type " + typeName);
    }

    private final Function<Context, Sort> mkZ3Sort;
    ValueType(Function<Context, Sort> mkZ3Sort) {
      this.mkZ3Sort = mkZ3Sort;
    }
    public Sort getSort(Context ctx) {
      return mkZ3Sort.apply(ctx);
    }
  }

  /**
   * A function family is a collection of functions
   * with a common prefix and different arity.
   */
  public interface FunctionFamily {
    /** Whether the family contains a specific function. */
    boolean contains(String funcName, int arity);
    Sort getReturnSort(Context ctx);
    Sort getArgSort(Context ctx, int index);
  }

  /**
   * A function (family) with a fixed name and arity.
   * In fact such family contains exactly one function.
   */
  public record FixedArityFunctionFamily(String name, int arity, ValueType returnType, ValueType[] argsType) implements FunctionFamily {
    @Override
    public boolean contains(String funcName, int arity) {
      return name.equals(funcName) && this.arity == arity;
    }
    @Override
    public Sort getReturnSort(Context ctx) {
      return returnType.getSort(ctx);
    }
    @Override
    public Sort getArgSort(Context ctx, int index) {
      return argsType[index].getSort(ctx);
    }
  }

  /**
   * A family of functions with a common prefix and a variable arity
   * which has a minimum value.
   */
  public record VariableArityFunctionFamily(String prefix, int minArity) implements FunctionFamily {
    // TODO: support different return value/arg type
    @Override
    public boolean contains(String funcName, int arity) {
      int argCount = PredefinedFunctions.parseArgCount(funcName, prefix);
      return argCount == arity && arity >= minArity;
    }
    @Override
    public Sort getReturnSort(Context ctx) {
      return ctx.mkIntSort();
    }
    @Override
    public Sort getArgSort(Context ctx, int index) {
      return ctx.mkIntSort();
    }
  }

  // TODO: properties of each function (e.g. LIKE returns only 0 or 1);
  //   types can be more precise (e.g. string, decimal)
  public static final FunctionFamily IS_NULL = new FixedArityFunctionFamily(NAME_IS_NULL, 1, INT, new ValueType[]{INT});
  public static final FunctionFamily DIVIDE = new FixedArityFunctionFamily(NAME_DIVIDE, 2, INT, new ValueType[]{INT, INT});
  public static final FunctionFamily SQRT = new FixedArityFunctionFamily(NAME_SQRT, 1, INT, new ValueType[]{INT});
  public static final FunctionFamily MINUS = new FixedArityFunctionFamily(NAME_MINUS, 2, INT, new ValueType[]{INT, INT});
  public static final FunctionFamily UPPER = new FixedArityFunctionFamily(NAME_UPPER, 1, INT, new ValueType[]{INT});
  public static final FunctionFamily LOWER = new FixedArityFunctionFamily(NAME_LOWER, 1, INT, new ValueType[]{INT});
  public static final FunctionFamily DATE = new FixedArityFunctionFamily(NAME_DATE, 1, INT, new ValueType[]{INT});
  public static final FunctionFamily YEAR = new FixedArityFunctionFamily(NAME_YEAR, 1, INT, new ValueType[]{INT});
  public static final FunctionFamily LIKE = new FixedArityFunctionFamily(NAME_LIKE, 2, INT, new ValueType[]{INT, INT});
  public static final FunctionFamily SUBSTRING = new FixedArityFunctionFamily(NAME_SUBSTRING, 3, STRING, new ValueType[]{STRING, INT, INT});
  public static final FunctionFamily IN_LIST = new VariableArityFunctionFamily(NAME_IN_LIST, 2);

  public static final List<FunctionFamily> ALL_FUNCTION_FAMILIES = List.of(
          IS_NULL,
          DIVIDE, SQRT, MINUS, UPPER, LOWER, DATE, YEAR, LIKE, SUBSTRING,
          IN_LIST
  );

  public static FunctionFamily getFunctionFamily(String funcName, int arity) {
    return linearFind(ALL_FUNCTION_FAMILIES, ff -> ff.contains(funcName, arity));
  }

  public static boolean isPredefinedFunction(String funcName, int arity) {
    return any(ALL_FUNCTION_FAMILIES, f -> f.contains(funcName, arity));
  }

  /** Whether a function returns a non-negative integer. */
  public static boolean returnsNonNegativeInt(String funcName, int arity) {
    return YEAR.contains(funcName, arity)
            || LIKE.contains(funcName, arity)
            || IN_LIST.contains(funcName, arity);
  }

  /**
   * Return (familyName + "_" + argCount). "argCount" must be non-negative.
   */
  public static String instantiateFamilyFunc(String familyName, int argCount) {
    if (argCount < 0) throw new IllegalArgumentException("argCount should be non-negative");
    return familyName + "_" + argCount;
  }

  /**
   * Check whether familyFuncName.equals(familyName + "_" + N) for some non-negative integer N.
   * Return N if so, or -1 otherwise.
   */
  public static int parseArgCount(String familyFuncName, String familyName) {
    if (!familyFuncName.startsWith(familyName)) return -1;
    if (familyFuncName.charAt(familyName.length()) == '_') {
      String suffix = familyFuncName.substring(familyName.length() + 1);
      try {
        int argCount = Integer.parseInt(suffix);
        return argCount >= 0 ? argCount : -1;
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }

  /**
   * Check whether a function term in uexp belongs to a function family.
   * That means, the function name is like (familyName + "_" + N) for some non-negative integer N.
   */
  public static boolean belongsToFamily(UFunc func, String familyName) {
    String funcName = func.funcName().toString();
    int size = func.subTerms().size();
    int argCount = PredefinedFunctions.parseArgCount(funcName, familyName);
    return argCount >= 0 && argCount == size;
  }

}
