package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LiaVarImpl extends LiaStar {
  String valueType;
  String varName;

  @Override
  public int embeddingLayers() {
    return 0;
  }

  @Override
  public Set<String> collectVarNames() {
    Set<String> varSet = new HashSet<>();
    varSet.add(varName);
    return varSet;
  }

  @Override
  public Set<String> collectAllVarNames() {
    Set<String> varSet = new HashSet<>();
    varSet.add(varName);
    return varSet;
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    return this;
  }


  public LiaVarImpl() {
    varName = "";
  }

  public LiaVarImpl(String s, String t) {
    varName = s;
    valueType = t;
  }

  String getValue() {
    return varName;
  }

  boolean valueIs(String name) {
    return Objects.equals(varName, name);
  }

  @Override
  public boolean isLia() {
    return true;
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    builder.print(varName);
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    return false;
  }

  @Override
  public LiaOpType getType() {
    return LiaOpType.LVAR;
  }

  @Override
  public Set<String> getVars() {
    Set<String> result = new HashSet<>();
    result.add(varName);
    return result;
  }

  public String getName() {
    return varName;
  }

  public String getValueType() {
    return valueType;
  }

  @Override
  public LiaStar deepcopy() {
    return mkVar(innerStar, varName, valueType);
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
    if (that == this)
      return true;
    if (that == null)
      return false;
    if (!(that instanceof LiaVarImpl tmp))
      return false;
    return valueType.equals(tmp.valueType) && varName.equals(tmp.varName);
  }

  @Override
  public int hashCode() {
    return varName.hashCode();
  }

  @Override
  public LiaStar expandStar() {
    return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    return varsName.get(varName);
  }

  @Override
  public EstimateResult estimate() {
    return new EstimateResult(
            new HashSet(List.of(varName)),
            0,
            0,
            1
    );
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    return ctx.mkIntConst(varName + suffix);
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    return transformer.apply(mkVar(innerStar, varName, valueType));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    return transformer.apply(mkVar(innerStar, varName, valueType), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    return mkVar(innerStar, varName, valueType);
  }
}
