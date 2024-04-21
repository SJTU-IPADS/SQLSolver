package sqlsolver.superopt.uexpr;

import java.util.List;

public interface UAdd extends UTerm {
  default UKind kind() {
    return UKind.ADD;
  }

  static UAdd mk(UTerm e0, UTerm e1) {
    return UAddImpl.mk(e0, e1);
  }

  static UAdd mk(UTerm e0, UTerm e1, UTerm... others) {
    return UAddImpl.mk(e0, e1, others);
  }

  static UAdd mk(List<UTerm> exprs) {
    return new UAddImpl(exprs);
  }
}
