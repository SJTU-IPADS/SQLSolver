package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaLtImpl extends LiaStar {

  LiaStar operand1;
  LiaStar operand2;


  LiaLtImpl(LiaStar op1, LiaStar op2) {
    operand1 = op1;
    operand2 = op2;
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
    prettyPrintBinaryOp(builder, operand1, operand2,
            false, false, " < ");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return operand1.isPrettyPrintMultiLine()
            || operand2.isPrettyPrintMultiLine();
  }

  @Override
  public LiaOpType getType() {
      return LiaOpType.LLT;
  }

  @Override
  public boolean isLia() {
      return true;
  }

  @Override
  public Set<String> getVars() {
    Set<String> result = operand1.getVars();
    result.addAll(operand2.getVars());
    return result;
  }

  @Override
  public LiaStar deepcopy() {
      return mkLt(innerStar, operand1.deepcopy(), operand2.deepcopy());
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

  @Override
  public LiaStar expandStar() {
      return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    ArithExpr one = (ArithExpr) operand1.transToSMT(ctx, varsName, funcsName);
    ArithExpr two = (ArithExpr) operand2.transToSMT(ctx, varsName, funcsName);
    return ctx.mkLt(one, two);
  }

  @Override
  public EstimateResult estimate() {
    EstimateResult r1 = operand1.estimate();
    EstimateResult r2 = operand2.estimate();
    r1.vars.addAll(r2.vars);
    return new EstimateResult(
      r1.vars,
      r1.n_extra + r2.n_extra + 1,
      r1.m + r2.m + 1,
      Math.max(r1.a, r2.a)
    );
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return ctx.mkLt(
      (ArithExpr)operand1.expandStarWithK(ctx, sol, suffix),
      (ArithExpr)operand2.expandStarWithK(ctx, sol, suffix)
    );
  }

  @Override
  public boolean equals(Object that) {
    if(that == this)
      return true;
    if(that == null)
      return false;
    if(!(that instanceof LiaLtImpl))
      return false;
    LiaLtImpl tmp = (LiaLtImpl) that;
    return operand1.equals(tmp.operand1) && operand2.equals(tmp.operand2);
  }

  @Override
  public int hashCode() {
      return operand1.hashCode() + operand2.hashCode();
  }

  @Override
  public LiaStar simplifyIte() {
    operand1 = operand1.simplifyIte();
    operand2 = operand2.simplifyIte();
    return this;
  }

  @Override
  public int embeddingLayers() {
    return 0;
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    LiaStar operand10 = operand1.transformPostOrder(transformer);
    LiaStar operand20 = operand2.transformPostOrder(transformer);
    return transformer.apply(mkLt(innerStar, operand10, operand20));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar operand10 = operand1.transformPostOrder(transformer, this);
    LiaStar operand20 = operand2.transformPostOrder(transformer, this);
    return transformer.apply(mkLt(innerStar, operand10, operand20), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar operand10 = operand1.transformPreOrder(transformer, this);
    LiaStar operand20 = operand2.transformPreOrder(transformer, this);
    return mkLt(innerStar, operand10, operand20);
  }

  @Override
  public List<LiaStar> subNodes() {
    return List.of(operand1, operand2);
  }
}
