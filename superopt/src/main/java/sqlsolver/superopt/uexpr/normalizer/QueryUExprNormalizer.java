package sqlsolver.superopt.uexpr.normalizer;

import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.logic.SetSolver;
import sqlsolver.superopt.logic.VerificationResult;
import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.util.Bag;

import java.util.*;
import java.util.function.BiFunction;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.calcite.CalciteSupport.*;
import static sqlsolver.superopt.uexpr.UExprSupport.*;
import static sqlsolver.superopt.uexpr.UExprSupport.isNullPred;

/**
 * This class provides the complex normalization for U-Expression.
 */
public class QueryUExprNormalizer extends UNormalization {
  private final Schema schema;

  private final List<UVar> icFreshVars;

  public QueryUExprNormalizer(UTerm expr, Schema schema, UExprConcreteTranslator.QueryTranslator translator) {
    super(expr, translator);
    this.schema = schema;
    this.icFreshVars = new ArrayList<>();
  }

  @Override
  public UTerm normalizeTerm() {
    do {
      expr = super.normalizeTerm();
      // ensure that BVs are not duplicate after UNormalization
      // UNormalization cannot create new BVs due to lack of tuple schema info
      renameSameBoundedVarSummation(expr, new HashSet<>());
      isModified = false;

      expr = performNormalizeRule(this::transformNulls);
      expr = performNormalizeRule(this::removeNulls);
      expr = performNormalizeRule(this::removeDeterminedBoundedVar);
      expr = performNormalizeRule(this::removeDeterminedBoundedColumn);
      expr = performNormalizeRule(this::removeUnaryBoundedVar);
      expr = performNormalizeRule(this::simplifySumByUnarySubTerm);
      expr = performNormalizeRule(this::simplifySumBySetSolver);
      expr = performNormalizeRule(this::simplifyMaxMinInSum);
      expr = performNormalizeRule(this::simplifyMultiplication);
      expr = performNormalizeRule(this::eliminateNegationTerm);
      expr = performNormalizeRule(this::transformUnrelatedSummation);
    } while (isModified);
    return expr;
  }

  /**
   * This normalization is done before the UExpr is sent to LIA*.
   */
  public UTerm finalNormalizeTerm(Set<UVar> boundVarSet) {
    // no need to performNormalizeRule because there is no add and mul need to be flatted.
    do {
      isModified = false;

      expr = performNormalizeRule(this::extractUnrelatedSummation);
      expr = performNormalizeRule(this::splitUnrelatedSummation);
      expr = performNormalizeRule(this::extractUnrelatedSummationTerms);
      expr = performNormalizeRule(this::replaceSummation);
      expr = performNormalizeRule(this::applyFunction);
      expr = performNormalizeRule(this::removeConstants);
      expr = performNormalizeRule(this::transformEqNullIsNull);
      expr = performNormalizeRule(this::transformNulls);
    } while (isModified);

    // bound vars in different UExpressions should not intersect
    // (i.e. bound vars should be globally unique)
    renameSameBoundedVarSummation(expr, boundVarSet);

    return expr;
  }

  /*
   * Helper functions.
   */

  /**
   * Does a term has a target kind term.
   */
  private static boolean haveTargetKind(UTerm term, UKind kind) {
    if (term.kind() == kind)
      return true;
    for (UTerm subTerm : term.subTerms())
      if (haveTargetKind(subTerm, kind)) return true;
    return false;
  }

  /**
   * Whether tuple contains exactly one column
   * and vt refers to that column or the tuple
   */
  private boolean isRefOfSingleColumnTuple(UVarTerm vt, UVar tuple) {
    final List<Value> schema = CalciteSupport.getValueListBySize(translator.getTupleVarSchema(tuple).size());
    if (schema.size() != 1) return false;
    final UVar v = vt.var();
    if (v.equals(tuple)) return true;
    final Value column = schema.get(0);
    return v.kind() == UVar.VarKind.PROJ
            && v.name().toString().equals(column.name())
            && v.isUsing(tuple);
  }

  /**
   * Check whether term is a table of x1.
   */
  private UTerm tableTermOf(UTerm term, UVar x1) {
    if (term instanceof UTable table && table.var().equals(x1))
      return term;
    return null;
  }

  /**
   * Check whether term is a [f(x1) op column].
   * If op is symmetric, [f(x1) op column] and [column op f(x1)] are both considered.
   * Return f(x1).
   */
  private UTerm isComparisonWith(UTerm term, UVar column, UVar x1, UPred.PredKind op) {
    if (term instanceof UPred pred && pred.isPredKind(op)) {
      if (pred.args().get(1) instanceof UVarTerm vt
              && vt.var().equals(column))
        if (pred.args().get(0).isUsing(x1))
          // [f(x1) op column]
          return pred.args().get(0);
      if ((op == UPred.PredKind.EQ || op == UPred.PredKind.NEQ)
              && pred.args().get(0) instanceof UVarTerm vt
              && vt.var().equals(column))
        if (pred.args().get(1).isUsing(x1))
          // [column op f(x1)]
          return pred.args().get(1);
    }
    return null;
  }

  /**
   * Check whether targetTerm and its children have a critical not null-safe pred that is using projVar.
   */
  private boolean isCriticalNotNullSafePredUsingProjVar(UTerm targetTerm, UVar projVar) {
    if (targetTerm.kind() == UKind.PRED) {
      final UPred targetPred = (UPred) targetTerm;
      if (!targetPred.nullSafe() && targetPred.isUsingProjVar(projVar)) return true;
    }

    // only SQUASH, MULTIPLY, SUMMATION maintain critical value.
    if (targetTerm.kind() == UKind.SQUASH
            || targetTerm.kind() == UKind.MULTIPLY
            || targetTerm.kind() == UKind.SUMMATION) {
      for (final UTerm subTerm : targetTerm.subTerms()) {
        if (isCriticalNotNullSafePredUsingProjVar(subTerm, projVar)) return true;
      }
    }

    return false;
  }

  /**
   * Check whether a prediction's arguments are not null.
   * NOTE: the argument's null property can be inferred by semantic of prediction.
   */
  private boolean isPredNotNullArguments(UPred pred) {
    if (pred.isPredKind(UPred.PredKind.EQ)) {
      if (any(pred.args(), a -> a.kind() == UKind.SUMMATION)) return true;
    }

    return false;
  }

