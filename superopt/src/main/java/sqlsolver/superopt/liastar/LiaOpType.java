package sqlsolver.superopt.liastar;

public enum LiaOpType {
  LAND,
  LOR,
  LCONST,
  LSTRING,
  LEQ,
  LLE,
  LLT,
  LITE,
  LMULT,
  LNOT,
  LPLUS,
  LDIV,
  LSUM,
  LVAR,
  LFUNC;

  public boolean isAtomicPredicate() {
    return this == LEQ || this == LLE || this == LLT;
  }
}
