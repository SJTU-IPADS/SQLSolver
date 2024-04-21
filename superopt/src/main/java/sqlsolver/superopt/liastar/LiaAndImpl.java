package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;


public class LiaAndImpl extends LiaStar {

  LiaStar operand1;
  LiaStar operand2;

  LiaAndImpl(LiaStar op1, LiaStar op2) {
    operand1 = op1;
    operand2 = op2;
    innerStar = false;
  }

  private void flattenFormula(List<LiaStar> operands, LiaStar f) {
    if (f instanceof LiaAndImpl and) {
      and.flatten(operands);
    } else {
      operands.add(f);
    }
  }

  /** Flatten an "AND" tree into a list of its leaves. */
  public void flatten(List<LiaStar> operands) {
    flattenFormula(operands, operand1);
    flattenFormula(operands, operand2);
  }

  @Override
  public Set<String> collectVarNames() {
    Set<String> varSet = operand1.collectVarNames();
    varSet.addAll(operand2.collectVarNames());
    return varSet;
  }

  @Override
  public Set<String> collectAllVarNames() {
    Set<String> varSet = operand1.collectAllVarNames();
    varSet.addAll(operand2.collectAllVarNames());
    return varSet;
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    boolean needsParen1 = (operand1 instanceof LiaOrImpl);
    boolean needsParen2 = (operand2 instanceof LiaOrImpl);
    prettyPrintBinaryOp(builder, operand1, operand2,
            needsParen1, needsParen2, true, " /\\ ");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return operand1.isPrettyPrintMultiLine()
            || operand2.isPrettyPrintMultiLine();
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LAND;
  }

  @Override
  public boolean isLia() {
    return operand1.isLia() && operand2.isLia();
  }

  @Override
  public Set<String> getVars() {
    Set<String> result = operand1.getVars();
    result.addAll(operand2.getVars());
    return result;
  }

  @Override
  public LiaStar deepcopy() {
    LiaStar tmp = mkAnd(innerStar, operand1.deepcopy(), operand2.deepcopy());
    return tmp;
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    operand1 = operand1.mergeMult(multToVar);
    operand2 = operand2.mergeMult(multToVar);
    return this;
  }

  @Override
  public LiaStar multToBin(int n) {
    operand1 = operand1.multToBin(n);
    operand2 = operand2.multToBin(n);
    return this;
  }

  @Override
  public LiaStar simplifyMult(Map<LiaStar, String> multToVar) {
    operand1.innerStar = innerStar;
    operand2.innerStar = innerStar;
    LiaStar[] tmp = new LiaStar[2];
    tmp[0] = operand1.simplifyMult(multToVar);
    tmp[1] = operand2.simplifyMult(multToVar);
    return liaAndConcat(tmp);
  }

  /**
   * If the formula is like f1 /\ f2 /\ ... /\ fM /\ s1* /\ s2* /\ ... /\ sN*,
   * f1 ... fM are added to <code>nonStars</code>,
   * and s1* ... sN* are added to <code>stars</code>.
   */
  private void separateNonStarsAndStars(List<LiaStar> nonStars, List<LiaSumImpl> stars) {
    // flatten the "AND" tree to a list of its leaves "a AND b AND ..."
    List<LiaStar> leaves = new ArrayList<>();
    flatten(leaves);
    // separate leaves into two lists
    for (LiaStar term : leaves) {
      if (term instanceof LiaSumImpl sum) {
        stars.add(sum);
      } else {
        nonStars.add(term);
      }
    }
  }

  @Override
  public LiaStar expandStar() {
    // separate star and non-star operands in its leaves
    List<LiaStar> nonStars = new ArrayList<>();
    List<LiaSumImpl> stars = new ArrayList<>();
    separateNonStarsAndStars(nonStars, stars);
    // expand stars in non-stars operands
    // then conjunct them into g
    LiaStar g = mkConjunction(innerStar,
            nonStars.stream().map(LiaStar::expandStar).toList());
    if (stars.isEmpty()) {
      return g;
    }
    // for each f* in stars, transform f* into LIA under constraint g
    LiaStar result = g;
    for (LiaSumImpl sum : stars) {
      result = mkAnd(innerStar, result, sum.expandStarWithExtraConstraint(g));
    }
    return result;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    Expr c1 = operand1.transToSMT(ctx, varsName, funcsName);
    Expr c2 = operand2.transToSMT(ctx, varsName, funcsName);
    return ctx.mkAnd((BoolExpr) c1, (BoolExpr) c2);
  }

