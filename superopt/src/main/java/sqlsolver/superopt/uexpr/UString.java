package sqlsolver.superopt.uexpr;

import java.util.Collections;
import java.util.List;

public interface UString extends UTerm {

  @Override
  default UKind kind() {
        return UKind.STRING;
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

  String value();

  UString NULL = UStringImpl.mkNull();

  static UString mk(String v) {
        return UStringImpl.mkVal(v);
    }

  static UString nullVal() {return UStringImpl.mkNull();}
}
