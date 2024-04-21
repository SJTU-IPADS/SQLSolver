package sqlsolver.superopt.liastar.destructor;

import sqlsolver.common.utils.SetSupport;
import sqlsolver.superopt.liastar.LiaIteImpl;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.LiaVarImpl;
import sqlsolver.superopt.util.Z3Support;

import java.util.*;
import java.util.function.Function;

import static sqlsolver.common.utils.SetSupport.intersects;
import static sqlsolver.superopt.liastar.LiaStar.*;

/**
 * Given "F(...ite(pK,aK,bK)...)"s with a finite domain of K
 * (e.g. 1<=K<=N),
 * a destructor tries to separate qK (with params) from each pK
 * so that ite expressions do not contain params
 * and the part with params (i.e. qK's) is separate from the rest of formula.
 * <br/>
 * Its principle is: e=ite(p/\q,a,b) -> (q/\e=ite(p,a,b)) \/ (~q/\e=b).
 */
public class IteDestructor extends Destructor {
  /** Context of destruction. "env" is the formula template where parameterized ite expressions are replaced by template vars. */
  private static class Context {
    final IteDestructor destructor;
    final Map<String, IteRecord> varIteMap;
    LiaStar env;

    Context(LiaStar formula, IteDestructor destructor) {
      this.destructor = destructor;
      final Map<LiaIteImpl, String> iteVarMap = new HashMap<>();
      env = replaceParameterizedItes(formula, iteVarMap, destructor::isParameterized);
      varIteMap = new HashMap<>();
      for (Map.Entry<LiaIteImpl, String> entry : iteVarMap.entrySet()) {
        varIteMap.put(entry.getValue(), new IteRecord(entry.getKey()));
      }
    }

    boolean isTrivial() {
      return varIteMap.isEmpty();
    }

    private void replaceParameterizedItesInEnv(Function<LiaStar, Boolean> isParameterized) {
      final Map<LiaIteImpl, String> iteVarMap = new HashMap<>();
      env = replaceParameterizedItes(env, iteVarMap, isParameterized);
      for (Map.Entry<LiaIteImpl, String> entry : iteVarMap.entrySet()) {
        varIteMap.put(entry.getValue(), new IteRecord(entry.getKey()));
      }
    }

    private LiaStar replaceParameterizedItes(LiaStar formula, Map<LiaIteImpl, String> iteVarMap, Function<LiaStar, Boolean> isParameterized) {
      return formula.transformPreOrder(f -> {
        // only replace ite's with parameterized condition
        if (f instanceof LiaIteImpl ite && isParameterized.apply(ite.subNodes().get(0))) {
          if (iteVarMap.containsKey(ite)) {
            return mkVar(true, iteVarMap.get(ite));
          }
          final String newVarName = LiaStar.newVarName();
          iteVarMap.put(ite, newVarName);
          return mkVar(true, newVarName);
        } else {
          return null;
        }
      });
    }
  }

  /** e = ite(p /\ q, a, b) -> (q /\ e = ite(p, a, b)) \/ (~q /\ e = b) */
  private static class IteRecord {
    LiaStar a, b;
    List<LiaStar> q, p;

    IteRecord(LiaIteImpl ite) {
      final List<LiaStar> children = ite.subNodes(); // cond, op1, op2
      this.a = deepcopy(children.get(1));
      this.b = deepcopy(children.get(2));
      this.p = new ArrayList<>();
      decomposeConjunction(deepcopy(children.get(0)), this.p);
      q = new ArrayList<>();
    }

    /** Return q in LIA form. */
    LiaStar getCond() {
      return mkConjunction(true, q);
    }

    /** [[q, ite(p,a,b)], [~q, b]] */
    LiaStar[][] toLiaCases() {
      final LiaStar[][] result = new LiaStar[2][2];
      result[0][0] = getCond();
      result[0][1] = toLiaIfCondIsTrue();
      result[1][0] = mkNot(true, getCond());
      result[1][1] = toLiaIfCondIsFalse();
      return result;
    }

    /** Return the expression when q is true. */
    LiaStar toLiaIfCondIsTrue() {
      final LiaStar ite;
      final LiaStar p = this.p.isEmpty() ? null : mkConjunction(true, this.p);
      if (p == null) {
        ite = deepcopy(a);
      } else {
        ite = mkIte(true, deepcopy(p), deepcopy(a), deepcopy(b));
      }
      return ite;
    }

    /** Return the expression when q is false. */
    LiaStar toLiaIfCondIsFalse() {
      return deepcopy(b);
    }
  }

