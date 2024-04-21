package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.superopt.liastar.destructor.Destructor;
import sqlsolver.superopt.liastar.transformer.LiaTransformer;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static sqlsolver.superopt.util.VectorSupport.*;

public class LiaSumImpl extends LiaStar {

  List<String> outerVector;
  List<String> innerVector;
  LiaStar constraints;

  LiaSumImpl(List<String> v1, List<String> v2, LiaStar c) {
    outerVector = v1;
    innerVector = v2;
    constraints = c;
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    Map<LiaMulImpl, LiaVarImpl> curMap = new HashMap<>();
    constraints = constraints.mergeMult(curMap);
    for (LiaMulImpl l : curMap.keySet()) {
      LiaStar result = curMap.get(l).deepcopy();
      result.innerStar = true;
      Set<LiaVarImpl> vars = l.collectAllVars();
      // vi = 0 -> v = 0
      for (LiaVarImpl v : vars) {
        final LiaStar cur = v.deepcopy();
        cur.innerStar = true;
        LiaStar newConstr = mkEq(true, cur, mkConst(true, 0));
        newConstr = mkImplies(true, newConstr, mkEq(true, result.deepcopy(), mkConst(true, 0)));
        constraints = mkAnd(true, constraints, newConstr);
      }
      // v = 0 -> v1 = 0 \/ ... \/ vn = 0
      LiaStar eqZeroConstr = null;
      for (LiaVarImpl v : vars) {
        final LiaStar cur = v.deepcopy();
        cur.innerStar = true;
        final LiaStar newConstr = mkEq(true, cur, mkConst(true, 0));
        eqZeroConstr = mkOr(true, eqZeroConstr, newConstr);
      }
      final LiaStar resultIsZero = mkEq(true, result.deepcopy(), mkConst(true, 0));
      constraints = mkAnd(true, constraints, mkImplies(true, resultIsZero, eqZeroConstr));
      // v1 >= 0 /\ ... /\ vn >= 0 -> v >= 0
      LiaStar nonNegative = null;
      for (LiaVarImpl v : vars) {
        final LiaStar cur = v.deepcopy();
        final LiaStar curNN = mkLe(true, mkConst(true, 0), cur);
        nonNegative = mkAnd(true, nonNegative, curNN);
      }
      final LiaStar resultNN = mkLe(true, mkConst(true, 0), result.deepcopy());
      constraints = mkAnd(true, constraints, mkImplies(true, nonNegative, resultNN));
    }
    updateInnerVector();

    return this;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LSUM;
  }

  @Override
  public boolean isLia() {
    return false;
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    builder.print("(");
    for (int i = 0, bound = outerVector.size(); i < bound; i++) {
      builder.print(outerVector.get(i));
      if (i < bound - 1) builder.print(", ");
    }
    builder.println(") ∈");
    builder.print("{(");
    for (int i = 0, bound = innerVector.size(); i < bound; i++) {
      builder.print(innerVector.get(i));
      if (i < bound - 1) builder.print(", ");
    }
    builder.print(") | ");
    builder.indent(4).println();
    constraints.prettyPrint(builder);
    builder.indent(-4).println();
    builder.print("}*");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return true;
  }

  @Override
  public Set<String> getVars() {
    Set<String> result = new HashSet<>(constraints.getVars());
    result.addAll(outerVector);
    return result;
  }

  @Override
  public LiaStar deepcopy() {
    List<String> v1 = new ArrayList<>();
    v1.addAll(outerVector);

    List<String> v2 = new ArrayList<>();
    v2.addAll(innerVector);

    return mkSum(innerStar, v1, v2, constraints.deepcopy());
  }

  @Override
  public Set<String> collectVarNames() {
    return new HashSet<>(outerVector);
  }

  @Override
  public Set<String> collectAllVarNames() {
    Set<String> varSet = constraints.collectAllVarNames();
    varSet.addAll(outerVector);
    return varSet;
  }

  @Override
  public LiaStar multToBin(int n) {
    Set<String> varSet = constraints.collectVarNames();
    int upperBound = 1 << n - 1;
    for (String v : varSet) {
      LiaStar boundCond = mkLe(true, mkVar(true, v), mkConst(true, upperBound));
      constraints = mkAnd(true, constraints, boundCond);
    }
    constraints = constraints.multToBin(n);
    return this;
  }

