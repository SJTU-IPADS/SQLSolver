package sqlsolver.superopt.liastar.translator;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.superopt.liastar.LiaStarSimplifier.simplify;
import static sqlsolver.superopt.liastar.translator.LiaStarTranslatorSupport.*;
import static sqlsolver.superopt.uexpr.PredefinedFunctions.*;
import static sqlsolver.superopt.uexpr.UExprSupport.isPredOfVarArg;

import java.util.*;

import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.liastar.*;
import sqlsolver.superopt.logic.LogicSupport;
import sqlsolver.superopt.uexpr.*;

public class LiaStarTranslator {
  private final Map<UVar, List<Value>> varSchema;
  private final UTerm query1;
  private final UTerm query2;
  private final UVar outVar;
  private final NameSequence liaVarName;

  public LiaStarTranslator(Map<UVar, List<Value>> varSchema, UTerm query1, UTerm query2, UVar outVar) {
    this.varSchema = new HashMap<>(varSchema);
    this.query1 = query1;
    this.query2 = query2;
    this.outVar = outVar;
    liaVarName = NameSequence.mkIndexed("u", 0);
  }

  public LiaStar uexpPairToLiastar(BVM bvm) {
    Map<USum, String> sumVarMap = new HashMap<>();
    Set<USum> sums1 = new HashSet<>(), sums2 = new HashSet<>();
    UTerm q1Lia = replaceSummations(sumVarMap, sums1, query1, liaVarName);
    UTerm q2Lia = replaceSummations(sumVarMap, sums2, query2, liaVarName);

    final LiaStar result;
    if (sumVarMap.isEmpty()) { // no summation
      result = uexpWOSumToLiastar(q1Lia, q2Lia);
    } else {
      result = uexpWithSumToLiastar(q1Lia, q2Lia, sumVarMap, bvm);
    }
    if (LogicSupport.dumpLiaFormulas) {
      System.out.println("Lia* before simplification: ");
      System.out.println(result);
    }
    // simplify the LIA* formula
    return simplify(result);
  }

  // +------------------------------+
  // | Utilities that may be common |
  // +------------------------------+

  private String newUliaVarName() {
    return liaVarName.next();
  }

  private static boolean isNullUTerm(UTerm t) {
    if (t.kind() != UKind.PRED) return false;
    UPred pred = (UPred) t;
    if (!isPredOfVarArg(pred)) return false;
    return pred.isPredKind(UPred.PredKind.FUNC) && pred.predName().equals(UName.NAME_IS_NULL);
  }

  // let term1 be mapped to v1 and term2 be mapped to v2
  // if v1=v2 then isnull(v1)=isnull(v2)
  // this is used to infer equivalence among "isnull" terms
  private static LiaStar isNullCongruence(
      Map<UTerm, String> termVarMap, Set<UVarTerm> isNullTuples) {
    if (isNullTuples.isEmpty() || termVarMap.isEmpty()) return null;
    Set<UTerm> isNullUTerms =
        new HashSet<>(termVarMap.keySet().stream().filter(LiaStarTranslator::isNullUTerm).toList());
    Set<UVarTerm> existingIsNullTuples = new HashSet<>();
    for (UTerm t : isNullUTerms) {
      UPred pred = (UPred) t;
      final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
      UVarTerm isNullTuple = UVarTerm.mk(pArgs.get(0));
      if (!isNullTuples.contains(isNullTuple)) existingIsNullTuples.add(isNullTuple);
    }
    LiaStar result = null;
    for (UVarTerm v1 : isNullTuples) {
      String var1 = termVarMap.get(v1);
      assert var1 != null;
      List<UTerm> isNullarg1 = new ArrayList<>();
      isNullarg1.add(v1);
      String isNullVar1 =
          termVarMap.get(UPred.mk(UPred.PredKind.FUNC, UName.NAME_IS_NULL, isNullarg1, true));
      LiaVarImpl liavar1 = (LiaVarImpl) LiaStar.mkVar(true, var1);
      LiaVarImpl liaIsNullvar1 = (LiaVarImpl) LiaStar.mkVar(true, isNullVar1);
      for (UVarTerm v2 : existingIsNullTuples) {
        String var2 = termVarMap.get(v2);
        assert var2 != null;
        List<UTerm> isNullarg2 = new ArrayList<>();
        isNullarg2.add(v2);
        String isNullVar2 =
            termVarMap.get(UPred.mk(UPred.PredKind.FUNC, UName.NAME_IS_NULL, isNullarg2, true));
        LiaVarImpl liavar2 = (LiaVarImpl) LiaStar.mkVar(true, var2);
        LiaVarImpl liaIsNullvar2 = (LiaVarImpl) LiaStar.mkVar(true, isNullVar2);
        LiaStar tmp =
            LiaStar.mkEq(
                true,
                liaIsNullvar1,
                LiaStar.mkIte(
                    true, LiaStar.mkEq(true, liavar1, liavar2), liaIsNullvar2, liaIsNullvar1));
        //        Liastar tmp = Liastar.mkOr(true,
        //            Liastar.mkNot(true, ),
        //            Liastar.mkEq(true, liaIsNullvar1, liaIsNullvar2));
        result = (result == null) ? tmp : LiaStar.mkAnd(true, result, tmp);
      }
    }
    return result;
  }

