package sqlsolver.superopt.constraint;

import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.superopt.fragment.Symbol;
import sqlsolver.superopt.fragment.SymbolNaming;
import sqlsolver.superopt.fragment.Symbols;

import java.util.List;
import java.util.Set;

public interface Constraints extends List<Constraint> {
  Symbols sourceSymbols();

  Symbols targetSymbols();

  List<Constraint> ofKind(Constraint.Kind kind);

  NaturalCongruence<Symbol> eqSymbols();

  Symbol sourceOf(Symbol sym);

  List<Symbol> attrsAndTableSourceChain(Symbol attrsSym);

  Symbol instantiationOf(Symbol tgtSym);

  StringBuilder canonicalStringify(SymbolNaming naming, StringBuilder builder);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  default boolean isEq(Symbol s0, Symbol s1) {
    return eqSymbols().isCongruent(s0, s1);
  }

  default Set<Symbol> eqClassOf(Symbol symbol) {
    return eqSymbols().eqClassOf(symbol);
  }

  /** srcSyms: symbols at the source side. */
  static Constraints mk(Symbols srcSyms, Symbols tgtSyms, List<Constraint> constraints) {
    return ConstraintsImpl.mk(srcSyms, tgtSyms, constraints);
  }
}
