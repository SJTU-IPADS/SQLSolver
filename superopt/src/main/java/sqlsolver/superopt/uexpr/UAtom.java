package sqlsolver.superopt.uexpr;

import java.util.Collections;
import java.util.List;

public interface UAtom extends UTerm {
  UVar var();

  @Override
  default List<UTerm> subTerms() {
    return Collections.emptyList();
  }
}
