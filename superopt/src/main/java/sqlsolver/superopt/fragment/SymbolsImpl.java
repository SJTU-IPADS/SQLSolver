package sqlsolver.superopt.fragment;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import sqlsolver.common.utils.Lazy;

import java.util.List;
import java.util.Map;

class SymbolsImpl implements Symbols {
  private final Lazy<ListMultimap<Op, Symbol>> tables, attrs, preds, schemas, funcs;

  SymbolsImpl() {
    tables = Lazy.mk(SymbolsImpl::initMap);
    attrs = Lazy.mk(SymbolsImpl::initMap);
    preds = Lazy.mk(SymbolsImpl::initMap);
    schemas = Lazy.mk(SymbolsImpl::initMap);
    funcs = Lazy.mk(SymbolsImpl::initMap);
  }

  SymbolsImpl(
      ListMultimap<Op, Symbol> tables,
      ListMultimap<Op, Symbol> attrs,
      ListMultimap<Op, Symbol> preds,
      ListMultimap<Op, Symbol> schemas,
      ListMultimap<Op, Symbol> funcs) {
    this.tables = Lazy.mk(tables);
    this.attrs = Lazy.mk(attrs);
    this.preds = Lazy.mk(preds);
    this.schemas = Lazy.mk(schemas);
    this.funcs = Lazy.mk(funcs);
  }

  static Symbols merge(Symbols symbols0, Symbols symbols1) {
    final SymbolsImpl s0 = (SymbolsImpl) symbols0, s1 = (SymbolsImpl) symbols1;
    final ListMultimap<Op, Symbol> tables = merge(s0.tables.get(), s1.tables.get());
    final ListMultimap<Op, Symbol> attrs = merge(s0.attrs.get(), s1.attrs.get());
    final ListMultimap<Op, Symbol> preds = merge(s0.preds.get(), s1.preds.get());
    final ListMultimap<Op, Symbol> schemas = merge(s0.schemas.get(), s1.schemas.get());
    final ListMultimap<Op, Symbol> funcs = merge(s0.funcs.get(), s1.funcs.get());
    return new SymbolsImpl(tables, attrs, preds, schemas, funcs);
  }

  private static ListMultimap<Op, Symbol> merge(
      ListMultimap<Op, Symbol> map0, ListMultimap<Op, Symbol> map1) {
    final ListMultimap<Op, Symbol> newMap = initMap();
    newMap.putAll(map0);
    newMap.putAll(map1);
    return newMap;
  }

  @Override
  public int size() {
    return tables.get().size() + attrs.get().size() + preds.get().size();
  }

  @Override
  public void bindSymbol(Op op) {
    switch (op.kind()) {
      case INPUT -> add(op, Symbol.Kind.TABLE);
      case IN_SUB_FILTER -> add(op, Symbol.Kind.ATTRS);
      case PROJ -> {
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.SCHEMA);
      }
      case SIMPLE_FILTER -> {
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.PRED);
      }
      case EXISTS_FILTER -> {}
      case INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN -> {
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.ATTRS);
      }
      case CROSS_JOIN -> {}
      case UNION, INTERSECT, EXCEPT -> {}
      case AGG -> {
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.FUNC);
        add(op, Symbol.Kind.SCHEMA);
        add(op, Symbol.Kind.PRED);
      }
    }
  }

  @Override
  public void reBindSymbol(Op newOp, Op oldOp, Symbols oldSyms) {
    if (newOp.kind() != oldOp.kind()) throw new IllegalArgumentException();

    switch (newOp.kind()) {
      case INPUT -> add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.TABLE, 0));
      case IN_SUB_FILTER -> add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.ATTRS, 0));
      case PROJ -> {
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.ATTRS, 0));
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.SCHEMA, 0));
      }
      case SIMPLE_FILTER -> {
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.ATTRS, 0));
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.PRED, 0));
      }
      case INNER_JOIN, LEFT_JOIN -> {
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.ATTRS, 0));
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.ATTRS, 1));
      }
      case UNION, INTERSECT, EXCEPT -> {}
      case AGG -> {
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.ATTRS, 0));
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.ATTRS, 1));
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.FUNC, 0));
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.SCHEMA, 0));
        add(newOp, oldSyms.symbolAt(oldOp, Symbol.Kind.PRED, 0));
      }
    }
  }

  @Override
  public Symbol symbolAt(Op op, Symbol.Kind kind, int ordinal) {
    return getMap(kind).get(op).get(ordinal);
  }

  @Override
  public List<Symbol> symbolAt(Op op, Symbol.Kind kind) {
    return getMap(kind).get(op);
  }

  @Override
  public List<Symbol> symbolsOf(Symbol.Kind kind) {
    return (List<Symbol>) getMap(kind).values();
  }

  @Override
  public Op ownerOf(Symbol symbol) {
    for (Map.Entry<Op, Symbol> entry : getMap(symbol.kind()).entries()) {
      if (entry.getValue() == symbol) return entry.getKey();
    }
    return null;
  }

  @Override
  public boolean contains(Symbol symbol) {
    return symbol.ctx() == this || ownerOf(symbol) != null;
  }

  private static ListMultimap<Op, Symbol> initMap() {
    return LinkedListMultimap.create(4);
  }

  private ListMultimap<Op, Symbol> getMap(Symbol.Kind kind) {
    return switch (kind) {
      case TABLE -> tables.get();
      case ATTRS -> attrs.get();
      case PRED -> preds.get();
      case SCHEMA -> schemas.get();
      case FUNC -> funcs.get();
    };
  }

  private void add(Op op, Symbol.Kind kind) {
    getMap(kind).get(op).add(Symbol.mk(kind, this));
  }

  private void add(Op op, Symbol sym) {
    getMap(sym.kind()).get(op).add(sym);
  }
}
