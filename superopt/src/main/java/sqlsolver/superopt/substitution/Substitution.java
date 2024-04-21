package sqlsolver.superopt.substitution;

import sqlsolver.superopt.constraint.Constraint;
import sqlsolver.superopt.constraint.Constraints;
import sqlsolver.superopt.fragment.Fragment;
import sqlsolver.superopt.fragment.SymbolNaming;

import java.util.List;

public interface Substitution {
  int id();

  void setId(int i);

  Fragment _0();

  Fragment _1();

  Constraints constraints();

  SymbolNaming naming();

  void resetNaming();

  String canonicalStringify();

  default boolean isExtended() {
    final String str0 = _0().toString();
    final String str1 = _1().toString();
    return str0.contains("Agg")
        || str0.contains("Union")
        || str1.contains("Agg")
        || str1.contains("Union");
  }

  static Substitution parse(String str) {
    return SubstitutionImpl.parse(str);
  }

  static Substitution mk(Fragment f0, Fragment f1, List<Constraint> constraints) {
    return SubstitutionImpl.mk(f0, f1, constraints);
  }
}
