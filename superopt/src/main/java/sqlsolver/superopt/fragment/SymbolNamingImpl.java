package sqlsolver.superopt.fragment;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import static sqlsolver.common.utils.IterableSupport.linearFind;

class SymbolNamingImpl implements SymbolNaming {
  private final BiMap<Symbol, String> names;
  private final List<Pair<String, Symbol>> extraNaming;
  private final NamingStrategy tableNaming, attrsNaming, predNaming, schemaNaming, funcNaming;

  SymbolNamingImpl() {
    names = HashBiMap.create();
    tableNaming = new NamingStrategyImpl();
    attrsNaming = new NamingStrategyImpl();
    predNaming = new NamingStrategyImpl();
    schemaNaming = new NamingStrategyImpl();
    funcNaming = new NamingStrategyImpl();
    extraNaming = new ArrayList<>(2);
  }

  @Override
  public SymbolNaming name(Symbols symbols) {
    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.TABLE))
      names.computeIfAbsent(symbol, tableNaming::mkName);

    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.ATTRS))
      names.computeIfAbsent(symbol, attrsNaming::mkName);

    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.PRED))
      names.computeIfAbsent(symbol, predNaming::mkName);

    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.SCHEMA))
      names.computeIfAbsent(symbol, schemaNaming::mkName);

    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.FUNC))
      names.computeIfAbsent(symbol, funcNaming::mkName);

    return this;
  }

  @Override
  public void setName(Symbol symbol, String name) {
    if (!names.containsKey(symbol)) names.put(symbol, name);
    else extraNaming.add(Pair.of(name, symbol));
  }

  @Override
  public String nameOf(Symbol symbol) {
    return names.get(symbol);
  }

  @Override
  public Symbol symbolOf(String name) {
    final Symbol symbol = names.inverse().get(name);
    if (symbol != null) return symbol;
    final Pair<String, Symbol> pair = linearFind(extraNaming, it -> name.equals(it.getKey()));
    return pair == null ? null : pair.getRight();
  }

  private interface NamingStrategy {
    String mkName(Symbol symbol);
  }

  private static class NamingStrategyImpl implements NamingStrategy {
    private int nextId;

    @Override
    public String mkName(Symbol symbol) {
      return ((char) (symbol.kind().name().charAt(0) + ('a' - 'A'))) + String.valueOf(nextId++);
    }
  }
}
