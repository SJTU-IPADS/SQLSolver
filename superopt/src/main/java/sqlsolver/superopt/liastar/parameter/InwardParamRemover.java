package sqlsolver.superopt.liastar.parameter;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sqlsolver.superopt.liastar.LiaAndImpl;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.LiaSumImpl;
import sqlsolver.superopt.liastar.LiaVarImpl;
import sqlsolver.superopt.util.Z3Support;

import static sqlsolver.superopt.liastar.LiaStar.*;

/**
 * Recognize and copy parameterized propositions into inner stars, and then replace parameters with
 * new inner vars. For example:
 *
 * <p>~(u1 = u2) /\ (u1, u2) ∈ {(u11, u12, u15, u16) | u11 = ite(u15 = u16, u13, 0) /\ u12 = u14 /\
 * (u13, u14) ∈ {(u21, u22) | u21 = 1 /\ u22 = ite(u15 = u16, 1, 0) }* }* -> ~(u1 = u2) /\ (u1, u2)
 * ∈ {(u11, u12, u15, u16) | ( u15 = u16 /\ u11 = ite(u15 = u16, u13, 0) /\ u12 = u14 /\ (u13, u14)
 * ∈ {(u21, u22) | u15 = u16 /\ u21 = 1 /\ u22 = ite(u15 = u16, 1, 0) }* ) \/ ( ~(u15 = u16) /\ u11
 * = ite(u15 = u16, u13, 0) /\ u12 = u14 /\ (u13, u14) ∈ {(u21, u22) | ~(u15 = u16) /\ u21 = 1 /\
 * u22 = ite(u15 = u16, 1, 0) }* ) }*
 */
public class InwardParamRemover {
  private final LiaStar formula;
  // states
  private Set<SemanticLiaStar> destructedLiterals;

  public static LiaStar removeParameter(LiaStar formula) {
    return new InwardParamRemover(formula).removeParameter();
  }

  public InwardParamRemover(LiaStar formula) {
    this.formula = formula;
  }

  public LiaStar removeParameter() {
    // reset/init
    destructedLiterals = new HashSet<>();
    // copy parameterized literals inward
    LiaStar result = formula.transformPreOrder(this::copyParamLiteralsInward);
    // remove parameters
    result = result.transformPostOrder(f -> {
      if (f instanceof LiaSumImpl sum) {
        return sum.removeParameterFallback();
      }
      return f;
    });
    return result;
  }

  private static boolean isParamLiteral(LiaStar formula, Set<LiaVarImpl> params) {
    if (!formula.getType().isAtomicPredicate()) return false;
    final List<LiaStar> subs = formula.subNodes();
    return all(subs, sub -> sub instanceof LiaVarImpl var && params.contains(var));
  }

  /**
   * Recognize the literals that should be destructed in the current star layer
   * (including the outermost layer, which can be something other than a star)
   * and destruct them.
   * Any literal in the current star layer
   * that contains params in direct sub-layers
   * should be destructed and copied inward.
   */
  // TODO: more complete impl
  private LiaStar copyParamLiteralsInward(LiaStar formula) {
    // collect params in direct sub-layers
    final Set<LiaVarImpl> params = formula.collectParams();
    if (params.isEmpty()) return null;
    // case analysis & implication
    final CaseAnalysis cases = new CaseAnalysis();
    doCaseAnalysisByParams(cases, params);
    final LiaStar conclusions = implyByParams(formula, params);
    if (cases.getUnitCount() == 0 && conclusions == null) return null;
    // destruct the current layer by case analysis
    final List<LiaStar> destruction = cases.destruct();
    // for each case, construct a formula
    final List<LiaStar> result = new ArrayList<>();
    final boolean innerStar = formula.isInStar();
    for (LiaStar cas : destruction) {
      // remove impossible cases
      if (fastReject(mkAnd(false, cas, formula))) continue;
      // copy case condition inward
      final LiaStar prefix = mkAnd(innerStar, LiaStar.deepcopy(conclusions), cas.deepcopy());
      final LiaStar caseCondInwardFormula = formula.transformPostOrder(f -> {
        if (f instanceof LiaSumImpl s) {
          // original constraints /\ case condition
          final LiaStar newConstraints = mkAnd(true, prefix.deepcopy(), s.getConstraints().deepcopy());
          return mkSum(s.isInStar(), s.getOuterVector(), s.getInnerVector(), newConstraints)
                  .updateInnerStar(s.isInStar());
        }
        return f;
      });
      result.add(mkAnd(innerStar, prefix.deepcopy(), caseCondInwardFormula));
    }
    return mkDisjunction(innerStar, result);
  }

  private void doCaseAnalysisByParams(CaseAnalysis cases, Set<LiaVarImpl> params) {
    // collect literals containing those params
    formula.transformPreOrder(f -> {
      // span literals in a conjunction
      final List<LiaStar> terms = new ArrayList<>();
      if (f instanceof LiaAndImpl and) {
        and.flatten(terms);
      } else {
        terms.add(f);
      }
      // check whether each literal
      if (all(terms, t -> isParamLiteral(t, params))) {
        cases.addCaseUnit(terms);
        // no further recursion
        return f;
      } else {
        return null;
      }
    });
  }

  private LiaStar implyByParams(LiaStar formula, Set<LiaVarImpl> params) {
    LiaStar result = null;
    final LiaStar formulaWithoutStar = ruleOutStars(formula);
    final List<LiaVarImpl> paramList = new ArrayList<>(params);
    final boolean innerStar = formula.isInStar();
    // for each pair of params
    for (int i = 0, bound = paramList.size(); i < bound; i++) {
      for (int j = i + 1; j < bound; j++) {
        final LiaStar param1 = LiaStar.mkVar(innerStar, paramList.get(i).getName(), paramList.get(i).getValueType());
        final LiaStar param2 = LiaStar.mkVar(innerStar, paramList.get(j).getName(), paramList.get(j).getValueType());
        // check whether formula implies "param1 = param2"
        final LiaStar eq = LiaStar.mkEq(innerStar, param1, param2);
        final LiaStar toCheckEq = LiaStar.mkImplies(innerStar, formulaWithoutStar, eq);
        if (Z3Support.isValidLia(toCheckEq)) {
          result = LiaStar.mkAnd(innerStar, result, eq);
          continue;
        }
        // check whether formula implies "param1 != param2"
        final LiaStar neq = LiaStar.mkNot(innerStar, eq);
        final LiaStar toCheckNeq = LiaStar.mkImplies(innerStar, formulaWithoutStar, neq);
        if (Z3Support.isValidLia(toCheckNeq)) {
          result = LiaStar.mkAnd(innerStar, result, neq);
        }
      }
    }
    return result;
  }

  private boolean fastReject(LiaStar formula) {
    // check unsatisfiability
    final LiaStar toCheck = mkNot(false, ruleOutStars(formula));
    return Z3Support.isValidLia(toCheck);
  }

  // rule out star formulas from formula
  private LiaStar ruleOutStars(LiaStar formula) {
    // flatten AND
    List<LiaStar> terms = new ArrayList<>();
    if (formula instanceof LiaAndImpl and) {
      and.flatten(terms);
    } else {
      terms.add(formula);
    }
    // ignore sums
    terms = filter(terms, t -> !(t instanceof LiaSumImpl));
    return mkConjunction(false, terms);
  }
}
