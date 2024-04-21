package sqlsolver.superopt.constraint;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.fragment.*;

import java.util.*;

class ConstraintsIndex extends AbstractList<Constraint> implements List<Constraint> {
  private static final int NUM_KINDS_OF_CONSTRAINTS = Constraint.Kind.values().length;
  private static final int NUM_KINDS_OF_SYMBOLS = Symbol.Kind.values().length;

  private final Fragment source, target;
  private final List<Constraint> constraints;
  private final TObjectIntMap<Symbol> symOrdinals;
  private final ListMultimap<Symbol, Symbol> viableSources;
  private final int[] segmentBase;

  ConstraintsIndex(Fragment source, Fragment target) {
    this.source = source;
    this.target = target;
    this.constraints = new ArrayList<>();
    this.symOrdinals = new TObjectIntHashMap<>();
    this.viableSources = MultimapBuilder.hashKeys().linkedListValues().build();
    this.segmentBase = new int[NUM_KINDS_OF_CONSTRAINTS + NUM_KINDS_OF_SYMBOLS + 1];

    initSymbolOrdinals();
    initPreconditions();
    initInstantiations();
  }

  private void initSymbolOrdinals() {
    for (Symbol.Kind kind : Symbol.Kind.values()) initSymbolOrdinals(kind);
  }

  private void initPreconditions() {
    analyzeSource(source.root());
    analyzeSource(target.root());
    for (Constraint.Kind kind : Constraint.Kind.values()) {
      segmentBase[kind.ordinal()] = constraints.size();
      initPrecondition(kind);
    }
    segmentBase[NUM_KINDS_OF_CONSTRAINTS] = constraints.size();
  }

  private void initInstantiations() {
    for (Symbol.Kind symKind : Symbol.Kind.values()) {
      segmentBase[NUM_KINDS_OF_CONSTRAINTS + symKind.ordinal()] = constraints.size();
      initInstantiation(symKind);
    }
    segmentBase[NUM_KINDS_OF_CONSTRAINTS + NUM_KINDS_OF_SYMBOLS] = constraints.size();
  }

  private void initSymbolOrdinals(Symbol.Kind kind) {
    final List<Symbol> symsOfKind = source.symbols().symbolsOf(kind);
    for (int i = 0, bound = symsOfKind.size(); i < bound; ++i)
      symOrdinals.put(symsOfKind.get(i), i);
  }

  private void initPrecondition(Constraint.Kind constraintKind) {
    switch (constraintKind) {
      case TableEq -> initEqs(Symbol.Kind.TABLE);
      case AttrsEq -> initEqs(Symbol.Kind.ATTRS);
      case PredicateEq -> initEqs(Symbol.Kind.PRED);
      case FuncEq -> initEqs(Symbol.Kind.FUNC);
      case AttrsSub -> initAttrsSub();
      case Unique -> initUnique();
      case NotNull -> initNotNull();
      case Reference -> initReference();
      default -> {}
    }
  }

  private void initEqs(Symbol.Kind symKind) {
    final Constraint.Kind kind = Constraint.Kind.eqOfSymbol(symKind);
    final List<Symbol> syms = source.symbols().symbolsOf(symKind);
    for (int i = 0, bound = syms.size() - 1; i < bound; i++)
      for (int j = i + 1; j <= bound; ++j)
        constraints.add(Constraint.mk(kind, syms.get(i), syms.get(j)));
  }

  private void initAttrsSub() {
    for (Symbol attrs : source.symbols().symbolsOf(Symbol.Kind.ATTRS))
      for (Symbol source : viableSources.get(attrs))
        constraints.add(Constraint.mk(Constraint.Kind.AttrsSub, attrs, source));

    if (source.toString().equals("Filter(InnerJoin(Input,Input))")
      && target.toString().equals("Filter(InnerJoin(Input,Input))")){
      final Symbol attrs = ((SimpleFilter) target.root()).attrs();
      final Symbol table = ((Input) source.root().predecessors()[0].predecessors()[0]).table();
      constraints.add(Constraint.mk(Constraint.Kind.AttrsSub, attrs, table));
    }
  }

  private void initUnique() {
    for (Symbol attrs : source.symbols().symbolsOf(Symbol.Kind.ATTRS))
      for (Symbol source : viableSources.get(attrs))
        if (source.kind() == Symbol.Kind.TABLE) {
          constraints.add(Constraint.mk(Constraint.Kind.Unique, source, attrs));
        }
  }

  private void initNotNull() {
    for (Symbol attrs : source.symbols().symbolsOf(Symbol.Kind.ATTRS))
      for (Symbol source : viableSources.get(attrs))
        if (source.kind() == Symbol.Kind.TABLE) {
          constraints.add(Constraint.mk(Constraint.Kind.NotNull, source, attrs));
        }
  }

  private void initReference() {
    initReference(source.root());
  }

  private void initReference(Op op) {
    for (Op predecessor : op.predecessors()) initReference(predecessor);

    if (op.kind().isJoin()) {
      final Join join = (Join) op;
      final Symbol lhs = join.lhsAttrs(), rhs = join.rhsAttrs();
      for (Symbol lhsSource : viableSources.get(lhs))
        if (lhsSource.kind() == Symbol.Kind.TABLE)
          for (Symbol rhsSource : viableSources.get(rhs))
            if (rhsSource.kind() == Symbol.Kind.TABLE) {
              constraints.add(Constraint.mk(Constraint.Kind.Reference, lhsSource, lhs, rhsSource, rhs));
              constraints.add(Constraint.mk(Constraint.Kind.Reference, rhsSource, rhs, lhsSource, lhs));
            }
    }
  }

