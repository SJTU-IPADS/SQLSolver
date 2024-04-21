package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaNotImpl extends LiaStar {

  LiaStar operand;

  LiaNotImpl(LiaStar op) {
    operand = op;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LNOT;
  }

  @Override
  public boolean isLia() {
    return operand.isLia();
  }

  @Override
  public Set<String> getVars() {
    return operand.getVars();
  }

  @Override
  public LiaStar deepcopy() {
    return mkNot(innerStar, operand.deepcopy());
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    builder.print("~(").indent(2);
    operand.prettyPrint(builder);
    builder.indent(-2).print(")");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return operand.isPrettyPrintMultiLine();
  }

  @Override
  public Set<String> collectVarNames() {
    return operand.collectVarNames();
  }

  @Override
  public Set<String> collectAllVarNames() {
    return operand.collectAllVarNames();
  }

  @Override
  public LiaStar multToBin(int n) {
    operand = operand.multToBin(n);
    return this;
  }

  @Override
  public LiaStar expandStar() {
    return this;
  }

  @Override
  public LiaStar simplifyMult(Map<LiaStar, String> multToVar) {
    operand.innerStar = innerStar;
    return operand.simplifyMult(multToVar);
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    operand = operand.mergeMult(multToVar);
    return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    Expr one = operand.transToSMT(ctx, varsName, funcsName);
    return ctx.mkNot((BoolExpr) one);
  }

  @Override
  public EstimateResult estimate() {
    if (operand instanceof LiaAndImpl) {
      LiaStar op1 = ((LiaAndImpl) operand).operand1;
      LiaStar op2 = ((LiaAndImpl) operand).operand2;
      return LiaStar.mkOr(innerStar,
              LiaStar.mkNot(innerStar, op1),
              LiaStar.mkNot(innerStar, op2)
      ).estimate();
    } else if (operand instanceof LiaEqImpl) {
      LiaStar op1 = ((LiaEqImpl) operand).operand1;
      LiaStar op2 = ((LiaEqImpl) operand).operand2;
      return LiaStar.mkOr(innerStar,
              LiaStar.mkLt(innerStar, op1, op2),
              LiaStar.mkLt(innerStar, op2, op1)
      ).estimate();
    } else if (operand instanceof LiaLeImpl) {
      LiaStar op1 = ((LiaLeImpl) operand).operand1;
      LiaStar op2 = ((LiaLeImpl) operand).operand2;
      return LiaStar.mkLt(innerStar, op2, op1).estimate();
    } else if (operand instanceof LiaLtImpl) {
      LiaStar op1 = ((LiaLtImpl) operand).operand1;
      LiaStar op2 = ((LiaLtImpl) operand).operand2;
      return LiaStar.mkLe(innerStar, op2, op1).estimate();
    } else if (operand instanceof LiaNotImpl) {
      return ((LiaNotImpl) operand).operand.estimate();
    } else if (operand instanceof LiaOrImpl) {
      LiaStar op1 = ((LiaOrImpl) operand).operand1;
      LiaStar op2 = ((LiaOrImpl) operand).operand2;
      return LiaStar.mkAnd(innerStar,
              LiaStar.mkNot(innerStar, op1),
              LiaStar.mkNot(innerStar, op2)
      ).estimate();
    } else {
      throw new RuntimeException("Using not onto non-logical expression or sum");
    }
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return ctx.mkNot(
            (BoolExpr)operand.expandStarWithK(ctx, sol, suffix)
    );
  }

  @Override
  public boolean equals(Object that) {
    if(that == this)
      return true;
    if(that == null)
      return false;
    if(!(that instanceof LiaNotImpl))
      return false;
    LiaNotImpl tmp = (LiaNotImpl) that;
    return operand.equals(tmp.operand);
  }

  @Override
  public int hashCode() {
    return operand.hashCode();
  }

  @Override
  public void removeFVsFromInnerVector(Set<String> vars) {
    operand.removeFVsFromInnerVector(vars);
  }

  @Override
  public LiaStar removeParameter() {
    operand = operand.removeParameter();
    return this;
  }

  @Override
  public LiaStar pushUpParameter(Set<String> newVars) {
    operand = operand.pushUpParameter(newVars);
    return this;
  }

  @Override
  public Set<String> collectParamNames() {
    return operand.collectParamNames();
  }

  @Override
  public Set<LiaVarImpl> collectParams() {
    return operand.collectParams();
  }

  @Override
  public LiaStar simplifyIte() {
    operand = operand.simplifyIte();
    if(operand instanceof LiaNotImpl) {
      LiaNotImpl tmp = (LiaNotImpl) operand;
      return tmp.operand.deepcopy();
    }
    return this;
  }

  @Override
  public int embeddingLayers() {
    return operand.embeddingLayers();
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    LiaStar operand0 = operand.transformPostOrder(transformer);
    return transformer.apply(mkNot(innerStar, operand0));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar operand0 = operand.transformPostOrder(transformer, this);
    return transformer.apply(mkNot(innerStar, operand0), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar operand0 = operand.transformPreOrder(transformer, this);
    return mkNot(innerStar, operand0);
  }

  @Override
  public List<LiaStar> subNodes() {
    return List.of(operand);
  }
}
