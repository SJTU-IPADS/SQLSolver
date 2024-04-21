package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaStringImpl extends LiaStar {

  String value;

  LiaStringImpl() {
    value = "";
  }

  LiaStringImpl(String v) {
    value = v;
  }

  @Override
  public boolean isLia() {
    return true;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LSTRING;
  }

  public String getValue() {
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
    return mkString(innerStar, value);
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
    if(!(that instanceof LiaStringImpl tmp))
      return false;
    return Objects.equals(value, tmp.value);
  }

  @Override
  public int hashCode() {
    return Math.toIntExact(value.hashCode());
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    builder.print('"').print(value).print('"');
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
    // todo: is it a constant or var?
    return ctx.mkString(value);
  }

  @Override
  public EstimateResult estimate() {
    return null;
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return null;
  }

  @Override
  public int embeddingLayers() {
    return 0;
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    return transformer.apply(mkString(innerStar, value));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    return transformer.apply(mkString(innerStar, value), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    return mkString(innerStar, value);
  }
}
