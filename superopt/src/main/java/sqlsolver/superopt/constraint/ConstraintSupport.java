package sqlsolver.superopt.constraint;

import sqlsolver.superopt.fragment.SymbolNaming;

public interface ConstraintSupport {
  int ENUM_FLAG_DRY_RUN = 1;
  int ENUM_FLAG_DISABLE_BREAKER_0 = 2 | ENUM_FLAG_DRY_RUN;
  int ENUM_FLAG_DISABLE_BREAKER_1 = 4 | ENUM_FLAG_DRY_RUN;
  int ENUM_FLAG_DISABLE_BREAKER_2 = 8 | ENUM_FLAG_DRY_RUN;
  int ENUM_FLAG_VERBOSE = 16;
  int ENUM_FLAG_USE_SPES = 32;
  int ENUM_FLAG_SINGLE_DIRECTION = 64;
  int ENUM_FLAG_DUMP = ENUM_FLAG_SINGLE_DIRECTION | ENUM_FLAG_VERBOSE;

  static boolean isVerbose(int tweak) {
    return (tweak & ENUM_FLAG_VERBOSE) == ENUM_FLAG_VERBOSE;
  }

  static StringBuilder stringify(
      Constraint c, SymbolNaming naming, boolean canonical, StringBuilder builder) {
    return new ConstraintStringifier(naming, canonical, builder).stringify(c);
  }

  static StringBuilder stringify(
      Constraints C, SymbolNaming naming, boolean canonical, StringBuilder builder) {
    return new ConstraintStringifier(naming, canonical, builder).stringify(C);
  }
}
