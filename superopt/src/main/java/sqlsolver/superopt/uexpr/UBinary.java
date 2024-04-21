package sqlsolver.superopt.uexpr;

import java.util.List;

public interface UBinary extends UTerm {
  UTerm lhs();

  UTerm rhs();

  @Override
  default List<UTerm> subTerms() {
    return List.of(lhs(), rhs());
  }
}