  /**
   * Judge that whether a term is natural number.
   */
  private boolean isTermNaturalNumber(UTerm term) {
    switch (term.kind()) {
      case CONST, TABLE, PRED, NEGATION, SQUASH -> {
        return true;
      }
      case FUNC, VAR, STRING -> {
        return false;
      }
      case ADD, MULTIPLY, SUMMATION -> {
        return !any(term.subTerms(), t -> !isTermNaturalNumber(t));
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported natural number type: " + term.kind());
    }
  }

  /**
   * When a summation matches ∑{x1}(r(x1) * p(x1) * [f(x1) op column])
   * where r,p,f do not contain column,
   * return [r(x1), p(x1), f(x1)], otherwise [].
   * This function is used by matchMaxMinNotSquash.
   */
  private List<UTerm> extractFromMaxMinSummation(USum sum, UVar column, UPred.PredKind op) {
    final List<UTerm> result = new ArrayList<>();
    // only consider one bounded var case
    if (sum.boundedVars().size() > 1 || sum.body().kind() != UKind.MULTIPLY) return result;
    final UVar x1 = sum.boundedVars().iterator().next();
    final UMul multiply = (UMul) sum.body();
    UTerm table = null, pred = null, func = null;
    // find r, p, f, and unrelated terms
    for (UTerm factor : multiply.subTerms()) {
      // r(x1)
      UTerm tmp = tableTermOf(factor, x1);
      if (tmp != null && table == null) {
        table = tmp;
        continue;
      }
      // [f(x1) op column]
      tmp = isComparisonWith(factor, column, x1, op);
      if (tmp != null && func == null) {
        func = tmp;
        continue;
      }
      // p(x1)
      pred = pred == null ? factor : UMul.mk(pred, factor);
    }
    if (table != null && func != null && !func.isUsingProjVar(column)
            && (pred == null || !pred.isUsingProjVar(column))) {
      result.add(table);
      result.add(pred);
      result.add(func);
    }

    return result;
  }

  /**
   * Given column and x1,
   * when a summation matches ∑{x1,y1}(r(x1) * p(x1) * [f(x1) op column] * E1)
   * where r,p,f,E1 do not contain column,
   * r,p,f do not contain y1,
   * and E1 does not contain x1,
   * return [r(x1), p(x1), f(x1), E1], otherwise [].
   * This function is used by matchMaxMinNotSquash.
   */
  private List<UTerm> extractFromMaxMinSummation(USum sum, UVar column, UVar x1, UPred.PredKind op) {
    final List<UTerm> result = new ArrayList<>();
    if (!(sum.body() instanceof UMul mul)) return result;
    final List<UTerm> bodyX1 = filter(mul.subTerms(), t -> t.isUsing(x1));
    final List<UTerm> e1 = filter(mul.subTerms(), t -> !bodyX1.contains(t));
    // match r,p,f
    final USum sumToMatch = USum.mk(UVar.getBaseVars(x1), UMul.mk(bodyX1));
    final List<UTerm> matchX1 = extractFromMaxMinSummation(sumToMatch, column, op);
    // check
    if (matchX1.isEmpty()) return result;
    final Set<UVar> y1 = new HashSet<>(sum.boundedVars());
    y1.remove(x1);
    if (any(matchX1, t -> t == null || t.isUsingProjVar(column) || any(y1, t::isUsing))) return result;
    if (any(e1, t -> t.isUsingProjVar(column) || t.isUsing(x1))) return result;
    result.addAll(matchX1);
    result.add(UMul.mk(e1));
    return result;
  }

  /**
   * The function is used by simplifyMaxMinInSum normalization rule.
   * If a negation and a squash match the case in simplifyMaxMinInSum using the specified column,
   * return the squash summation result of the simplifyMaxMinInSum; otherwise return null.
   * <br/>Specifically,
   * Given "not(∑{x1}(r(x1) * p(x1) * [f(x1) >/< x]))",
   * "||∑{x1,y1}(r(x1) * p(x1) * [f(x1) = x] * E1)||" and x,
   * if r,p,f,E1 do not contain x,
   * r,p,f do not contain y1,
   * and E1 does not contain x1,
   * return
   * "||∑{x1,y1}(r(x1) * p(x1)) * E1||".
   */
  private UTerm matchMaxMinNotSquash(UNeg neg, USquash squash, UVar column) {
    List<UTerm> negMatches = extractFromMaxMinSummation((USum) neg.body(), column, UPred.PredKind.GT);
    if (negMatches.isEmpty())
      negMatches = extractFromMaxMinSummation((USum) neg.body(), column, UPred.PredKind.LT);
    if (negMatches.isEmpty()) return null;
    // when negMatches is not empty, the summation boundedVars size must be 1.
    final UVar negSumVar = ((USum) neg.body()).boundedVars().iterator().next();
    // try to match for each bound var of the squashed sum
    final USum squashSum = (USum) squash.body();
    for (final UVar squashSumVar : squashSum.boundedVars()) {
      final List<UTerm> squashMatches = extractFromMaxMinSummation(squashSum, column, squashSumVar, UPred.PredKind.EQ);
      if (squashMatches.isEmpty()) continue;
      // check whether tuple schemas match
      if (!CalciteSupport.isExactlyEqualTwoValueList(translator.getTupleVarSchema(negSumVar),
              translator.getTupleVarSchema(squashSumVar))) return null;
      final UVar newX1 = mkFreshBaseVar();
      // check whether their r, p, f are the same
      UTerm newTable = null, newPred = null;
      interface Replacer {
        UTerm replace(UTerm t, UVar o, UVar r);
      }
      Replacer replacer = (UTerm t, UVar o, UVar r) -> t.replaceVar(o, r, true);
      // r match
      if (!replacer.replace(negMatches.get(0), negSumVar, newX1).equals(
              replacer.replace(squashMatches.get(0), squashSumVar, newX1))) return null;
      newTable = replacer.replace(negMatches.get(0), negSumVar, newX1);
      // p match
      if (negMatches.get(1) != null && squashMatches.get(1) != null) {
        if (!replacer.replace(negMatches.get(1), negSumVar, newX1).equals(
                replacer.replace(squashMatches.get(1), squashSumVar, newX1))) return null;
        newPred = replacer.replace(negMatches.get(1), negSumVar, newX1);
      }
      // f match
      if (!replacer.replace(negMatches.get(2), negSumVar, newX1).equals(
              replacer.replace(squashMatches.get(2), squashSumVar, newX1))) return null;

      // replace vars (y1) in E1
      final Set<UVar> newBoundedVars = new HashSet<>();
      UTerm newE1 = squashMatches.get(3);
      for (final UVar var : squashSum.boundedVars()) {
        if (!var.equals(squashSumVar)) {
          // for each var in y1, register a fresh var and schema
          final UVar newVar = mkFreshBaseVar();
          newE1 = replacer.replace(newE1, var, newVar);
          newBoundedVars.add(newVar);
          translator.putTupleVarSchema(newVar, translator.getTupleVarSchema(var));
        }
      }

      // construct the new term
      newBoundedVars.add(newX1);
      translator.putTupleVarSchema(newX1, translator.getTupleVarSchema(negSumVar));
      final USum resultSum = USum.mk(newBoundedVars, newPred != null ? UMul.mk(newTable, newPred) : UMul.mk(newTable));
      return USquash.mk(UMul.mk(resultSum, newE1));
    }

    return null;
  }

  /**
   * Try to apply simplifyMaxMinInSum concerning one column.
   * @param bvs the bound var set of summation; this method modifies it
   * @param factors the factor list representing the summation body; this method modifies it
   * @param column the column, in the form "$i(x)"
   */
  private void simplifyMaxMinInSumByOneColumn(Set<UVar> bvs, List<UTerm> factors, UVar column) {
    // terms can be partitioned into negations, squashes, and unrelated terms.
    final List<UTerm> negations = filter(factors, t -> t instanceof UNeg neg
            && neg.body().kind() == UKind.SUMMATION);
    final List<UTerm> squashes = filter(factors, t -> t instanceof USquash squash
            && squash.body().kind() == UKind.SUMMATION);
    final List<UTerm> unrelatedTerms = filter(factors, t -> !negations.contains(t) && !squashes.contains(t));
    // unrelated terms should not use $i(x).
    if (any(unrelatedTerms, t -> t.isUsingProjVar(column))) return;
    // $i(x) should appear in exactly one negation and one squash
    final List<UTerm> concernedNegations = filter(negations, t -> t.isUsingProjVar(column));
    if (concernedNegations.size() != 1) return;
    final List<UTerm> concernedSquashes = filter(squashes, t -> t.isUsingProjVar(column));
    if (concernedSquashes.size() != 1) return;
    // match pattern
    final UNeg negation = (UNeg) concernedNegations.get(0);
    final USquash squash = (USquash) concernedSquashes.get(0);
    final UTerm match = matchMaxMinNotSquash(negation, squash, column);
    if (match == null) return;
    // modify summation
    isModified = true;
    factors.remove(negation);
    factors.remove(squash);
    factors.add(match);
    factors.replaceAll(t -> removeSingleColumn(t, column));
    // update schema and column indices
    final UVar tuple = column.args()[0];
    if (translator.getTupleVarSchema(tuple).size() == 1) {
      // the only remaining column is removed; remove the bound var
      // updating schema is unnecessary
      bvs.remove(tuple);
    } else {
      // update schema
      final int index = columnNameToIndex(column.name().toString());
      translator.deleteTupleVarSchemaByIndexs(tuple, List.of(index));
    }
  }

  /**
   * Check whether bigSum contains smallSum.
   */
  private boolean containSubTerms(USum bigSum, USum smallSum) {
    final UTerm bigBody = bigSum.body().copy();
    final UTerm smallBody = smallSum.body().copy();
    final HashSet<UVar> bigBoundVars = new HashSet<>(bigSum.boundedVars());
    final ArrayList<UVar> smallBoundVars = new ArrayList<>(smallSum.boundedVars());

    if (bigBoundVars.size() < smallBoundVars.size()) return false;
    if (bigSum.equals(smallSum)) return true;

    return checkSubSum(0, bigBoundVars, bigBody, smallBoundVars, smallBody);
  }

  /**
   * This is a recursive function, which replaces the boundedVar and check their equivalency.
   */
  private boolean checkSubSum(
          int cur, HashSet<UVar> bigBoundVars, UTerm bigBody, ArrayList<UVar> smallBoundVars, UTerm smallBody) {

    // check whether all terms of smallBody is included by bigBody
    if (cur == smallBoundVars.size()) {
      for (UTerm smallTerm : smallBody.subTerms()) {
        if (bigBody.subTerms().contains(smallTerm))
          continue;
        if (smallTerm.kind() == UKind.PRED) {
          UPred pred = (UPred) smallTerm;
          if (pred.isPredKind(UPred.PredKind.EQ)) {
            final UTerm op0 = pred.args().get(0);
            final UTerm op1 = pred.args().get(1);
            final NaturalCongruence<UTerm> congruence = NaturalCongruence.mk();
            UExprSupport.getEqCongruenceRecursive(bigBody, p -> true, congruence, bigBody, false);
            if (congruence.isCongruent(op0, op1)) continue;
          }
        }
        if (isNotNullPred(smallTerm)) {
          final UPred pred = (UPred) smallTerm.subTerms().get(0);
          final NaturalCongruence<UTerm> congruence = NaturalCongruence.mk();
          UExprSupport.getEqCongruenceRecursive(bigBody, p -> true, congruence, bigBody, false);
          Set<UTerm> eqTerms = congruence.eqClassOf(pred.args().get(0));
          if (any(eqTerms,
                  t -> bigBody.subTerms().contains(UNeg.mk(mkIsNullPred(t)))))
            continue;
        }
        return false;
      }
      return true;
    }

    final UVar curVar = smallBoundVars.get(cur);
    final UVar newVar = mkFreshBaseVar();
    translator.putTupleVarSchema(newVar, translator.getTupleVarSchema(curVar));
    smallBody = smallBody.replaceVar(curVar, newVar, true);

    for (final UVar v : bigBoundVars) {
      if (!isExactlyEqualTwoValueList(translator.getTupleVarSchema(v), translator.getTupleVarSchema(newVar))) continue;
      final HashSet<UVar> tmpVars = new HashSet<>(bigBoundVars);
      tmpVars.remove(v);
      final UTerm tmpBigBody = bigBody.replaceVar(v, newVar, true);
      final UTerm tmpSmallBody = smallBody.replaceVar(v, newVar, false);
      final boolean result = checkSubSum(cur + 1, tmpVars, tmpBigBody, smallBoundVars, tmpSmallBody);
      if (result) return true;
    }
    return false;
  }

  /**
   * ||sum{x}(r(x)*f(x)*[$i(x)>e]*not(sum{y}(r(y)*f(y)*g(x,y)*[$i(y)>$i(x)])))||
   * ->
   * ||sum{x}(r(x)*f(x)*[$i(x)>e])||,
   * where r is a table, and f,g return natural numbers.
   * [$i(x)>e] is optional.
   */
  private UTerm removeNegatedSumByMaxMin(UTerm term, boolean isUnderSet) {
    // update the "isUnderSet" flag
    final boolean isUnderSetFinal;
    if (term instanceof UPred || term instanceof UFunc) {
      isUnderSetFinal = false;
    } else if (term.kind().isUnary()) {
      isUnderSetFinal = true;
    } else {
      isUnderSetFinal = isUnderSet;
    }
    // recursion
    term = transformSubTerms(term, t -> removeNegatedSumByMaxMin(t, isUnderSetFinal));

    // current
    if (!isUnderSetFinal
            || !(term instanceof USum sumX)
            || !(sumX.body() instanceof UMul bodyX))
      return term;
    // compute current level congruence
    final NaturalCongruence<UTerm> eqClasses = NaturalCongruence.mk();
    for (UTerm factor : bodyX.subTerms()) {
      if (factor instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) {
        eqClasses.putCongruent(pred.args().get(0), pred.args().get(1));
      }
    }

    // helpful data structure
    // colName: $i; op: >/<
    record Cmp(String colName, UPred.PredKind op) {
      static final Set<UPred.PredKind> CMP_OPS = Set.of(
              UPred.PredKind.GT, UPred.PredKind.GE,
              UPred.PredKind.LT, UPred.PredKind.LE
      );
      static final Set<UPred.PredKind> CMP_OPS_STRICT = Set.of(
              UPred.PredKind.GT, UPred.PredKind.LT
      );
      static Cmp mk(UPred pred, UVar x) {
        // [$i(x) >/>=/</<= c]
        return mk(pred, false, (l, r) -> {
          final String iX = getColName(l, x);
          if (iX != null && !r.isUsing(x)) return iX;
          return null;
        });
      }
      static Cmp mk(UPred pred, UVar y, UVar x) {
        // [$i(y) >/< $i(x)]
        return mk(pred, true, (l, r) -> {
          final String iY = getColName(l, y);
          final String iX = getColName(r, x);
          if (iY != null && iY.equals(iX)) return iY;
          return null;
        });
      }
      static Cmp mk(UPred pred, boolean strictPredKind, BiFunction<UTerm, UTerm, String> matchesLeftRight) {
        // [matchesLeft >/< matchesRight]
        final UPred.PredKind predKind = pred.predKind();
        if (!CMP_OPS.contains(predKind)) return null;
        if (strictPredKind && !CMP_OPS_STRICT.contains(predKind)) return null;
        final UTerm arg0 = pred.args().get(0);
        final UTerm arg1 = pred.args().get(1);
        String colName = matchesLeftRight.apply(arg0, arg1);
        if (colName != null) return new Cmp(colName, toGTLT(predKind));
        colName = matchesLeftRight.apply(arg1, arg0);
        if (colName != null) return new Cmp(colName, toGTLTInverse(predKind));
        return null;
      }
      static UPred.PredKind toGTLT(UPred.PredKind kind) {
        switch (kind) {
          case GT, GE -> { return UPred.PredKind.GT; }
          case LT, LE -> { return UPred.PredKind.LT; }
        }
        return null;
      }
      static UPred.PredKind toGTLTInverse(UPred.PredKind kind) {
        switch (kind) {
          case GT, GE -> { return UPred.PredKind.LT; }
          case LT, LE -> { return UPred.PredKind.GT; }
        }
        return null;
      }
      // if term is $i(x), return $i
      static String getColName(UTerm term, UVar x) {
        if (term instanceof UVarTerm vt
                && vt.var().kind() == UVar.VarKind.PROJ
                && vt.var().isUsing(x))
          return vt.var().name().toString();
        return null;
      }
    }

    // for each bound var x, recognize related part of the summation body
    outer:
    for (UVar x : sumX.boundedVars()) {
      final List<Value> schemaX = translator.getTupleVarSchema(x);
      final List<UTerm> newFactors = new ArrayList<>(bodyX.subTerms());
      UVar y = null;
      UTable rX = null;
      final List<UTerm> fX = new ArrayList<>();
      final Set<Cmp> cmpsX = new HashSet<>(); // [$i(x)>/>=/</<=c]
      UTable rY = null;
      final List<UTerm> fgY = new ArrayList<>(); // f(y)/g(x,y)
      final Set<Cmp> cmpsY = new HashSet<>(); // [$i(y)>/<$i(x)]
      // classify each factor involving x in sum{x} body
      for (UTerm factor : filter(bodyX.subTerms(), t -> t.isUsing(x))) {
        if (factor instanceof UTable table) {
          // r(x)
          rX = table;
        } else if (factor instanceof UPred pred) {
          // [$i(x)>/>=/</<=c]
          // or other predicates (belong to f(x))
          final Cmp cmp = Cmp.mk(pred, x);
          if (cmp != null) cmpsX.add(cmp);
          else fX.add(factor);
        } else if (factor.kind().isUnary()
                || factor instanceof UConst cst && cst.value() >= 0) {
          // not(sum{y})
          // or part of f(x)
          if (factor instanceof UNeg neg
                  && neg.body() instanceof USum sumY
                  && sumY.boundedVars().size() == 1
                  && sumY.body() instanceof UMul bodyY) {
            if (y != null) continue outer;
            // not(sum{y}(r(y)*f(y)*g(x,y)*[$i(y)>/<$i(x)]))
            y = sumY.boundedVars().stream().toList().get(0);
            // check tuple schema
            final List<Value> schemaY = translator.getTupleVarSchema(y);
            if (!isExactlyEqualTwoValueList(schemaX, schemaY))
              // x,y have different schemas; not match the rule
              continue outer;
            // classify each factor in sum{y} body
            for (UTerm factorY : bodyY.subTerms()) {
              if (factorY instanceof UTable table) {
                // r(y)
                // or other table terms (belong to f(y)/g(x,y))
                if (table.isUsing(y)) rY = table;
                else fgY.add(factorY);
              } else if (factorY instanceof UPred pred) {
                // [$i(y)>/<$i(x)]
                // or other predicates (belong to f(y)/g(x,y))
                final Cmp cmp = Cmp.mk(pred, y, x);
                if (cmp != null) cmpsY.add(cmp);
                else fgY.add(factorY);
              } else if (factorY.kind().isUnary()
                      || factorY instanceof UConst cst && cst.value() >= 0) {
                // part of f(y)/g(x,y)
                fgY.add(factorY);
              } else {
                // not match the rule
                continue outer;
              }
            }
            newFactors.remove(factor);
          } else {
            fX.add(factor);
          }
        } else {
          // not match the rule
          continue outer;
        }
      }
      // if a required part is absent, skip
      if (y == null || rX == null || rY == null) continue;
      // check if r(x),r(y) match
      if (!rX.tableName().equals(rY.tableName())) continue;
      // check if cmp columns and OPs match
      // hack: allow at most one [$i(x)>/>=/</<=c]
      //   and exactly one [$i(y)>/<$i(x)]
      if (cmpsX.size() > 1 || cmpsY.size() != 1) continue;
      if (cmpsX.size() == 1) {
        final Cmp cmpX = cmpsX.stream().toList().get(0);
        final Cmp cmpY = cmpsY.stream().toList().get(0);
        if (!cmpX.equals(cmpY)) continue;
      }
      // check if fX[x->y] is part of fgY
      // t[x->y] means replacing x in t with y
      final UVar yFinal = y;
      final List<UTerm> fXRenamed = map(fX, t -> t.replaceVar(x, yFinal, false));
      if (!contains(fgY, fXRenamed, eqClasses)) continue;
      // modify here
      // remove not(sum{y})
      isModified = true;
      final Set<UVar> newBVs = new HashSet<>(sumX.boundedVars());
      final UTerm newBody = UMul.mk(newFactors);
      return USum.mk(newBVs, newBody);
    }
    return term;
  }

  /**
   * Whether all terms in terms2 appear in terms1
   * w.r.t the specified congruence.
   */
  private boolean contains(List<UTerm> terms1, List<UTerm> terms2, NaturalCongruence<UTerm> congruence) {
    final List<UTerm> l1 = map(terms1, t -> {
      final UTerm result = t.copy();
      result.sortCommAssocItems();
      return result;
    });
    final List<UTerm> l2 = map(terms2, t -> {
      final UTerm result = t.copy();
      result.sortCommAssocItems();
      return result;
    });
    outer:
    for (UTerm term2 : terms2) {
      for (UTerm term1 : terms1) {
        if (term2 instanceof UPred pred2 && pred2.isBinaryPred()
                && term1 instanceof UPred pred1 && pred1.isBinaryPred()
                && pred2.predKind() == pred1.predKind()) {
          final UPred.PredKind predKind = pred2.predKind();
          final UTerm arg20 = pred2.args().get(0);
          final UTerm arg21 = pred2.args().get(1);
          final UTerm arg10 = pred1.args().get(0);
          final UTerm arg11 = pred1.args().get(1);
          if (predKind == UPred.PredKind.EQ || predKind == UPred.PredKind.NEQ) {
            // symmetric predicate
            if (congruence.isCongruent(arg10, arg20) && congruence.isCongruent(arg11, arg21)
                    || congruence.isCongruent(arg11, arg20) && congruence.isCongruent(arg10, arg21))
              // term2 is found in terms1
              continue outer;
          } else {
            // asymmetric predicate
            if (congruence.isCongruent(arg10, arg20)
                    && congruence.isCongruent(arg11, arg21))
              // term2 is found in terms1
              continue outer;
          }
        } else {
          if (term2.equals(term1))
            // term2 is found in terms1
            continue outer;
        }
      }
      // term2 is not found in terms1
      return false;
    }
    return true;
  }

  /**
   * not(sum{x}f(x)) * (...sum(... sum{y,...}(f(y)*g(y)) ...)...)
   * ->
   * not(sum{x}f(x)) * (...sum(... 0 ...)...)
   */
  private UTerm removeSumByContainment(UTerm term, Set<UTerm> ctx) {
    // update "critical" context and transform subterms
    if (term instanceof UMul mul) {
      final List<UTerm> factors = mul.subTerms();
      term = transformSubTerms(term, t -> {
        final Set<UTerm> newCtx = new HashSet<>(ctx);
        newCtx.addAll(filter(factors, factor -> {
          if (factor.equals(t)) return false;
          if (!(factor instanceof UNeg neg)) return false;
          return neg.body() instanceof USum sum
                  && sum.body() instanceof UMul
                  && sum.boundedVars().size() == 1;
        }));
        return removeSumByContainment(t, newCtx);
      });
    } else {
      // only copy on write
      term = transformSubTerms(term, t -> removeSumByContainment(t, ctx));
    }

    // check type of the current term
    if (!(term instanceof USum sumY)
            || !(sumY.body() instanceof UMul bodyY))
      return term;

    for (UVar y : sumY.boundedVars()) {
      final List<Value> schemaY = translator.getTupleVarSchema(y);

      // find the target "not(sum{x})" in ctx
      for (UTerm candidate : ctx) {
        final USum sumX = (USum) ((UNeg) candidate).body();
        final UVar x = sumX.boundedVars().stream().toList().get(0);
        // check tuple schema
        final List<Value> schemaX = translator.getTupleVarSchema(x);
        if (!isExactlyEqualTwoValueList(schemaX, schemaY))
          continue;
        // whether bodyY contains bodyX[x->y]
        final UMul bodyX = (UMul) sumX.body();
        final Bag<UTerm> factorsX = new Bag<>(map(bodyX.subTerms(), t -> t.replaceVar(x, y, false)));
        final Bag<UTerm> factorsY = new Bag<>(bodyY.subTerms());
        if (factorsY.containsAll(factorsX)) {
          // modify here
          isModified = true;
          return UConst.zero();
        }
      }
    }

    return term;
  }

  /**
   * Reduce redundant unary summation's table terms.
   */
  private UTerm reduceRedundantTables(UTerm term) {
    if (term.kind() == UKind.MULTIPLY) {
      final List<UTerm> tableSubTerms = filter(term.subTerms(), t -> t.kind() == UKind.TABLE);
      final List<UTerm> newTableSubTerms = new ArrayList<>();
      final List<UTerm> notTableSubTerms = filter(term.subTerms(), t -> t.kind() != UKind.TABLE);
      for (final UTerm tableSubTerm : tableSubTerms) {
        if (any(newTableSubTerms, t -> t.equals(tableSubTerm))) continue;
        newTableSubTerms.add(tableSubTerm);
      }
      term = UMul.mk(concat(newTableSubTerms, notTableSubTerms));
    }

    final List<UTerm> newSubTerms = new ArrayList<>();
    for (final UTerm subTerm : term.subTerms()) {
      newSubTerms.add(reduceRedundantTables(subTerm));
    }
    term = remakeTerm(term, newSubTerms);

    return term;
  }

  /**
   * Replace the inequality term with 0 according to the congruence
   */
  private UTerm replaceInequalityWithZero(UTerm context, NaturalCongruence<UTerm> congruence) {
    if (context instanceof UPred pred
            && (pred.isPredKind(UPred.PredKind.GT)
                || pred.isPredKind(UPred.PredKind.LT))) {
      assert pred.args().size() == 2;
      if (congruence.eqClassOf(pred.args().get(0)).contains(pred.args().get(1))) {
        isModified = true;
        return UConst.zero();
      }
    }

    final List<UTerm> newSubTerms = new ArrayList<>();

    for (final UTerm subTerm : context.subTerms()) {
      newSubTerms.add(replaceInequalityWithZero(subTerm, congruence));
    }

    return remakeTerm(context, newSubTerms);
  }

  /*
   * Normalization rules.
   */

  /**
   * Transform all notNull(expr) / isNull(expr) according expr's type.
   */
  private UTerm transformNulls(UTerm expr) {
    expr = transformSubTerms(expr, this::transformNulls);
    if (!isNotNullPred(expr) && !isNullPred(expr))
      return expr;

    // this flag indicates that the expr's type: notnull or isnull.
    boolean isNull = isNullPred(expr);

    final UPred pred = (UPred) (isNull ? expr : expr.subTerms().get(0));
    assert pred.args().size() == 1;
    final UTerm arg = pred.args().get(0);

    switch (arg.kind()) {
      case CONST, STRING, TABLE, PRED, FUNC, SUMMATION, NEGATION, SQUASH -> {
        isModified = true;
        // special case for null const
        if (arg.equals(UConst.nullVal())) {
          if (isNull)
            return UConst.one();
          return UConst.zero();
        }
        if (isNull)
          return UConst.zero();
        return UConst.one();
      }
      case MULTIPLY, ADD -> {
        isModified = true;
        final List<UTerm> subTerms = arg.subTerms();
        final List<UTerm> newSubTerms = new ArrayList<>();
        if (isNull) {
          all(subTerms, t -> newSubTerms.add(mkIsNullPred(t)));
        } else {
          all(subTerms, t -> newSubTerms.add(mkNotNullPred(t)));
        }
        if (all(subTerms, t1 -> all(subTerms, t1::equals))) {
          return newSubTerms.get(0);
        }
        if (isNull) {
          return USquash.mk(UAdd.mk(newSubTerms));
        }
        return UMul.mk(newSubTerms);
      }
    }

    return expr;
  }

  /**
   * Remove Nulls by different rules.
   */
  private UTerm removeNulls(UTerm expr) {
    expr = removeNullPredInMulByPred(expr);
    expr = removeNullPredInMulCriticalSubTermsByPred(expr);
    expr = removeNullPredInMulRecursiveTerms(expr);
    return expr;
  }

  /**
   * Remove all isNull and notNull in multiply if there exists a predication whose nullSafe is false.
   * e.g. notNull(a(x)) * [a(x) = 1] where [a(x) = 1] is not null-safe, notNull(a) is equal to 1.
   */
  private UTerm removeNullPredInMulByPred(UTerm expr) {
    expr = transformSubTerms(expr, this::removeNullPredInMulByPred);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    final List<UTerm> notNullSafePreds = filter(expr.subTerms(), t -> t.kind() == UKind.PRED && !((UPred) t).nullSafe());

    for (int i = 0; i < subTerms.size(); i++) {
      final UTerm nullRelatedTerm = subTerms.get(i);

      UPred isNull = null;
      if (isNullPred(nullRelatedTerm)) {
        // isNullPred case
        isNull = (UPred) nullRelatedTerm;
      } else if (isNotNullPred(nullRelatedTerm)) {
        // isNotNullPred case
        isNull = (UPred) nullRelatedTerm.subTerms().get(0);
      }

      if (isNull == null) continue;
      assert isNull.args().size() == 1;
      if (isNull.args().get(0).kind() != UKind.VAR) continue;
      final UVar targetVar = ((UVarTerm) isNull.args().get(0)).var();
      if (!targetVar.is(UVar.VarKind.PROJ)) continue;

      if (any(notNullSafePreds, t -> t.isUsingProjVar(targetVar))) {
        // match the case.
        if (isNullPred(nullRelatedTerm)) {
          subTerms.set(i, UConst.zero());
        } else {
          subTerms.set(i, UConst.one());
        }
      }
    }

    return UMul.mk(subTerms);
  }

  /**
   * Remove all isNull and notNull in multiply and critical sub-terms if there exists a predication whose nullSafe is false.
   * e.g. notnull(a) * || [a=b] || => notnull(a) = 1
   */
  private UTerm removeNullPredInMulCriticalSubTermsByPred(UTerm expr) {
    expr = transformSubTerms(expr, this::removeNullPredInMulCriticalSubTermsByPred);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    final List<UTerm> nullNotRelatedTerms = filter(expr.subTerms(), t -> !isNullPred(t) && !isNotNullPred(t));

    for (int i = 0; i < subTerms.size(); i++) {
      final UTerm nullRelatedTerm = subTerms.get(i);

      UPred isNull = null;
      if (isNullPred(nullRelatedTerm)) {
        // isNullPred case
        isNull = (UPred) nullRelatedTerm;
      } else if (isNotNullPred(nullRelatedTerm)) {
        // isNotNullPred case
        isNull = (UPred) nullRelatedTerm.subTerms().get(0);
      }

      if (isNull == null) continue;
      assert isNull.args().size() == 1;
      if (isNull.args().get(0).kind() != UKind.VAR) continue;
      final UVar targetVar = ((UVarTerm) isNull.args().get(0)).var();
      if (!targetVar.is(UVar.VarKind.PROJ)) continue;

      if (any(nullNotRelatedTerms, t -> isCriticalNotNullSafePredUsingProjVar(t, targetVar))) {
        // match the case.
        if (isNullPred(nullRelatedTerm)) {
          subTerms.set(i, UConst.zero());
        } else {
          subTerms.set(i, UConst.one());
        }
      }
    }

    return UMul.mk(subTerms);
  }

  /**
   * Remove all isNull and notNull in multiply and critical sub-terms if there exists a predication that implies null value.
   * e.g. [a = sum] or [a = b] and not null-safe  * || notnull(a) || => [a = sum] or [a = b] and not null-safe
   */
  private UTerm removeNullPredInMulRecursiveTerms(UTerm expr) {
    expr = transformSubTerms(expr, this::removeNullPredInMulRecursiveTerms);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();

    for (final UTerm subTerm : subTerms) {
      if (subTerm instanceof UPred pred && isPredNotNullArguments(pred)) {
        // all the pred arguments are not null
        // recursively remove not null of these arguments
        for (final UTerm arg : pred.args()) {
          replaceTermRecursiveCritical(expr, mkNotNullPred(arg), UConst.one());
          replaceTermRecursiveCritical(expr, mkIsNullPred(arg), UConst.zero());
        }
      }
    }

    return UMul.mk(subTerms);
  }

  /**
   * Remove determined bounded var by different rules.
   */
  private UTerm removeDeterminedBoundedVar(UTerm expr) {
    expr = removeDeterminedBoundedVarByConst(expr);
    expr = removeDeterminedBoundedVarByTuple(expr);
    return expr;
  }

  /**
   * For a summation \sum{..., x, ...}, if x is determined, then x can be removed and its column can be replaced.
   * determined: For every column i of x, if it exists a term like [$i(x) = f()], where f is not related to x,
   * then x can be removed and $i(x) can be replaced by f().
   * e.g. \sum{x, y} {[$0(x) = 1] * [$1(x) = $0(y)]}
   * Only consider the summation and its sub-term must be multiplication.
   */
  private UTerm removeDeterminedBoundedVarByConst(UTerm expr) {
    expr = transformSubTerms(expr, this::removeDeterminedBoundedVarByConst);
    if (expr.kind() != UKind.SUMMATION || expr.subTerms().get(0).kind() != UKind.MULTIPLY) return expr;

    final USum summation = (USum) expr;
    UMul multiply = (UMul) expr.subTerms().get(0);
    final NaturalCongruence<UTerm> congruence = getEqIsNullCongruenceInTermsOfMul(multiply, pred -> true);
    final List<UTerm> tableTerms = filter(multiply.subTerms(), t -> t.kind() == UKind.TABLE);

    for (final UVar boundedVar : summation.boundedVars()) {
      final List<Value> varIndexSchema = getValueListBySize(translator.getTupleVarSchema(boundedVar).size());
      final Map<UVar, UTerm> replaceMap = new HashMap<>();

      // check whether every column of boundedVar has an equal constant.
      for (final Value varIndex : varIndexSchema) {
        final UVar projVar = translator.mkProjVar(varIndex, boundedVar);
        final Set<UTerm> eqTerms = congruence.eqClassOf(UVarTerm.mk(projVar));
        UTerm chooseTerm = null;
        // heuristically pick one term that is equivalent to projVar and is not using boundedVar
        for (final UTerm eqTerm : eqTerms) {
          if (!eqTerm.isUsing(boundedVar)) {
            chooseTerm = eqTerm;
            break;
          }
        }
        if (chooseTerm != null)
          replaceMap.put(projVar, chooseTerm);
      }

      // if a var can be removed, it must not be in a table.
      if (any(tableTerms, t -> t.isUsing(boundedVar))) continue;

      // if a var can be removed, then the replaceMap size is equal to its schema size.
      if (varIndexSchema.size() != replaceMap.size()) continue;

      boolean successReplace = true;
      UTerm tempMultiply = multiply.copy();
      for (final Value varIndex : varIndexSchema) {
        final UVar projVar = translator.mkProjVar(varIndex, boundedVar);
        tempMultiply = tempMultiply.replaceAtomicTerm(UVarTerm.mk(projVar), replaceMap.get(projVar));
        if (replaceMap.get(projVar).equals(UConst.nullVal())) {
          tempMultiply = transformNullTerm(tempMultiply);
          if (tempMultiply == null) {
            successReplace = false;
            break;
          }
        }
      }
      if (!successReplace) continue;
      multiply = UMul.mk(tempMultiply);

      // remove this boundedVar and replace every column of it.
      final Set<UVar> newBoundedVars = summation.boundedVars();
      newBoundedVars.remove(boundedVar);


      isModified = true;
      if (newBoundedVars.isEmpty()) {
        return multiply;
      }
      return USum.mk(newBoundedVars, multiply);
    }

    return expr;
  }

  /**
   * For a summation \sum{..., x, ...}, if x is determined by another var y, then x can be replaced by y.
   * determined by another var: For every column i of x, if it exists a term like [$i(x) = $i(y)], then x can be replaced by y.
   * e.g. \sum{x, y} {[$0(x) = $0(y)] * [$1(x) = $1(y)] * T(x)} -> \sum{y} {[$0(y) = $0(y)] * [$1(y) = $1(y)] * T(y)}
   * Only consider the summation and its sub-term must be multiplication.
   */
  private UTerm removeDeterminedBoundedVarByTuple(UTerm expr) {
    expr = transformSubTerms(expr, this::removeDeterminedBoundedVarByTuple);
    if (expr.kind() != UKind.SUMMATION || expr.subTerms().get(0).kind() != UKind.MULTIPLY) return expr;

    final USum summation = (USum) expr;
    UMul multiply = (UMul) expr.subTerms().get(0);
    final NaturalCongruence<UTerm> congruence = getEqIsNullCongruenceInTermsOfMul(multiply, pred -> true);

    for (final UVar boundedVar : summation.boundedVars()) {
      final List<Value> varIndexSchema = getValueListBySize(translator.getTupleVarSchema(boundedVar).size());
      final List<UVar> projVars = map(varIndexSchema, v -> translator.mkProjVar(v, boundedVar));
      final List<Set<UTerm>> tupleEqualSet = map(projVars, v -> congruence.eqClassOf(UVarTerm.mk(v)));
      // tupleEqualSet size should be same to varIndexSchema.
      if (tupleEqualSet.size() != varIndexSchema.size()) continue;
      final Set<UTerm> firstEqualSet = tupleEqualSet.get(0);
      for (final UTerm firstEqualTerm : firstEqualSet) {
        // check whether firstEqualTerm is a matched var.
        if (firstEqualTerm.kind() != UKind.VAR) continue;
        final UVar firstEqualVar = ((UVarTerm) firstEqualTerm).var();
        if (firstEqualVar.kind() != UVar.VarKind.PROJ || !firstEqualVar.isUnaryVar()) continue;
        final UVar firstBaseVar = firstEqualVar.args()[0];
        if (firstBaseVar.equals(boundedVar)) continue;
        if (!isEqualTwoValueList(translator.getTupleVarSchema(firstBaseVar), translator.getTupleVarSchema(boundedVar)))
          continue;
        if (Objects.equals(firstEqualVar.name().toString(), varIndexSchema.get(0).name())) {
          boolean hasTarget = false;
          for (int i = 1; i < varIndexSchema.size(); i++) {
            final Set<UTerm> curEqualSet = tupleEqualSet.get(i);
            hasTarget = false;
            for (final UTerm curEqualTerm : curEqualSet) {
              if (curEqualTerm.kind() != UKind.VAR) continue;
              final UVar curEqualVar = ((UVarTerm) curEqualTerm).var();
              if (curEqualVar.isUsing(firstBaseVar)
                      && Objects.equals(curEqualVar.name().toString(), varIndexSchema.get(i).name())) {
                hasTarget = true;
                break;
              }
            }
            if (!hasTarget) break;
          }
          // should modify here.
          if (hasTarget) {
            isModified = true;
            summation.removeBoundedVarForce(boundedVar);
            if (summation.boundedVars().isEmpty())
              return summation.body().replaceVar(boundedVar, firstBaseVar, false);
            return summation.replaceVar(boundedVar, firstBaseVar, false);
          }
        }
      }

    }

    return expr;
  }

  /**
   * Remove determined bounded var's column by different rules.
   */
  private UTerm removeDeterminedBoundedColumn(UTerm expr) {
    expr = removeDeterminedBoundedColumnByConst(expr);
    return expr;
  }

  /**
   * For a summation \sum{..., x, ...}, if x's one column $i determined,
   * then x.$i can be removed, and it can be replaced.
   */
  private UTerm removeDeterminedBoundedColumnByConst(UTerm expr) {
    expr = transformSubTerms(expr, this::removeDeterminedBoundedColumnByConst);
    if (expr.kind() != UKind.SUMMATION || expr.subTerms().get(0).kind() != UKind.MULTIPLY) return expr;

    final USum summation = (USum) expr;
    UTerm multiply = expr.subTerms().get(0);
    final NaturalCongruence<UTerm> congruence = NaturalCongruence.mk();
    getEqCongruenceRecursive(multiply, pred -> true, congruence, multiply, true);
    final List<UTerm> tableTerms = new ArrayList<>();
    getTargetUExprRecursive(multiply, t -> t.kind() == UKind.TABLE, tableTerms, multiply);
    final Set<UVar> recurBoundedVars = new HashSet<>();
    getBoundedVarsRecursive(multiply, recurBoundedVars, multiply);

    for (final UVar boundedVar : summation.boundedVars()) {
      final List<Value> varIndexSchema = getValueListBySize(translator.getTupleVarSchema(boundedVar).size());
      final Map<UVar, List<UTerm>> replaceMap = new HashMap<>();

      // if a var can be removed, it must not be in a table.
      if (any(tableTerms, t -> t.isUsing(boundedVar))) continue;

      // check whether any column of boundedVar has an equal constant.
      for (final Value varIndex : varIndexSchema) {
        final UVar projVar = translator.mkProjVar(varIndex, boundedVar);
        final Set<UTerm> eqTerms = congruence.eqClassOf(UVarTerm.mk(projVar));
        // heuristically pick one term that is equivalent to projVar and is not using boundedVar
        for (final UTerm eqTerm : eqTerms) {
          if (!eqTerm.isUsingProjVar(projVar)) {
            // make sure eqTerm is const related to boundedVar
            if (any(recurBoundedVars, eqTerm::isUsing)) {
              continue;
            }
            if (!replaceMap.containsKey(projVar)) replaceMap.put(projVar, new ArrayList<>());
            replaceMap.get(projVar).add(eqTerm);
            break;
          }
        }
      }

      // if a var's columns can be removed, then the replaceMap size should be greater than 0
      if (replaceMap.isEmpty()) continue;

      final UVar newBoundedVar = mkFreshBaseVar();
      translator.putTupleVarSchema(newBoundedVar, translator.getTupleVarSchema(boundedVar));
      final Set<UVar> newBoundedVars = summation.boundedVars();
      newBoundedVars.remove(boundedVar);
      newBoundedVars.add(newBoundedVar);

      // change schema of var and U-expression
      int deleteCount = 0;
      List<Integer> deleteIndexs = new ArrayList<>();
      for (final Value varIndex : varIndexSchema) {
        final int index = columnNameToIndex(varIndex.name());
        final UVar projVar = translator.mkProjVar(varIndex, boundedVar);
        if (replaceMap.containsKey(projVar)) {
          // should delete this column
          boolean find = false;
          for (final UTerm replaceTerm : replaceMap.get(projVar)) {
            if (replaceTerm.isUsingProjVar(projVar)) continue;
            if (replaceTerm.equals(UConst.nullVal())) {
              final UTerm newMultiply = transformNullTerm(multiply.replaceAtomicTerm(UVarTerm.mk(projVar), replaceTerm));
              // when newMultiply is not null in transformNulls, it can be applied
              if (newMultiply == null) continue;
              multiply = newMultiply;
              for (final UVar key : replaceMap.keySet()) {
                if (key.equals(projVar)) continue;
                replaceMap.put(key, map(replaceMap.get(key), t ->
                        transformNullTerm(t.replaceAtomicTerm(UVarTerm.mk(projVar), replaceTerm))));
              }
              deleteIndexs.add(index);
              deleteCount++;
              find = true;
              break;
            }

            multiply = multiply.replaceAtomicTerm(UVarTerm.mk(projVar), replaceTerm);
            for (final UVar key : replaceMap.keySet()) {
              if (key.equals(projVar)) continue;
              replaceMap.put(key, map(replaceMap.get(key), t ->
                      t.replaceAtomicTerm(UVarTerm.mk(projVar), replaceTerm)));
            }
            deleteIndexs.add(index);
            deleteCount++;
            find = true;
            break;
          }
          if (find) continue;
        }
        // not delete this projVar, replace it with its new index. e.g. $5(x) -> $1(x)
        final UVar projReplaceVar = translator.mkProjVar(getValueByIndex(index - deleteCount), boundedVar);
        multiply = multiply.replaceAtomicTerm(UVarTerm.mk(projVar), UVarTerm.mk(projReplaceVar));
        for (final UVar key : replaceMap.keySet()) {
          if (key.equals(projVar)) continue;
          replaceMap.put(key, map(replaceMap.get(key), t ->
                  t.replaceAtomicTerm(UVarTerm.mk(projVar), UVarTerm.mk(projReplaceVar))));
        }
      }
      multiply.replaceVarInplace(boundedVar, newBoundedVar, false);
      translator.deleteTupleVarSchemaByIndexs(newBoundedVar, deleteIndexs);

      isModified = true;
      if (!multiply.isUsing(newBoundedVar)) {
        newBoundedVars.remove(newBoundedVar);
      }
      if (newBoundedVars.isEmpty()) return multiply;
      return USum.mk(newBoundedVars, multiply);
    }

    return expr;
  }

  /**
   * Remove unary's bounded var by different rules.
   */
  private UTerm removeUnaryBoundedVar(UTerm expr) {
    expr = removeUnaryBoundedVarBySameSchema(expr);
    expr = removeNegatedSumByMaxMin(expr, false);
    expr = removeSumByContainment(expr, new HashSet<>());
    return expr;
  }

  /**
   * For a unary which have summation or add of summation.
   * Remove boundedVar by same schema.
   */
  private UTerm removeUnaryBoundedVarBySameSchema(UTerm expr) {
    expr = transformSubTerms(expr, this::removeUnaryBoundedVarBySameSchema);
    if (!expr.kind().isUnary()) return expr;

    UTerm body = expr.subTerms().get(0);
    UTerm newBody = null;
    switch (body.kind()) {
      case SUMMATION: {
        newBody = removeUnaryBoundedVarBySameSchemaInSum((USum) body);
        break;
      }
      case ADD: {
        if (any(body.subTerms(), t -> !isTermNaturalNumber(t))) return expr;
        ArrayList<UTerm> newSubTerms = new ArrayList<>();
        for (UTerm subTerm : body.subTerms()) {
          if (subTerm.kind() == UKind.SUMMATION)
            newSubTerms.add(removeUnaryBoundedVarBySameSchemaInSum((USum) subTerm));
          else
            newSubTerms.add(subTerm);
        }
        newBody = UAdd.mk(newSubTerms);
        break;
      }
      default:
        newBody = body;
    }

    return remakeTerm(expr, new ArrayList<>(List.of(newBody)));
  }

  /**
   * For ||∑{..., v0, v1,...} (f(v0) * u(v1) * z(v0, v1)||, if v0 and v1 has the same schema,
   * and if we replace v1 with v0 in u(v1) * z(v0, v1), the u(v0) * z(v0, v0) becomes f(v0)'s subset,
   * then delete v1's related terms.
   */
  private UTerm removeUnaryBoundedVarBySameSchemaInSum(USum summation) {
    if (summation.body().kind() != UKind.MULTIPLY) return summation;
    final UMul multiply = (UMul) summation.body();

    for (final UVar boundedVar : summation.boundedVars()) {
      final List<UVar> otherBoundedVars = filter(summation.boundedVars(), v -> !v.equals(boundedVar));

      for (final UVar otherBoundedVar : otherBoundedVars) {
        if (!isExactlyEqualTwoValueList(translator.getTupleVarSchema(otherBoundedVar), translator.getTupleVarSchema(boundedVar)))
          continue;
        // f contains all terms that doesn't contain v1 -> f(v0)
        // u contains all terms that contain v1 -> u(v1) * z(v0, v1)
        final List<UTerm> f = new ArrayList<>();
        final List<UTerm> u = new ArrayList<>();
        // find f, u.
        for (final UTerm subTerm : multiply.subTerms()) {
          if (subTerm.isUsing(otherBoundedVar)) u.add(subTerm.copy());
          else f.add(subTerm.copy());
        }
        // replace u's v1 with v0 and normalize it.
        u.replaceAll(termU -> new QueryUExprNormalizer(termU.replaceVar(otherBoundedVar, boundedVar, true),
                this.schema,
                this.translator)
                .normalizeTerm());
        // check whether u and z have term that f doesn't have.
        final NaturalCongruence<UTerm> congruence = getEqCongruenceInTermsOfMul(UMul.mk(f), pred -> true);
        boolean canModify = true;
        for (final UTerm target : u) {
          if (target.equals(UConst.one())) continue;
          // case like [a = b], use congruence to determine.
          if (target.kind() == UKind.PRED && ((UPred) target).isPredKind(UPred.PredKind.EQ)) {
            final UTerm eqTerm = ((UPred) target).args().get(0).isUsing(boundedVar) ? ((UPred) target).args().get(1) : ((UPred) target).args().get(0);
            if (any(congruence.eqClassOf(eqTerm), t -> !t.equals(eqTerm) && t.isUsing(boundedVar))) continue;
          }
          // case like notnull(a), use congruence to determine whether a is not null.
          if (isNotNullPred(target)) {
            final UTerm eqTerm = ((UNeg) target).body().subTerms().get(0);
            if (any(congruence.eqClassOf(eqTerm), t -> !t.equals(eqTerm) && f.contains(mkNotNullPred(t)))) continue;
          }
          if (all(f, t -> !t.equals(target))) {
            canModify = false;
            break;
          }
        }
        // modify here.
        if (canModify) {
          isModified = true;
          final Set<UVar> newBoundedVars = summation.boundedVars();
          newBoundedVars.remove(otherBoundedVar);
          return USum.mk(newBoundedVars, UMul.mk(f));
        }

      }
    }

    return summation;
  }

  /**
   * Simplify summation by its unary subTerms.
   */
  private UTerm simplifySumByUnarySubTerm(UTerm expr) {
    expr = removeUselessSquashSum(expr);
    expr = simplifySumToZeroByContradictNegSum(expr);
    return expr;
  }

  /**
   * \sum{t1, ...}(||\sum{t2}(E1(t2))|| * E1(t1) * E2(t1))
   * = \sum{t1, ...}(E1(t1) * E2(t1))
   * NOTE: the schema of t1 and t2 must be the same.
   */
  private UTerm removeUselessSquashSum(UTerm term) {
    term = transformSubTerms(term, this::removeUselessSquashSum);
    if (term.kind() != UKind.SUMMATION) return term;

    final USum expr = (USum) term;
    final UTerm body = expr.body();
    if (!(body instanceof UMul))
      return expr;
    for (UTerm subTerm : body.subTerms()) {
      if (subTerm instanceof USquash) {
        final UTerm squashBody = ((USquash) subTerm).body();
        if (squashBody instanceof final USum squashSum) {
          if (squashSum.boundedVars().size() > expr.boundedVars().size())
            continue;
          final List<UTerm> unrelatedTerms = filter(squashSum.body().subTerms(),
                  t -> all(squashSum.boundedVars(), v -> !t.isUsing(v)));
          final List<UTerm> relatedTerms = filter(squashSum.body().subTerms(), t -> !unrelatedTerms.contains(t));
          final ArrayList<UTerm> subTerms = new ArrayList<>(body.subTerms());
          subTerms.remove(subTerm);
          final USum tmp = USum.mk(new HashSet<>(expr.boundedVars()), UMul.mk(subTerms).copy());
          final USum smallSum = USum.mk(new HashSet<>(squashSum.boundedVars()),
                  remakeTerm(squashSum.body(), relatedTerms).copy());
          if (containSubTerms(tmp, smallSum)) {
            isModified = true;
            if (!unrelatedTerms.isEmpty()) {
              return USum.mk(tmp.boundedVars(), UMul.mk(remakeTerm(squashSum.body(), unrelatedTerms), tmp.body()));
            }
            return tmp;
          }
        }
      }
    }
    return expr;
  }

  /**
   * Simplify summation to zero by its contradict negation subTerm
   * \sum{t1, ...}(not(\sum{t2}(E1(t2))) * E1(t1) * E2(t1))
   * = 0
   * NOTE: the schema of t1 and t2 must be the same.
   */
  private UTerm simplifySumToZeroByContradictNegSum(UTerm term) {
    term = transformSubTerms(term, this::simplifySumToZeroByContradictNegSum);
    if (term.kind() != UKind.SUMMATION) return term;

    final USum expr = (USum) term;
    final UTerm body = expr.body();
    if (!(body instanceof UMul))
      return expr;
    for (UTerm subTerm : body.subTerms()) {
      if (subTerm instanceof UNeg) {
        final UTerm negBody = ((UNeg) subTerm).body();
        if (negBody instanceof final USum negSum) {
          if (negSum.boundedVars().size() > expr.boundedVars().size())
            continue;
          final ArrayList<UTerm> subTerms = new ArrayList<>(body.subTerms());
          subTerms.remove(subTerm);
          final USum tmp = (USum) USum.mk(expr.boundedVars(), UMul.mk(subTerms)).copy();
          final USum smallSum = (USum) negSum.copy();
          if (containSubTerms(tmp, smallSum)) {
            isModified = true;
            return UConst.zero();
          }
        }
      }
    }
    return expr;
  }

  /**
   * Simplify summation by set solver.
   */
  private UTerm simplifySumBySetSolver(UTerm expr) {
    expr = removeUselessSetSemanticSumBySetSolver(expr);
    expr = removeUselessBoundedVarInUnarySumBySetSolver(expr);
    return expr;
  }

  /**
   * \sum{t1, ...}(||\sum{t2}(E1(t2))|| * E1(t1) * E2(t1))
   * = \sum{t1, ...}(E1(t1) * E2(t1))
   * NOTE: the schema of t1 and t2 must be the same.
   */
  private UTerm removeUselessSetSemanticSumBySetSolver(UTerm term) {
    term = transformSubTerms(term, this::removeUselessSetSemanticSumBySetSolver);
    if (!term.kind().isUnary() || term.subTerms().get(0).kind() != UKind.SUMMATION) return term;

    final USum sum = (USum) term.subTerms().get(0);
    final List<UTerm> subTerms = sum.body().subTerms();
    // try to eliminate the unary summation subTerm
    for (final UTerm subTerm : subTerms) {
      if (subTerm.kind().isUnary() && subTerm.subTerms().get(0).kind() == UKind.SUMMATION) {
        final USum newSum = USum.mk(sum.boundedVars(), remakeTerm(sum.body(), filter(subTerms, t -> !t.equals(subTerm))));
        VerificationResult equalOrNot = new SetSolver(remakeTerm(term, new ArrayList<>(List.of(sum))),
                remakeTerm(term, new ArrayList<>(List.of(newSum))),
                translator.getSchema(),
                schema).proveEq();
        if (equalOrNot == VerificationResult.EQ) {
          isModified = true;
          final List<UTerm> newSubTerms = new ArrayList<>();
          newSubTerms.add(newSum);
          return remakeTerm(term, new ArrayList<>(newSubTerms));
        }
      }
    }

    return term;
  }

  /**
   * for || \sum{..., t1, t2 ...} (f(t1, t2)) ||, if we replace t2 with t1 and get equivalent term, then replace it.
   * => || \sum{..., t1, ...} (f(t1)) ||
   * NOTE: the schema of t1 and t2 must be the same.
   */
  private UTerm removeUselessBoundedVarInUnarySumBySetSolver(UTerm term) {
    term = transformSubTerms(term, this::removeUselessBoundedVarInUnarySumBySetSolver);
    if (!term.kind().isUnary() || term.subTerms().get(0).kind() != UKind.SUMMATION) return term;

    final USum sum = (USum) term.subTerms().get(0);
    USum sumCompared = (USum) sum.copy();
    final List<UVar> boundedVars = new ArrayList<>(sum.boundedVars());
    // try to eliminate the boundedVar
    for (int i = 0; i < boundedVars.size(); i++) {
      for (int j = 0; j < boundedVars.size(); j++) {
        if (i == j) continue;
        final UVar srcVar = boundedVars.get(i);
        final UVar tgtVar = boundedVars.get(j);
        if (!isExactlyEqualTwoValueList(translator.getTupleVarSchema(srcVar), translator.getTupleVarSchema(tgtVar)))
          continue;
        sumCompared.boundedVars().remove(tgtVar);
        sumCompared.replaceVarInplace(tgtVar, srcVar, false);
        VerificationResult equalOrNot = new SetSolver(remakeTerm(term, new ArrayList<>(List.of(sum))),
                remakeTerm(term, new ArrayList<>(List.of(sumCompared))),
                translator.getSchema(),
                schema).proveEq();
        if (equalOrNot == VerificationResult.EQ) {
          isModified = true;
          return remakeTerm(term, new ArrayList<>(List.of(reduceRedundantTables(sumCompared))));
        }
        sumCompared = (USum) sum.copy();
      }
    }

    return term;
  }

  /**
   * Simplify max min term in a summation:
   * ∑{x,y}(not(∑{x1}(r(x1) * p(x1) * [f(x1) >/< x]))
   *          * ||∑{x1,y1}(r(x1) * p(x1) * [f(x1) = x] * E1)||
   *          * E2)
   * ->
   * ∑{y}(||∑{x1,y1}(r(x1) * p(x1) * E1)||
   *        * E2),
   * where x is a single column,
   * r,p,f,E1,E2 do not contain x,
   * r,p,f do not contain y1,
   * and E1 does not contain x1.
   * The first U-expression indicates that there exists at least one tuple in r(x1).
   */
  private UTerm simplifyMaxMinInSum(UTerm expr) {
    expr = transformSubTerms(expr, this::simplifyMaxMinInSum);

    if (expr instanceof USum sum && sum.body() instanceof UMul mul) {
      final List<UTerm> newFactors = copyTermList(mul.subTerms());
      final Set<UVar> newBVs = new HashSet<>(sum.boundedVars());
      for (final UVar bv : sum.boundedVars()) {
        final List<Value> schema = translator.getTupleVarSchema(bv);
        for (int i = schema.size() - 1; i >= 0; i--) {
          final UVar column = UVar.mkProj(UName.mk(indexToColumnName(i)), bv.copy());
          simplifyMaxMinInSumByOneColumn(newBVs, newFactors, column);
        }
      }
      if (newBVs.isEmpty()) return UMul.mk(newFactors);
      return USum.mk(newBVs, UMul.mk(newFactors));
    }

    return expr;
  }

  /**
   * Simplify multiplication to zero by different rules.
   */
  private UTerm simplifyMultiplication(UTerm expr) {
    expr = simplifyMulToZeroByContradictCongruence(expr);
    expr = simplifyInequalityByCongruence(expr);
    return expr;
  }

  /**
   * For ||∑{..., v0, v1,...} (f(v0) * u(v1) * z(v0, v1)||, if v0 and v1 has the same schema,
   * and if we replace v1 with v0 in u(v1) * z(v0, v1), the u(v0) * z(v0, v0) becomes f(v0)'s subset,
   * then replace v1 with v0.
   */
  private UTerm simplifyMulToZeroByContradictCongruence(UTerm expr) {
    expr = transformSubTerms(expr, this::simplifyMulToZeroByContradictCongruence);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final UMul multiply = (UMul) expr;

    final NaturalCongruence<UTerm> congruence = getEqCongruenceInTermsOfMul(multiply, pred -> true);

    for (final UTerm key : congruence.keys()) {
      final Set<UTerm> eqTerms = congruence.eqClassOf(key);
      final List<UTerm> eqConsts = filter(eqTerms, t -> t.kind() == UKind.CONST || t.kind() == UKind.STRING);
      if (any(eqConsts, c -> any(eqConsts, c1 -> !c.equals(c1)))) {
        isModified = true;
        return UConst.zero();
      }
    }

    return expr;
  }

  /**
   * For a multiply, get the critical natural congruence and normalize the >/< to zero.
   */
  private UTerm simplifyInequalityByCongruence(UTerm expr) {
    expr = transformSubTerms(expr, this::simplifyInequalityByCongruence);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final UMul multiply = (UMul) expr;

    final NaturalCongruence<UTerm> congruence = NaturalCongruence.mk();
    getEqCongruenceRecursive(multiply, pred -> true, congruence, false);
    expr = replaceInequalityWithZero(multiply, congruence);

    return expr;
  }

  /**
   * For not(p) * E -> change every p in E to 0.
   */
  private UTerm eliminateNegationTerm(UTerm expr) {
    expr = transformSubTerms(expr, this::eliminateNegationTerm);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final List<UTerm> notSubTerms = filter(expr.subTerms(), t -> t.kind() == UKind.NEGATION);
    final List<UTerm> otherSubTerms = filter(expr.subTerms(), t -> t.kind() != UKind.NEGATION);
    final List<UTerm> newOtherSubTerms = new ArrayList<>();
    loop:
    for (final UTerm otherSubTerm : otherSubTerms) {
      for (final UTerm notSubTerm : notSubTerms) {
        final UNeg negation = (UNeg) notSubTerm;
        final UTerm newTerm = replaceTermRecursive(otherSubTerm,  negation.body(), UConst.zero());
        if (!otherSubTerm.equals(newTerm)) {
          isModified = true;
          newOtherSubTerms.add(newTerm);
          continue loop;
        }
      }
      newOtherSubTerms.add(otherSubTerm);
    }

    return UMul.mk(concat(notSubTerms, newOtherSubTerms));
  }

  /**
   * For ∑{v} (f(v)), if f(v) has another summation, use f(v)'s congruence to transform another summation to be unrelated
   * with this summation as much as possible.
   */
  private UTerm transformUnrelatedSummation(UTerm expr) {
    expr = transformSubTerms(expr, this::transformUnrelatedSummation);
    if (expr.kind() != UKind.SUMMATION) return expr;

    final USum summation = (USum) expr;
    final List<NaturalCongruence<UTerm>> congruences = new ArrayList<>();
    UTerm newBody = null;

    newBody = applyTransformationUnrelatedSummationRecursively(summation.body(), congruences, summation.boundedVars());
    newBody = applyTransformationUnrelatedSummationTerm(newBody, summation.boundedVars());
    newBody = applyTransformationUnrelatedSummationInMul(newBody, summation.boundedVars());
    return USum.mk(summation.boundedVars(), newBody);
  }

  /**
   * Recursively transform summation based on congruences.
   * e.g. \sum{x} ([x = c] * \sum{y} ([x = y])) -> \sum{x} ([x = c] * \sum{y} ([c = y]))
   */
  private UTerm applyTransformationUnrelatedSummationRecursively(UTerm context,
                                                                 List<NaturalCongruence<UTerm>> congruences,
                                                                 Set<UVar> boundedVars) {
    final int beforeCongruencesSize = congruences.size();
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR -> {
        return context;
      }
      case NEGATION, SQUASH, PRED, ADD -> {
        context = transformSubTerms(context, t -> applyTransformationUnrelatedSummationRecursively(t, congruences, boundedVars));
      }
      case MULTIPLY -> {
        congruences.add(getEqCongruenceInTermsOfMul(context, pred -> true));
        context = transformSubTerms(context, t -> applyTransformationUnrelatedSummationRecursively(t, congruences, boundedVars));
        assert congruences.size() == beforeCongruencesSize + 1;
        congruences.remove(congruences.size() - 1);
      }
      case SUMMATION -> {
        final Set<UVar> newBoundedVars = new HashSet<>(boundedVars);
        newBoundedVars.addAll(((USum) context).boundedVars());
        context = transformSubTerms(context, t -> applyTransformationUnrelatedSummationRecursively(t, congruences, newBoundedVars));
        // doing transformation here.
        // traverse congruence to replace term.
        for (final NaturalCongruence<UTerm> congruence : congruences) {
          for (final UTerm key : congruence.keys()) {
            // key must be atomic term.
            if (!key.kind().isTermAtomic()) continue;
            if (context.isUsingTerm(key) && any(boundedVars, key::isUsing)) {
              // try to replace this key.
              final Set<UTerm> eqTerms = congruence.eqClassOf(key);
              for (final UTerm eqTerm : eqTerms) {
                if (all(boundedVars, v -> !eqTerm.isUsing(v))) {
                  // successfully replace.
                  isModified = true;
                  context = context.replaceAtomicTerm(key, eqTerm);
                  break;
                }
              }
            }
          }
        }
      }
    }

    return context;
  }

