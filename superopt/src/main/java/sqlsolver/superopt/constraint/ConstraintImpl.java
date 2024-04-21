package sqlsolver.superopt.constraint;

import sqlsolver.common.utils.ArraySupport;
import sqlsolver.superopt.fragment.Symbol;
import sqlsolver.superopt.fragment.SymbolNaming;

import java.util.Arrays;

import static java.util.Arrays.asList;

class ConstraintImpl implements Constraint {
  private final Kind kind;
  private final Symbol[] symbols;

  ConstraintImpl(Kind kind, Symbol[] symbols) {
    this.kind = kind;
    this.symbols = symbols;
  }

  static Constraint parse(String str, SymbolNaming naming) {
    final String[] fields = str.split("[(),\\[\\] ]+");
    final Kind kind = Kind.valueOf(fields[0].replace("Pick", "Attrs") /* backward compatible */);

    if (fields.length != kind.numSyms() + 1)
      throw new IllegalArgumentException("invalid serialized constraint: " + str);

    final Symbol[] symbols =
        ArraySupport.map(asList(fields).subList(1, fields.length), naming::symbolOf, Symbol.class);

    return new ConstraintImpl(kind, symbols);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public Symbol[] symbols() {
    return symbols;
  }

  @Override
  public String canonicalStringify(SymbolNaming naming) {
    return ConstraintSupport.stringify(this, naming, true, new StringBuilder()).toString();
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    return ConstraintSupport.stringify(this, naming, false, builder);
  }

  @Override
  public String toString() {
    return kind.name();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Constraint)) return false;
    final Constraint that = (Constraint) o;
    return kind == that.kind() && Arrays.equals(symbols, that.symbols());
  }

  @Override
  public int hashCode() {
    int result = kind.hashCode();
    result = 31 * result + Arrays.hashCode(symbols);
    return result;
  }
}