  // +---------------------------------+
  // | Methods specific for this class |
  // +---------------------------------+

  private LiaStar uexpWOSumToLiastar(UTerm t1, UTerm t2) {
    final Map<UVar, String> varMap = new HashMap<>();
    final LiaStar l1 = LiaTranslator.translate(t1, varSchema, liaVarName, varMap, new HashMap<>());
    final LiaStar l2 = LiaTranslator.translate(t2, varSchema, liaVarName, varMap, new HashMap<>());
    return LiaStar.mkNot(false, LiaStar.mkEq(false, l1, l2));
  }

  private LiaStar uexpWithSumToLiastar(UTerm t1, UTerm t2, Map<USum, String> sumVarMap, BVM bvm) {
    // collect free var terms (e.g. columns of the output tuple)
    // free var terms should not appear in inner vectors
    //   and should be mapped immediately
    final Map<UTerm, String> termMap = new HashMap<>();
    mapFreeVarTerms(query1, query1.getFVs(), termMap);
    mapFreeVarTerms(query2, query2.getFVs(), termMap);
    final Set<String> fvs = new HashSet<>(termMap.values());
    // construct the inequality between both U-expressions without stars
    LiaStar result =
        LiaStar.mkNot(
            false,
            LiaStar.mkEq(
                false,
                transUexpWithoutSum(false, t1, termMap, new HashSet<>()),
                transUexpWithoutSum(false, t2, termMap, new HashSet<>())));

    // convert the map "sumVarMap" to two lists "sums" and "sumVars"
    // For each i, (sums[i] -> sumVars[i]) represents an entry in "sumVarMap"
    List<UTerm> sums = new ArrayList<>();
    List<String> sumVars = new ArrayList<>();
    for (Map.Entry<USum, String> entry : sumVarMap.entrySet()) {
      sums.add(entry.getKey());
      sumVars.add(entry.getValue());
    }

    result =
        LiaStar.mkAnd(
            false, result, sumsToLiaStar(false, bvm, 0, sums, sumVars, termMap));
    fvs.addAll(result.collectVarNames());
    result.removeFVsFromInnerVector(fvs);
    return result;
  }

  private void mapFreeVarTerms(UTerm t, Set<String> fvs, Map<UTerm, String> termMap) {
    if (t instanceof UAtom atom) {
      final UVar var = atom.var();
      final List<UVar> args = Arrays.asList(var.args());
      if (all(args, arg -> fvs.contains(arg.name().toString()))) {
        if (!termMap.containsKey(t)) {
          termMap.put(t, newUliaVarName());
        }
      }
    }
    for (UTerm sub : t.subTerms()) {
      mapFreeVarTerms(sub, fvs, termMap);
    }
  }