  /**
   * Recursively transform summation's term based on congruences, trying to make the term unrelated to the summation's boundedVars.
   * e.g. \sum{x} ([x = c] * \sum{y} ([x = y] * [y > C])) -> \sum{x} ([x = c] * \sum{y} ([x = y] * [x > c]))
   */
  private UTerm applyTransformationUnrelatedSummationTerm(UTerm context, Set<UVar> boundedVars) {
    if (context.kind() != UKind.MULTIPLY) return context;
    final NaturalCongruence<UTerm> congruence = getEqCongruenceInTermsOfMul(context, pred -> all(pred.args(), v -> v.kind().isTermAtomic()));
    UMul multiply = (UMul) context;

    // for every projVar of boundedVars, try to replace it
    for (final UVar boundedVar : boundedVars) {
      final List<Value> schema = getValueListBySize(translator.getTupleVarSchema(boundedVar).size());
      for (final Value column : schema) {
        final UVar projVar = UVar.mkProj(UName.mk(column.name()), boundedVar);
        final List<UTerm> newSubTerms = new ArrayList<>();
        for (final UTerm subTerm : multiply.subTerms()) {
          newSubTerms.add(subTerm);
          if (subTerm instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) continue;
          if (!subTerm.isUsingProjVar(projVar)) continue;
          // try to modify
          UTerm choose = null;
          for (final UTerm replaceTerm : congruence.eqClassOf(UVarTerm.mk(projVar))) {
            if (any(boundedVars, replaceTerm::isUsing)) continue;
            choose = replaceTerm;
            break;
          }
          if (choose != null) {
            isModified = true;
            newSubTerms.remove(newSubTerms.size() - 1);
            newSubTerms.add(subTerm.replaceAtomicTerm(UVarTerm.mk(projVar), choose));
          }
        }
        multiply = UMul.mk(newSubTerms);
      }
    }

    return multiply;
  }

