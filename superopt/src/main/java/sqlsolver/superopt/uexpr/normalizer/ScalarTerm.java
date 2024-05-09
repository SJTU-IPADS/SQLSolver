package sqlsolver.superopt.uexpr.normalizer;

import sqlsolver.common.utils.NameSequence;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.translator.LiaTranslator;
import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.util.Timeout;
import sqlsolver.superopt.util.Z3Support;

import java.util.*;

import static sqlsolver.superopt.uexpr.UTerm.returnsNatural;

/**
 * A term f(x) is scalar if \sum{x}f(x) must be 0/1.
 * Here f should always return a natural number.
 * You should never directly use the constructor to create new instances of this class;
 * use {@link ScalarTerm#mk(UVar, List, UTerm)} instead.
 * @param outVar x
 * @param term f(x)
 */
public record ScalarTerm(UVar outVar, List<Value> schema, UTerm term) {
  public static ScalarTerm mk(UVar outVar, List<Value> schema, UTerm term) {
    if (!returnsNatural(term)) return null;
    final ScalarTerm result = new ScalarTerm(outVar, schema, term);
    final USum summation = USum.mk(UVar.getBaseVars(outVar.copy()), term.copy());
    // Unsupported: queries with params
    if (!summation.getFVs().isEmpty()) return null;
    return result;
  }

  /**
   * Return [\sum{x}f(x) <= 1].
   */
  public UTerm toConstraint() {
    final UTerm summation = USum.mk(UVar.getBaseVars(outVar.copy()), term.copy());
    return UMul.mk(UPred.mkBinary(UPred.PredKind.LE, summation, UConst.one()));
  }

  /**
   * Whether "that" is an (equivalent or more narrow) instance of "this"
   * w.r.t "varSchema".
   * Specifically, let "this" be {x, f(x)} and "that" be {y, g(y)},
   * return whether x and y have the same schema and f(z)>=||g(z)|| for all z.
   * This "matches" relation (between "this" and "that")
   * is reflexive and transitive.
   * @see ScalarTerm#matches(UTerm, Collection, Map) 
   */
  public UVar matches(ScalarTerm that, Map<UVar, List<Value>> varSchema) {
    final Collection<UVar> bvs = Set.of(that.outVar);
    // COW
    if (!varSchema.containsKey(that.outVar)) {
      varSchema = new HashMap<>(varSchema);
      varSchema.put(that.outVar, that.schema);
    }
    return matches(that.term, bvs, varSchema);
  }

  /**
   * Let "that" be g(y) for some y.
   * If x and y have the same schema and f(z)>=||g(z)|| for all z,
   * then "that" and "this" match.
   * @param bvs y is only chosen from this set
   * @return y, or null if "that" is not f(y) for any var y
   */
  public UVar matches(UTerm that, Collection<UVar> bvs, Map<UVar, List<Value>> varSchema) {
    // choose a var name not used in g
    // x and y will both be renamed to that name in order to perform check
    final Set<String> fvNames = that.getFVs();
    String xName = outVar.name().toString();
    while (fvNames.contains(xName) || varSchema.containsKey(UVar.mkBase(UName.mk(xName)))) {
      xName = xName + "p";
    }
    final UVar x = UVar.mkBase(UName.mk(xName));
    UTerm thisTerm = term;
    if (!x.equals(outVar)) {
      thisTerm = term.replaceVar(outVar, x, true);
    }
    // COW
    if (!varSchema.containsKey(x)) {
      varSchema = new HashMap<>(varSchema);
      varSchema.put(x, schema);
    }
    // convert bvs to a set of var names
    final Set<String> bvNames = new HashSet<>();
    for (UVar bv : bvs) {
      bvNames.add(bv.name().toString());
    }
    // try each possible var y
    for (String varName : SetSupport.intersect(fvNames, bvNames)) {
      final UVar y = UVar.mkBase(UName.mk(varName));
      final List<Value> schemaY = varSchema.get(y);
      // vars with different schema do not match
      if (!CalciteSupport.isExactlyEqualTwoValueList(schema, schemaY))
        continue;
      final UTerm thatTerm = that.replaceVar(y, x, true); // that[y->x]
      final NameSequence liaVarName = NameSequence.mkIndexed("u", 0);
      final Map<UVar, String> varMap = new HashMap<>();
      final Map<USum, String> sumMap = new HashMap<>();
      try {
        final LiaStar f = LiaTranslator.translate(thisTerm, varSchema, liaVarName, varMap, sumMap);
        final LiaStar gSquash = LiaTranslator.translate(USquash.mk(thatTerm), varSchema, liaVarName, varMap, sumMap);
        final LiaStar toCheck = LiaStar.mkLe(false, gSquash, f);
        if (Z3Support.isValidLia(toCheck)) {
          return y;
        }
      } catch (Throwable e) {
        Timeout.bypassTimeout(e);
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ScalarTerm that)) return false;
    return term.replaceVar(outVar, that.outVar, true)
            .equals(that.term);
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