  /** Translate U-expressions whose summations have been replaced. */
  private LiaStar transUexpWithoutSum(
      boolean innerStar,
      UTerm exp,
      Map<UTerm, String> uTermToLiaVar,
      Set<UVarTerm> isnullTuples) {
    return transUexpWithoutSum(innerStar, exp, uTermToLiaVar, isnullTuples, null);
  }

  // summations in exp have been replaced by new variables
  // Then transUexpWithoutSum translates exp into LIA formula
  // uTermToLiaVar is used to replace the same terms with same variables
  // isnullTuples collects vars surrounded by isnull
  // stringVars collects vars representing strings
  private LiaStar transUexpWithoutSum(
      boolean innerStar,
      UTerm exp,
      Map<UTerm, String> uTermToLiaVar,
      Set<UVarTerm> isnullTuples,
      Set<LiaVarImpl> stringVars) {
    switch (exp.kind()) {
      case SUMMATION -> throw new UnsupportedOperationException(
          "should not have summation when transforming to Lia!");
      case ADD -> {
        LiaStar result = null;
        final List<UTerm> subTerms = exp.subTerms();
        if (any(subTerms, this::containsNonInteger)) {
          // non-LIA operation
          final String varName = uTermToLiaVar.computeIfAbsent(exp, t -> newUliaVarName());
          return LiaStar.mkVar(innerStar, varName);
        }
        for (UTerm t : subTerms) {
          LiaStar curLia =
              transUexpWithoutSum(innerStar, t, uTermToLiaVar, isnullTuples, stringVars);
          if (result == null) {
            result = curLia;
          } else if (result instanceof LiaIteImpl) {
            result = ((LiaIteImpl) result).plusIte(curLia);
          } else if (curLia instanceof LiaIteImpl) {
            result = ((LiaIteImpl) curLia).plusIte(result);
          } else {
            result = LiaStar.mkPlus(innerStar, result, curLia);
          }
        }
        return result;
      }
      case MULTIPLY -> {
        LiaStar result = null;
        final List<UTerm> subTerms = exp.subTerms();
        // Note: non-LIA multiplication is handled later
        LiaStar iteNonzeroCond = null;
        for (UTerm t : subTerms) {
          LiaStar curLia =
              transUexpWithoutSum(innerStar, t, uTermToLiaVar, isnullTuples, stringVars);
          LiaStar[] condOpArray = new LiaStar[2];
          boolean hasZeroIte = LiaStar.ishasZeroIte(innerStar, curLia, condOpArray);
          if (hasZeroIte) {
            LiaStar cond = condOpArray[0];
            LiaStar op = condOpArray[1];
            iteNonzeroCond =
                (iteNonzeroCond == null) ? cond : LiaStar.mkAnd(innerStar, iteNonzeroCond, cond);
          } else {
            if (result == null) result = curLia;
            else {
              if (result instanceof LiaIteImpl) result = ((LiaIteImpl) result).MultIte(curLia);
              else if (curLia instanceof LiaIteImpl) result = ((LiaIteImpl) curLia).MultIte(result);
              else result = LiaStar.mkMul(innerStar, result, curLia);
            }
          }
        }
        if (iteNonzeroCond != null) {
          if (result == null) {
            result = LiaStar.mkConst(innerStar, 1);
          }
          result = LiaStar.mkIte(innerStar, iteNonzeroCond, result, LiaStar.mkConst(innerStar, 0));
        }
        return result;
      }
      case NEGATION, SQUASH -> {
        final UTerm body = ((UUnary) exp).body();
        final LiaStar cond =
            LiaStar.mkEq(
                innerStar,
                transUexpWithoutSum(innerStar, body, uTermToLiaVar, isnullTuples, stringVars),
                LiaStar.mkConst(innerStar, 0));
        return exp.kind() == UKind.NEGATION
            ? LiaStar.mkIte(
                innerStar, cond, LiaStar.mkConst(innerStar, 1), LiaStar.mkConst(innerStar, 0))
            : LiaStar.mkIte(
                innerStar, cond, LiaStar.mkConst(innerStar, 0), LiaStar.mkConst(innerStar, 1));
      }
      case CONST -> {
        return LiaStar.mkConst(false, ((UConst) exp).value());
      }
      case TABLE -> {
        final String varName = uTermToLiaVar.computeIfAbsent(exp, t -> newUliaVarName());
        return LiaStar.mkVar(innerStar, varName, Value.TYPE_NAT);
      }
      case PRED -> {
        // Case 1. ULiaVar
        if (exp instanceof ULiaVar) return LiaStar.mkVar(innerStar, exp.toString());
        final UPred pred = (UPred) exp;
        if (pred.isPredKind(UPred.PredKind.FUNC) && isPredOfVarArg(pred)) {
          // Case 2. [p(a(t))]. View `p(a(t))` as a variable
          final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
          assert pArgs.size() == 1;
          uTermToLiaVar.computeIfAbsent(UVarTerm.mk(pArgs.get(0)), v -> newUliaVarName());
          final String varName = uTermToLiaVar.computeIfAbsent(pred, v -> newUliaVarName());
          final LiaStar liaVar = LiaStar.mkVar(innerStar, varName);
          if (pred.predName().equals(UName.NAME_IS_NULL))
            isnullTuples.add(UVarTerm.mk(pArgs.get(0)));
          return LiaStar.mkIte(
              innerStar,
              LiaStar.mkEq(innerStar, liaVar, LiaStar.mkConst(innerStar, 0)),
              LiaStar.mkConst(innerStar, 0),
              LiaStar.mkConst(innerStar, 1));
        } else if (pred.isBinaryPred()) {
          // Case 3. [U-expr0 <binary op> U-expr1]
          if (!pred.isPredKind(UPred.PredKind.EQ)
                  && !pred.isPredKind(UPred.PredKind.NEQ)
                  && any(exp.subTerms(), this::containsNonInteger)) {
            // non-LIA comparison
            final String varName = uTermToLiaVar.computeIfAbsent(exp, t -> newUliaVarName());
            final LiaStar liaVar = LiaStar.mkVar(innerStar, varName);
            return LiaStar.mkIte(
                    innerStar,
                    LiaStar.mkEq(innerStar, liaVar, LiaStar.mkConst(innerStar, 0)),
                    LiaStar.mkConst(innerStar, 0),
                    LiaStar.mkConst(innerStar, 1));
          }
          final LiaStar liaVar0 =
              transUexpWithoutSum(
                  innerStar, pred.args().get(0), uTermToLiaVar, isnullTuples, stringVars);
          final LiaStar liaVar1 =
              transUexpWithoutSum(
                  innerStar, pred.args().get(1), uTermToLiaVar, isnullTuples, stringVars);
          final LiaStar target =
              switch (pred.predKind()) {
                case EQ -> LiaStar.mkEq(innerStar, liaVar0, liaVar1);
                case NEQ -> LiaStar.mkNot(innerStar, LiaStar.mkEq(innerStar, liaVar0, liaVar1));
                case LE -> LiaStar.mkLe(innerStar, liaVar0, liaVar1);
                case LT -> LiaStar.mkLt(innerStar, liaVar0, liaVar1);
                case GE -> LiaStar.mkLe(innerStar, liaVar1, liaVar0);
                case GT -> LiaStar.mkLt(innerStar, liaVar1, liaVar0);
                default -> throw new IllegalArgumentException("unsupported predicate in Uexpr.");
              };
          return LiaStar.mkIte(
              innerStar, target, LiaStar.mkConst(innerStar, 1), LiaStar.mkConst(innerStar, 0));
        } else {
          throw new UnsupportedOperationException("unsupported UPred var type.");
        }
      }
      case VAR -> {
        final UVarTerm vt = (UVarTerm) exp;
        final String varName = uTermToLiaVar.computeIfAbsent(exp, v -> newUliaVarName());
        return LiaStar.mkVar(innerStar, varName, getVarTypeInSchema(vt.var()));
      }
      case STRING -> {
        final String varName = uTermToLiaVar.computeIfAbsent(exp, v -> newUliaVarName());
        final LiaVarImpl var = LiaStar.mkVar(innerStar, varName, Value.TYPE_CHAR);
        if (stringVars != null) stringVars.add(var);
        return var;
      }
      case FUNC -> {
        final UFunc func = (UFunc) exp;
        final List<UTerm> args = func.args();
        if (none(args, this::containsNonInteger)) {
          final String funcName = func.funcName().toString();
          final int arity = args.size();
          if (DIVIDE.contains(funcName, arity)) {
            // DIVIDE is translated into LiaDiv
            final LiaStar liaOp0 = transUexpWithoutSum(innerStar, args.get(0), uTermToLiaVar, isnullTuples, stringVars);
            final LiaStar liaOp1 = transUexpWithoutSum(innerStar, args.get(1), uTermToLiaVar, isnullTuples, stringVars);
            return LiaStar.mkDiv(innerStar, liaOp0, liaOp1);
          } else if (MINUS.contains(funcName, arity)) {
            // MINUS is translated into LiaFunc then subtraction in Z3
            return transFuncWithoutSum(innerStar, (UFunc) exp, uTermToLiaVar, isnullTuples, stringVars);
          }
        }
        final String varName = uTermToLiaVar.computeIfAbsent(exp, t -> newUliaVarName());
        return LiaStar.mkVar(innerStar, varName);
      }
      default -> throw new UnsupportedOperationException("unsupported UTerm type.");
    }
  }