  @Override
  public LiaStar simplifyMult(Map<LiaStar, String> multToVar) {
    constraints.innerStar = true;

    LiaStar tmp = constraints.simplifyMult(multToVar);
    if (tmp != null) constraints = mkAnd(true, constraints, tmp);

    return null;
  }

  public LiaSumImpl updateInnerVector() {
    // remove unused vars
    final Set<String> allVars = constraints.collectAllVarNames();
    final Set<String> toRemove = new HashSet<>();
    for (int i = innerVector.size() - 1, bound = outerVector.size(); i >= bound; i--) {
      // Assumption: var names in inner vector should be unique
      final String var = innerVector.get(i);
      if (!allVars.contains(var)) toRemove.add(var);
    }
    innerVector.removeAll(toRemove);
    // append absent vars
    final Set<String> varSet = constraints.collectVarNames();
    for (String s : varSet) {
      if (!innerVector.contains(s)) innerVector.add(s);
    }
    return this;
  }

  /**
   * Expand stars with assistance of an extra constraint.
   * If the extra constraint contradicts with <code>this</code>,
   * it may return a LIA formula representing "false" quickly.
   */
  public LiaStar expandStarWithExtraConstraint(LiaStar extra) {
    // destruct constraints into terms connected by AND
    List<LiaStar> terms = new ArrayList<>();
    if (constraints instanceof LiaAndImpl and) {
      and.flatten(terms);
    } else {
      terms.add(constraints);
    }
    // separate terms with/without star
    LiaStar termLia = null;
    LiaStar termNonLia = null;
    for (LiaStar term : terms) {
      if (term.isLia()) {
        termLia = mkAnd(innerStar, termLia, term);
      } else {
        termNonLia = mkAnd(innerStar, termNonLia, term);
      }
    }
    // expand stars in terms with star
    if (termNonLia != null) {
      termNonLia = termNonLia.expandStar();
    }
    constraints = mkAnd(innerStar, termLia, termNonLia);
    updateInnerVector();
    // transform to LIA
    return new LiaTransformer().transform(extra, outerVector, innerVector, termLia, termNonLia);
  }

  @Override
  public LiaStar expandStar() {
    return expandStarWithExtraConstraint(mkTrue(innerStar));
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    System.err.println("there should not be star");
    assert false;
    return null;
  }

  @Override
  public EstimateResult estimate() {
    EstimateResult r = constraints.estimate();
    int rn = r.vars.size() + r.n_extra;
    int d = innerVector.size();
    int n =
        (int)
            Math.ceil(
                6
                    * d
                    * (Math.log(4 * d) / Math.log(2)
                        + 2 * r.m * Math.log(2 + (rn + 1) * r.a) / Math.log(2)));
    double a = Math.pow(2 + (rn + 1) * r.a, 2 * r.m);
    return new EstimateResult(new HashSet(outerVector), n, d, a);
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    EstimateResult r = constraints.estimate();
    int d = innerVector.size(), n = r.vars.size() + r.n_extra, m = r.m;
    double a = r.a;
    int k =
        (int)
            Math.ceil(
                4
                    * d
                    * (Math.log(4 * d) / Math.log(2) + 2 * m * Math.log(2 + (n + 1) * a))
                    / Math.log(2));
    if (suffix.matches("(\\_1)*")) {
      // System.out.printf("expanding star for %s; suffix '%s'...\n", this.toString(), suffix);
      // System.out.printf("expanding star: n = %d, m = %d, a = %f; k = %d\n", n, m, a, k);
    }

    int l = outerVector.size();
    ArithExpr[] sum_vec = new ArithExpr[l];
    for (int i = 0; i < l; i++) sum_vec[i] = ctx.mkInt(0);

    for (int i = 1; i <= k; i++) {
      String suf = String.format("%s_%d", suffix, i);
      for (String var : r.vars) {
        sol.add(ctx.mkGe(ctx.mkIntConst(var + suf), ctx.mkInt(0)));
      }
      for (int j = 0; j < l; j++) {
        sol.add(ctx.mkGe(ctx.mkIntConst(innerVector.get(j) + suf), ctx.mkInt(0)));
        sum_vec[j] = ctx.mkAdd(sum_vec[j], ctx.mkIntConst(innerVector.get(j) + suf));
      }
      sol.add((BoolExpr) constraints.expandStarWithK(ctx, sol, suf));
    }

    BoolExpr e = ctx.mkTrue();
    for (int i = 0; i < l; i++)
      e = ctx.mkAnd(e, ctx.mkEq(ctx.mkIntConst(outerVector.get(i) + suffix), sum_vec[i]));
    return e;
  }

