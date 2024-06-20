package sqlsolver.superopt.logic;

import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.config.GlobalConfig;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.liastar.LiaSolverStatus;
import sqlsolver.superopt.liastar.LiaSolver;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.translator.BVM;
import sqlsolver.superopt.liastar.translator.BvmEnumerator;
import sqlsolver.superopt.liastar.translator.HeuristicBvmEnumerator;
import sqlsolver.superopt.liastar.translator.LiaStarTranslator;
import sqlsolver.superopt.uexpr.*;

import java.util.*;

import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.superopt.uexpr.UExprSupport.isPredOfVarArg;

public class SqlSolver {
  public static final Integer Z3_TIMEOUT = GlobalConfig.SQLSOLVER_Z3_TIMEOUT; // ms
  private static final Properties[] LIA_SOLVER_CONFIGS;

  static {
    LIA_SOLVER_CONFIGS = new Properties[2];
    LIA_SOLVER_CONFIGS[0] = new Properties();
    LIA_SOLVER_CONFIGS[0].setProperty(LiaSolver.CONFIG_KEY_PARAM_REMOVAL_MODE, LiaSolver.CONFIG_VALUE_PARAM_REMOVAL_MODE_INWARD);
    LIA_SOLVER_CONFIGS[1] = new Properties();
    LIA_SOLVER_CONFIGS[1].setProperty(LiaSolver.CONFIG_KEY_PARAM_REMOVAL_MODE, LiaSolver.CONFIG_VALUE_PARAM_REMOVAL_MODE_OUTWARD);
  }

  private Schema tableSchema;
  private Map<UVar, List<Value>> varSchema;
  private UTerm query1;
  private UTerm query2;
  private UVar outVar1;
  private UVar outVar2;

  public SqlSolver(UExprTranslationResult uExprs) {
    this.query1 = uExprs.sourceExpr().copy();
    this.query2 = uExprs.targetExpr().copy();
    this.outVar1 = uExprs.sourceOutVar().copy();
    this.outVar2 = uExprs.targetOutVar().copy();
  }

  public SqlSolver(UExprConcreteTranslationResult uExprs, Schema schema) {
    tableSchema = schema;
    this.varSchema = uExprs.getTupleVarSchemas();
    this.query1 = uExprs.sourceExpr().copy();
    this.query2 = uExprs.targetExpr().copy();
    this.outVar1 = uExprs.sourceOutVar().copy();
    this.outVar2 = uExprs.targetOutVar().copy();
  }

  /* return EQ when q1 == q2 */
  public VerificationResult proveEq() {
    if (!outVar1.equals(outVar2)) return VerificationResult.NEQ;

    // compare both U-expressions
    // completely the same U-expressions are equal obviously
    preprocess();
    if (LogicSupport.dumpLiaFormulas) {
      System.out.println("==> Rewritten UExpressions sent to Lia solver: ");
      System.out.println("[[q0]](" + outVar1 + ") := ");
      query1.prettyPrint();
      System.out.println();
      System.out.println("[[q1]](" + outVar2 + ") := ");
      query2.prettyPrint();
      System.out.println();
    }
    UMulImpl.useWeakEquals = true;
    if (query1.equals(query2)) {
      UMulImpl.useWeakEquals = false;
      return VerificationResult.EQ;
    }
    UMulImpl.useWeakEquals = false;

    // try to prove equivalence when summations are under set semantics
    if (LogicSupport.dumpLiaFormulas) {
      System.out.println("==> Try set solver:");
    }
    final VerificationResult setResult = new SetSolver(query1, query2, varSchema, tableSchema).proveEq();
    if (LogicSupport.dumpLiaFormulas) {
      System.out.println("Set solver result: " + setResult);
    }
    if (setResult == VerificationResult.EQ) return VerificationResult.EQ;

    // prove under bag semantics
    // generate LIA* formulas and solve
    boolean allSAT = true;
    LiaStarTranslator translator = new LiaStarTranslator(varSchema, query1, query2, outVar1);
    BvmEnumerator bvmEnumerator = new HeuristicBvmEnumerator(query1, query2, outVar1);
    BVM currentBVM = bvmEnumerator.next();
    int count = 0;
    while (currentBVM != null) {
      // given a BVM, translate U-exp to LIA*
      if (LogicSupport.dumpLiaFormulas) {
        System.out.println("==> Using bound var matching " + (++count) + ": ");
        System.out.println(currentBVM);
      }
      LiaStar fstar = translator.uexpPairToLiastar(currentBVM);
      if (LogicSupport.dumpLiaFormulas) {
        System.out.println("==> Lia* formula: ");
        System.out.println(fstar);
      }
      // solve LIA*
      for (Properties config : LIA_SOLVER_CONFIGS) {
        final LiaSolverStatus result = LiaSolver.solveWithConfig(fstar, config);
        switch (result) {
          case UNSAT:
            // one UNSAT indicates query equivalence
            return VerificationResult.EQ;
          default:
            allSAT = allSAT && result == LiaSolverStatus.UNKNOWN;
        }
      }
      // move onto next BVM
      currentBVM = bvmEnumerator.next();
    }
    // a handful of BVMs have been enumerated; the result is perceived as NEQ
    return VerificationResult.NEQ;
  }

