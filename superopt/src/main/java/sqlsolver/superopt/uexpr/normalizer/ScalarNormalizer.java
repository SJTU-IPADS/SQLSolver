package sqlsolver.superopt.uexpr.normalizer;

import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.uexpr.*;

import java.util.*;
import java.util.function.Function;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.filter;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.superopt.uexpr.UExprSupport.*;
import static sqlsolver.superopt.uexpr.UTerm.returnsNatural;

/**
 * This class provides scalar-term-related normalization.
 * @see ScalarTerm
 */
public class ScalarNormalizer extends UNormalization {
  private final Schema schema;
  private final UExprConcreteTranslator.Context ctx;

  public ScalarNormalizer(UTerm expr,
                          Schema schema,
                          UExprConcreteTranslator.QueryTranslator translator,
                          UExprConcreteTranslator.Context ctx) {
    super(expr, translator);
    this.schema = schema;
    this.ctx = ctx;
  }

  @Override
  public UTerm normalizeTerm() {
    // first runs: transform non-scalar terms according to environmental scalar terms
    do {
      expr = new QueryUExprNormalizer(expr, schema, translator).normalizeTerm();
      isModified = false;

      expr = performNormalizeRule(t -> convertBV2FV(t, new HashSet<>(), true));
    } while (isModified);
    // last run: transform scalar terms themselves
    expr = performNormalizeRule(t -> convertBV2FV(t, new HashSet<>(), false));
    return expr;
  }

  @Override
  protected UTerm performNormalizeRule(Function<UTerm, UTerm> transformation) {
    expr = transformation.apply(expr);
    expr = new QueryUExprNormalizer(expr, schema, translator).normalizeTerm();
    return expr;
  }

  /*
   * Rules.
   */

  /**
   * Given \sum{y}g(y),
   * if f is a scalar term in sum{y}'s context (i.e. \sum{x}f(x) <= 1),
   * and f(y)>=||g(y)||,
   * replace y in f with a FV bound to the scalar term
   * and remove y from the summation BV set.
   * Here f and g return natural numbers, and x,y have the same schema.
   * @param forbidsSelfTransform if false, this rule will be also applied to scalar terms themselves
   */
  public UTerm convertBV2FV(UTerm term, Set<ScalarTerm> scalarTerms, boolean forbidsSelfTransform) {
    // update "critical" context and transform subterms
    if (term instanceof UMul mul) {
      final List<UTerm> factors = mul.subTerms();
      // factor-to-scalar-term map
      final Map<UTerm, ScalarTerm> scalarTermMap = new HashMap<>();
      // compute scalar term for each factor
      for (UTerm factor : factors) {
        final ScalarTerm st = extractScalarTerm(factor);
        if (st == null) continue;
        // new scalar term
        // bind it to a new free var and schema
        final ScalarTerm actualSt = ctx.addScalarTerm(st,
                UExprSupport::mkFreshFreeVar,
                () -> translator.getTupleVarSchema(st.outVar()),
                translator.getSchema());
        scalarTermMap.put(factor, actualSt);
        final UVar fv = ctx.scalarToFV().get(actualSt);
        final List<Value> varSchema = ctx.scalarFVSchema().get(fv);
        // if a free var is not present in the var-schema map, add the mapping
        if (translator.getTupleVarSchema(fv) == null) {
          translator.putTupleVarSchema(fv, varSchema);
        }
      }
      // recursion
      term = transformSubTerms(term, t -> {
        final Set<ScalarTerm> newScalarTerms = new HashSet<>(scalarTerms);
        // this rule also applies to the scalar term itself (remove its bound var)
        // according to the switch
        final List<UTerm> factorsAsEnv;
        if (forbidsSelfTransform) {
          factorsAsEnv = filter(factors, factor -> !factor.equals(t));
        } else {
          factorsAsEnv = factors;
        }
        newScalarTerms.addAll(filter(map(factorsAsEnv, scalarTermMap::get), Objects::nonNull));
        return convertBV2FV(t, newScalarTerms, forbidsSelfTransform);
      });
    } else {
      // only copy on write
      term = transformSubTerms(term, t -> convertBV2FV(t, scalarTerms, forbidsSelfTransform));
    }

    if (!(term instanceof USum sum)) return term;
    final UTerm body = sum.body();

    for (ScalarTerm scalar : scalarTerms) {
      // check whether sum matches scalar
      final UVar y = scalar.matches(body, sum.boundedVars(), translator.getSchema());
      if (y != null) {
        // sum{y} matches scalar
        final UVar fv = ctx.scalarToFV().get(scalar);
        final UTerm newBody = body.replaceVar(y, fv, false);
        final Set<UVar> newBVs = new HashSet<>(sum.boundedVars());
        newBVs.remove(y);
        return newBVs.isEmpty() ? newBody : USum.mk(newBVs, newBody);
      }
    }
    return term;
  }

  /*
   * Helper functions.
   */

  /**
   * Given [\sum{x}f(x) <= 1] / ||\sum{x}f(x)|| / not(\sum{x}f(x)),
   * return {x, f(x)},
   * which means there's at most one tuple x that makes f=1
   * and any other tuples make f=0.
   * Here f should return a natural number.
   * Return null otherwise.
   */
  private ScalarTerm extractScalarTerm(UTerm term) {
    // [\sum{x}f(x) <= 1]
    if (term instanceof UPred pred
            && pred.predKind() == UPred.PredKind.LE
            && pred.args().get(1).equals(UConst.one())
            && pred.args().get(0) instanceof USum sum
            && sum.boundedVars().size() == 1) {
      final UVar bv = sum.boundedVars().stream().toList().get(0).copy();
      return ScalarTerm.mk(bv, translator.getTupleVarSchema(bv), sum.body().copy());
    }
    // ||\sum{x}f(x)||
    if (term instanceof USquash squash
            && squash.body() instanceof USum sum
            && sum.boundedVars().size() == 1) {
      final UVar bv = sum.boundedVars().stream().toList().get(0).copy();
      return ScalarTerm.mk(bv, translator.getTupleVarSchema(bv), sum.body().copy());
    }
    // not(\sum{x}f(x))
    if (term instanceof UNeg neg
            && neg.body() instanceof USum sum
            && sum.boundedVars().size() == 1) {
      final UVar bv = sum.boundedVars().stream().toList().get(0).copy();
      return ScalarTerm.mk(bv, translator.getTupleVarSchema(bv), sum.body().copy());
    }
    return null;
  }

}
