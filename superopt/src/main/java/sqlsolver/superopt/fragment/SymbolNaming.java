package sqlsolver.superopt.fragment;

public interface SymbolNaming {
  SymbolNaming name(Symbols symbols);

  void setName(Symbol symbol, String name);

  String nameOf(Symbol symbol);

  Symbol symbolOf(String name);

  static SymbolNaming mk() {
    return new SymbolNamingImpl();
  }

  static SymbolNaming mk(Symbols symbols) {
    return mk().name(symbols);
  }
}