  public static void initialize() {
    LiaStar.resetId();
  }

  Set<UVar> decomposeUVar(UVar outVar) {
    Set<UVar> result = new HashSet<>();
    if (outVar.kind() == UVar.VarKind.BASE) {
      result.add(outVar.copy());
    } else {
      result.addAll(List.of(outVar.args()));
    }
    return result;
  }

  void preprocess() {
    Set<UVar> outerVars = decomposeUVar(outVar1);
    query1 = concretizeBoundedVars(query1.copy(), outerVars);
    query1 = propagateNullValue(query1);
    query1 = propagateConstant(query1, new HashMap<>());
    query2 = concretizeBoundedVars(query2.copy(), outerVars);
    query2 = propagateNullValue(query2);
    query2 = propagateConstant(query2, new HashMap<>());
  }

  public static UTerm propagateConstant(UTerm expr, Map<UTerm, Integer> tupleToConst) {
    UKind kind = expr.kind();
    switch (kind) {
      case ADD:
        {
          List<UTerm> subterms = new ArrayList<>();
          for (UTerm t : expr.subTerms()) {
            Map<UTerm, Integer> tmpBoard = new HashMap<>(tupleToConst);
            tmpBoard.putAll(tupleToConst);
            UTerm tmp = propagateConstant(t, tmpBoard);
            subterms.add(tmp);
          }
          switch (subterms.size()) {
            case 0:
              assert false;
              return null;
            case 1:
              return subterms.get(0);
            default:
              return UAdd.mk(subterms);
          }
        }
      case MULTIPLY:
        {
          List<UTerm> subterms = new ArrayList<>();
          subterms.addAll(expr.subTerms());
          List<UTerm> newSubTerms = new ArrayList<>();
          int hashMapSize = 0;
          do {
            hashMapSize = tupleToConst.size();
            for (UTerm t : subterms) {
              UTerm tmp = propagateConstant(t, tupleToConst);
              newSubTerms.add(tmp);
            }
            subterms.clear();
            subterms.addAll(newSubTerms);
            newSubTerms.clear();
          } while (hashMapSize != tupleToConst.size());
          switch (subterms.size()) {
            case 0:
              {
                assert false;
                return null;
              }
            case 1:
              {
                return subterms.get(0);
              }
            default:
              {
                if (any(subterms, t -> t.equals(UConst.ZERO))) return UConst.zero();
                return UMul.mk(subterms);
              }
          }
        }
      case SUMMATION:
        {
          USum sum = (USum) expr;
          UTerm body = propagateConstant(((USum) expr).body(), tupleToConst);
          if (body.equals(UConst.ZERO)) return UConst.zero();
          return USum.mk(sum.boundedVars(), body);
        }
      case NEGATION:
        {
          Map<UTerm, Integer> tmpTupleToConst = new HashMap<>();
          tmpTupleToConst.putAll(tupleToConst);
          UTerm body = propagateConstant(((UNeg) expr).body(), tmpTupleToConst);
          return UNeg.mk(body);
        }
      case SQUASH:
        {
          UTerm body = propagateConstant(((USquash) expr).body(), tupleToConst);
          return USquash.mk(body);
        }
      case PRED:
        {
          UPred pred = (UPred) expr.copy();
          if (pred.predKind() == UPred.PredKind.EQ) {
            UTerm left = pred.args().get(0);
            UTerm right = pred.args().get(1);
            if (left instanceof UVarTerm && right instanceof UVarTerm) {
              Integer leftConstVal = tupleToConst.get(left);
              Integer rightConstVal = tupleToConst.get(right);
              if (leftConstVal != null && rightConstVal == null) {
                tupleToConst.put(right, leftConstVal);
                pred.args().set(0, UConst.mk(leftConstVal));
              } else if (leftConstVal == null && rightConstVal != null) {
                tupleToConst.put(left, rightConstVal);
                pred.args().set(1, UConst.mk(rightConstVal));
              }
            } else if (left instanceof UConst && right instanceof UVarTerm) {
              Integer rightConstVal = tupleToConst.get(right);
              if (rightConstVal == null) {
                tupleToConst.put(right, ((UConst) left).value());
              } else if (!rightConstVal.equals(((UConst) left).value())) {
                return UConst.mk(0);
              }
            } else if (left instanceof UVarTerm && right instanceof UConst) {
              Integer leftConstVal = tupleToConst.get(left);
              if (leftConstVal == null) {
                tupleToConst.put(left, ((UConst) right).value());
              } else if (!leftConstVal.equals(((UConst) right).value())) {
                return UConst.mk(0);
              }
            }
            return pred;
          } else {
            return expr;
          }
        }
      default:
        {
          return expr;
        }
    }
  }

