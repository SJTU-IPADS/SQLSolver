package sqlsolver.superopt.liastar.destructor;

import sqlsolver.common.utils.SetSupport;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.LiaSumImpl;

import java.util.*;

public abstract class Destructor {
  private final Set<String> params, importantVars;

  protected Destructor(Set<String> params, Set<String> importantVars) {
    this.params = params;
    this.importantVars = importantVars;
  }

  // +-------------------+
  // | Public interfaces |
  // +-------------------+

  /**
   * Destruct a formula using a default routine (i.e. a fixed combination of concrete destructors).
   */
  public static List<LiaStar> destruct(LiaStar formula, Set<String> params, Set<String> importantVars) {
    final Destructor iteEqDestructor = new IteDestructor(params, importantVars);
    List<LiaStar> result = iteEqDestructor.destruct(formula);
    if (result == null) return null;
    final Destructor orDestructor = new OrDestructor(params, importantVars);
    return orDestructor.destruct(result);
  }

  /**
   * Destruct formulas in a batch.
   * All parts after destruction are included in the returned list.
   * @see #destruct(LiaStar)
   */
  public List<LiaStar> destruct(List<LiaStar> formulas) {
    List<LiaStar> result = new ArrayList<>();
    for (LiaStar formula : formulas) {
      result.addAll(destruct(formula));
    }
    return result;
  }

  /**
   * Destruct formula into a disjunction of cases.
   */
  public abstract List<LiaStar> destruct(LiaStar formula);

  // +--------------------+
  // | Subclass utilities |
  // +--------------------+

  protected Set<String> getParams() {
    return new HashSet<>(params);
  }

  /**
   * Whether parameters in formula can be separated from formula.
   */
  protected boolean canSeparate(LiaStar formula) {
    return LiaSumImpl.canSeparate(formula, params, importantVars);
  }

  /**
   * Whether paramPartVars contain no important vars and no vars from noParamPartVars.
   * When you invoke this method, make sure that noParamPartVars does not contain parameters.
   */
  protected boolean isSeparateFrom(Set<String> paramPartVars, Set<String> noParamPartVars) {
    return LiaSumImpl.isSeparateFrom(paramPartVars, noParamPartVars, importantVars);
  }

  protected boolean isParameterized(LiaStar f) {
    return SetSupport.intersects(f.collectVarNames(), params);
  }

  /** Classify literals according to whether each literal is parameterized. */
  protected void classifyConjunctionLiteralsByParam(LiaStar formula, List<LiaStar> noParamPart, List<LiaStar> paramPart) {
    final List<LiaStar> literals = new ArrayList<>();
    LiaStar.decomposeConjunction(formula, literals);
    for (LiaStar literal : literals) {
      if (isParameterized(literal)) {
        paramPart.add(literal);
      } else {
        noParamPart.add(literal);
      }
    }
  }
}
