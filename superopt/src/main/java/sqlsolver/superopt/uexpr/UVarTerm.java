package sqlsolver.superopt.uexpr;

public interface UVarTerm extends UAtom {
  @Override
  default UKind kind() {
    return UKind.VAR;
  }

  static UVarTerm mk(UVar var) {
    // assert var.isUnaryVar();
    return new UVarTermImpl(var);
  }
}