  public IteDestructor(Set<String> params, Set<String> importantVars) {
    super(params, importantVars);
  }

  @Override
  public List<LiaStar> destruct(LiaStar formula) {
    final Context ctx = new Context(formula, this);
    // trivial case cannot be further destructed
    if (ctx.isTrivial()) {
      final List<LiaStar> result = new ArrayList<>();
      result.add(formula);
      return result;
    }
    // extract params from each ite condition
    extractParamsFromIteCond(ctx);
    final List<LiaStar> result = constructResult(ctx);
    // recursion: destruct ite's inside ite conditions
    return destruct(result);
  }

  /** Extract parameterized part of each ite condition in ctx to do case analysis. */
  private void extractParamsFromIteCond(Context ctx) {
    // find "closure" of params
    boolean changed;
    final Set<String> paramClosure = getParams();
    do {
      changed = false;
      // add new vars to the param closure
      // also add literals to destruct and update candidate ite's
      for (IteRecord record : ctx.varIteMap.values()) {
        final List<LiaStar> toSeparate = new ArrayList<>();
        for (LiaStar term : record.p) {
          final Set<String> termVars = term.collectVarNames();
          if (intersects(termVars, paramClosure)) {
            toSeparate.add(term);
            record.q.add(term);
            paramClosure.addAll(termVars);
            changed = true;
          }
        }
        record.p.removeAll(toSeparate);
      }
      ctx.replaceParameterizedItesInEnv(f -> SetSupport.intersects(f.collectVarNames(), paramClosure));
    } while (changed);
  }

  /** Construct formulas for each case in ctx. */
  private List<LiaStar> constructResult(Context ctx) {
    List<LiaStar> result = new ArrayList<>();
    constructResult(result, new ArrayList<>(ctx.varIteMap.entrySet()), 0, ctx.env);
    return result;
  }

  private void constructResult(List<LiaStar> results,
          List<Map.Entry<String, IteRecord>> varIteEntries, int depth, LiaStar result) {
    if (depth >= varIteEntries.size()) {
      // border
      results.add(result);
      return;
    }
    final Map.Entry<String, IteRecord> entry = varIteEntries.get(depth);
    final String var = entry.getKey();
    final IteRecord record = entry.getValue();
    final LiaStar[][] cases = record.toLiaCases();
    // for each case (ite cond is true/false)
    for (LiaStar[] cas : cases) {
      final LiaStar cond = cas[0], exp = cas[1];
      // instantiate ite (previously replaced with var) with exp
      LiaStar newResult = replaceVarWithExp(result, var, exp);
      // ignore impossible cases
      if (isImpossible(newResult, cond)) {
        continue;
      }
      // ignore duplicates
      newResult = addNonDuplicates(newResult, cond);
      // recursion
      constructResult(results, varIteEntries, depth + 1, newResult);
    }
  }

  private LiaStar replaceVarWithExp(LiaStar formula, String var, LiaStar exp) {
    if (formula == null) return null;
    return formula.transformPreOrder(f -> {
      if (f instanceof LiaVarImpl v && v.getName().equals(var)) {
        return exp.deepcopy();
      }
      return null;
    });
  }

  // add each non-duplicate term in formula into env
  private LiaStar addNonDuplicates(LiaStar env, LiaStar formula) {
    LiaStar result = env;
    final List<LiaStar> terms = new ArrayList<>();
    decomposeConjunction(formula, terms);
    for (LiaStar term : terms) {
      if (!isDuplicate(result, term)) {
        result = mkAnd(false, result, term);
      }
    }
    return result;
  }

  // whether env -> term (ignore stars in env)
  private boolean isDuplicate(LiaStar env, LiaStar term) {
    if (env == null) {
      return false;
    }
    List<LiaStar> noStarList = new ArrayList<>();
    decomposeConjunction(env, noStarList);
    noStarList = noStarList.stream().filter(LiaStar::isLia).toList();
    LiaStar toCheck = mkConjunction(false, noStarList);
    toCheck = mkImplies(false, toCheck, term);
    return Z3Support.isValidLia(toCheck);
  }

  // whether env -> not term (ignore stars in env)
  private boolean isImpossible(LiaStar env, LiaStar term) {
    if (env == null) {
      return false;
    }
    List<LiaStar> noStarList = new ArrayList<>();
    decomposeConjunction(env, noStarList);
    noStarList = noStarList.stream().filter(LiaStar::isLia).toList();
    LiaStar toCheck = mkConjunction(false, noStarList);
    toCheck = mkImplies(false, toCheck, mkNot(false, term));
    return Z3Support.isValidLia(toCheck);
  }
}