  private void initInstantiation(Symbol.Kind symKind) {
    final Constraint.Kind constraintKind = Constraint.Kind.eqOfSymbol(symKind);
    for (Symbol targetSym : target.symbols().symbolsOf(symKind))
      for (Symbol sourceSym : source.symbols().symbolsOf(symKind))
        constraints.add(Constraint.mk(constraintKind, targetSym, sourceSym));
  }

  int beginIndexOfKind(Constraint.Kind kind) {
    return segmentBase[kind.ordinal()];
  }

  int endIndexOfKind(Constraint.Kind kind) {
    return segmentBase[kind.ordinal() + 1];
  }

  int beginIndexOfEq(Symbol.Kind kind) {
    return beginIndexOfKind(Constraint.Kind.eqOfSymbol(kind));
  }

  int endIndexOfEq(Symbol.Kind kind) {
    return endIndexOfKind(Constraint.Kind.eqOfSymbol(kind));
  }

  int beginIndexOfInstantiation(Symbol.Kind kind) {
    return segmentBase[NUM_KINDS_OF_CONSTRAINTS + kind.ordinal()];
  }

  int endIndexOfInstantiation(Symbol.Kind kind) {
    return segmentBase[NUM_KINDS_OF_CONSTRAINTS + kind.ordinal() + 1];
  }

  int indexOfInstantiation(Symbol from, Symbol to) {
    assert from.kind() == to.kind();
    final int begin = beginIndexOfInstantiation(from.kind());
    final int end = endIndexOfInstantiation(from.kind());
    for (int i = begin; i < end; ++i) {
      final Constraint instantiation = constraints.get(i);
      if (instantiation.symbols()[0] == to && instantiation.symbols()[1] == from) return i;
    }
    assert false;
    return -1;
  }

  int indexOfEq(Symbol sym0, Symbol sym1) {
    assert sym0 != sym1;
    assert sym0.kind() == sym1.kind();
    final Symbol.Kind kind = sym0.kind();
    final int base = beginIndexOfEq(kind);
    final int symCount = source.symbolCount(kind);
    final int i = symOrdinals.get(sym0), j = symOrdinals.get(sym1);
    final int x = Math.min(i, j), y = Math.max(i, j);
    return base + ((((symCount << 1) - x - 1) * x) >> 1) + y - x - 1;
  }

  Collection<Symbol> viableSourcesOf(Symbol attrs) {
    assert attrs.kind() == Symbol.Kind.ATTRS;
    return viableSources.get(attrs);
  }

  Substitution mkRule(BitSet enabled) {
    final int bound = size();
    final List<Constraint> enabledConstraints = new ArrayList<>(bound);
    for (int i = 0; i < bound; i++) {
      if (enabled.get(i)) enabledConstraints.add(constraints.get(i));
    }
    return Substitution.mk(source, target, enabledConstraints);
  }

  Fragment sourceTemplate() {
    return source;
  }

  Fragment targetTemplate() {
    return target;
  }

  Symbols sourceSymbols() {
    return source.symbols();
  }

  Symbols targetSymbols() {
    return target.symbols();
  }

  int ordinalOf(Symbol sym) {
    return symOrdinals.get(sym);
  }

  String toString(SymbolNaming naming, BitSet enabled) {
    final int bound = size();
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < bound; i++) {
      if (enabled.get(i)) {
        constraints.get(i).stringify(naming, builder);
        builder.append(';');
      }
    }
    return builder.toString();
  }

  String toString(SymbolNaming naming) {
    final int bound = size();
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < bound; i++) {
      constraints.get(i).stringify(naming, builder);
      builder.append(';');
    }
    return builder.toString();
  }

  @Override
  public Constraint get(int index) {
    return constraints.get(index);
  }

  @Override
  public int size() {
    return constraints.size();
  }

  private List<Symbol> analyzeSource(Op op) {
    final OpKind kind = op.kind();
    final Op[] predecessor = op.predecessors();
    final List<Symbol> lhs = kind.numPredecessors() > 0 ? analyzeSource(predecessor[0]) : null;
    final List<Symbol> rhs = kind.numPredecessors() > 1 ? analyzeSource(predecessor[1]) : null;

    switch (kind) {
      case INPUT:
        return Collections.singletonList(((Input) op).table());
      case INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN:
        final Join join = (Join) op;
        viableSources.putAll(join.lhsAttrs(), lhs);
        viableSources.putAll(join.rhsAttrs(), rhs);
        return ListSupport.join(lhs, rhs);
      case CROSS_JOIN:
        return ListSupport.join(lhs, rhs);
      case SIMPLE_FILTER, IN_SUB_FILTER:
        viableSources.putAll(((AttrsFilter) op).attrs(), lhs);
      case EXISTS_FILTER:
        return lhs;
      case PROJ:
        final Proj proj = (Proj) op;
        viableSources.putAll(proj.attrs(), lhs);
        return Collections.singletonList(proj.schema());
      case UNION, INTERSECT, EXCEPT:
        return ListSupport.join(lhs, rhs);
      case AGG:
        final Agg agg = (Agg) op;
        viableSources.putAll(agg.groupByAttrs(), lhs);
        viableSources.putAll(agg.aggregateAttrs(), lhs);
        viableSources.putAll(agg.aggregateOutputAttrs(), Collections.singletonList(agg.schema()));
        return Collections.singletonList(agg.schema());
      default:
        throw new IllegalArgumentException("unsupported op kind " + kind);
    }
  }
}
