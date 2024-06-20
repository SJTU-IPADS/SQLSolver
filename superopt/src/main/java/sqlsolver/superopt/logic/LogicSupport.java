package sqlsolver.superopt.logic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Global;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.*;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.fragment.Agg;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.substitution.SubstitutionTranslatorResult;
import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.uexpr.normalizer.QueryUExprICRewriter;
import sqlsolver.superopt.fragment.AggFuncKind;
import sqlsolver.superopt.fragment.Symbol;
import sqlsolver.superopt.util.Timeout;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static sqlsolver.common.utils.IterableSupport.all;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.sql.calcite.CalciteSupport.hasNodeOfKind;
import static sqlsolver.sql.calcite.CalciteSupport.isEqualTwoValueList;

public abstract class LogicSupport {
  static {
//    final String timeout = System.getProperty("sqlsolver.smt_timeout", "20");
    Global.setParameter("smt.random_seed", "9876543210");
    Global.setParameter("smt.qi.quick_checker", "2");
    Global.setParameter("smt.qi.max_multi_patterns", "1024");
    Global.setParameter("smt.mbqi.max_iterations", "3");
    Global.setParameter("timeout", SqlSolver.Z3_TIMEOUT.toString());
    Global.setParameter("combined_solver.solver2_unknown", "0");
    Global.setParameter("pp.max_depth", "100");
  }

  private static final AtomicInteger NUM_INVOCATIONS = new AtomicInteger(0);
  public static boolean dumpFormulas;
  public static boolean dumpLiaFormulas;

  public static final int PROVER_DISABLE_INTEGRITY_CONSTRAINTS_THEOREM = 1;

  private LogicSupport() {
  }

  static void incrementNumInvocations() {
    NUM_INVOCATIONS.incrementAndGet();
  }

  public static void setDumpFormulas(boolean dumpFormulas) {
    LogicSupport.dumpFormulas = dumpFormulas;
  }

  public static void setDumpLiaFormulas(boolean dumpLiaFormulas) {
    LogicSupport.dumpLiaFormulas = dumpLiaFormulas;
  }

  public static int numInvocations() {
    return NUM_INVOCATIONS.get();
  }

  public static VerificationResult proveEq(UExprTranslationResult uExprs) {
    try (final Context z3 = new Context()) {
      return new LogicProver(uExprs, z3, 0).proveEq();
    }
  }

  public static VerificationResult proveEqNotNeedLia(UExprTranslationResult uExprs) {
    try (final Context z3 = new Context()) {
      return new LogicProver(uExprs, z3, 0).proveEqNotNeedLia();
    }
  }

  public static VerificationResult proveEq(UExprTranslationResult uExprs, int tweaks) {
    try (final Context z3 = new Context()) {
      return new LogicProver(uExprs, z3, tweaks).proveEq();
    }
  }