  /**
   * Transform summation in a single multiply.
   * e.g. \sum{x, y} ([x = c] * [x = y]) -> \sum{x, y} ([x = c] * [c = y] * [x = c])
   */
  private UTerm applyTransformationUnrelatedSummationInMul(UTerm context, Set<UVar> boundedVars) {
    if (context.kind() != UKind.MULTIPLY) return context;

    final NaturalCongruence<UTerm> congruence = getEqCongruenceInTermsOfMul(context, pred -> true);
    final List<UTerm> subTerms = context.subTerms();

    interface varChecker {
      boolean check(UTerm t, UVar v, Set<UVar> vs);
    }
    varChecker singleVarChecker = (UTerm t, UVar v, Set<UVar> vs) -> {
      if (t.isUsing(v)) {
        for (final UVar vi : vs) {
          if (vi.equals(v)) continue;
          if (t.isUsing(vi)) return false;
        }
        return true;
      }
      return false;
    };
    varChecker otherVarChecker = (UTerm t, UVar v, Set<UVar> vs) -> {
      if (t.isUsing(v)) return false;
      return any(vs, t::isUsing);
    };

    for (final UVar boundedVar : boundedVars) {
      final List<UTerm> newSubTerms = new ArrayList<>();
      for (final UTerm subTerm : subTerms) {
        if (subTerm instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) {
          final UTerm firstArgument = pred.args().get(0);
          final UTerm secondArgument = pred.args().get(1);
          final List<UTerm> constEqTerms = new ArrayList<>();
          if (singleVarChecker.check(firstArgument, boundedVar, boundedVars)
                  && otherVarChecker.check(secondArgument, boundedVar, boundedVars)) {
            constEqTerms.addAll(filter(congruence.eqClassOf(firstArgument), t -> all(boundedVars, v -> !t.isUsing(v))));
          } else if (singleVarChecker.check(secondArgument, boundedVar, boundedVars)
                  && otherVarChecker.check(firstArgument, boundedVar, boundedVars)) {
            constEqTerms.addAll(filter(congruence.eqClassOf(secondArgument), t -> all(boundedVars, v -> !t.isUsing(v))));
          }
          if (constEqTerms.isEmpty()) {
            newSubTerms.add(subTerm);
            continue;
          }
          // just heuristically pick one const term and construct the eqTerms
          newSubTerms.add(UPred.mkBinary(UPred.PredKind.EQ, firstArgument, constEqTerms.get(0)));
          newSubTerms.add(UPred.mkBinary(UPred.PredKind.EQ, secondArgument, constEqTerms.get(0)));
        } else {
          newSubTerms.add(subTerm);
        }
      }
      // because every modification will bring the bigger size of newSubTerms, we can judge by size.
      if (newSubTerms.size() == subTerms.size()) continue;

      isModified = true;
      return UMul.mk(newSubTerms);
    }

    return context;
  }