  @Override
  public boolean equals(Object that) {
    if (that == this) return true;
    if (that == null) return false;
    if (!(that instanceof LiaSumImpl)) return false;
    LiaSumImpl tmp = (LiaSumImpl) that;
    return outerVector.equals(tmp.outerVector)
        && innerVector.equals(tmp.innerVector)
        && constraints.equals(tmp.constraints);
  }

  @Override
  public int hashCode() {
    return constraints.hashCode();
  }

  @Override
  public void removeFVsFromInnerVector(Set<String> vars) {
    updateInnerVector();
    List<String> tmp = new ArrayList<>(innerVector);
    for (String v : tmp) {
      if (vars.contains(v)) {
        innerVector.remove(v);
      }
    }
    vars.addAll(innerVector);
    constraints.removeFVsFromInnerVector(vars);
  }

  Set<String> getImportantInnerVars() {
    Set<String> result = new HashSet<>();
    for (int i = 0; i < outerVector.size(); ++i) result.add(innerVector.get(i));
    return result;
  }

  @Override
  public Set<String> collectParamNames() {
    final Set<String> params = constraints.collectParamNames();
    for (String var : innerVector) {
      params.remove(var);
    }
    final Set<String> allVars = constraints.collectVarNames();
    for (String v : allVars) {
      if (!innerVector.contains(v)) {
        params.add(v);
      }
    }
    return params;
  }

  @Override
  public Set<LiaVarImpl> collectParams() {
    final Set<LiaVarImpl> params = constraints.collectParams();
    for (String var : innerVector) {
      params.removeIf(p -> var.equals(p.getName()));
    }
    final Set<LiaVarImpl> allVars = constraints.collectVars();
    for (LiaVarImpl v : allVars) {
      if (!innerVector.contains(v.getName())) {
        params.add(v);
      }
    }
    return params;
  }

  public List<String> getOuterVector() {
    return new ArrayList<>(outerVector);
  }

  public List<String> getInnerVector() {
    return new ArrayList<>(innerVector);
  }

  public LiaStar getConstraints() {
    return constraints;
  }

  @Override
  public LiaStar removeParameter() {
    // TODO: Assumptions:
    //  - This sum is never in a negation (i.e. no "~(... this sum ...)")
    //  - constraints.removeParameter() does not generate new params
    final Set<String> params = collectParamNames();
    constraints = constraints.removeParameter();
    Set<String> vars = constraints.collectVarNames();
    for (String var : vars) {
      if (!params.contains(var) && !innerVector.contains(var)) {
        innerVector.add(var);
      }
    }
    return removeParameterByDestruction();
  }

  // Destruct constraint into a disjunction
  // so that parameters can be removed
  private LiaStar removeParameterByDestruction() {
    // try to destruct the constraint
    final Set<String> params = collectParamNames();
    List<LiaStar> destruction = null;
    try {
      destruction = destructToRemoveParam(constraints, params);
    } catch (OutOfMemoryError e) {
      // #cases is too large; following code does silent fallback
    }
    if (destruction == null) {
      // destruction fails; fallback
      return removeParameterFallback();
    }
    // generate the formula without param
    return reconstruct(destruction, params);
  }

