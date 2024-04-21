package sqlsolver.superopt.fragment;

import sqlsolver.common.utils.Copyable;

public interface Fragment extends Copyable<Fragment> {
  int id();

  Op root();

  Symbols symbols();

  void setId(int i);

  void setRoot(Op root);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  void acceptVisitor(OpVisitor visitor);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  default int symbolCount(Symbol.Kind kind) {
    return symbols().symbolsOf(kind).size();
  }

  static Fragment mk(/* nullable */ Op head) {
    return new FragmentImpl(head);
  }

  static Fragment mk(/* nullable */ Op head, boolean setupContext) {
    return new FragmentImpl(head, setupContext);
  }

  static Fragment mk(/* nullable */ Op head, Symbols symbols, boolean setupContext) {
    return new FragmentImpl(head, symbols, setupContext);
  }

  static Fragment parse(String str, SymbolNaming naming) {
    return FragmentImpl.parse(str, naming);
  }

  static boolean hasAggUnion(Fragment f) {
    String tmp = f.toString();
    return (tmp.indexOf("Agg") >= 0) || (tmp.indexOf("Union") >= 0);
  }
}