  private LiaStar transFuncWithoutSum(
      boolean innerStar,
      UFunc func,
      Map<UTerm, String> uTermToLiaVar,
      Set<UVarTerm> isnullTuples,
      Set<LiaVarImpl> stringVars) {
    // translate the function case by case
    final String funcName = func.funcName().toString();
    final int arity = func.args().size();
    // non-negative integral functions can be replaced with a var
    if (returnsNonNegativeInt(funcName, arity)) {
      final String varName = uTermToLiaVar.computeIfAbsent(func, t -> newUliaVarName());
      return LiaStar.mkVar(innerStar, varName, Value.TYPE_NAT);
    }
    // other functions are translated into different LIA terms
    // first translate function arguments
    final List<LiaStar> liaOps = new ArrayList<>();
    final List<UTerm> args = func.args();
    for (UTerm arg : args) {
      LiaStar liaOp = transUexpWithoutSum(innerStar, arg, uTermToLiaVar, isnullTuples, stringVars);
      liaOps.add(liaOp);
    }
    // then translate the function
    return translateFunc(innerStar, funcName, liaOps);
  }

  private boolean containsNonInteger(UTerm term) {
    if (term instanceof UConst || term instanceof UTable || term instanceof ULiaVar) return false;
    if (term instanceof UString) return true;
    if (term instanceof UVarTerm vt) {
      return !Value.isIntegralType(getVarTypeInSchema(vt.var()));
    }
    return all(term.subTerms(), this::containsNonInteger);
  }