  /*
   * Final normalization.
   */

  /**
   * Extract unrelated summation.
   * sum{t}(f(t) * sum{y}(g(y))) -> sum{t}(f(t)) * sum{y}(g(y))
   */
  private UTerm extractUnrelatedSummation(UTerm expr) {
    expr = transformSubTerms(expr, this::extractUnrelatedSummation);
    if (expr.kind() != UKind.SUMMATION || ((USum) expr).body().kind() != UKind.MULTIPLY) return expr;

    final USum summation = (USum) expr;
    final UMul multiply = (UMul) summation.body();
    final List<UTerm> newMulTerms = new ArrayList<>();
    final List<UTerm> newSumTerms = new ArrayList<>();

    for (final UTerm subTerm : multiply.subTerms()) {
      if (haveTargetKind(subTerm, UKind.SUMMATION)
              && all(summation.boundedVars(), v -> !subTerm.isUsing(v))) {
        newMulTerms.add(subTerm);
        continue;
      }
      newSumTerms.add(subTerm);
    }

    if (!newMulTerms.isEmpty()) {
      isModified = true;
      return UMul.mk(USum.mk(summation.boundedVars(), UMul.mk(newSumTerms)), newMulTerms);
    }

    return expr;
  }

  /**
   * Split unrelated summation.
   * sum{t, y}(f(t) * g(y)) -> sum{t}(f(t)) * sum{y}(g(y))
   */
  private UTerm splitUnrelatedSummation(UTerm expr) {
    expr = transformSubTerms(expr, this::splitUnrelatedSummation);
    if (expr.kind() != UKind.SUMMATION || ((USum) expr).body().kind() != UKind.MULTIPLY) return expr;

    final USum summation = (USum) expr;
    final List<UVar> boundedVars = new ArrayList<>(summation.boundedVars());
    UMul multiply = (UMul) summation.body();
    final List<UTerm> subTerms = new ArrayList<>();
    final List<UTerm> newSumTerms = new ArrayList<>();
    // handle || f(t) * g(y) || cases -> ||f(t)|| * ||g(y)||
    for (final UTerm subTerm : multiply.subTerms()) {
      if (subTerm instanceof USquash squash && squash.body().kind() == UKind.MULTIPLY) {
        final List<UTerm> originSquashTerms = squash.body().copy().subTerms();
        final List<UTerm> newSquashTerms = new ArrayList<>();
        for (final UVar boundedVar : boundedVars) {
          final List<UTerm> boundedVarRelated = filter(originSquashTerms, t -> t.isUsing(boundedVar));
          newSquashTerms.add(USquash.mk(UMul.mk(boundedVarRelated)));
          originSquashTerms.removeAll(boundedVarRelated);
        }
        if (!originSquashTerms.isEmpty()) {
          originSquashTerms.add(USquash.mk(UMul.mk(originSquashTerms)));
        }
        subTerms.addAll(newSquashTerms);
        continue;
      }
      subTerms.add(subTerm);
    }
    multiply = UMul.mk(subTerms);

    for (final UVar boundedVar : boundedVars) {
      // consider those unvisited boundedVars
      if (any(newSumTerms, t -> ((USum) t).boundedVars().contains(boundedVar))) continue;
      List<UTerm> mulTerms = filter(multiply.subTerms(), t -> t.isUsing(boundedVar));
      final Set<UVar> newBoundedVars = new HashSet<>();
      newBoundedVars.add(boundedVar);
      boolean continueLooping = true;
      while (continueLooping) {
        continueLooping = false;
        // get all related boundedVars
        for (final UVar otherBoundedVar : boundedVars) {
          if (newBoundedVars.contains(otherBoundedVar)) continue;
          if (any(mulTerms, t -> t.isUsing(otherBoundedVar))) {
            continueLooping = true;
            newBoundedVars.add(otherBoundedVar);
          }
        }
        // get all related mulTerms
        mulTerms = filter(multiply.subTerms(), t -> any(newBoundedVars, t::isUsing));
      }

      newSumTerms.add(USum.mk(newBoundedVars, UMul.mk(mulTerms)));
    }

    // get unrelated terms of summation
    final List<UTerm> unrelatedTerms = new ArrayList<>();
    for (final UTerm subTerm : multiply.subTerms()) {
      if (all(boundedVars, v -> !subTerm.isUsing(v))) unrelatedTerms.add(subTerm);
    }

    if (newSumTerms.size() > 1) {
      isModified = true;
      return UMul.mk(concat(newSumTerms, unrelatedTerms));
    }

    return expr;
  }

