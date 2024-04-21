package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaOrImpl extends LiaStar {

  LiaStar operand1;
  LiaStar operand2;

  LiaOrImpl(LiaStar op1, LiaStar op2) {
    operand1 = op1;
    operand2 = op2;
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    operand1 = operand1.mergeMult(multToVar);
    operand2 = operand2.mergeMult(multToVar);
    return this;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LOR;
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
    LiaStar tmp = mkOr(innerStar, operand1.deepcopy(), operand2.deepcopy());
    return tmp;
  }

  @Override
  public Set<String> collectVarNames() {
    final Set<String> varSet = operand1.collectVarNames();
    varSet.addAll(operand2.collectVarNames());
    return varSet;
  }

  @Override
  public Set<LiaVarImpl> collectParams() {
    final Set<LiaVarImpl> result = operand1.collectParams();
    result.addAll(operand2.collectParams());
    return result;
  }

  @Override
  public Set<String> collectAllVarNames() {
    Set<String> varSet = operand1.collectAllVarNames();
    varSet.addAll(operand2.collectAllVarNames());
    return varSet;
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    boolean needsParen1 = (operand1 instanceof LiaAndImpl);
    boolean needsParen2 = (operand2 instanceof LiaAndImpl);
    prettyPrintBinaryOp(builder, operand1, operand2,
            needsParen1, needsParen2, " \\/ ");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return operand1.isPrettyPrintMultiLine()
            || operand2.isPrettyPrintMultiLine();
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

  @Override
  public LiaStar expandStar() {
    operand1 = operand1.expandStar();
    operand2 = operand2.expandStar();
    return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    BoolExpr one = (BoolExpr) operand1.transToSMT(ctx, varsName, funcsName);
    BoolExpr two = (BoolExpr) operand2.transToSMT(ctx, varsName, funcsName);
    return ctx.mkOr(one, two);
  }

  @Override
  public EstimateResult estimate() {
    EstimateResult r1 = operand1.estimate();
    EstimateResult r2 = operand2.estimate();
    r1.vars.addAll(r2.vars);
    return new EstimateResult(
            r1.vars,
            Math.max(r1.n_extra, r2.n_extra),
            Math.max(r1.m, r2.m),
            Math.max(r1.a, r2.a)
    );
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return ctx.mkOr(
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
    if(!(that instanceof LiaOrImpl))
      return false;
    LiaOrImpl tmp = (LiaOrImpl) that;
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
    Set<String> result = operand1.collectParamNames();
    result.addAll(operand2.collectParamNames());
    return result;
  }

  @Override
  public LiaStar simplifyIte() {
    operand1 = operand1.simplifyIte();
    operand2 = operand2.simplifyIte();
    if(isFalse(operand1)) {
      return operand2;
    } else if(isFalse(operand2)) {
      return operand1;
    } else if(operand1.equals(operand2)) {
      return operand1;
    } else {
      return this;
    }
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
    return transformer.apply(mkOr(innerStar, operand10, operand20));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    final LiaStar operand10 = operand1.transformPostOrder(transformer, this);
    final LiaStar operand20 = operand2.transformPostOrder(transformer, this);
    return transformer.apply(mkOr(innerStar, operand10, operand20), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    final LiaStar operand10 = operand1.transformPreOrder(transformer, this);
    final LiaStar operand20 = operand2.transformPreOrder(transformer, this);
    return mkOr(innerStar, operand10, operand20);
  }

  @Override
  public List<LiaStar> subNodes() {
    return List.of(operand1, operand2);
  }
}
