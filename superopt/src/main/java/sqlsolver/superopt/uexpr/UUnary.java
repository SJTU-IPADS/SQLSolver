package sqlsolver.superopt.uexpr;

import java.util.Collections;
import java.util.List;

public interface UUnary extends UTerm {
  UTerm body();

  @Override
  default List<UTerm> subTerms() {
    return Collections.singletonList(body());
  }
}