  /**
   * Extract unrelated terms from summation.
   * sum{t}(f(t) * h()) -> sum{t}(f(t)) * h()
   */
  private UTerm extractUnrelatedSummationTerms(UTerm expr) {
    expr = transformSubTerms(expr, this::extractUnrelatedSummationTerms);
    if (expr.kind() != UKind.SUMMATION || ((USum) expr).body().kind() != UKind.MULTIPLY) return expr;

    final USum summation = (USum) expr;
    final UMul multiply = (UMul) summation.body();

    final List<UTerm> unrelatedTerms = filter(multiply.subTerms(), t -> all(summation.boundedVars(), v -> !t.isUsing(v)));

    if (!unrelatedTerms.isEmpty()) {
      isModified = true;
      final List<UTerm> relatedTerms = filter(multiply.subTerms(), t -> !unrelatedTerms.contains(t));
      final List<UTerm> resultTerms = new ArrayList<>(unrelatedTerms);
      resultTerms.add(USum.mk(summation.boundedVars(), UMul.mk(relatedTerms)));
      return UMul.mk(resultTerms);
    }

    return expr;
  }

  /**
   * Replace summations with equivalent non-summation terms.
   */
  public UTerm replaceSummation(UTerm term) {
    return replaceSummation(term, NaturalCongruence.mk());
  }

