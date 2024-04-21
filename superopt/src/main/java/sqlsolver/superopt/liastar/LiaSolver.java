package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import java.util.*;
import sqlsolver.superopt.liastar.parameter.InwardParamRemover;
import sqlsolver.superopt.logic.LogicSupport;
import sqlsolver.superopt.logic.SqlSolver;
import sqlsolver.superopt.uexpr.PredefinedFunctions;
import sqlsolver.superopt.util.Z3Support;

public class LiaSolver {
  public static final String CONFIG_KEY_PARAM_REMOVAL_MODE = "PARAM_REMOVAL_MODE";
  public static final String CONFIG_VALUE_PARAM_REMOVAL_MODE_INWARD = "INWARD";
  public static final String CONFIG_VALUE_PARAM_REMOVAL_MODE_OUTWARD = "OUTWARD";

  private final Properties config;
  private final LiaStar liaFormula;

  /**
   * Solve satisfiability of a LIA* formula with certain configuration.
   * @param f the formula to solve
   * @param config the configuration; it contains several keys: <ul>
   *               {@code PARAM_REMOVAL_MODE} - which strategy to use during removal of parameters
   * </ul>
   * @return the satisfiability result
   */
  public static LiaSolverStatus solveWithConfig(LiaStar f, Properties config) {
    return new LiaSolver(config, f).solve();
  }

  public LiaSolver(Properties config, LiaStar f) {
    this.config = config;
    liaFormula = f;
  }

  public LiaSolverStatus solve() {
    try {
      String result = checkUnderapp();
      if (result.equals("SAT")) return LiaSolverStatus.SAT;
    } catch (Exception e) {
    }

    try {
      String result = checkOverapp();
      if (result.equals("UNSAT")) return LiaSolverStatus.UNSAT;
      if (result.equals("SAT")) return LiaSolverStatus.SAT;
      return LiaSolverStatus.UNKNOWN;
    } catch (Exception e) {
      if (LogicSupport.dumpLiaFormulas) {
        e.printStackTrace();
      }
      return LiaSolverStatus.UNKNOWN;
    }
  }

  String checkUnderapp() {
    try {
      LiaStar curexp = liaFormula.deepcopy();
      curexp = LiaStar.calculateUnderApprox(curexp, curexp.embeddingLayers() > 4 ? 1 : 2);
      return solveLia(curexp);
    } catch (Exception e) {
      return "UNKNOWN";
    }
  }

  String checkOverapp() throws Exception {
    if (LogicSupport.dumpLiaFormulas) System.out.println("init: " + liaFormula);

    LiaStar tmpFormula = liaFormula.deepcopy();
    tmpFormula = tmpFormula.pushUpParameter(new HashSet<>());
    if (LogicSupport.dumpLiaFormulas) System.out.println("pushed up param: " + tmpFormula);
    final String removeParamMode = config.getProperty(CONFIG_KEY_PARAM_REMOVAL_MODE);
    if (removeParamMode.equals(CONFIG_VALUE_PARAM_REMOVAL_MODE_INWARD)) {
      tmpFormula = InwardParamRemover.removeParameter(tmpFormula);
    } else if (removeParamMode.equals(CONFIG_VALUE_PARAM_REMOVAL_MODE_OUTWARD)) {
      tmpFormula = tmpFormula.removeParameter();
    }
    if (LogicSupport.dumpLiaFormulas) System.out.println("remove param (mode: " + removeParamMode + "): " + tmpFormula);

    tmpFormula.simplifyMult(new HashMap<>());
    tmpFormula.mergeMult(new HashMap<>());
    if (LogicSupport.dumpLiaFormulas) System.out.println("remove multiplication: " + tmpFormula);

    return solveNestedLiastar(tmpFormula);
  }

