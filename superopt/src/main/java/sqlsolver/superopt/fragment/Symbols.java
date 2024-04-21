package sqlsolver.superopt.fragment;

import sqlsolver.common.utils.TreeContext;

import java.util.List;

public interface Symbols extends TreeContext<Symbols> {
  int size();

  void bindSymbol(Op op);

  void reBindSymbol(Op newOp, Op oldOp, Symbols oldSyms);

  Symbol symbolAt(Op op, Symbol.Kind kind, int ordinal);

  List<Symbol> symbolAt(Op op, Symbol.Kind kind);

  // Guarantee: keep the prefix appearance order.
  List<Symbol> symbolsOf(Symbol.Kind kind);

  Op ownerOf(Symbol symbol);

  boolean contains(Symbol symbol);

  @Override
  default Symbols dup() {
    return mk();
  }

  default void reBindSymbol(Op op) {
    reBindSymbol(op, op, op.context());
  }

  default void reBindSymbol(Op op, Symbols oldSyms) {
    reBindSymbol(op, op, oldSyms);
  }

  static Symbols mk() {
    return new SymbolsImpl();
  }

  static Symbols merge(Symbols symbols0, Symbols symbols1) {
    return SymbolsImpl.merge(symbols0, symbols1);
  }
}