  public UTerm replaceSummation(UTerm term, NaturalCongruence<UTerm> congruence) {
    if (containsNoSum(term)) return term;
    // recursion
    if (term instanceof UMul mul) {
      // append new equivalence relation
      final List<UTerm> factors = mul.subTerms();
      term = transformSubTerms(term, t -> {
        NaturalCongruence<UTerm> newCongruence = congruence;
        for (UTerm factor : factors) {
          if (factor.equals(t)) continue;
          if (factor instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) {
            // COW
            if (congruence == newCongruence) {
              newCongruence = (NaturalCongruence<UTerm>) congruence.copy();
            }
            newCongruence.putCongruent(pred.args().get(0), pred.args().get(1));
          }
        }
        return replaceSummation(t, newCongruence);
      });
    } else {
      // only copy on write
      term = transformSubTerms(term, t -> replaceSummation(t, congruence));
    }

    if (!(term instanceof USum sum)) return term;

    // find suitable alternative in its equivalence class
    final Set<UTerm> eqClass = congruence.eqClassOf(sum);
    for (UTerm candidate : eqClass) {
      if (candidate.kind().isTermAtomic()) {
        return candidate.copy();
      }
    }
    return term;
  }

  private boolean containsNoSum(UTerm term) {
    if (term instanceof USum) return false;
    return all(term.subTerms(), this::containsNoSum);
  }
}