package sqlsolver.superopt.fragment;

class SymbolImpl implements Symbol {
  private final Kind kind;
  private final Symbols ctx;

  SymbolImpl(Kind kind, Symbols ctx) {
    this.kind = kind;
    this.ctx = ctx;
  }

  @Override
  public Symbols ctx() {
    return ctx;
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public String toString() {
    return kind.name();
  }
}