  /**
   * Regard <code>formula</code> as a conjunction of literals,
   * and classify literals according to whether each literal is in the parameter closure.
   * The parameter closure is defined as follows:
   * <ul>
   *     <li>If a literal f contains parameters, then f is in the closure;</li>
   *     <li>If a literal f has common vars with a literal g in the closure, then f is in the closure.</li>
   * </ul>
   * @param formula the conjunction of literals to classify
   * @param noParamPart literals outside the parameter closure will be appended here
   * @param paramPart literals inside the parameter closure will be appended here
   * @param params define which vars are parameters
   */
  public static void classifyConjunctionLiteralsByParamClosure(LiaStar formula, List<LiaStar> noParamPart, List<LiaStar> paramPart, Set<String> params) {
    final List<LiaStar> literals = new ArrayList<>();
    LiaStar.decomposeConjunction(formula, literals);
    final Set<String> paramPartVars = new HashSet<>();
    final Queue<LiaStar> candidates = new LinkedList<>();
    // initial iteration
    for (LiaStar literal : literals) {
      if (SetSupport.intersects(literal.collectVarNames(), params)) {
        paramPart.add(literal);
        paramPartVars.addAll(literal.collectVarNames());
      } else {
        candidates.add(literal);
      }
    }
    // compute closure of params; paramPart is exactly the closure
    while (!candidates.isEmpty()) {
      final List<LiaStar> toMove = new ArrayList<>();
      for (LiaStar f : candidates) {
        final Set<String> fVars = f.collectVarNames();
        if (SetSupport.intersects(fVars, paramPartVars)) {
          toMove.add(f);
          paramPart.add(f);
          paramPartVars.addAll(fVars);
        }
      }
      if (toMove.isEmpty()) break;
      candidates.removeAll(toMove);
    }
    // remaining literals belong to noParamPart
    noParamPart.addAll(candidates);
  }

  // Turn formula into a disjunction.
  private List<LiaStar> destructToRemoveParam(LiaStar formula, Set<String> params) {
    final Set<String> importantVars = getImportantInnerVars();
    // border case
    if (canSeparate(formula, params, importantVars)) {
      // paramPart is separate from noParamPart; no need to destruct anymore
      final List<LiaStar> result = new ArrayList<>();
      result.add(formula.deepcopy());
      return result;
    }
    // otherwise, destruct paramPart
    final List<LiaStar> result = Destructor.destruct(formula, params, importantVars);
    if (result == null) return null;
    for (LiaStar f : result) {
      if (!canSeparate(f, params, importantVars)) return null;
    }
    return result;
  }

  public static boolean canSeparate(LiaStar formula, Set<String> params, Set<String> importantVars) {
    // formula is a conjunction of literals
    // classify the literals: without param; with param
    final List<LiaStar> noParamPart = new ArrayList<>();
    final List<LiaStar> paramPart = new ArrayList<>();
    classifyConjunctionLiteralsByParamClosure(formula, noParamPart, paramPart, params);
    // collect vars
    final Set<String> noParamPartVars = collectVarNames(noParamPart);
    final Set<String> paramPartVars = collectVarNames(paramPart);
    return isSeparateFrom(paramPartVars, noParamPartVars, importantVars);
  }

  /**
   * Whether paramPartVars contain no important vars and no vars from noParamPartVars.
   * When you invoke this method, make sure that noParamPartVars does not contain parameters.
   */
  public static boolean isSeparateFrom(Set<String> paramPartVars, Set<String> noParamPartVars, Set<String> importantVars) {
    if (SetSupport.intersects(paramPartVars, importantVars)) {
      return false;
    }
    return !SetSupport.intersects(paramPartVars, noParamPartVars);
  }

  // directly replace params with new inner vars
  public LiaStar removeParameterFallback() {
    final Set<String> params = collectParamNames();
    final Map<String, String> paramRemap = new HashMap<>();
    for (String param : params) {
      paramRemap.put(param, newVarName());
    }
    final LiaSumImpl result = (LiaSumImpl) replaceFreeAndBoundVars(this, paramRemap);
    result.innerVector.addAll(paramRemap.values());
    return result;
  }

  // construct an equivalent formula with this sum based on the destruction
  private LiaStar reconstruct(List<LiaStar> destruction, Set<String> params) {
    final int vectorSize = outerVector.size();
    // zero case
    if (destruction.isEmpty()) {
      // v in {v'|false}* (i.e. v = 0)
      return eq(innerStar, nameToLia(innerStar, outerVector), liaZero(vectorSize));
    }
    // single case
    if (destruction.size() == 1) {
      // v in {v'|f(v')}*
      return mkSum(innerStar, new ArrayList<>(outerVector), new ArrayList<>(innerVector), destruction.get(0)).separateParams(params);
    }
    // for each element in destruction, generate a vector and a star
    List<LiaStar> vectorSum = null;
    LiaStar starItems = null;
    for (LiaStar constr : destruction) {
      // vector
      final List<String> outV = newVector(vectorSize);
      vectorSum = plus(innerStar, vectorSum, nameToLia(innerStar, outV));
      final List<String> inV = new ArrayList<>(innerVector);
      // star
      final LiaSumImpl star = mkSum(innerStar, outV, inV, constr);
      final LiaStar starItem = star.separateParams(params);
      starItems = mkAnd(innerStar, starItems, starItem);
    }
    // construct the new formula
    final LiaStar vectorEq = eq(innerStar, nameToLia(innerStar, outerVector), vectorSum);
    return mkAnd(innerStar, vectorEq, starItems);
  }

