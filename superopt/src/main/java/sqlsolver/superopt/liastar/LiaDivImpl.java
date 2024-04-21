package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaDivImpl extends LiaStar {
  LiaStar operand1;
  LiaStar operand2;

  LiaDivImpl(LiaStar op1, LiaStar op2) {
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
  public boolean isLia() {
    return true;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LDIV;
  }

  @Override
  public Set<String> getVars() {
    Set<String> result = operand1.getVars();
    result.addAll(operand2.getVars());
    return result;
  }

  @Override
  public LiaStar deepcopy() {
    return mkDiv(innerStar, operand1.deepcopy(), operand2.deepcopy());
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    boolean needsParen1 = (operand1 instanceof LiaPlusImpl);
    boolean needsParen2 = (operand1 instanceof LiaPlusImpl);
    prettyPrintBinaryOp(builder, operand1, operand2,
            needsParen1, needsParen2, " / ");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return operand1.isPrettyPrintMultiLine()
            || operand2.isPrettyPrintMultiLine();
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
  public LiaStar multToBin(int n) {
    operand1 = operand1.multToBin(n);
    operand2 = operand2.multToBin(n);
    return this;
  }

  @Override
  public LiaStar simplifyMult(Map<LiaStar, String> multToVar) {
    assert innerStar == false;
    return this;
  }

  @Override
  public LiaStar expandStar() {
    return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    Expr one = operand1.transToSMT(ctx, varsName, funcsName);
    Expr two = operand2.transToSMT(ctx, varsName, funcsName);
    return ctx.mkDiv((ArithExpr) one, (ArithExpr) two);
  }

  @Override
  public LiaStar.EstimateResult estimate() {
    assert false;
    System.err.println("Estimate not support division !!!");
    return null;
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    assert false;
    System.err.println("Estimate not support division !!!");
    return null;
  }


  @Override
  public boolean equals(Object obj) {
    if(obj == this)
      return true;
    if(obj == null)
      return false;
    if(!(obj instanceof LiaDivImpl))
      return false;
    LiaDivImpl that = (LiaDivImpl) obj;
    return operand1.equals(that.operand1) && operand2.equals(that.operand2);
  }

  @Override
  public int hashCode() {
    int result = 0;
    result = result + operand1.hashCode();
    result = result + operand2.hashCode();
    return result;
  }

  @Override
  public LiaStar simplifyIte() {
    operand1 = operand1.simplifyIte();
    operand2 = operand2.simplifyIte();
    LiaStar tmpZero = mkConst(innerStar, 0);
    if (operand1.equals(tmpZero)) {
      return tmpZero;
    }
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
    return transformer.apply(mkDiv(innerStar, operand10, operand20));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar operand10 = operand1.transformPostOrder(transformer, this);
    LiaStar operand20 = operand2.transformPostOrder(transformer, this);
    return transformer.apply(mkDiv(innerStar, operand10, operand20), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar operand10 = operand1.transformPreOrder(transformer, this);
    LiaStar operand20 = operand2.transformPreOrder(transformer, this);
    return mkDiv(innerStar, operand10, operand20);
  }

  @Override
  public List<LiaStar> subNodes() {
    return List.of(operand1, operand2);
  }
}
