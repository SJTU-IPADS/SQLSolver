package sqlsolver.superopt.uexpr.normalizer;

import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.uexpr.*;

import java.util.List;
import java.util.ListIterator;

import static sqlsolver.common.utils.IterableSupport.all;
import static sqlsolver.superopt.uexpr.UExprSupport.isPredOfVarArg;
import static sqlsolver.superopt.uexpr.UExprSupport.transformSubTerms;

public class UNormalizationEnhance extends UNormalization{

  public UNormalizationEnhance(UTerm expr) {
    super(expr);
  }

  @Override
  public UTerm normalizeTerm() {
    do {
      isModified = false;
      // A round of normalizations
      expr = super.normalizeTerm();

      // Some additional rewrites
      // expr = performNormalizeRule(this::removeRedundantBoundedVar);
      expr = performNormalizeRule(this::removeNestedProjOnProjVar);
      expr = performNormalizeRule(this::removeRedundantProjOnBaseVar);
      expr = performNormalizeRule(this::replaceBaseVars);
    } while (isModified);

    return expr;
  }

  /**
   * In a summation, remove `[t0 = t1]` and replace all occurrence of `t1` with `t0`;
   * Then remove the summation bounded var `t1`
   */
  UTerm removeRedundantBoundedVar(UTerm expr) {
    expr = transformSubTerms(expr, this::removeRedundantBoundedVar);
    if (expr.kind() != UKind.SUMMATION) return expr;

    final USum summation = (USum) expr;
    final ListIterator<UTerm> iter = summation.body().subTerms().listIterator();
    while (iter.hasNext()) {
      final UTerm subTerm = iter.next();
      if (subTerm.kind() == UKind.PRED) {
        final UPred pred = (UPred) subTerm;
        if (!pred.isPredKind(UPred.PredKind.EQ) || !isPredOfVarArg(pred)) continue;
        final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
        assert pArgs.size() == 2;
        final UVar predVar0 = pArgs.get(0), predVar1 = pArgs.get(1);
        if (!predVar0.is(UVar.VarKind.BASE) || !predVar1.is(UVar.VarKind.BASE)) continue;
        if (!summation.isUsingBoundedVar(predVar0) && !summation.isUsingBoundedVar(predVar1)) continue;
        // By default, rmVar is `t1` and repVar is `t0`; But if `t1` isn't bounded var, then exchange.
        final UVar rmVar = summation.isUsingBoundedVar(predVar1) ? predVar1 : predVar0;
        final UVar repVar = rmVar == predVar1 ? predVar0 : predVar1;
        isModified = true;
        iter.remove();
        summation.body().replaceVarInplace(rmVar, repVar, false);
        summation.removeBoundedVar(rmVar);
      }
    }
    // If no bounded vars, remove the summation
    if (summation.boundedVars().isEmpty()) expr = summation.body();
    return expr;
  }

  /** a1(a0(t)) -> a1(t) */
  UTerm removeNestedProjOnProjVar(UTerm expr) {
    expr = transformSubTerms(expr, this::removeNestedProjOnProjVar);
    if (!expr.kind().isVarAtomic()) return expr;

    // only TABLE or Var type UTerms
    UVar var = ((UAtom) expr).var();
    final UVar newVar = UVar.removeNestedProjVar(var);
    if (var.equals(newVar)) return expr;
    else {
      isModified = true;
      return expr.kind() == UKind.TABLE ? UTable.mk(((UTable) expr).tableName(), newVar) : UVarTerm.mk(newVar);
    }
  }

  /** If having `[x2 = a0(x1)]` in expr, then `a0(x2)` -> `x2` */
  UTerm removeRedundantProjOnBaseVar(UTerm expr) {
    final NaturalCongruence<UVar> schemaEqVarClass = UExprSupport.getSchemaEqVarCongruence(expr);
    for (UVar var0 : schemaEqVarClass.keys()) {
      if (!var0.is(UVar.VarKind.BASE)) continue; // `var0` -> `x2`
      for (UVar var1 : schemaEqVarClass.eqClassOf(var0)) {
        if (!var1.is(UVar.VarKind.PROJ) || !var1.args()[0].is(UVar.VarKind.BASE)) continue; // `var1` -> `a0(x1)`
        final UVar redundantVarModel = UVar.mkProj(var1.name(), var0); // `redundantVarModel` -> `a0(x2)`
        if (expr.replaceVarInplace(redundantVarModel, var0, false)) isModified = true; // perform `a0(x2)` -> `x2`
      }
    }
    return expr;
  }