  // Separate the part containing params from constraint,
  // which should be separate from non-param part.
  // Return the formula after separation.
  // Note: this method does not change "this".
  private LiaStar separateParams(Set<String> params) {
    final List<LiaStar> noParamPart = new ArrayList<>(), paramPart = new ArrayList<>();
    classifyConjunctionLiteralsByParamClosure(constraints, noParamPart, paramPart, params);
    if (paramPart.isEmpty()) {
      // this star contains no params; no need to do separation
      return this;
    }
    // separate; turn "this" into "vectorIsZero \/ (newStar /\ paramPartPrime)"
    final LiaStar noParamFormula = mkConjunction(true, noParamPart);
    final LiaStar paramFormula = mkConjunction(innerStar, paramPart).updateInnerStar(innerStar);
    final LiaStar vectorIsZero = eq(innerStar, nameToLia(innerStar, new ArrayList<>(outerVector)), liaZero(innerStar, outerVector.size()));
    final LiaStar newStar = mkSum(innerStar, new ArrayList<>(outerVector), new ArrayList<>(innerVector), noParamFormula).updateInnerVector();
    final Set<String> unimportantVars = paramFormula.collectVarNames();
    unimportantVars.removeAll(params);
    final LiaStar paramPartPrime = replaceVars(paramFormula, unimportantVars);
    return mkOr(innerStar, vectorIsZero, mkAnd(innerStar, newStar, paramPartPrime));
  }

