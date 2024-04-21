package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaIteImpl extends LiaStar {

  LiaStar cond;
  LiaStar operand1;
  LiaStar operand2;


  LiaIteImpl(LiaStar c, LiaStar op1, LiaStar op2) {
    cond = c;
    operand1 = op1;
    operand2 = op2;
  }

  @Override
  public Set<String> collectVarNames() {
    Set<String> varSet = operand1.collectVarNames();
    varSet.addAll(operand2.collectVarNames());
    varSet.addAll(cond.collectVarNames());
    return varSet;
  }

  @Override
  public Set<String> collectAllVarNames() {
    Set<String> varSet = operand1.collectAllVarNames();
    varSet.addAll(operand2.collectAllVarNames());
    varSet.addAll(cond.collectAllVarNames());
    return varSet;
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    builder.print("ite(").indent(4);

    boolean multiLine = cond.isPrettyPrintMultiLine();
    cond.prettyPrint(builder);
    builder.print(", ");
    if (multiLine) builder.println();

    multiLine = operand1.isPrettyPrintMultiLine();
    if (multiLine) builder.println();
    operand1.prettyPrint(builder);
    builder.print(", ");
    if (multiLine) builder.println();

    multiLine = operand2.isPrettyPrintMultiLine();
    if (multiLine) builder.println();
    operand2.prettyPrint(builder);
    if (multiLine) builder.println();

    builder.indent(-4).print(")");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return cond.isPrettyPrintMultiLine()
            || operand1.isPrettyPrintMultiLine()
            || operand2.isPrettyPrintMultiLine();
  }

  @Override
  public boolean isLia() {
    return true;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LITE;
  }

  @Override
  public Set<String> getVars() {
    Set<String> result = operand1.getVars();
    result.addAll(operand2.getVars());
    result.addAll(cond.getVars());
    return result;
  }

  @Override
  public LiaStar deepcopy() {
    return mkIte(innerStar, cond.deepcopy(), operand1.deepcopy(), operand2.deepcopy());
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    cond = cond.mergeMult(multToVar);
    operand1 = operand1.mergeMult(multToVar);
    operand2 = operand2.mergeMult(multToVar);
    return this;
  }


  @Override
  public LiaStar multToBin(int n) {
    cond = cond.multToBin(n);
    operand1 = operand1.multToBin(n);
    operand2 = operand2.multToBin(n);
    return this;
  }

  @Override
  public boolean equals(Object that) {
    if(that == this)
      return true;
    if(that == null)
      return false;
    if(!(that instanceof LiaIteImpl))
      return false;
    LiaIteImpl tmp = (LiaIteImpl) that;
    return cond.equals(tmp.cond) && operand1.equals(tmp.operand1) && operand2.equals(tmp.operand2);
  }

  @Override
  public int hashCode() {
    return operand1.hashCode();
  }

  @Override
  public LiaStar simplifyMult(Map<LiaStar, String> multToVar) {
    cond.innerStar = innerStar;
    operand1.innerStar = innerStar;
    operand2.innerStar = innerStar;

    LiaStar[] tmp = new LiaStar[3];
    tmp[0] = cond.simplifyMult(multToVar);
    tmp[1] = operand1.simplifyMult(multToVar);
    tmp[2] = operand2.simplifyMult(multToVar);

    return liaAndConcat(tmp);
  }

  public boolean binIte() {
    if( operand1.getType().equals(LiaOpType.LCONST) &&
        operand2.getType().equals(LiaOpType.LCONST) ) {
      LiaConstImpl c1 = (LiaConstImpl) operand1;
      LiaConstImpl c2 = (LiaConstImpl) operand2;
      if( c1.getValue() <= 1 && c2.getValue() <= 1)
        return true;
      else
        return false;
    }
    return false;
  }

  @Override
  public LiaStar expandStar() {
    return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    BoolExpr f = (BoolExpr) cond.transToSMT(ctx, varsName, funcsName);
    Expr one = operand1.transToSMT(ctx, varsName, funcsName);
    Expr two = operand2.transToSMT(ctx, varsName, funcsName);
    return ctx.mkITE(f, one, two);
  }

  static LiaStar multIteOneOp(boolean innerStar, LiaStar operand, LiaStar f) {
    if(operand.isConstV(1)) {
      return f.deepcopy();
    } else if(operand instanceof LiaIteImpl) {
      return ((LiaIteImpl) operand).MultIte(f);
    } else if(f instanceof LiaIteImpl) {
      return ((LiaIteImpl) f.deepcopy()).MultIte(operand);
    } else if(!operand.isConstV(0)){
      return mkMul(innerStar, operand, f).deepcopy();
    } else {
      return operand.deepcopy();
    }
  }

  public LiaStar MultIte(LiaStar f) {
    if(f.isConstV(1)) {
      return this;
    } else if(f.isConstV(0)) {
      return f.deepcopy();
    }

    if(f instanceof LiaIteImpl) {
      LiaIteImpl fIte = (LiaIteImpl) f;
      if(fIte.cond.equals(cond)) {
        operand1 = multIteOneOp(innerStar, operand1, fIte.operand1);
        operand2 = multIteOneOp(innerStar, operand2, fIte.operand2);
        return this;
      }
    }

    operand1 = multIteOneOp(innerStar, operand1, f);
    operand2 = multIteOneOp(innerStar, operand2, f);

    return this;
  }

  static LiaStar plusIteOneOp(boolean innerStar, LiaStar operand, LiaStar f) {
    if(operand instanceof LiaIteImpl) {
      return ((LiaIteImpl) operand).plusIte(f);
    } else if(f instanceof LiaIteImpl) {
      return ((LiaIteImpl) f.deepcopy()).plusIte(operand);
    } else {
      return mkPlus(innerStar, operand, f).deepcopy();
    }
  }

  public LiaStar plusIte(LiaStar f) {
    if(f.isConstV(0)) {
      return this;
    }

    if(f instanceof LiaIteImpl) {
      LiaIteImpl fIte = (LiaIteImpl) f;
      if(fIte.cond.equals(cond)) {
        LiaStar fop1 = fIte.operand1;
        LiaStar fop2 = fIte.operand2;
        operand1 = plusIteOneOp(innerStar, operand1, fop1);
        operand2 = plusIteOneOp(innerStar, operand2, fop2);
        return this;
      }
    }
    operand1 = plusIteOneOp(innerStar, operand1, f);
    operand2 = plusIteOneOp(innerStar, operand2, f);
    return this;
  }

  @Override
  public EstimateResult estimate() {
    EstimateResult rc = cond.estimate();
    EstimateResult rn = LiaStar.mkNot(innerStar, cond).estimate();
    EstimateResult r1 = operand1.estimate();
    EstimateResult r2 = operand2.estimate();
    rc.vars.addAll(r1.vars);
    rc.vars.addAll(r2.vars);
    return new EstimateResult(
            rc.vars,
            Math.max(rc.n_extra + r1.n_extra, rn.n_extra + r2.n_extra),
            Math.max(rc.m + r1.m, rn.m + r2.m),
            Math.max(Math.max(rc.a, rn.a), Math.max(r1.a, r2.a))
    );
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return ctx.mkITE(
            (BoolExpr)cond.expandStarWithK(ctx, sol, suffix),
            operand1.expandStarWithK(ctx, sol, suffix),
            operand2.expandStarWithK(ctx, sol, suffix)
    );
  }

  @Override
  public LiaStar simplifyIte() {
    cond = cond.simplifyIte();
    operand1 = operand1.simplifyIte();
    operand2 = operand2.simplifyIte();
    if(operand1.equals(operand2)) {
      return operand1.deepcopy();
    }
    if(isTrue(cond)) {
      return operand1.deepcopy();
    }
    else if(isFalse(cond)) {
      return operand2.deepcopy();
    }
    return this;
  }

  @Override
  public int embeddingLayers() {
    return cond.embeddingLayers();
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    LiaStar cond0 = cond.transformPostOrder(transformer);
    LiaStar operand10 = operand1.transformPostOrder(transformer);
    LiaStar operand20 = operand2.transformPostOrder(transformer);
    return transformer.apply(mkIte(innerStar, cond0, operand10, operand20));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar cond0 = cond.transformPostOrder(transformer, this);
    LiaStar operand10 = operand1.transformPostOrder(transformer, this);
    LiaStar operand20 = operand2.transformPostOrder(transformer, this);
    return transformer.apply(mkIte(innerStar, cond0, operand10, operand20), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    LiaStar cond0 = cond.transformPreOrder(transformer, this);
    LiaStar operand10 = operand1.transformPreOrder(transformer, this);
    LiaStar operand20 = operand2.transformPreOrder(transformer, this);
    return mkIte(innerStar, cond0, operand10, operand20);
  }

  @Override
  public List<LiaStar> subNodes() {
    return List.of(cond, operand1, operand2);
  }
}