  public static VerificationResult proveEqByLIAStar(UExprTranslationResult uExprs) {
    try {
      return new SqlSolver(uExprs).proveEq();
    } catch (Exception e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  public static VerificationResult proveEqByLIAStar(UExprConcreteTranslationResult uExprs, Schema schema) {
    try {
      return new SqlSolver(uExprs, schema).proveEq();
    } catch (Exception e) {
      Timeout.bypassTimeout(e);
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  public static VerificationResult proveEqByLIAStar(Substitution rule) {
    return proveEqByLIAStar(rule, null, false);
  }

  public static VerificationResult proveEqByLIAStar(
          Substitution rule,
          SubstitutionTranslatorResult extraInfo,
          boolean isConcretePlan) {
    try {
      int flag = UExprSupport.UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE;
      if (isConcretePlan) flag |= UExprSupport.UEXPR_FLAG_VERIFY_CONCRETE_PLAN;

      final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule, flag, extraInfo);
      if (uExprs == null) return VerificationResult.UNKNOWN;
      return new SqlSolver(uExprs).proveEq();
    } catch (Exception e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  public static VerificationResult proveEqByLIAStar2(Substitution rule) {
    return proveEqByLIAStar2(rule, null, false);
  }

  public static VerificationResult proveEqByLIAStar2(
          Substitution rule,
          SubstitutionTranslatorResult extraInfo,
          boolean isConcretePlan) {
    if (isConcretePlan) assert extraInfo != null;

    try {
      final List<Symbol> srcFuncSymbols = rule._0().symbols().symbolsOf(Symbol.Kind.FUNC);
      final List<Symbol> tgtFuncSymbols = rule._1().symbols().symbolsOf(Symbol.Kind.FUNC);
      // Rules without Agg nodes
      if (srcFuncSymbols.isEmpty() && tgtFuncSymbols.isEmpty())
        return proveEqByLIAStar(rule, extraInfo, isConcretePlan);
      // Concrete queries with deterministic Agg functions, no need to enum all cases
      if (all(srcFuncSymbols, f -> ((Agg) rule._0().symbols().ownerOf(f)).aggFuncKind() != AggFuncKind.UNKNOWN) &&
              all(tgtFuncSymbols, f -> ((Agg) rule._1().symbols().ownerOf(f)).aggFuncKind() != AggFuncKind.UNKNOWN))
        return proveEqByLIAStar(rule, extraInfo, isConcretePlan);

      // For UNKNOWN type Agg function: enumerate all cases of agg functions on each Agg node
      final Set<Symbol> visitedFuncSymbols = new HashSet<>();
      final Map<Set<Symbol>, List<AggFuncKind>> feasibleFuncMap = new HashMap<>();
      for (Symbol fSym : srcFuncSymbols) {
        if (visitedFuncSymbols.contains(fSym)) continue;
        final Set<Symbol> eqFuncSyms = new HashSet<>(rule.constraints().eqClassOf(fSym));
        for (Symbol tgtFuncSym : tgtFuncSymbols) {
          if (any(eqFuncSyms, s -> rule.constraints().instantiationOf(tgtFuncSym) == s))
            eqFuncSyms.add(tgtFuncSym);
        }
        if (any(eqFuncSyms, s -> ((Agg) s.ctx().ownerOf(s)).deduplicated()))
          feasibleFuncMap.put(eqFuncSyms, AggFuncKind.dedupAggFuncKinds);
        else feasibleFuncMap.put(eqFuncSyms, AggFuncKind.commonAggFuncKinds);
        visitedFuncSymbols.addAll(eqFuncSyms);
      }

      final List<Set<Symbol>> eqFuncSymList = feasibleFuncMap.keySet().stream().toList();
      return enumAggFunc(rule, extraInfo, isConcretePlan, eqFuncSymList, feasibleFuncMap, 0);
    } catch (Exception e) {
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  private static VerificationResult enumAggFunc(
          Substitution rule,
          SubstitutionTranslatorResult extraInfo,
          boolean isConcretePlan,
          List<Set<Symbol>> eqFuncSymList,
          Map<Set<Symbol>, List<AggFuncKind>> feasibleFuncMap,
          int index) {
    if (index == eqFuncSymList.size()) return proveEqByLIAStar(rule, extraInfo, isConcretePlan);

    final Set<Symbol> eqFuncSyms = eqFuncSymList.get(index);
    VerificationResult res;
    for (AggFuncKind aggFuncKind : feasibleFuncMap.get(eqFuncSyms)) {
      for (Symbol funcSym : eqFuncSyms) {
        ((Agg) funcSym.ctx().ownerOf(funcSym)).setAggFuncKind(aggFuncKind);
      }
      res = enumAggFunc(rule, extraInfo, isConcretePlan, eqFuncSymList, feasibleFuncMap, index + 1);
      for (Symbol funcSym : eqFuncSyms) {
        ((Agg) funcSym.ctx().ownerOf(funcSym)).setAggFuncKind(AggFuncKind.UNKNOWN);
      }
      if (res != VerificationResult.EQ) return res;
    }
    return VerificationResult.EQ;
  }

  private static VerificationResult proveEqByLIAStarSelectedIC(RelNode p0, RelNode p1, Schema schema,
                                                               int extraFlags) {
    int selectedIC = 0;
    do {
      QueryUExprICRewriter.selectIC(selectedIC);
      final UExprConcreteTranslationResult uExprsWithICRewrite =
              UExprSupport.translateQueryToUExpr(p0, p1, schema,
                      UExprSupport.UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE
                              | extraFlags);
      if (uExprsWithICRewrite != null) {
        final VerificationResult res1 = LogicSupport.proveEqByLIAStar(uExprsWithICRewrite, schema);
        if (res1 == VerificationResult.EQ) {
          QueryUExprICRewriter.selectIC(-1);
          return res1;
        }
      }
      selectedIC = selectedIC + 1;
    } while (QueryUExprICRewriter.hasIC());
    QueryUExprICRewriter.selectIC(-1);
    return VerificationResult.NEQ;
  }

  private static VerificationResult proveEqByLIAStarConcreteNoSortWithIC(RelNode p0, RelNode p1, Schema schema, int extraFlags) {
    final UExprConcreteTranslationResult uExprsWithIC =
            UExprSupport.translateQueryToUExpr(p0, p1, schema,
                    UExprSupport.UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE
                            | extraFlags);
    if (uExprsWithIC != null) {
      if (!isEqualTwoValueList(uExprsWithIC.srcTupleVarSchemaOf(uExprsWithIC.sourceOutVar()), uExprsWithIC.tgtTupleVarSchemaOf(uExprsWithIC.targetOutVar()))) {
        return VerificationResult.NEQ;
      }
      final VerificationResult noSelectedICResult = LogicSupport.proveEqByLIAStar(uExprsWithIC, schema);
      if (noSelectedICResult == VerificationResult.EQ) return VerificationResult.EQ;
      if (proveEqByLIAStarSelectedIC(p0, p1, schema, extraFlags) == VerificationResult.EQ) return VerificationResult.EQ;
    }
    return (uExprsWithIC == null) ? VerificationResult.UNKNOWN : VerificationResult.NEQ;
  }

  private static VerificationResult proveEqByLIAStarConcreteNoSort(RelNode p0, RelNode p1, Schema schema) {
    VerificationResult resICUnexplainedPred = proveEqByLIAStarConcreteNoSortWithIC(p0, p1, schema,
            UExprSupport.UEXPR_FLAG_NO_EXPLAIN_PREDICATES);
    if (resICUnexplainedPred == VerificationResult.EQ) return VerificationResult.EQ;
    VerificationResult resIC = proveEqByLIAStarConcreteNoSortWithIC(p0, p1, schema, 0);
    if (resIC == VerificationResult.EQ) return VerificationResult.EQ;
    final UExprConcreteTranslationResult uExprs = UExprSupport.translateQueryToUExpr(p0, p1, schema, 0);
    if (uExprs != null) {
      if (!isEqualTwoValueList(uExprs.srcTupleVarSchemaOf(uExprs.sourceOutVar()), uExprs.tgtTupleVarSchemaOf(uExprs.targetOutVar()))) {
        return VerificationResult.NEQ;
      }
      final VerificationResult res0 = LogicSupport.proveEqByLIAStar(uExprs, schema);
      if (res0 == VerificationResult.EQ) return VerificationResult.EQ;
    }
    return (resICUnexplainedPred == VerificationResult.UNKNOWN || resIC == VerificationResult.UNKNOWN) ?
            VerificationResult.UNKNOWN : VerificationResult.NEQ;
  }

  public static VerificationResult proveEqByLIAStarConcrete(RelNode p0, RelNode p1, Schema schema) {
    if (!hasNodeOfKind(p0, Sort.class) && !hasNodeOfKind(p1, Sort.class)) {
      return proveEqByLIAStarConcreteNoSort(p0, p1, schema);
    }
    return OrderbySupport.sortHandler(p0, p1, schema);
  }

  public static boolean isMismatchedOutput(UExprTranslationResult uExprs) {
    // case 1: different output schema
    final UVar sourceOutVar = uExprs.sourceOutVar();
    final UVar targetOutVar = uExprs.targetOutVar();
    final SchemaDesc srcOutSchema = uExprs.schemaOf(sourceOutVar);
    final SchemaDesc tgtOutSchema = uExprs.schemaOf(targetOutVar);
    assert srcOutSchema != null && tgtOutSchema != null;
    return !srcOutSchema.equals(tgtOutSchema);
  }

  public static boolean isMismatchedSummation(UExprTranslationResult uExprs) {
    // cast 2: unaligned variables
    // master: the side with more bounded variables, or the source side if the numbers are equal
    // master: the side with less bounded variables, or the target side if the numbers are equal
    final UTerm srcTerm = uExprs.sourceExpr(), tgtTerm = uExprs.targetExpr();
    final UTerm masterTerm = getMaster(srcTerm, tgtTerm);
    final UTerm slaveTerm = getSlave(srcTerm, tgtTerm);
    return !getBoundedVars(masterTerm).containsAll(getBoundedVars(slaveTerm));
  }

  public static boolean isLatentSummation(UExprTranslationResult uExprs) {
    return containsLatentSummation(getBody(uExprs.sourceExpr()))
            || containsLatentSummation(getBody(uExprs.targetExpr()));
  }

  static boolean containsLatentSummation(UTerm term) {
    // Sum + Sum or Sum * Sum
    final UKind kind = term.kind();
    if (kind == UKind.SUMMATION) return true;
    if (kind == UKind.SQUASH || kind == UKind.NEGATION || kind.isTermAtomic()) return false;
    if (kind == UKind.ADD || kind == UKind.MULTIPLY)
      for (UTerm subTerm : term.subTerms()) {
        if (containsLatentSummation(subTerm)) return true;
      }
    return false;
  }

  static boolean isFastRejected(UExprTranslationResult uExprs) {
    // return isMismatchedOutput(uExprs) || isMismatchedSummation(uExprs) || isLatentSummation(uExprs);
    return isMismatchedOutput(uExprs);
  }

  static Set<UVar> getBoundedVars(UTerm expr) {
    // Returns the summation variables for a summation, otherwise an empty list.
    if (expr.kind() == UKind.SUMMATION) return ((USum) expr).boundedVars();
    else return Collections.emptySet();
  }

  static UTerm getMaster(UTerm e0, UTerm e1) {
    final Set<UVar> vars0 = getBoundedVars(e0);
    final Set<UVar> vars1 = getBoundedVars(e1);
    if (vars0.size() >= vars1.size()) return e0;
    else return e1;
  }

  static UTerm getSlave(UTerm e0, UTerm e1) {
    final Set<UVar> vars0 = getBoundedVars(e0);
    final Set<UVar> vars1 = getBoundedVars(e1);
    if (vars0.size() < vars1.size()) return e0;
    else return e1;
  }

  static UTerm getBody(UTerm expr) {
    if (expr.kind() == UKind.SUMMATION) return ((USum) expr).body();
    else return expr;
  }
}
