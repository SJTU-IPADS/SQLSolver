package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaConstImpl extends LiaStar {

  long value;

  LiaConstImpl() {
    value = 0;
  }

  LiaConstImpl(long v) {
    value = v;
  }

  @Override
  public boolean isLia() {
    return true;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LCONST;
  }

  public long getValue() {
    return value;
  }

  @Override
  public Set<String> collectVarNames() {
    return new HashSet<>();
  }

  @Override
  public Set<String> collectAllVarNames() {
    return new HashSet<>();
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    return this;
  }

  @Override
  public Set<String> getVars() {
    return new HashSet<>();
  }

  @Override
  public LiaStar deepcopy() {
    return mkConst(innerStar, value);
  }

  @Override
  public LiaStar multToBin(int n) {
    return this;
  }

  @Override
  public LiaStar simplifyMult(Map<LiaStar, String> multToVar) {
    return null;
  }

  @Override
  public boolean equals(Object that) {
    if(that == this)
      return true;
    if(that == null)
      return false;
    if(!(that instanceof LiaConstImpl))
      return false;
    LiaConstImpl tmp = (LiaConstImpl) that;
    return value == tmp.value;
  }

  @Override
  public int hashCode() {
    return Math.toIntExact(value % 10000);
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    builder.print(value);
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return false;
  }

  @Override
  public LiaStar expandStar() {
    return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    return ctx.mkInt(value);
  }

  @Override
  public EstimateResult estimate() {
    return new EstimateResult(
            new HashSet(),
            0,
            0,
            value
    );
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return ctx.mkInt(value);
  }


  @Override
  public int embeddingLayers() {
    return 0;
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    return transformer.apply(mkConst(innerStar, value));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    return transformer.apply(mkConst(innerStar, value), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    return mkConst(innerStar, value);
  }
}