  private static List<String> newVector(int size) {
    final List<String> vector = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      vector.add(newVarName());
    }
    return vector;
  }

  @Override
  public LiaStar simplifyIte() {
    constraints = constraints.simplifyIte();
    return this;
  }

  @Override
  public LiaStar pushUpParameter(Set<String> newVars) {
    Set<String> newInnerVars = new HashSet<>();
    constraints.pushUpParameter(newInnerVars);
    innerVector.addAll(newInnerVars);
    mergeParameterIte();

    LiaStar newFormula = this;
    for (int i = 0; i < outerVector.size(); i++) {
      LiaStar tmp = pushUpParameterForOneOutVar(i, newVars, constraints);
      if (tmp != null) {
        newFormula = mkAnd(innerStar, newFormula, tmp.deepcopy());
      }
    }

    return newFormula.deepcopy();
  }

  private LiaStar pushUpParameterForOneOutVar(int index, Set<String> newVars, LiaStar formula) {
    switch (formula.getType()) {
      case LAND: {
        final LiaStar eq1 = pushUpParameterForOneOutVar(index, newVars, ((LiaAndImpl)formula).operand1);
        final LiaStar eq2 = pushUpParameterForOneOutVar(index, newVars, ((LiaAndImpl)formula).operand2);
        return mkAnd(innerStar, eq1, eq2);
      }
      case LEQ: {
        // recognize equations like "innerVector[index] = exp"
        final String innerVar = innerVector.get(index);
        final LiaEqImpl equation = (LiaEqImpl) formula;
        final LiaStar left = equation.operand1;
        final LiaStar right = equation.operand2;
        final LiaStar exp;
        boolean varOnLeft = true;
        if (left instanceof LiaVarImpl var && innerVar.equals(var.varName)) {
          exp = right;
        } else if (right instanceof LiaVarImpl var && innerVar.equals(var.varName)) {
          exp = left;
          varOnLeft = false;
        } else {
          return null;
        }
        // exp should be in certain form
        LiaStar[] result;
        if (exp instanceof LiaVarImpl || exp instanceof LiaMulImpl) {
          // innerVar = v1 * ... * vN
          result = pushUpParameterInIte(index, null, exp, newVars);
        } else if (exp instanceof LiaIteImpl ite) {
          if (!(ite.operand2 instanceof LiaConstImpl)) {
            return null;
          }
          if (((LiaConstImpl) ite.operand2).value != 0) {
            return null;
          }
          // innerVar = ite(..., ..., 0)
          result = pushUpParameterInIte(index, ite.cond, ite.operand1, newVars);
        } else {
          return null;
        }
        // case: no param is pushed up
        if (result == null) {
          return null;
        }
        // otherwise, update expression with result[1]
        if (varOnLeft) {
          equation.operand2 = result[1];
        } else {
          equation.operand1 = result[1];
        }
        // return the part of exp pushed up
        return result[0];
      }
      default: {
        return null;
      }
    }
  }

  // Given
  // (...u...) in {(...u'...) |
  //   u' = ite(condition, trueValue, 0) /\ ...
  // }*
  // where u is outerVector[varIndex] and u' is innerVector[varIndex],
  // try to push up parameters in condition and trueValue.
  // The formula will become
  // u = ite(condition_param_part, trueValue_param_part * v, 0) /\
  // (...v...) in {(...u'...) |
  //   u' = ite(condition_no_param_part, trueValue_no_param_part, 0) /\ ...
  // }*
  // where condition is condition_param_part * condition_no_param_part
  // and trueValue is trueValue_param_part * trueValue_no_param_part.
  // Return [paramExp, updatedIte]
  // where paramExp is the part of expression pushed up outside the star (i.e. u=ite)
  // and updatedIte is the remaining part of ite.
  private LiaStar[] pushUpParameterInIte(int varIndex, LiaStar condition, LiaStar trueValue, Set<String> newVars) {
    final LiaStar newCondition0, newTrueValue0; // the updated ite
    final LiaStar newCondition1, newTrueValue1; // pushed up

    // push up parameters in trueValue
    final List<LiaStar> items = new ArrayList<>();
    decomposeMults(trueValue, items);
    LiaStar paramMult = null, innerMult = null;
    for (LiaStar item : items) {
      if (item instanceof LiaVarImpl v && !innerVector.contains(v.varName)) {
        paramMult = (paramMult == null) ? item.deepcopy() : mkMul(true, paramMult, item.deepcopy());
      } else {
        innerMult = (innerMult == null) ? item.deepcopy() : mkMul(true, innerMult, item.deepcopy());
      }
    }
    newTrueValue0 = (innerMult == null) ? mkConst(true, 1) : innerMult;

    // push up parameters in newCondition
    LiaStar[] conds = new LiaStar[2];
    conds[0] = null;
    conds[1] = null;
    if (condition != null) {
      decomposeConditions(condition, conds);
    }
    if (paramMult == null && conds[1] == null) {
      // no param is pushed up; stay unchanged
      return null;
    }
    newCondition0 = conds[0];
    newCondition1 = conds[1];
    final String newOutVarName = newVarName();
    final LiaStar newOutVar = mkVar(innerStar, newOutVarName);
    newTrueValue1 = (paramMult == null) ? newOutVar : mkMul(innerStar, paramMult, newOutVar);

    // construct the result
    final LiaStar oldOutVar = mkVar(innerStar, outerVector.get(varIndex));
    final LiaStar ite1 = newCondition1 == null ? newTrueValue1 :
            mkIte(innerStar, newCondition1, newTrueValue1, mkConst(innerStar, 0));
    final LiaStar paramExp = mkEq(innerStar, oldOutVar, ite1);
    final LiaStar updatedIte = newCondition0 == null ? newTrueValue0 :
            mkIte(innerStar, newCondition0, newTrueValue0, mkConst(innerStar, 0));
    // update states
    newVars.add(newOutVarName);
    outerVector.set(varIndex, newOutVarName);
    return new LiaStar[]{paramExp, updatedIte};
  }

  void decomposeConditions(LiaStar cond, LiaStar[] condArray) {
    if (cond instanceof LiaAndImpl) {
      decomposeConditions(((LiaAndImpl) cond).operand1, condArray);
      decomposeConditions(((LiaAndImpl) cond).operand2, condArray);
    } else {
      Set<String> vars = cond.collectVarNames();
      for (String var : vars) {
        if (innerVector.contains(var)) {
          condArray[0] = (condArray[0] == null) ? cond : mkAnd(innerStar, condArray[0], cond);
          return;
        }
      }
      condArray[1] = (condArray[1] == null) ? cond : mkAnd(innerStar, condArray[1], cond);
    }
  }

  boolean notOutInnverVar(String name) {
    for (int i = 0; i < outerVector.size(); ++i) {
      if (innerVector.get(i).equals(name)) {
        return false;
      }
    }
    return innerVector.contains(name);
  }

  Map<LiaVarImpl, LiaStar> findParamVars(List<LiaStar> literals) {
    Map<LiaVarImpl, LiaStar> result = new HashMap<>();
    for (int i = 0; i < literals.size(); ++i) {
      LiaStar lit = literals.get(i);
      if (lit instanceof LiaEqImpl) {
        LiaEqImpl equation = (LiaEqImpl) lit;
        if (equation.operand1 instanceof LiaVarImpl && equation.operand2 instanceof LiaIteImpl) {
          LiaVarImpl var = (LiaVarImpl) equation.operand1;
          LiaIteImpl ite = (LiaIteImpl) equation.operand2;
          LiaStar iteCond = ite.cond;
          LiaStar iteOp1 = ite.operand1;
          LiaStar iteOp2 = ite.operand2;
          if (notOutInnverVar(var.varName)
              && iteOp2.equals(mkConst(innerStar, 0))
              && (iteOp1 instanceof LiaVarImpl)) {
            Set<String> condVars = iteCond.collectVarNames();
            if (!overLap(condVars, new HashSet<>(innerVector))
                && existsOnlyInIteOrEq(literals, var.varName)) {
              result.put(var, ite);
              literals.set(i, null);
            }
          }
        }
      }
    }
    return result;
  }

  LiaStar replaceNotZeroCondInItecond(
          LiaStar iteCond, Map<LiaVarImpl, LiaStar> paramNotZeroCond) {
    if (iteCond instanceof LiaAndImpl) {
      LiaAndImpl tmp = (LiaAndImpl) iteCond;
      tmp.operand1 = replaceNotZeroCondInItecond(tmp.operand1, paramNotZeroCond);
      tmp.operand2 = replaceNotZeroCondInItecond(tmp.operand2, paramNotZeroCond);
    } else if (iteCond instanceof LiaNotImpl) {
      LiaStar body = ((LiaNotImpl) iteCond).operand;
      if (body instanceof LiaEqImpl) {
        LiaStar left = ((LiaEqImpl) body).operand1;
        LiaStar right = ((LiaEqImpl) body).operand2;
        if (paramNotZeroCond.containsKey(left) && right.equals(mkConst(innerStar, 0))) {
          LiaIteImpl tmpIte = (LiaIteImpl) paramNotZeroCond.get(left).deepcopy();
          return mkAnd(
              innerStar,
              tmpIte.cond,
              mkNot(innerStar, mkEq(innerStar, tmpIte.operand1, mkConst(innerStar, 0))));
        }
      }
    }
    return iteCond;
  }

  void replaceNotZeroCondInLit(LiaStar literal, Map<LiaVarImpl, LiaStar> paramNotZeroCond) {
    if (literal instanceof LiaEqImpl) {
      LiaEqImpl equation = (LiaEqImpl) literal;
      LiaStar eqOp1 = equation.operand1;
      LiaStar eqOp2 = equation.operand2;
      if (eqOp2 instanceof LiaIteImpl) {
        LiaIteImpl right = (LiaIteImpl) ((LiaEqImpl) literal).operand2;
        right.cond = replaceNotZeroCondInItecond(right.cond, paramNotZeroCond);
      } else if (eqOp1 instanceof LiaVarImpl && paramNotZeroCond.containsKey(eqOp2)) {
        equation.operand2 = paramNotZeroCond.get(eqOp2);
      }
    }
  }

  void replaceNotZeroCond(
          List<LiaStar> literals, Map<LiaVarImpl, LiaStar> paramNotZeroCond) {
    for (LiaStar lit : literals) {
      if (lit != null) {
        replaceNotZeroCondInLit(lit, paramNotZeroCond);
      }
    }
  }

  void mergeParameterIte() {
    List<LiaStar> literals = new ArrayList<>();
    decomposeConjunction(constraints, literals);
    Map<LiaVarImpl, LiaStar> paramNotZeroCond = findParamVars(literals);
    replaceNotZeroCond(literals, paramNotZeroCond);
    constraints = mkConjunction(innerStar, literals);
  }

  boolean existsOnlyInItecond(LiaStar iteCond, String var) {
    if (iteCond instanceof LiaAndImpl) {
      LiaAndImpl tmp = (LiaAndImpl) iteCond;
      return existsOnlyInItecond(tmp.operand1, var) && existsOnlyInItecond(tmp.operand2, var);
    } else if (iteCond instanceof LiaNotImpl) {
      LiaStar body = ((LiaNotImpl) iteCond).operand;
      if (body instanceof LiaEqImpl) {
        LiaStar left = ((LiaEqImpl) body).operand1;
        LiaStar right = ((LiaEqImpl) body).operand2;
        if (left.equals(mkVar(innerStar, var)) && right.equals(mkConst(innerStar, 0))) {
          return true;
        }
      }
    }
    Set<String> vars = iteCond.collectVarNames();
    return !vars.contains(var);
  }

  boolean existsOnlyInIteOrEqForLiteral(LiaStar literal, String var) {
    if (literal instanceof LiaEqImpl) {
      LiaEqImpl equation = (LiaEqImpl) literal;
      LiaStar eqOp1 = equation.operand1;
      LiaStar eqOp2 = equation.operand2;
      if (eqOp2 instanceof LiaIteImpl) {
        LiaIteImpl ite = (LiaIteImpl) eqOp2;
        LiaStar iteOp1 = ite.operand1;
        Set<String> vars = iteOp1.collectVarNames();
        if (vars.contains(var)) {
          return false;
        }
        LiaStar iteOp2 = ite.operand2;
        vars = iteOp2.collectVarNames();
        if (vars.contains(var)) {
          return false;
        }
        LiaStar iteCond = ite.cond;
        return existsOnlyInItecond(iteCond, var);
      } else if (eqOp1 instanceof LiaVarImpl && eqOp2 instanceof LiaVarImpl) {
        if (((LiaVarImpl) eqOp2).varName.equals(var)) {
          return true;
        }
      }
    }
    Set<String> vars = literal.collectVarNames();
    return !vars.contains(var);
  }

  boolean existsOnlyInIteOrEq(List<LiaStar> literals, String var) {
    for (LiaStar literal : literals) {
      if (literal != null) {
        if (literal instanceof LiaVarImpl && ((LiaVarImpl) literal).varName.equals(var)) {
          continue;
        }
        if (!existsOnlyInIteOrEqForLiteral(literal, var)) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean OuterVarsEquals() {
    if (outerVector.size() != 2) return false;

    String inVar1 = innerVector.get(0);
    String inVar2 = innerVector.get(1);

    List<LiaStar> literals = new ArrayList<>();
    decomposeConjunction(constraints, literals);
    List<LiaEqImpl> equations = new ArrayList<>();
    for (LiaStar l : literals) {
      if (!(l instanceof LiaEqImpl)) continue;
      LiaEqImpl equation = (LiaEqImpl) l;
      if (equation.operand1 instanceof LiaVarImpl && equation.operand2 instanceof LiaVarImpl) {
        equations.add(equation);
      }
    }

    Set<String> vars = new HashSet<>();
    vars.add(inVar1);
    boolean modified = false;
    do {
      modified = false;
      int size = vars.size();
      for (LiaEqImpl l : equations) {
        String name1 = ((LiaVarImpl) l.operand1).varName;
        String name2 = ((LiaVarImpl) l.operand2).varName;
        if (vars.contains(name1) || vars.contains(name2)) {
          vars.add(name1);
          vars.add(name2);
        }
      }
      modified = (vars.size() != size);
    } while (modified);

    return vars.contains(inVar2);
  }

  @Override
  public LiaStar subformulaWithoutStar() {
    return LiaStar.mkTrue(false);
  }

  @Override
  public int embeddingLayers() {
    return constraints.embeddingLayers() + 1;
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    LiaStar constraints0 = constraints.transformPostOrder(transformer);
    return transformer.apply(
            mkSum(innerStar, new ArrayList<>(outerVector), new ArrayList<>(innerVector), constraints0));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar constraints0 = constraints.transformPostOrder(transformer, this);
    return transformer.apply(
            mkSum(innerStar, new ArrayList<>(outerVector), new ArrayList<>(innerVector), constraints0),
            parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar constraints0 = constraints.transformPreOrder(transformer, this);
    return mkSum(innerStar, new ArrayList<>(outerVector), new ArrayList<>(innerVector), constraints0);
  }

  @Override
  public List<LiaStar> subNodes() {
    return List.of(constraints);
  }
}