  private UTerm replaceAndRemoveBoundVar(USum sum, UVar oldVar, UVar newVar) {
    sum.boundedVars().remove(oldVar);
    sum.replaceVarInplace(oldVar, newVar, true);
    if (sum.boundedVars().isEmpty()) {
      return sum.body().copy();
    } else {
      return sum.copy();
    }
  }

  /** Replace a bound var in each summation with a common tuple according to the chosen BVM (a set of bound vars). */
  private void injectCommonTuple(List<UTerm> sums, Set<UVar> bvm) {
    // useful data structure
    record Schema(List<Value> list) {
      @Override
      public boolean equals(Object o) {
        if (!(o instanceof Schema that)) return false;
        return CalciteSupport.isEqualTypeTwoValueList(list, that.list);
      }
      @Override
      public int hashCode() {
        return list.size();
      }
    }

    // group together tuples with the same schema
    final Map<Schema, Set<UVar>> schemaGroups = new HashMap<>();
    for (UVar bv : bvm) {
      final Schema schema = new Schema(varSchema.get(bv));
      final Set<UVar> set = schemaGroups.computeIfAbsent(schema, s -> new HashSet<>());
      set.add(bv);
    }
    // for each group of tuples with the same schema, generate a common tuple
    final Map<UVar, UVar> commonTuples = new HashMap<>();
    for (Map.Entry<Schema, Set<UVar>> entry : schemaGroups.entrySet()) {
      final UVar commonTuple = UVar.mkBase(UName.mk(newUliaVarName()));
      for (UVar bv : entry.getValue())
        commonTuples.put(bv, commonTuple);
      varSchema.put(commonTuple, entry.getKey().list());
    }

    // map different bound vars to different common tuples
    for (UVar bv : bvm) {
      sums.replaceAll(
          term -> {
            if (term instanceof USum sum && sum.boundedVars().contains(bv)) {
              return replaceAndRemoveBoundVar(sum, bv, commonTuples.get(bv));
            }
            return term;
          });
    }
  }