  public static void analyseNullPredicate(
      UTerm expr, Set<UVar> nullTuples, Set<UVar> notnullTuples) {
    UKind kind = expr.kind();
    switch (kind) {
      case MULTIPLY:
        {
          for (UTerm t : expr.subTerms()) {
            analyseNullPredicate(t, nullTuples, notnullTuples);
          }
          return;
        }
      case SUMMATION:
        {
          analyseNullPredicate(((USum) expr).body(), nullTuples, notnullTuples);
          return;
        }
      case NEGATION:
        {
          UTerm negBody = ((UNeg) expr).body();
          if (negBody instanceof UPred) {
            UPred pred = (UPred) negBody;
            if (pred.isPredKind(UPred.PredKind.FUNC)
                && isPredOfVarArg(pred)
                && pred.predName().equals(UName.NAME_IS_NULL)) {
              final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
              assert pArgs.size() == 1;
              notnullTuples.add(pArgs.get(0));
            }
          }
          //        else {
          //          analyseNullPredicate(negBody, notnullTuples, nullTuples);
          //        }
          return;
        }
      case SQUASH:
        {
          analyseNullPredicate(((USquash) expr).body(), nullTuples, notnullTuples);
          return;
        }
      case PRED:
        {
          UPred pred = (UPred) expr;
          if (pred.isPredKind(UPred.PredKind.FUNC)
              && isPredOfVarArg(pred)
              && pred.predName().equals(UName.NAME_IS_NULL)) {
            final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
            assert pArgs.size() == 1;
            nullTuples.add(pArgs.get(0));
          } else if (pred.isPredKind(UPred.PredKind.EQ) && !isPredOfVarArg(pred)) {
            List<UTerm> args = pred.args();
            for (UTerm arg : args) {
              if (arg instanceof UVarTerm) {
                notnullTuples.add(((UVarTerm) arg).var());
              }
            }
          } else if (pred.predKind() != UPred.PredKind.FUNC
              && pred.predKind() != UPred.PredKind.EQ) {
            List<UTerm> args = pred.args();
            for (UTerm arg : args) {
              if (arg instanceof UVarTerm) {
                notnullTuples.add(((UVarTerm) arg).var());
              }
            }
          }
          return;
        }
      default:
        {
          return;
        }
    }
  }

