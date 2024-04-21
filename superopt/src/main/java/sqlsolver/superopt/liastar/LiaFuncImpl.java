package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.superopt.uexpr.PredefinedFunctions;
import sqlsolver.superopt.util.PrettyBuilder;
import sqlsolver.superopt.util.Z3Support;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static sqlsolver.superopt.uexpr.PredefinedFunctions.*;

public class LiaFuncImpl extends LiaStar {
  String funcName;
  List<LiaStar> vars;
  boolean allowsUndefined; // allow this function not to be pre-defined

  LiaFuncImpl(String funcName, LiaStar var) {
    this(funcName, List.of(var), false);
  }

  LiaFuncImpl(String funcName, LiaStar var, boolean allowsUndefined) {
    this(funcName, List.of(var), allowsUndefined);
  }

  LiaFuncImpl(String funcName, List<LiaStar> vars) {
    this(funcName, vars, false);
  }

  LiaFuncImpl(String funcName, List<LiaStar> vars, boolean allowsUndefined) {
    this.funcName = funcName;
    this.vars = new ArrayList<>(vars);
    this.allowsUndefined = allowsUndefined;
  }

  @Override
  public boolean isLia() {
    return true;
  }

  @Override
  public LiaStar deepcopy() {
    List<LiaStar> newVars = new ArrayList<>(vars);
    return LiaStar.mkFunc(innerStar, funcName, newVars, allowsUndefined);
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
  public LiaOpType getType() {
    return LiaOpType.LFUNC;
  }

  @Override
  public LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar) {
    return this;
  }

  @Override
  public Set<String> collectVarNames() {
    HashSet<String> result = new HashSet<>();
    for (LiaStar var : vars)
      result.addAll(var.collectVarNames());
    return result;
  }

  @Override
  public Set<String> collectAllVarNames() {
    HashSet<String> result = new HashSet<>();
    for (LiaStar var : vars)
      result.addAll(var.collectAllVarNames());
    return result;
  }

  @Override
  public LiaStar expandStar() {
    return this;
  }

  @Override
  public Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName) {
    final List<Expr> args = vars.stream().map(x -> x.transToSMT(ctx, varsName, funcsName)).toList();
    final int arity = vars.size();
    if (MINUS.contains(funcName, arity)) {
      // MINUS is a dedicated Z3 operator
      return ctx.mkSub((ArithExpr) args.get(0), (ArithExpr) args.get(1));
    }
    if (allowsUndefined)
      // undefined functions are assumed to return int
      return Z3Support.assembleFunction(funcName, args, PredefinedFunctions.ValueType.INT, ctx, funcsName);
    return Z3Support.assemblePredefinedFunction(funcName, args, ctx, funcsName);
  }

  @Override
  public EstimateResult estimate() {
    // Unsupport
    return null;
  }

  @Override
  public Expr expandStarWithK(Context ctx, Solver sol, String suffix) {
    // Unsupport
    return null;
  }

  @Override
  public Set<String> getVars() {
    HashSet<String> result = new HashSet<>();
    for (LiaStar var : vars)
      result.addAll(var.getVars());
    return result;
  }

  @Override
  protected void prettyPrint(PrettyBuilder builder) {
    int indent = funcName.length() + 1;
    builder.print(funcName).print("(").indent(indent);
    for (int i = 0, bound = vars.size(); i < bound; i++) {
      LiaStar var = vars.get(i);
      boolean multiLine = var.isPrettyPrintMultiLine();
      if (i > 0 && multiLine) builder.println();
      var.prettyPrint(builder);
      if (i < bound - 1) builder.print(", ");
      if (multiLine) builder.println();
    }
    builder.indent(-indent).print(")");
  }

  @Override
  protected boolean isPrettyPrintMultiLine() {
    for (LiaStar var : vars) {
      if (var.isPrettyPrintMultiLine()) return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object that) {
    if(that == this)
      return true;
    if(that == null)
      return false;
    if(!(that instanceof LiaFuncImpl))
      return false;
    LiaFuncImpl tmp = (LiaFuncImpl) that;
    return funcName.equals(tmp.funcName) && vars.equals(tmp.vars);
  }

  @Override
  public int hashCode() {
    return vars.hashCode();
  }

  @Override
  public int embeddingLayers() {
    return 0;
  }

  @Override
  public LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer) {
    List<LiaStar> newVars = new ArrayList<>(vars.stream().map(v -> v.transformPostOrder(transformer)).toList());
    return transformer.apply(mkFunc(innerStar, funcName, newVars, allowsUndefined));
  }

  @Override
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    List<LiaStar> newVars = new ArrayList<>(vars.stream().map(v -> v.transformPostOrder(transformer, this)).toList());
    return transformer.apply(mkFunc(innerStar, funcName, newVars, allowsUndefined), parent);
  }

  @Override
  public LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    List<LiaStar> newVars = new ArrayList<>(vars.stream().map(v -> v.transformPreOrder(transformer, this)).toList());
    return mkFunc(innerStar, funcName, newVars, allowsUndefined);
  }

  @Override
  public List<LiaStar> subNodes() {
    return new ArrayList<>(vars);
  }
}