  private LiaStar sumsToLiaStar(
      boolean isInner,
      BVM bvm,
      int depth,
      List<UTerm> sumList,
      List<String> sumVarList,
      Map<UTerm, String> termVarMap) {

    List<UTerm> newSumList = new ArrayList<>();
    copyUExprList(sumList, newSumList);

    injectCommonTuple(newSumList, bvm.get(depth));

    final List<UTerm> subSums = new ArrayList<>();
    final List<String> subVars = new ArrayList<>();
    replaceSumsInList(newSumList, subSums, subVars, liaVarName);
    LiaStar constraints = null;
    List<String> innerVector = new ArrayList<>();
    Set<UVarTerm> isNullTuples = new HashSet<>();
    Set<LiaVarImpl> stringVars = new HashSet<>();
    // equations
    for (int i = 0; i < sumVarList.size(); ++i) {
      String innerVarName = newUliaVarName();
      innerVector.add(innerVarName);
      LiaStar equation =
          transUexpWithoutSum(true, newSumList.get(i), termVarMap, isNullTuples, stringVars);
      equation = LiaStar.mkEq(true, LiaStar.mkVar(true, innerVarName), equation);
      if (constraints == null) {
        constraints = equation;
      } else {
        constraints = LiaStar.mkAnd(true, constraints, equation);
      }
    }
    // IsNull constraints
    LiaStar isNullConstraints = isNullCongruence(termVarMap, isNullTuples);
    if (isNullConstraints != null)
      constraints = LiaStar.mkAnd(true, constraints, isNullConstraints);
    if (!subSums.isEmpty()) {
      constraints =
          LiaStar.mkAnd(
              true,
              constraints,
              sumsToLiaStar(
                  true,
                  bvm,
                  depth + 1,
                  subSums,
                  subVars,
                  termVarMap));
    }
    // string var constraints
    LiaStar stringVarConstraints = null;
    Set<LiaVarImpl> usedStringVars = new HashSet<>();
    for (LiaVarImpl v1 : stringVars) {
      usedStringVars.add(v1);
      for (LiaVarImpl v2 : stringVars) {
        if (!usedStringVars.contains(v2)) {
          LiaStar neq = LiaStar.mkNot(true, LiaStar.mkEq(true, v1, v2));
          if (stringVarConstraints == null) stringVarConstraints = neq;
          else stringVarConstraints = LiaStar.mkAnd(true, stringVarConstraints, neq);
        }
      }
    }
    constraints = LiaStar.mkAnd(true, constraints, stringVarConstraints);

    return LiaStar.mkSum(isInner, sumVarList, innerVector, constraints);
  }

  private String getVarTypeInSchema(UVar var) {
    assert var.kind() == UVar.VarKind.PROJ;
    final int index = CalciteSupport.columnNameToIndex(var.name().toString());
    return varSchema.get(var.args()[0]).get(index).type();
  }
}
