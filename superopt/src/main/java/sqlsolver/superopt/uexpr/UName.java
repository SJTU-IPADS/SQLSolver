package sqlsolver.superopt.uexpr;

import sqlsolver.common.utils.Copyable;

/**
 * Abstract name.
 *
 * <p>Contract: inter-translatable with a string.
 *
 * <p>Memo: this wrapper serves for the potential demand that something other than string would be
 * used as naming.
 */
public interface UName extends Copyable<UName> {
  String FUNC_IS_NULL_NAME = "IsNull";
  UName NAME_IS_NULL = mk(FUNC_IS_NULL_NAME);

  static UName mk(String str) {
    return new UNameImpl(str);
  }
}
