package sqlsolver.superopt.uexpr;

import java.util.Collections;
import java.util.List;

public interface UConst extends UTerm {
  @Override
  default UKind kind() {
    return UKind.CONST;
  }

  @Override
  default List<UTerm> subTerms() {
    return Collections.emptyList();
  }

  @Override
  default boolean isUsing(UVar var) {
    return false;
  }

  @Override
  default boolean isUsingProjVar(UVar var) {
    return false;
  }

  @Override
  default UTerm replaceVar(UVar baseVar, UVar repVar, boolean freshVar) {
    return this.copy();
  }

  @Override
  default boolean replaceVarInplace(UVar baseVar, UVar repVar, boolean freshVar) {
    return false;
  }

  int value();

  boolean isZeroOneVal();

  UConst ONE = one();
  UConst ZERO = zero();
  UConst NULL = UConstImpl.mkNull();

  static UConst mk(int v) {
    return UConstImpl.mkVal(v);
  }

  static UConst zero() {
    return mk(0);
  }

  static UConst one() {
    return mk(1);
  }

  static UConst nullVal() {return UConstImpl.mkNull();}
}
