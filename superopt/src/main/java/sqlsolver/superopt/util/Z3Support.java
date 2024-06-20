package sqlsolver.superopt.util;

import com.microsoft.z3.*;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.LiaVarImpl;
import sqlsolver.superopt.logic.SqlSolver;
import sqlsolver.superopt.uexpr.PredefinedFunctions;
import sqlsolver.superopt.uexpr.PredefinedFunctions.ValueType;

import java.util.*;

import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.plan.Value.*;

public class Z3Support {
  /**
   * Add var definitions and non-negativity to a Z3 context. It also updates the var definition map.
   * Vars already in the map are ignored.
   *
   * @return the non-negativity constraint of vars
   */
  public static BoolExpr defineNonNegativeVars(
      Context ctx, Map<String, Expr> varDef, Iterable<String> vars) {
    return defineNonNegativeVarsWithLimit(ctx, varDef, vars, 0);
  }

  /**
   * Add var definitions and non-negativity to a Z3 context. It also updates the var definition map.
   * Vars already in the map are ignored.
   *
   * @param limit if positive, the upper bound of dimensions of vars
   * @return the non-negativity constraint of vars
   */
  public static BoolExpr defineNonNegativeVarsWithLimit(
      Context ctx, Map<String, Expr> varDef, Iterable<String> vars, int limit) {
    BoolExpr constraint = ctx.mkBool(true);
    for (String var : vars) {
      if (!varDef.containsKey(var)) {
        // for each undefined var
        // create its definition
        IntExpr varExp = ctx.mkIntConst(var);
        varDef.put(var, varExp);
        // and its non-negativity constraint
        BoolExpr nonNeg = ctx.mkLe(ctx.mkInt(0), varExp);
        constraint = ctx.mkAnd(constraint, nonNeg);
        if (limit > 0) {
          BoolExpr withLimit = ctx.mkLe(varExp, ctx.mkInt(limit));
          constraint = ctx.mkAnd(constraint, withLimit);
        }
      }
    }
    return constraint;
  }

  /**
   * Add var definitions to a Z3 context. It also updates the var definition map.
   * Vars already in the map are ignored.
   */
  public static BoolExpr defineVarsByNames(
          Context ctx, Map<String, Expr> varDef, Iterable<String> varNames) {
    return defineVarsByNamesWithLimit(ctx, varDef, varNames, false, 0, 0);
  }

  public static BoolExpr defineVarsByVars(
          Context ctx, Map<String, Expr> varDef, Iterable<LiaVarImpl> vars) {
    return defineVarsByVarsWithLimit(ctx, varDef, vars, false, 0, 0);
  }

  public static BoolExpr defineVarsByNamesWithLimit(
          Context ctx, Map<String, Expr> varDef, Iterable<String> varNames, boolean withLimit, int lowerBound, int upperBound) {
    final List<LiaVarImpl> vars = new ArrayList<>();
    for (String varName : varNames) {
      vars.add(LiaStar.mkVar(false, varName));
    }
    return defineVarsByVarsWithLimit(ctx, varDef, vars, withLimit, lowerBound, upperBound);
  }

  /**
   * Add var definitions to a Z3 context. It also updates the var definition map.
   * Vars already in the map are ignored.
   *
   * @param withLimit if set, integral vars are restricted within [lowerBound,upperBound]
   * @return the constraint of vars
   */
  public static BoolExpr defineVarsByVarsWithLimit(
          Context ctx, Map<String, Expr> varDef, Iterable<LiaVarImpl> vars, boolean withLimit, int lowerBound, int upperBound) {
    BoolExpr constraint = ctx.mkBool(true);
    for (LiaVarImpl var : vars) {
      final String varName = var.getName();
      if (!varDef.containsKey(varName)) {
        // for each undefined var
        // create its definition
        // and constraint according to its type
        final String valueType = var.getValueType();
        final Expr varExp = mkExprByTypeString(varName, valueType, ctx);
        if (TYPE_NAT.equals(valueType)) {
          constraint = ctx.mkAnd(constraint, ctx.mkLe(ctx.mkInt(0), (IntExpr) varExp));
        }
        varDef.put(varName, varExp);
        // and its constraint
        if (withLimit && isIntegralType(valueType)) {
          final IntExpr intExpr = (IntExpr) varExp;
          final BoolExpr withLowerBound = ctx.mkLe(ctx.mkInt(lowerBound), intExpr);
          constraint = ctx.mkAnd(constraint, withLowerBound);
          final BoolExpr withUpperBound = ctx.mkLe(intExpr, ctx.mkInt(upperBound));
          constraint = ctx.mkAnd(constraint, withUpperBound);
        }
      }
    }
    return constraint;
  }