  /**
   * If having `[t = e]` in a UMul, then
   * 1. replace `t` with `e` in every other factor
   * 2. remove useless bounded var: \sum{t0,t1} [t1 = e] * E(t0) -> \sum{t0}E(t0) */
  UTerm replaceBaseVars(UTerm expr) {
    expr = replaceBaseVarsWithEqVars(expr); // Step 1
    expr = removeUselessBoundedVar(expr); // Step 2
    return expr;
  }

  private UTerm replaceBaseVarsWithEqVars(UTerm expr) {
    expr = transformSubTerms(expr, this::replaceBaseVarsWithEqVars);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final UMul mul = (UMul) expr;
    for (UTerm curTerm : mul.subTerms()) {
      if (curTerm.kind() != UKind.PRED) continue;
      final UPred pred = (UPred) curTerm;
      if (pred.isPredKind(UPred.PredKind.EQ) && UExprSupport.isPredOfVarArg(pred)) {
        final List<UVar> eqPredArgs = UExprSupport.getPredVarArgs(pred);
        assert eqPredArgs.size() == 2;
        final UVar predVar0 = eqPredArgs.get(0), predVar1 = eqPredArgs.get(1);
        if (predVar0.is(UVar.VarKind.BASE)) {
          for (UTerm subTerm : mul.subTerms()) {
            if (subTerm.equals(curTerm)) continue;
            if (subTerm.replaceVarInplace(predVar0, predVar1, false)) isModified = true;
          }
        } else if (predVar1.is(UVar.VarKind.BASE)) {
          for (UTerm subTerm : mul.subTerms()) {
            if (subTerm.equals(curTerm)) continue;
            if (subTerm.replaceVarInplace(predVar1, predVar0, false)) isModified = true;
          }
        }
      }
    }
    return mul;
  }

  private UTerm removeUselessBoundedVar(UTerm expr) {
    expr = transformSubTerms(expr, this::removeUselessBoundedVar);
    if (expr.kind() != UKind.SUMMATION) return expr;

    final USum summation = (USum) expr;
    final List<UTerm> subTerms = summation.body().subTerms();
    final ListIterator<UTerm> iter = subTerms.listIterator();
    while (iter.hasNext()) {
      final UTerm curTerm = iter.next();
      if (curTerm.kind() != UKind.PRED) continue;
      final UPred pred = (UPred) curTerm;
      if (pred.isPredKind(UPred.PredKind.EQ)) {
        final List<UTerm> eqPredArgs = pred.args();
        assert eqPredArgs.size() == 2;
        final UTerm predArg0 = eqPredArgs.get(0), predArg1 = eqPredArgs.get(1);
        // [t = e], e can be UVarTerm or other UTerm (like \sum(..))
        if (predArg0.kind() == UKind.VAR
            && summation.isUsingBoundedVar(((UVarTerm) predArg0).var())
            && all(subTerms, t -> t.equals(pred) || !t.isUsing(((UVarTerm) predArg0).var()))) {
          iter.remove();
          summation.removeBoundedVar(((UVarTerm) predArg0).var());
          isModified = true;
        } else if (predArg1.kind() == UKind.VAR
            && summation.isUsingBoundedVar(((UVarTerm) predArg1).var())
            && all(subTerms, t -> t.equals(pred) || !t.isUsing(((UVarTerm) predArg1).var()))) {
          iter.remove();
          summation.removeBoundedVar(((UVarTerm) predArg1).var());
          isModified = true;
        }
      } else if (UExprSupport.varIsNullPred(curTerm)) {
        final UVar nullVar = UExprSupport.getIsNullPredVar((UPred) curTerm);
        if (summation.isUsingBoundedVar(nullVar)
            && all(subTerms, t -> t.equals(curTerm) || !t.isUsing(nullVar))) {
          iter.remove();
          summation.removeBoundedVar(nullVar);
          isModified = true;
        }
      }
    }
    return summation.boundedVars().isEmpty() ? summation.body() : summation;
  }
}
