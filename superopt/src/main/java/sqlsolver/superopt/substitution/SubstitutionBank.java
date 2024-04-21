package sqlsolver.superopt.substitution;

import sqlsolver.superopt.util.Fingerprint;

import java.util.Collection;
import java.util.function.Predicate;

public interface SubstitutionBank {
  int size();

  boolean add(Substitution substitution);

  boolean contains(Substitution rule);

  boolean isExtended();

  void remove(Substitution substitution);

  void removeIf(Predicate<Substitution> check);

  Collection<Substitution> rules();

  Iterable<Substitution> ruleOfFingerprint(Fingerprint fingerprint);

  static SubstitutionBank mk() {
    return new SubstitutionBankImpl();
  }
}