  public static Expr mkExprByTypeString(String varName, String type, Context ctx) {
    if (isIntegralType(type)) {
      return ctx.mkIntConst(varName);
    } else if (isRealType(type)) {
      return ctx.mkRealConst(varName);
    } else if (isStringType(type)) {
      return ctx.mkConst(ctx.mkSymbol(varName), ctx.mkStringSort());
    } else {
      throw new UnsupportedOperationException("unknown type " + type);
    }
  }

  /**
   * Assemble a predefined function term in Z3.
   * Before invoking this method, you should check on your own
   * whether {@code funcName} and {@code args} form a "valid" function
   * (e.g. via {@link PredefinedFunctions#isPredefinedFunction(String, int)}).
   */
  public static Expr assemblePredefinedFunction(String funcName, List<Expr> args, Context ctx, Map<String, FuncDecl> funcs) {
    final int arity = args.size();
    switch (funcName) {
      case PredefinedFunctions.NAME_SQRT: {
        final IntExpr varExpr = (IntExpr) args.get(0);
        RealExpr realExpr = ctx.mkInt2Real(varExpr);
        FPExpr fpExpr = ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), realExpr, ctx.mkFPSortDouble());
        Expr result = ctx.mkFPSqrt(ctx.mkFPRoundNearestTiesToEven(), fpExpr);
        result = ctx.mkFPToReal((FPExpr) result);
        return ctx.mkReal2Int((RealExpr) result);
      }
      case PredefinedFunctions.NAME_MINUS: {
        final IntExpr varExpr0 = (IntExpr) args.get(0);
        final IntExpr varExpr1 = (IntExpr) args.get(1);
        return ctx.mkSub(varExpr0, varExpr1);
      }
      case PredefinedFunctions.NAME_DIVIDE: {
        final IntExpr varExpr0 = (IntExpr) args.get(0);
        final IntExpr varExpr1 = (IntExpr) args.get(1);
        return ctx.mkDiv(varExpr0, varExpr1);
      }
      default: {
        // uninterpreted func
        FuncDecl func = funcs.get(funcName);
        if (func == null) {
          // create function definition upon first use
          final PredefinedFunctions.FunctionFamily ff = PredefinedFunctions.getFunctionFamily(funcName, arity);
          if (ff == null) {
            // unknown functions are regarded as uninterpreted integer functions
            final Sort intSort = ctx.mkIntSort();
            final Sort[] argSorts = new Sort[arity];
            Arrays.fill(argSorts, intSort);
            func = ctx.mkFuncDecl(funcName, argSorts, intSort);
          } else {
            // predefined functions have registered arg/return types
            final Sort returnSort = ff.getReturnSort(ctx);
            final Sort[] argSorts = new Sort[arity];
            for (int i = 0; i < arity; i++) {
              argSorts[i] = ff.getArgSort(ctx, i);
            }
            func = ctx.mkFuncDecl(funcName, argSorts, returnSort);
          }
          funcs.put(funcName, func);
        }
        return ctx.mkApp(func, args.toArray(new Expr[0]));
      }
    }
  }

  /**
   * Assemble a function term in Z3 with given return type.
   */
  public static Expr assembleFunction(
      String funcName,
      List<Expr> args,
      ValueType returnType,
      Context ctx,
      Map<String, FuncDecl> funcs) {
    // uninterpreted func
    FuncDecl func = funcs.get(funcName);
    if (func == null) {
      // create function definition upon first use
      final Sort returnSort = returnType.getSort(ctx);
      final Sort[] argSorts = map(args, Expr::getSort).toArray(new Sort[0]);
      func = ctx.mkFuncDecl(funcName, argSorts, returnSort);
      funcs.put(funcName, func);
    }
    return ctx.mkApp(func, args.toArray(new Expr[0]));
  }

  /**
   * Assemble a function term in Z3 with given arg/return type.
   */
  public static Expr assembleFunction(String funcName, List<Expr> args, ValueType returnType, List<ValueType> argTypes, Context ctx, Map<String, FuncDecl> funcs) {
    final int arity = args.size();
    // uninterpreted func
    FuncDecl func = funcs.get(funcName);
    if (func == null) {
      // create function definition upon first use
      final Sort returnSort = returnType.getSort(ctx);
      final Sort[] argSorts = new Sort[arity];
      for (int i = 0; i < arity; i++) {
        argSorts[i] = argTypes.get(i).getSort(ctx);
      }
      func = ctx.mkFuncDecl(funcName, argSorts, returnSort);
      funcs.put(funcName, func);
    }
    return ctx.mkApp(func, args.toArray(new Expr[0]));
  }

  /**
   * Check whether a LIA formula is always true.
   * When the input is a non-LIA formula, the method behavior is undefined.
   *
   * @param lia the LIA formula to check
   * @return whether the LIA formula "forall vars. lia" is valid
   *     (i.e. always true) where vars refer to all vars in lia
   */
  public static boolean isValidLia(LiaStar lia) {
    return isValidLia(lia, lia.collectVarNames());
  }

  /**
   * Check whether a LIA formula is always true.
   * When the input is a non-LIA formula, the method behavior is undefined.
   *
   * @param lia the LIA formula to check
   * @param universalBVNames the vars bound by "forall"
   * @return whether the LIA formula "forall universalBVs. (exists ... ). lia" is valid
   *     (i.e. always true); the "exists" part covers free vars in lia except universalBVs.
   */
  public static boolean isValidLia(LiaStar lia, Set<String> universalBVNames) {
    Timeout.checkTimeout();
    // Since LIA does not contain forall/exists,
    // let FVs in universalBVs be bound by "forall",
    // and other FVs be bound by "exists"
    final Set<String> existBVNames = SetSupport.minus(lia.collectVarNames(), universalBVNames);
    final Set<LiaVarImpl> universalBVs = new HashSet<>();
    lia.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var && universalBVNames.contains(var.getName())) {
        universalBVs.add(var);
      }
      return f;
    });
    final Set<LiaVarImpl> existBVs = new HashSet<>();
    lia.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var && existBVNames.contains(var.getName())) {
        existBVs.add(var);
      }
      return f;
    });
    // We want to check validity of "forall universalBVs. some universalBVs >= 0 -> exists existBVs. some existBVs >= 0 /\ lia",
    // which can be reduced to falsehood of
    // "exists universalBVs. some universalBVs >= 0 /\ forall existBVs. some existBVs >= 0 -> not lia",
    // or unsatisfiability of
    // "some universalBVs >= 0 /\ forall existBVs. some existBVs >= 0 -> not lia".
    // Non-negativity of vars (some universalBVs and existBVs >= 0) is considered.
    // Whether a var is non-negative is determined by its type. (natural vars are non-negative)
    try (final Context ctx = new Context()) {
      final Map<String, Expr> varDef = new HashMap<>();
      final BoolExpr nnEx = defineVarsByVars(ctx, varDef, existBVs);
      final BoolExpr nnUn = defineVarsByVars(ctx, varDef, universalBVs);
      final BoolExpr notLia = ctx.mkNot((BoolExpr) lia.transToSMT(ctx, varDef));
      final BoolExpr body;
      if (existBVNames.isEmpty()) {
        body = notLia;
      } else {
        body =
                ctx.mkForall(
                        existBVNames.stream().map(varDef::get).toList().toArray(new Expr[0]),
                        ctx.mkImplies(nnEx, notLia),
                        0,
                        null,
                        null,
                        null,
                        null);
      }
      final BoolExpr toCheck = ctx.mkAnd(nnUn, body);
      final Solver s = ctx.mkSolver(ctx.tryFor(ctx.mkTactic("lia"), SqlSolver.Z3_TIMEOUT));
      s.add(toCheck);
      return s.check() == Status.UNSATISFIABLE;
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * Check whether a LIA formula is satisfiable and try to get its model.
   * When the input is a non-LIA formula, the method behavior is undefined.
   *
   * @param lia the LIA formula to check
   * @param existBVNames the vars bound by "exists."
   * @return if the LIA formula "(forall ... ). lia" is satisfiable,
   *     return the model; otherwise <code>null</code>;
   *     the "forall" part covers free vars in lia except existBVs.
   */
  public static boolean isSatisfiable(LiaStar lia, Set<String> existBVNames) {
    try (final Context ctx = new Context()) {
      final Model model = findModel(lia, existBVNames, ctx);
      return model != null;
    }
  }

  /**
   * Check whether a LIA formula is satisfiable and try to get its model.
   * When the input is a non-LIA formula, the method behavior is undefined.
   *
   * @param lia the LIA formula to check
   * @param existBVNames the vars bound by "exists."
   * @return if the LIA formula "(forall ... ). lia" is satisfiable,
   *     return the model; otherwise <code>null</code>;
   *     the "forall" part covers free vars in lia except existBVs.
   */
  public static Model findModel(LiaStar lia, Set<String> existBVNames, Context ctx) {
    // Since LIA does not contain forall/exists,
    // let FVs in existBVs be bound by "exists",
    // and other FVs be bound by "exists"
    final Set<String> universalBVNames = SetSupport.minus(lia.collectVarNames(), existBVNames);
    final Set<LiaVarImpl> existBVs = new HashSet<>();
    lia.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var && existBVNames.contains(var.getName())) {
        existBVs.add(var);
      }
      return f;
    });
    final Set<LiaVarImpl> universalBVs = new HashSet<>();
    lia.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var && universalBVNames.contains(var.getName())) {
        universalBVs.add(var);
      }
      return f;
    });
    // Check satisfiability of "some existBVs >= 0 /\ (forall universalBVs. some universalBVs >= 0 -> lia)"
    // Non-negativity of vars (some universalBVs and existBVs >= 0) is considered.
    // Whether a var is non-negative is determined by its type. (natural vars are non-negative)
    final Map<String, Expr> varDef = new HashMap<>();
    final BoolExpr nnUn = defineVarsByVars(ctx, varDef, universalBVs);
    final BoolExpr nnEx = defineVarsByVars(ctx, varDef, existBVs);
    final BoolExpr liaZ3 = (BoolExpr) lia.transToSMT(ctx, varDef);
    final BoolExpr body;
    if (universalBVNames.isEmpty()) {
      body = liaZ3;
    } else {
      body =
          ctx.mkForall(
              universalBVNames.stream().map(varDef::get).toList().toArray(new Expr[0]),
              ctx.mkImplies(nnUn, liaZ3),
              0,
              null,
              null,
              null,
              null);
    }
    final BoolExpr toCheck = ctx.mkAnd(nnEx, body);
    final Solver s = ctx.mkSolver(ctx.tryFor(ctx.mkTactic("lia"), SqlSolver.Z3_TIMEOUT));
    s.add(toCheck);
    s.check();
    return s.getModel();
  }
}