  /** forall t1 t2. ((isnull(t1)<>0) /\ (isnull(t2)<>0)) -> t1 = t2 */
  private BoolExpr ruleNullEquals(Context ctx) {
    String strVar1 = "t1", strVar2 = "t2", strIsNull = "IsNull";
    Expr[] vars = new Expr[2];
    vars[0] = ctx.mkIntConst(strVar1);
    vars[1] = ctx.mkIntConst(strVar2);

    final Sort I = ctx.getIntSort();
    final FuncDecl func = ctx.mkFuncDecl(strIsNull, I, I);
    Expr isNull1 = ctx.mkApp(func, vars[0]);
    Expr isNull2 = ctx.mkApp(func, vars[1]);
    BoolExpr notNull1 = ctx.mkNot(ctx.mkEq(isNull1, ctx.mkInt(0)));
    BoolExpr notNull2 = ctx.mkNot(ctx.mkEq(isNull2, ctx.mkInt(0)));
    BoolExpr eq = ctx.mkEq(vars[0], vars[1]);

    Expr body = ctx.mkImplies(ctx.mkAnd(notNull1, notNull2), eq);
    return ctx.mkForall(vars, body, 1, null, null, null, null);
  }

  private BoolExpr appendRules(Context ctx, BoolExpr target) {
    target = ctx.mkAnd(ruleNullEquals(ctx), target);
    return target;
  }

  private Set<BoolExpr> getMultipleConditions(Context ctx, Expr expr) {
    Set<BoolExpr> conditions = new HashSet<>();
    if (!expr.isApp()) return conditions;

    Expr[] args = expr.getArgs();
    if (expr.isIDiv() && args[0] instanceof IntExpr i0 && args[1] instanceof IntExpr i1) {
      conditions.add(ctx.mkEq(ctx.mkMod(i0, i1), ctx.mkInt(0)));
    }
    // recursion
    for (Expr sub : args) {
      conditions.addAll(getMultipleConditions(ctx, sub));
    }
    return conditions;
  }

  // upon occurrence of "div u1 u2": append "= (mod u1 u2) 0"
  // this restricts valid division between integers
  private BoolExpr appendMultipleConditions(Context ctx, BoolExpr target) {
    Set<BoolExpr> conditions = getMultipleConditions(ctx, target);
    for (BoolExpr condition : conditions) {
      target = ctx.mkAnd(condition, target);
    }
    return target;
  }

  String solveLia(LiaStar f) {
    try (final Context ctx = new Context()) {
      BoolExpr target = ctx.mkTrue();

      Set<LiaVarImpl> vars = new HashSet<>();
      f.transformPostOrder(
          lia -> {
            if (lia instanceof LiaVarImpl var) {
              vars.add(var);
            }
            return lia;
          });
      Map<String, Expr> varDef = new HashMap<>();
      final BoolExpr varConstraints = Z3Support.defineVarsByVars(ctx, varDef, vars);
      target = ctx.mkAnd(target, varConstraints);

      BoolExpr coreExpr = (BoolExpr) f.transToSMT(ctx, varDef);
      coreExpr = appendMultipleConditions(ctx, coreExpr);
      target = ctx.mkAnd(target, coreExpr);

      // the formula f does not contain stars
      // append rules applicable to f
      target = appendRules(ctx, target);

      if (LogicSupport.dumpLiaFormulas) {
        System.out.println("FOL: " + target.toString());
      }

      Solver s =
          (f.toString().contains(PredefinedFunctions.NAME_SQRT))
              ? ctx.mkSolver()
              : ctx.mkSolver(ctx.tryFor(ctx.mkTactic("qflia"), SqlSolver.Z3_TIMEOUT));
      //       Solver s = ctx.mkSolver();
      s.add(target);
      Status q = s.check();
      if (LogicSupport.dumpLiaFormulas) {
        System.out.println("smt solver: " + q.toString());
      }
      return switch (q) {
        case UNKNOWN -> "UNKNOWN";
        case SATISFIABLE -> "SAT";
        case UNSATISFIABLE -> "UNSAT";
      };
    }
  }

  String solveNestedLiastar(LiaStar f) throws Exception {
    if (LogicSupport.dumpLiaFormulas) System.out.println("liastar: " + f.toString());
    f = f.expandStar();

    if (LogicSupport.dumpLiaFormulas) {
      System.out.println("lia: " + f.toString());
      System.out.println("#variables in LIA without *: " + f.getVars().size());
    }

    return solveLia(f);
  }
}
