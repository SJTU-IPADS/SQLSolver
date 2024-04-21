package sqlsolver.superopt.fragment;

/** Identity-based immutable class. */
public interface Symbol {
  enum Kind {
    TABLE,
    ATTRS,
    PRED,
    SCHEMA,
    FUNC
  }

  Kind kind();

  Symbols ctx();

  static Symbol mk(Kind kind, Symbols ctx) {
    return new SymbolImpl(kind, ctx);
  }
}
