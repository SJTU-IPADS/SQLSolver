package sqlsolver.superopt.liastar.parameter;

import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.util.Z3Support;

/**
 * A LIA* formula except that its {@code equals(Object)} method
 * uses Z3.
 * Two instances of this class are equal
 * if they are equivalent propositions (checked by a solver).
 */
public record SemanticLiaStar(LiaStar formula) {
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SemanticLiaStar that)) return false;
    final LiaStar implies1 = LiaStar.mkImplies(false, formula, that.formula);
    final LiaStar implies2 = LiaStar.mkImplies(false, that.formula, formula);
    final LiaStar toCheck = LiaStar.mkAnd(false, implies1, implies2);
    return Z3Support.isValidLia(toCheck);
  }

  @Override
  public int hashCode() {
    // make sure that equivalent formulas have the same hash code
    // this makes HashSet queries fall back to linear queries
    return 0;
  }
}
