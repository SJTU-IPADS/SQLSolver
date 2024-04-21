package sqlsolver.superopt.constraint;

import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.superopt.constraint.Constraint.Kind;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.fragment.*;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.common.utils.IterableSupport.lazyFilter;

class ConstraintsImpl extends AbstractList<Constraint> implements Constraints {
  private final List<Constraint> constraints;
  private final Symbols sourceSyms, targetSyms;
  private final NaturalCongruence<Symbol> congruence;
  private final Map<Symbol, Symbol> instantiations;
  private final int[] segBases;

  ConstraintsImpl(
      List<Constraint> constraints,
      Symbols sourceSyms,
      Symbols targetSyms,
      NaturalCongruence<Symbol> congruence,
      Map<Symbol, Symbol> instantiations) {
    this.constraints = constraints;
    this.sourceSyms = sourceSyms;
    this.targetSyms = targetSyms;
    this.congruence = congruence;
    this.instantiations = instantiations;
    this.segBases = new int[Constraint.Kind.values().length + 1];
    calcSegments();
  }

  static Constraints mk(Symbols srcSyms, Symbols tgtSyms, List<Constraint> constraints) {
    // This congruence contains only the symbols at the source side
    final NaturalCongruence<Symbol> eqSymbols = NaturalCongruence.mk();
    for (Constraint c : lazyFilter(constraints, it -> it.kind().isEq())) {
      if (any(asList(c.symbols()), it -> it.ctx() != srcSyms)) continue;
      eqSymbols.putCongruent(c.symbols()[0], c.symbols()[1]);
    }

    // This map contains <tgtSym -> srcSym>, which means the `tgtSym` is instantiated from `srcSym`
    final Map<Symbol, Symbol> instantiationSource = new IdentityHashMap<>();
    for (Constraint c : lazyFilter(constraints, it -> it.kind().isEq())) {
      Symbol sym0 = c.symbols()[0], sym1 = c.symbols()[1];
      if (sym0.ctx() == srcSyms && sym1.ctx() == srcSyms) continue;

      if (sym0.ctx() != srcSyms && sym1.ctx() == srcSyms) {
        Symbol tmp = sym0;
        sym0 = sym1;
        sym1 = tmp;
      }

      assert c.kind() == Kind.AttrsSub || (sym0.ctx() == srcSyms && sym1.ctx() != srcSyms);

      instantiationSource.put(sym1, sym0);
    }

    constraints.removeIf(it -> it.kind().isEq() && it.symbols()[0].ctx() != it.symbols()[1].ctx());
    constraints.sort(comparing(Constraint::kind));

    return new ConstraintsImpl(constraints, srcSyms, tgtSyms, eqSymbols, instantiationSource);
  }

  private void calcSegments() {
    int base = 0;
    for (Kind kind : Constraint.Kind.values()) {
      base = findBaseOf(kind, base);
      segBases[kind.ordinal()] = base;
    }
    segBases[segBases.length - 1] = constraints.size();
  }

  private int findBaseOf(Kind targetKind, int fromIndex) {
    int i = fromIndex;
    for (final int bound = constraints.size(); i < bound; ++i) {
      final Kind kind = constraints.get(i).kind();
      if (kind == targetKind) return i;
      if (kind.ordinal() > targetKind.ordinal()) return i;
    }
    return i;
  }

  @Override
  public Symbols sourceSymbols() {
    return sourceSyms;
  }

  @Override
  public Symbols targetSymbols() {
    return targetSyms;
  }

  @Override
  public Constraint get(int index) {
    return constraints.get(index);
  }

  @Override
  public int size() {
    return constraints.size();
  }

  @Override
  public Symbol sourceOf(Symbol /* Attrs or Schema */ sym) {
    /* Haven't dealt with schema on Agg */
    if (sym.kind() == Symbol.Kind.SCHEMA) {
      final Op owner = sourceSyms.ownerOf(sym);
      if (owner.kind() == OpKind.AGG) return null;
      else sym = ((Proj) owner).attrs();
    }

    for (Constraint attrsSub : ofKind(Kind.AttrsSub))
      if (attrsSub.symbols()[0] == sym) {
        return attrsSub.symbols()[1];
      }
    return null;
  }

  @Override
  public List<Symbol> attrsAndTableSourceChain(Symbol /* Attrs only */ attrsSym) {
    /* Get all attrs or table which is directly a superset of `attrsSym` */
    /* Only consider sources going through `Proj` node, do not consider source schema on `Agg` node */
    assert attrsSym.kind() == Symbol.Kind.ATTRS;
    final List<Symbol> sourceChain = new ArrayList<>(3);
    Symbol sym = attrsSym;
    while (sym.kind() != Symbol.Kind.TABLE) {
      final Symbol source = sourceOf(sym);
      if (source.kind() == Symbol.Kind.SCHEMA) {
        final Op owner = sourceSyms.ownerOf(source);
        if (owner.kind() == OpKind.PROJ) {
          sourceChain.add(((Proj) owner).attrs());
          sym = ((Proj) owner).attrs();
        }
        else break; /* owner.kind() == OpKind.Agg */
      } else { /* source.kind() == TABLE */
        sourceChain.add(source);
        sym = source;
      }
    }
    return sourceChain;
  }

  @Override
  public List<Constraint> ofKind(Kind kind) {
    return constraints.subList(beginIndexOf(kind), endIndexOf(kind));
  }

  @Override
  public NaturalCongruence<Symbol> eqSymbols() {
    return congruence;
  }

  @Override
  public Symbol instantiationOf(Symbol tgtSym) {
    return instantiations.get(tgtSym);
  }

  @Override
  public StringBuilder canonicalStringify(SymbolNaming naming, StringBuilder builder) {
    return ConstraintSupport.stringify(this, naming, true, builder);
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    return ConstraintSupport.stringify(this, naming, false, builder);
  }

  private int beginIndexOf(Constraint.Kind kind) {
    return segBases[kind.ordinal()];
  }

  private int endIndexOf(Constraint.Kind kind) {
    return segBases[kind.ordinal() + 1];
  }
}
