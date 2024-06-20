package sqlsolver.superopt.liastar.transformer;

import com.microsoft.z3.*;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.logic.SqlSolver;
import sqlsolver.superopt.util.Timeout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sqlsolver.superopt.util.Z3Support.defineVarsByNamesWithLimit;
import static sqlsolver.superopt.util.Z3Support.defineVarsByVars;
import static sqlsolver.superopt.util.VectorSupport.*;

public class AugmentationFinder {
  public static final String MSG_SLS_UNKNOWN = "the corresponding SLS is unknown";
  // absolute value of each dimension of an augmenting vector must not exceed this value
  public static final int AUGMENT_VECTOR_LIMIT = 4;

  private final SemiLinearSet sls;
  // the name of vector dimensions; useful to check satisfiability
  private final List<String> slsVector;
  // the target to approach
  private final LiaStar target;

  // finder states
  // the maximum absolute value of dimensions in found vectors
  private int limitCheckpoint = 1;
  // the maximum of sum of dimensions in found vectors
  private int totalLimitCheckpoint = 1;

  public AugmentationFinder(SemiLinearSet sls, List<String> slsVector, LiaStar target) {
    this.sls = sls;
    this.slsVector = slsVector;
    this.target = target;
  }

  /**
   * Find an augmentation vector.
   * Return null if no vector is found.
   * Raise an exception if existence of such vector cannot be decided.
   */
  public List<Long> find() {
    // if there is no augmentation vector with unlimited scale
    // then end augmentation
    if (find(false, 0, 0) == null) {
      return null;
    }
    // try to find an augmentation vector with increasing scale
    for (int limit = limitCheckpoint; limit <= AUGMENT_VECTOR_LIMIT;
         limit <<= 1, limitCheckpoint <<= 1, totalLimitCheckpoint = 0) {
      final List<Long> result = find(true, -limit, limit);
      if (result != null) {
        return result;
      }
    }
    // too large vector may cause performance degrade; abort
    reportUnknownSls();
    return null;
  }

  /**
   * Find an augmentation vector.
   * If withLimit is set, all dimension of the vector is required to be within the given bound.
   * Return null if no vector is found.
   */
  private List<Long> find(boolean withLimit, int lowerBound, int upperBound) {
    try (final Context ctx = new Context()) {
      // construct "target(vector) /\ ~LIA(sls)(vector)"
      // where LIA(sls)(vector) is a LIA formula indicating that vector is in sls
      final Map<String, Expr> varDef = new HashMap<>();
      final BoolExpr limitFormula = defineVarsByNamesWithLimit(ctx, varDef, slsVector, withLimit, lowerBound, upperBound);
      final BoolExpr varConstraint = defineVarsByVars(ctx, varDef, target.collectAllVars());
      final BoolExpr targetFormula = (BoolExpr) target.transToSMT(ctx, varDef);
      final BoolExpr slsNegFormula = sls.toLiaZ3Neg(ctx, varDef, slsVector);
      final BoolExpr mainFormula = ctx.mkAnd(limitFormula, varConstraint, targetFormula, slsNegFormula);
      if (!withLimit) {
        return findByZ3(ctx, varDef, mainFormula);
      }
      // try different constraints to guide the search for augmentation
      boolean isUnknown = false;
      final int vectorSize = slsVector.size();
      final int maxTotalUpperBound = upperBound * vectorSize;
      final List<LiaStar> vectorLia = nameToLia(slsVector);
      // sum of dimensions of slsVector
      final LiaStar vectorTotalLia = vectorLia.stream()
              .reduce((a, b) -> LiaStar.mkPlus(false, a, b)).get();
      final ArithExpr vectorTotal = (ArithExpr) vectorTotalLia.transToSMT(ctx, varDef);
      // try augmentation vectors (prefer small sum of dimensions)
      for (int totalUpperBound = totalLimitCheckpoint; totalUpperBound < maxTotalUpperBound;
           totalUpperBound++, totalLimitCheckpoint++) {
        final BoolExpr constraint = ctx.mkLe(vectorTotal, ctx.mkInt(totalUpperBound));
        try {
          final List<Long> result = findByZ3(ctx, varDef, ctx.mkAnd(mainFormula, constraint));
          if (result != null) return result;
        } catch (Throwable e) {
          Timeout.bypassTimeout(e);
          isUnknown = true;
        }
      }
      // if some constraint leads to unknown
      // and augmentation vector has not been found,
      // then existence of augmentation vector is considered unknown
      if (isUnknown) {
        reportUnknownSls();
      }
      return null;
    }
  }

  /**
   * Find augmentation using the given formula.
   */
  private List<Long> findByZ3(Context ctx, Map<String, Expr> varDef, BoolExpr toCheck) {
    // check its satisfiability
    final Solver s = ctx.mkSolver(ctx.tryFor(ctx.mkTactic("lia"), SqlSolver.Z3_TIMEOUT));
    s.add(toCheck);
    final Status q = s.check();
    if (q == Status.UNSATISFIABLE) {
      return null;
    }
    if (q == Status.UNKNOWN) {
      reportUnknownSls();
    }
    // satisfiable; get vector from the model
    final List<Long> vectorInstance = new ArrayList<>();
    for (String var : slsVector) {
      final Long value = Long.parseLong(s.getModel().getConstInterp(varDef.get(var)).toString());
      vectorInstance.add(value);
    }
    return vectorInstance;
  }

  private void reportUnknownSls() {
    throw new RuntimeException(MSG_SLS_UNKNOWN);
  }
}