  public static void expandNullTuples(Set<UVar> nullTuples, List<Set<UVar>> eqPairs) {
    boolean isModified = true;
    while (isModified == true) {
      isModified = false;
      Set<UVar> newNullTuples = new HashSet<>();
      for (UVar nullTuple : nullTuples) {
        for (Set<UVar> eqTuples : eqPairs) {
          boolean containNullTuple = eqTuples.contains(nullTuple);
          boolean useNullTuple =
              (nullTuple.kind() == UVar.VarKind.BASE)
                  ? any(eqTuples, arg -> arg.isUsing(nullTuple))
                  : false;
          if (containNullTuple || useNullTuple) {
            for (UVar v : eqTuples) {
              if (!nullTuples.contains(v)) {
                isModified = true;
                newNullTuples.add(v);
              }
            }
          }
        }
      }
      nullTuples.addAll(newNullTuples);
    }
  }

  public static UTerm propagateNullMultiStep2(UMul expr) {
    List<UTerm> subterms = new ArrayList<>();
    for (UTerm t : expr.subTerms()) {
      UTerm tmp = propagateNullValue(t);
      if (!tmp.equals(UConst.ZERO)) {
        subterms.add(tmp);
      } else {
        return UConst.zero();
      }
    }
    switch (subterms.size()) {
      case 0:
        return UConst.zero();
      case 1:
        return subterms.get(0);
      default:
        return UMul.mk(subterms);
    }
  }

  public static UTerm propagateNullMultiStep1(UMul expr) {
    Set<Pair<UVar, UVar>> eqPairs = collectAllPredicates(expr);
    List<Set<UVar>> eqRels = SqlSolverSupport.buildEqRelation(eqPairs);
    Set<UVar> nullTuples = new HashSet<>();
    Set<UVar> notnullTuples = new HashSet<>();
    analyseNullPredicate(expr, nullTuples, notnullTuples);
    expandNullTuples(nullTuples, eqRels);
    if (SetSupport.intersects(nullTuples, notnullTuples)) {
      return UConst.zero();
    } else {
      for (UVar nullTuple : nullTuples) {
        if (nullTuple.kind() == UVar.VarKind.BASE) {
          if (any(notnullTuples, arg -> arg.isUsing(nullTuple))) {
            return UConst.zero();
          }
        }
      }
    }
    return expr;
  }

  public static UTerm propagateNullValue(UTerm expr) {
    UKind kind = expr.kind();
    switch (kind) {
      case ADD:
        {
          List<UTerm> subterms = new ArrayList<>();
          for (UTerm t : expr.subTerms()) {
            UTerm tmp = propagateNullValue(t);
            if (!tmp.equals(UConst.ZERO)) {
              subterms.add(tmp);
            }
          }
          switch (subterms.size()) {
            case 0:
              return UConst.zero();
            case 1:
              return subterms.get(0);
            default:
              return UAdd.mk(subterms);
          }
        }
      case MULTIPLY:
        {
          UTerm tmp = propagateNullMultiStep1((UMul) expr);
          if (tmp instanceof UMul) {
            return propagateNullMultiStep2((UMul) tmp);
          } else {
            return tmp;
          }
        }
      case SUMMATION:
        {
          USum sum = (USum) expr;
          UTerm body = propagateNullValue(((USum) expr).body());
          if (body.equals(UConst.ZERO)) {
            return UConst.zero();
          } else {
            return USum.mk(sum.boundedVars(), body);
          }
        }
      case NEGATION:
        {
          UTerm body = propagateNullValue(((UNeg) expr).body());
          if (body.equals(UConst.ZERO)) {
            return UConst.one();
          } else {
            return UNeg.mk(body);
          }
        }
      case SQUASH:
        {
          UTerm body = propagateNullValue(((USquash) expr).body());
          if (body.equals(UConst.ZERO)) {
            return UConst.zero();
          } else {
            return USquash.mk(body);
          }
        }
      case PRED:
        {
          UPred pred = (UPred) expr;
          if (pred.predKind() == UPred.PredKind.EQ) {
            for (int i = 0; i < pred.args().size(); ++i) {
              UTerm tmp = propagateNullValue(pred.args().get(i));
              pred.args().set(i, tmp);
            }
            return pred;
          } else {
            return expr;
          }
        }
      default:
        {
          return expr;
        }
    }
  }

