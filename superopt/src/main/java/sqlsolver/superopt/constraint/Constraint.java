package sqlsolver.superopt.constraint;

import sqlsolver.superopt.fragment.Symbol;
import sqlsolver.superopt.fragment.SymbolNaming;

public interface Constraint {
  enum Kind {
    TableEq(2),
    AttrsEq(2),
    PredicateEq(2),
    SchemaEq(2),
    FuncEq(2),
    AttrsSub(2),
    Unique(2),
    NotNull(2),
    Reference(4),
    ; // DON'T change the order. Some implementations trick depends on this.

    private static final Constraint.Kind[] EQ_CONSTRAINT_KIND_OF_SYM_KIND =
        new Constraint.Kind[Symbol.Kind.values().length];

    static {
      EQ_CONSTRAINT_KIND_OF_SYM_KIND[Symbol.Kind.TABLE.ordinal()] = TableEq;
      EQ_CONSTRAINT_KIND_OF_SYM_KIND[Symbol.Kind.ATTRS.ordinal()] = AttrsEq;
      EQ_CONSTRAINT_KIND_OF_SYM_KIND[Symbol.Kind.PRED.ordinal()] = PredicateEq;
      EQ_CONSTRAINT_KIND_OF_SYM_KIND[Symbol.Kind.SCHEMA.ordinal()] = SchemaEq;
      EQ_CONSTRAINT_KIND_OF_SYM_KIND[Symbol.Kind.FUNC.ordinal()] = FuncEq;
    }

    private final int numSyms;

    Kind(int numSyms) {
      this.numSyms = numSyms;
    }

    public int numSyms() {
      return numSyms;
    }

    public boolean isEq() {
      return this == TableEq || this == AttrsEq || this == PredicateEq || this == SchemaEq || this == FuncEq;
    }

    public boolean isIntegrityConstraint() {
      return this == Unique || this == NotNull || this == Reference;
    }

    public static Constraint.Kind eqOfSymbol(Symbol.Kind symKind) {
      return EQ_CONSTRAINT_KIND_OF_SYM_KIND[symKind.ordinal()];
    }
  }

  Kind kind();

  Symbol[] symbols();

  String canonicalStringify(SymbolNaming naming);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Constraint parse(String str, SymbolNaming naming) {
    return ConstraintImpl.parse(str, naming);
  }

  static Constraint mk(Kind kind, Symbol... symbols) {
    return new ConstraintImpl(kind, symbols);
  }
}