  @Override
  public EstimateResult estimate() {
    EstimateResult r1 = operand1.estimate();
    EstimateResult r2 = operand2.estimate();
    r1.vars.addAll(r2.vars);
    return new EstimateResult(
            r1.vars,
            r1.n_extra + r2.n_extra,
            r1.m + r2.m,
            Math.max(r1.a,r2.a)
    );
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return ctx.mkAnd(
            (BoolExpr)operand1.expandStarWithK(ctx, sol, suffix),
            (BoolExpr)operand2.expandStarWithK(ctx, sol, suffix)
    );
  }

  @Override
  public boolean equals(Object that) {
    if(that == this)
      return true;
    if(that == null)
      return false;
    if(!(that instanceof LiaAndImpl))
      return false;
    LiaAndImpl tmp = (LiaAndImpl) that;
    return operand1.equals(tmp.operand1) && operand2.equals(tmp.operand2);
  }

  @Override
  public int hashCode() {
    return operand1.hashCode() + operand2.hashCode();
  }

  @Override
  public void removeFVsFromInnerVector(Set<String> vars) {
    operand1.removeFVsFromInnerVector(vars);
    operand2.removeFVsFromInnerVector(vars);
  }

  @Override
  public LiaStar removeParameter() {
    operand1 = operand1.removeParameter();
    operand2 = operand2.removeParameter();
    return this;
  }

  @Override
  public LiaStar pushUpParameter(Set<String> newVars) {
    operand1 = operand1.pushUpParameter(newVars);
    operand2 = operand2.pushUpParameter(newVars);
    return this;
  }

  @Override
  public Set<String> collectParamNames() {
    final Set<String> result = operand1.collectParamNames();
    result.addAll(operand2.collectParamNames());
    return result;
  }

  @Override
  public Set<LiaVarImpl> collectParams() {
    final Set<LiaVarImpl> result = operand1.collectParams();
    result.addAll(operand2.collectParams());
    return result;
  }

  @Override
  public LiaStar simplifyIte() {
    operand1 = operand1.simplifyIte();
    operand2 = operand2.simplifyIte();
    if(isTrue(operand1)) {
      return operand2;
    } else if(isTrue(operand2)) {
      return operand1;
    } else if(operand1.equals(operand2)) {
      return operand1;
    } else {
      return this;
    }
  }



  @Override
  public LiaStar mergeSameVars() {
    ArrayList<LiaStar> literals = new ArrayList<>();
    decomposeConjunction(this, literals);
    HashMap<String, String> renameMapping = new HashMap<>();
    for (int i = 0; i < literals.size(); ++ i) {
      LiaStar l = literals.get(i);
      if (l == null) continue;
      if (l instanceof LiaSumImpl) {
        LiaSumImpl sum = (LiaSumImpl) l;
        sum.constraints = sum.constraints.mergeSameVars();
        if (sum.OuterVarsEquals()) {
          String var1 = sum.outerVector.get(0);
          String var2 = sum.outerVector.get(1);
          renameMapping.put(var1, var2);
//          literals.set(i, null);
        }
      }
    }

    LiaStar result = constructConjunctions(innerStar, literals);
    return replaceFreeAndBoundVars(result, renameMapping);
  }

  @Override
  public LiaStar subformulaWithoutStar() {
    operand1 = operand1.subformulaWithoutStar();
    operand2 = operand2.subformulaWithoutStar();
    return this;
  }

  @Override
  public int embeddingLayers() {
    int leftLayer = operand1.embeddingLayers();
    int rightLayer = operand2.embeddingLayers();
    return (leftLayer > rightLayer) ? leftLayer : rightLayer;
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    final LiaStar operand10 = operand1.transformPostOrder(transformer);
    final LiaStar operand20 = operand2.transformPostOrder(transformer);
    return transformer.apply(mkAnd(innerStar, operand10, operand20));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    final LiaStar operand10 = operand1.transformPostOrder(transformer, this);
    final LiaStar operand20 = operand2.transformPostOrder(transformer, this);
    return transformer.apply(mkAnd(innerStar, operand10, operand20), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    final LiaStar operand10 = operand1.transformPreOrder(transformer, this);
    final LiaStar operand20 = operand2.transformPreOrder(transformer, this);
    return mkAnd(innerStar, operand10, operand20);
  }

  @Override
  public List<LiaStar> subNodes() {
    return List.of(operand1, operand2);
  }
}