  public static Set<Pair<UVar, UVar>> collectAllPredicates(UTerm term) {
    Set<Pair<UVar, UVar>> result = new HashSet<>();
    switch (term.kind()) {
      case MULTIPLY:
        {
          for (UTerm t : term.subTerms()) {
            result.addAll(collectAllPredicates(t));
          }
          return result;
        }
      case SUMMATION:
        {
          return collectAllPredicates(((USum) term).body());
        }
      case SQUASH:
        {
          return collectAllPredicates(((USquash) term).body());
        }
      case PRED:
        {
          final UPred pred = (UPred) term;
          if (pred.isPredKind(UPred.PredKind.EQ) && isPredOfVarArg(pred)) {
            final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
            assert pArgs.size() == 2;
            result.add(Pair.of(pArgs.get(0).copy(), pArgs.get(1).copy()));
          }
          return result;
        }
      default:
        {
          return result;
        }
    }
  }

  public static UVar selectConcreteTuple(
      List<Set<UVar>> eqRels, UVar boundedVar, Set<UVar> outVars) {
    for (Set<UVar> eqVars : eqRels) {
      if (eqVars.contains(boundedVar)) {
        for (UVar v : eqVars) {
          if (SqlSolverSupport.hasFreeTuple(v, outVars)) {
            return v;
          }
        }
      }
    }
    return null;
  }

  UTerm concretizeBoundedVars(UTerm expr, Set<UVar> outerVars) {
    UKind kind = expr.kind();
    switch (kind) {
      case ADD:
      case MULTIPLY:
        {
          List<UTerm> subterms = new ArrayList<>();
          for (UTerm t : expr.subTerms()) {
            subterms.add(concretizeBoundedVars(t, outerVars));
          }
          return (kind == UKind.ADD) ? UAdd.mk(subterms) : UMul.mk(subterms);
        }
      case SUMMATION:
        {
          Set<UVar> boundedVars = ((USum) expr).boundedVars();
          Set<UVar> newOuterVars = new HashSet<>(outerVars);
          newOuterVars.addAll(boundedVars);
          UTerm newBody = concretizeBoundedVars(((USum) expr).body(), newOuterVars);
          Set<Pair<UVar, UVar>> eqPairs = collectAllPredicates(((USum) expr).body());
          List<Set<UVar>> eqRels = SqlSolverSupport.buildEqRelation(eqPairs);
          Set<UVar> newBoundedVars = new HashSet<>();
          for (UVar v : boundedVars) {
            UVar newVar = selectConcreteTuple(eqRels, v, outerVars);
            if (newVar == null) {
              newBoundedVars.add(v);
            } else {
              newBody.replaceVarInplace(v, newVar, false);
            }
          }
          if (newBoundedVars.isEmpty()) {
            return newBody;
          } else {
            return USum.mk(newBoundedVars, newBody);
          }
        }
      case NEGATION:
        {
          return UNeg.mk(concretizeBoundedVars(((UNeg) expr).body(), outerVars));
        }
      case SQUASH:
        {
          return USquash.mk(concretizeBoundedVars(((USquash) expr).body(), outerVars));
        }
      default:
        {
          return expr;
        }
    }
  }
}
