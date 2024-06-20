package sqlsolver.superopt.uexpr.normalizer;

import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.util.Timeout;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.concat;
import static sqlsolver.common.utils.ListSupport.filter;
import static sqlsolver.superopt.uexpr.UExprSupport.*;

/**
 * This class provides the basic normalization for U-Expression.
 */
public class UNormalization {
  public boolean isModified;
  public UTerm expr;
  protected final UExprConcreteTranslator.QueryTranslator translator;

  public UNormalization(UTerm expr) {
    this.isModified = false;
    this.expr = expr;
    this.translator = null;
  }

  public UNormalization(UTerm expr, UExprConcreteTranslator.QueryTranslator translator) {
    this.isModified = false;
    this.expr = expr;
    this.translator = translator;
  }

  public static boolean isNormalForm(UTerm expr) {
    return isNormalFormExpr(expr);
  }

  /**
   * Sum-Product Normal Form: E = T1 + .. + Tn, Ti = \sum{t1..tm}([b1] * .. * [bt] * R1(t1) * .. *
   * Rs(ts) * ||E1|| * not(E2)). If m = 0, Ti = [b1] * .. * [bt] * R1(t1) * .. * Rs(ts) * ||E1|| *
   * not(E2). isNormalFormExpr() checks an E, isNormalFormTerm() checks a T.
   */
  private static boolean isNormalFormExpr(UTerm expr) {
    switch (expr.kind()) {
      case ADD:
        for (UTerm subTerm : expr.subTerms()) {
          if (!isNormalFormTerm(subTerm)) return false;
        }
        return true;
      case MULTIPLY:
        return isNormalFormTerm(expr);
      case SUMMATION:
        return isNormalFormTerm(((USum) expr).body());
      case SQUASH, NEGATION:
        return isNormalFormExpr(((UUnary) expr).body());
      case TABLE, PRED:
        // To be considered..., whether we need to consider a single [b] or R(t) to be an E or T?
        // Or wrap them in a UMul() during normalization?
        return true;
      default:
        return false;
    }
  }

  private static boolean isNormalFormTerm(UTerm term) {
    switch (term.kind()) {
      case MULTIPLY:
        // int squashNum = 0, negNum = 0;
        for (UTerm factor : term.subTerms()) {
          if (factor.kind().isBinary() || factor.kind() == UKind.SUMMATION) return false;
          if (factor.kind().isUnary()) {
            // if (factor.kind() == SQUASH) ++squashNum;
            // else ++negNum;
            if (!isNormalFormExpr(((UUnary) factor).body())) return false;
          }
        }
        // return squashNum <= 1 && negNum <= 1;
        return true;
      case SUMMATION:
        final UTerm body = ((USum) term).body();
        if (body.kind() != UKind.MULTIPLY) return false;
        return isNormalFormTerm(body);
      case SQUASH, NEGATION:
        return isNormalFormExpr(((UUnary) term).body());
      case TABLE, PRED:
        return true;
      default:
        return false;
    }
  }

  public UTerm normalizeTerm() {
    do {
      isModified = false;
      // A round of normalizations

      expr = performNormalizeRule(this::eliminateSquash);
      expr = performNormalizeRule(this::eliminateNegation);
      expr = performNormalizeRule(this::promoteSummation);
      expr = performNormalizeRule(this::mergeSummation);
      expr = performNormalizeRule(this::combineSquash);
      expr = performNormalizeRule(this::distributeAddToMul);
      expr = performNormalizeRule(this::distributeAddToSummation);
      expr = performNormalizeRule(this::removeConstants);
      expr = performNormalizeRule(this::transformEqNullIsNull);
      expr = performNormalizeRule(this::removeNullConstant);
      expr = performNormalizeRule(this::foldConstants);
      expr = performNormalizeRule(this::removeRedundantFactors);
      expr = performNormalizeRule(this::removeRedundantSameEqual);
      expr = performNormalizeRule(this::removeRedundantFunction);
      expr = performNormalizeRule(this::applyFunction);
      expr = performNormalizeRule(this::removeSquashOfContradictAddition);
      expr = performNormalizeRule(this::removeDuplicatesInSet);
      expr = performNormalizeRule(this::simplifyUnaryTerm);
    } while (isModified);

    return expr;
  }

  protected UTerm performNormalizeRule(Function<UTerm, UTerm> transformation) {
    expr = transformation.apply(expr);
    Timeout.checkTimeout();
    // Routine normalizations
    expr = flatSingletonAddAndMul(expr);
    expr = flatAddAndMul(expr);
    if (translator != null)
      // Only normalizers with translator (i.e. tuple schema info) apply this rule
      // Other normalizers have problems in maintaining schema of new tuples
      renameSameBoundedVarSummation(expr, new HashSet<>());
    return expr;
  }

  /**
   * Rename all the vars between different summation if their boundedVars have same name
   * e.g. sum{t1} + sum{t1} => sum{t1} + sum{t2}     t1 and t2 have the same schema
   */
  void renameSameBoundedVarSummation(UTerm expr, Set<UVar> varSet) {
    for (UTerm subTerm : expr.subTerms())
      renameSameBoundedVarSummation(subTerm, varSet);
    final UKind kind = expr.kind();
    if (kind != UKind.SUMMATION) return;

    final USum sum = (USum) expr;
    final Set<UVar> boundedVars = sum.boundedVars();
    final Map<UVar, UVar> replaceMap = new HashMap<>();

    for (final UVar boundedVar : boundedVars) {
      assert boundedVar.kind() == UVar.VarKind.BASE;
      if (varSet.contains(boundedVar)) {
        final UVar newVar = mkFreshBaseVar();
        translator.putTupleVarSchema(newVar, translator.getTupleVarSchema(boundedVar));
        varSet.add(newVar);
        replaceMap.put(boundedVar, newVar);
      } else {
        varSet.add(boundedVar);
      }
    }

    for (final Map.Entry<UVar, UVar> entry : replaceMap.entrySet()) {
      sum.replaceVarInplace(entry.getKey(), entry.getValue(), true);
    }
  }

  /**
   * ADD/MUL[ E1, .., ADD/MUL[Ei, .., Ej], .., En ] -> ADD/MUL[E1, .., Ei, .., Ej, .., En]
   */
  UTerm flatAddAndMul(UTerm expr) {
    expr = transformSubTerms(expr, this::flatAddAndMul);

    final UKind kind = expr.kind();
    if (!kind.isBinary()) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    for (int i = 0, bound = subTerms.size(); i < bound; ++i) {
      final UTerm subTerm = subTerms.get(i);
      if (subTerm.kind() == kind) subTerms.addAll(subTerm.subTerms());
    }

    // This will not remove the flattened sub-terms since the kind of sub-terms must be
    // different from `kind` here, because `flatAddAndMul` starts from the innermost layer
    // of U-expression. e.g. ADD(x1, ADD(ADD(x3, x4), x5), x2) does not exists.
    if (subTerms.removeIf(t -> t.kind() == kind)) isModified = true;
    return expr;
  }

  /**
   * Remove ADD/MUL with only one element
   */
  UTerm flatSingletonAddAndMul(UTerm expr) {
    if (expr.kind() == UKind.SUMMATION && ((USum) expr).body().subTerms().size() == 1) {
      // Ignore cases for summation's single-subTerm body (which is always an ADD or MUL)
      UTerm singletonSubTerm = ((USum) expr).body().subTerms().get(0);
      singletonSubTerm = transformSubTerms(singletonSubTerm, this::flatSingletonAddAndMul);
      ((USum) expr).body().subTerms().set(0, singletonSubTerm);
      return expr;
    }
    expr = transformSubTerms(expr, this::flatSingletonAddAndMul);

    final UKind kind = expr.kind();
    if (!kind.isBinary()) return expr;

    if (expr.subTerms().size() == 1) {
      isModified = true;
      return expr.subTerms().get(0);
    }
    return expr;
  }

  /**
   * E * \sum{t}f(t) -> \sum{t}(E * f(t))
   */
  UTerm promoteSummation(UTerm expr) {
    expr = transformSubTerms(expr, this::promoteSummation);
    if (expr.kind() != UKind.MULTIPLY) return expr;

    Set<UVar> freeVars = null;
    final ListIterator<UTerm> iter = expr.subTerms().listIterator();
    while (iter.hasNext()) {
      final UTerm factor = iter.next();
      if (factor.kind() == UKind.SUMMATION) {
        final USum sum = (USum) factor;
        if (freeVars == null) freeVars = new HashSet<>(sum.boundedVars().size());
        freeVars.addAll(sum.boundedVars());
        iter.set(sum.body());
      }
    }
    if (freeVars != null) {
      isModified = true;
      return USum.mk(freeVars, expr.copy());
    } else return expr;
  }

  /**
   * \sum{x}(f(x) * \sum{y}(g(y))) -> Sum[x,y](f(x)*g(y))
   */
  UTerm mergeSummation(UTerm expr) {
    expr = transformSubTerms(expr, this::mergeSummation);
    if (expr.kind() != UKind.SUMMATION) return expr;

    final USum summation = (USum) expr;
    if (summation.body().kind() != UKind.MULTIPLY) return expr;

    final Set<UVar> boundedVars = summation.boundedVars();

    // Sum[x](Prod(..,Sum[y](..),..) -> Sum[x,y](..,..,..)
    final List<UTerm> subTerms = summation.body().subTerms();
    for (int i = 0, bound = subTerms.size(); i < bound; i++) {
      final UTerm subTerm = subTerms.get(i);
      if (subTerm.kind() != UKind.SUMMATION) continue;

      final USum subSummation = (USum) subTerm;
      boundedVars.addAll(subSummation.boundedVars());
      subTerms.addAll(subSummation.body().subTerms());
      isModified = true;
    }
    subTerms.removeIf(it -> it.kind() == UKind.SUMMATION);

    return expr;
  }

  /**
   * ||E1 * ||E|| * E2|| -> ||E1 * E * E2|| <p/>
   * not(E1 * ||E|| * E2) -> not(E1 * E * E2)
   */
  UTerm eliminateSquash(UTerm expr) {
    return eliminateSquash0(expr, false);
  }

  private UTerm eliminateSquash0(UTerm expr, boolean isActivated) {
    final UKind kind = expr.kind();
    if (isActivated && kind == UKind.SQUASH) {
      isModified = true;
      return eliminateSquash0(((USquash) expr).body(), true);
    } else {
      final boolean activated;
      if (kind == UKind.PRED) activated = false;
      else activated = isActivated || kind == UKind.SQUASH || kind == UKind.NEGATION;
      return transformSubTerms(expr, t -> eliminateSquash0(t, activated));
    }
  }

  /**
   * not(not(E)) -> E
   */
  UTerm eliminateNegation(UTerm expr) {
    expr = transformSubTerms(expr, this::eliminateNegation);

    final UKind kind = expr.kind();
    if (kind != UKind.NEGATION) return expr;

    final UNeg neg = (UNeg) expr;
    if (neg.body().kind() == UKind.NEGATION) {
      isModified = true;
      return ((UNeg) neg.body()).body();
    }

    return expr;
  }

  /**
   * ||E1|| * ||E2|| -> ||E1 * E2||
   */
  UTerm combineSquash(UTerm expr) {
    expr = transformSubTerms(expr, this::combineSquash);

    final UKind kind = expr.kind();
    if (kind != UKind.MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    final List<UTerm> squashedTerms = new ArrayList<>();
    for (int i = 0, bound = subTerms.size(); i < bound; ++i) {
      final UTerm subTerm = subTerms.get(i);
      if (subTerm.kind() != UKind.SQUASH) continue;
      squashedTerms.add(((USquash) subTerm).body());
    }
    if (squashedTerms.size() > 1) {
      final USquash combinedSquash = USquash.mk(UMul.mk(squashedTerms));
      subTerms.removeIf(t -> t.kind() == UKind.SQUASH);
      subTerms.add(combinedSquash);
      isModified = true;
    }

    return expr;
  }

  /**
   * ||\sum{t1}f(t1)|| * ||\sum{t2}(f(t2)*g(t2))|| -> ||\sum{y2}(f(t2)*g(t2))||
   */
  UTerm removeSummationSquash(UTerm expr) {
    expr = transformSubTerms(expr, this::removeSummationSquash);

    final UKind kind = expr.kind();
    if (kind != UKind.MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    final List<UTerm> squashedSummationTerms = new ArrayList<>();
    final Map<UTerm, Boolean> deletedMap = new HashMap<>();
    for (UTerm subTerm : subTerms) {
      deletedMap.put(subTerm, false);
      if (subTerm.kind() != UKind.SQUASH) continue;
      if (((USquash) subTerm).body().kind() != UKind.SUMMATION) continue;
      squashedSummationTerms.add(subTerm);
    }
    if (squashedSummationTerms.size() > 1) {
      for (int i = 0, bound = squashedSummationTerms.size(); i < bound; ++i) {
        final UTerm firstTerm = squashedSummationTerms.get(i);
        final USum firstSummation = (USum) ((USquash) firstTerm).body();
        if (firstSummation.boundedVars().size() > 1) continue;
        for (int j = i + 1; j < bound; ++j) {
          final UTerm secondTerm = squashedSummationTerms.get(j);
          final USum secondSummation = (USum) ((USquash) secondTerm).body();
          if (secondSummation.boundedVars().size() > 1) continue;
          boolean find = false;
          if (containTargetTerm(firstSummation.body(), secondSummation.body(),
                  (UVar) firstSummation.boundedVars().toArray()[0], (UVar) secondSummation.boundedVars().toArray()[0], false)) {
            deletedMap.put(secondTerm, true);
            isModified = true;
            find = true;
          }
          if (find) continue;
          if (containTargetTerm(firstSummation.body(), secondSummation.body(),
                  (UVar) firstSummation.boundedVars().toArray()[0], (UVar) secondSummation.boundedVars().toArray()[0], true)) {
            deletedMap.put(firstTerm, true);
            isModified = true;
          }
        }
      }
      subTerms.removeIf(deletedMap::get);
    }

    return expr;
  }

  private static boolean containTargetTerm(UTerm firstTerm, UTerm secondTerm, UVar firstBoundedVar, UVar secondBoundedVar, boolean firstIsTarget) {
    UTerm srcTerm = firstIsTarget ? secondTerm : firstTerm;
    UTerm tgtTerm = firstIsTarget ? firstTerm : secondTerm;
    UVar srcBoundedVar = firstIsTarget ? secondBoundedVar : firstBoundedVar;
    UVar tgtBoundedVar = firstIsTarget ? firstBoundedVar : secondBoundedVar;
    if (srcTerm.kind() == UKind.MULTIPLY && tgtTerm.kind() == UKind.MULTIPLY) {
      for (UTerm subTgtTerm : tgtTerm.subTerms()) {
        UTerm subTgtTermCopy = subTgtTerm.copy();
        if (subTgtTerm.isUsing(tgtBoundedVar))
          subTgtTermCopy.replaceVarInplace(tgtBoundedVar, srcBoundedVar, false);
        if (!srcTerm.subTerms().contains(subTgtTermCopy)) {
          return false;
        }
      }
      return true;
    }
    if (srcTerm.kind() == UKind.MULTIPLY && tgtTerm.kind() != UKind.MULTIPLY) {
      UTerm tgtTermCopy = tgtTerm.copy();
      if (tgtTerm.isUsing(tgtBoundedVar))
        tgtTermCopy.replaceVarInplace(tgtBoundedVar, srcBoundedVar, false);
      return srcTerm.subTerms().contains(tgtTermCopy);
    }
    return false;
  }

  /**
   * not(E1) * not(E2) -> not(E1 + E2)
   */
  UTerm combineNegation(UTerm expr) {
    expr = transformSubTerms(expr, this::combineNegation);

    final UKind kind = expr.kind();
    if (kind != UKind.MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    final List<UTerm> negTerms = new ArrayList<>();
    for (final UTerm subTerm : subTerms) {
      if (subTerm.kind() != UKind.NEGATION) continue;
      negTerms.add(((UNeg) subTerm).body());
    }
    if (negTerms.size() > 1) {
      final UNeg combinedNeg = UNeg.mk(UAdd.mk(negTerms));
      subTerms.removeIf(t -> t.kind() == UKind.NEGATION);
      subTerms.add(combinedNeg);
      isModified = true;
    }

    return expr;
  }

  /**
   * E1 * (E2 + E3) -> E1 * E2 + E1 * E3
   */
  UTerm distributeAddToMul(UTerm expr) {
    expr = transformSubTerms(expr, this::distributeAddToMul);

    final UKind kind = expr.kind();
    if (kind != UKind.MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    UAdd subAdd = null;
    for (int i = 0, bound = subTerms.size(); i < bound; ++i) {
      final UTerm subTerm = subTerms.get(i);
      if (subTerm.kind() == UKind.ADD) {
        subAdd = (UAdd) subTerm;
        break;
      }
    }

    // Get one of the subTerm to be ADD
    if (subAdd != null) {
      subTerms.remove(subAdd);
      final List<UTerm> addTerms = new ArrayList<>();
      for (int i = 0, bound = subAdd.subTerms().size(); i < bound; ++i) {
        final UTerm subAddFactor = subAdd.subTerms().get(i);
        final List<UTerm> subMulFactors = UExprSupport.copyTermList(subTerms);
        subMulFactors.add(subAddFactor);
        addTerms.add(UMul.mk(subMulFactors));
      }
      expr = UAdd.mk(addTerms);
      isModified = true;
    }

    return expr;
  }

  /**
   * \sum{t} (f1(t) + f2(t)) -> \sum{t}f1(t) + \sum{t}f2(t) Cases like \sum{t} (E1 * (f1(t) + f2(t))
   * * E2) are transformed into \sum{t} (E1 * E2 * f1(t) + E1 * E2 * f2(t)) by `distributeAddToMul`.
   */
  UTerm distributeAddToSummation(UTerm expr) {
    expr = transformSubTerms(expr, this::distributeAddToSummation);

    final UKind kind = expr.kind();
    if (kind != UKind.SUMMATION) return expr;

    // Check the pattern: \sum(ADD(.., ..))
    final UTerm body = ((USum) expr).body();
    if (body.kind() == UKind.ADD) {
      final List<UTerm> subTerms = body.subTerms();
      final List<UTerm> addFactors = new ArrayList<>();
      for (UTerm term : subTerms) {
        final UTerm subTerm = term.copy();
        final Set<UVar> usedVars = new HashSet<>();
        for (UVar var : ((USum) expr).boundedVars()) {
          if (subTerm.isUsing(var)) usedVars.add(var);
        }
        addFactors.add(USum.mk(usedVars, subTerm));
      }
      expr = UAdd.mk(addFactors);
      isModified = true;
    }

    return expr;
  }

  /**
   * Remove constants in expression
   */
  UTerm removeConstants(UTerm expr) {
    expr = transformSubTerms(expr, this::removeConstants);

    switch (expr.kind()) {
      case ADD -> {
        if (expr.subTerms().removeIf(subTerm -> subTerm.equals(UConst.ZERO))) isModified = true;
        if (expr.subTerms().isEmpty()) {
          isModified = true;
          expr = UConst.zero();
        }
      }
      case MULTIPLY -> {
        if (expr.subTerms().removeIf(subTerm -> subTerm.equals(UConst.ONE))) isModified = true;
        if (expr.subTerms().isEmpty()) {
          isModified = true;
          expr = UConst.one();
        }
        if (any(expr.subTerms(), t -> t.equals(UConst.ZERO))) {
          isModified = true;
          expr = UConst.zero();
        }
      }
      case PRED -> {
        UPred pred = (UPred) expr;
        if (pred.isPredKind(UPred.PredKind.EQ)) {
          assert expr.subTerms().size() == 2;
          List<UTerm> subTerms = expr.subTerms();
          if (subTerms.get(0).kind() == UKind.CONST && subTerms.get(1).kind() == UKind.CONST) {
            UConst firstConst = (UConst) subTerms.get(0);
            UConst secondConst = (UConst) subTerms.get(1);
            if (firstConst.value() == secondConst.value())
              expr = UConst.one();
            else
              expr = UConst.zero();
          }
        }
      }
      case SQUASH, NEGATION -> {
        final UTerm body = ((UUnary) expr).body();
        if (body.kind() == UKind.CONST && ((UConst) body).isZeroOneVal()) {
          isModified = true;
          if (expr.kind() == UKind.SQUASH) expr = body;
          else expr = body.equals(UConst.ONE) ? UConst.zero() : UConst.one();
        }
      }
      case SUMMATION -> {
        final UTerm body = ((USum) expr).body();
        assert body.kind().isBinary();
        if (body.subTerms().size() == 1 && body.subTerms().get(0).equals(UConst.ZERO)) {
          isModified = true;
          expr = UConst.zero();
        }
      }
      default -> {
      }
    }
    return expr;
  }

  /**
   * Transform equal null to IsNull.
   */
  UTerm transformEqNullIsNull(UTerm expr) {
    expr = transformSubTerms(expr, this::transformEqNullIsNull);

    if (expr instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) {
      final UTerm firstArg = pred.args().get(0);
      final UTerm secondArg = pred.args().get(1);

      if (firstArg.equals(UConst.nullVal())) {
        isModified = true;
        return mkIsNullPred(secondArg);
      }

      if (secondArg.equals(UConst.nullVal())) {
        isModified = true;
        return mkIsNullPred(firstArg);
      }
    }


    return expr;
  }

  /**
   * Remove Null Constant in different context (multiply, add, ...).
   * NOTE: In uexpr, we cannot derive that a * null -> null, but we can derive that a + null -> null
   */
  UTerm removeNullConstant(UTerm expr) {
    expr = transformSubTerms(expr, this::removeNullConstant);

    switch (expr.kind()) {
      case SQUASH -> {
        final USquash squash = (USquash) expr;
        if (squash.body().equals(UConst.nullVal())) {
          isModified = true;
          return UConst.nullVal();
        }
      }
      case ADD -> {
        if (any(expr.subTerms(), t -> t.equals(UConst.nullVal()))) {
          isModified = true;
          return UConst.nullVal();
        }
      }
      case MULTIPLY -> {
        // for multiply cases, if all terms are constant, and there is no term equals to 0, return null
        if (any(expr.subTerms(), t -> t.equals(UConst.nullVal()))) {
          if (all(expr.subTerms(), t -> t.kind() == UKind.CONST && !t.equals(UConst.zero()))) {
            return UConst.nullVal();
          }
        }
      }
    }

    return expr;
  }

  /**
   * fold the constants, such as 1+2 -> 3
   */
  UTerm foldConstants(UTerm expr) {
    expr = transformSubTerms(expr, this::foldConstants);
    switch (expr.kind()) {
      case ADD -> {
        List<UTerm> subTerms = expr.subTerms();
        List<UTerm> newSubTerms = new ArrayList<>();
        int count = 0;
        int foldingVal = 0;
        for (UTerm subTerm : subTerms) {
          if (subTerm.equals(UConst.nullVal())) return expr;
          if (subTerm.kind() == UKind.CONST) {
            foldingVal += ((UConst) subTerm).value();
            count++;
          } else
            newSubTerms.add(subTerm);
        }
        if (count <= 1) return expr;
        isModified = true;
        newSubTerms.add(UConst.mk(foldingVal));
        return UAdd.mk(newSubTerms);
      }
      case MULTIPLY -> {
        List<UTerm> subTerms = expr.subTerms();
        List<UTerm> newSubTerms = new ArrayList<>();
        int count = 0;
        int foldingVal = 1;
        for (UTerm subTerm : subTerms) {
          if (subTerm.equals(UConst.nullVal())) return expr;
          if (subTerm.kind() == UKind.CONST) {
            foldingVal *= ((UConst) subTerm).value();
            count++;
          } else
            newSubTerms.add(subTerm);
        }
        if (count <= 1) return expr;
        isModified = true;
        newSubTerms.add(UConst.mk(foldingVal));
        return UMul.mk(newSubTerms);
      }
      default -> {
      }
    }

    return expr;
  }

  /**
   * Remove duplicate factors in multiplication.
   * e.g. [IsNull(a)] * [IsNull(a)] => [IsNull(a)]
   */
  UTerm removeRedundantFactors(UTerm expr) {
    return removeRedundantFactors(expr, false);
  }

  UTerm removeRedundantFactors(UTerm expr, boolean isUnderSet) {
    final boolean isUnderSetFinal;
    if (expr instanceof UPred || expr instanceof UFunc) {
      isUnderSetFinal = false;
    } else if (expr.kind().isUnary()) {
      isUnderSetFinal = true;
    } else {
      isUnderSetFinal = isUnderSet;
    }
    expr = transformSubTerms(expr, t -> removeRedundantFactors(t, isUnderSetFinal));
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final List<UTerm> removableTerms;
    if (isUnderSetFinal) {
      removableTerms = filter(expr.subTerms(), UTerm::returnsNatural);
    } else {
      removableTerms = filter(expr.subTerms(), t -> t.kind() == UKind.PRED
              || t.kind() == UKind.SQUASH
              || t.kind() == UKind.NEGATION);
    }
    final List<UTerm> nonRemovableTerms = filter(expr.subTerms(), t -> !removableTerms.contains(t));

    final Set<UTerm> uniqueSubTermSet = new LinkedHashSet<>(removableTerms);
    final List<UTerm> uniqueSubTerms = new ArrayList<>(uniqueSubTermSet);

    if (uniqueSubTerms.size() != removableTerms.size()) {
      isModified = true;
      return UMul.mk(concat(nonRemovableTerms, uniqueSubTerms));
    }

    return expr;
  }

  /**
   * Remove duplicate same equal.
   * e.g. [a = a] => 1
   */
  UTerm removeRedundantSameEqual(UTerm expr) {
    expr = transformSubTerms(expr, this::removeRedundantSameEqual);
    if (expr.kind() != UKind.PRED || !((UPred) expr).isPredKind(UPred.PredKind.EQ)) return expr;

    final UPred pred = (UPred) expr;

//    if (!pred.args().get(0).kind().isTermAtomic() || !pred.args().get(1).kind().isTermAtomic()) return expr;

    assert pred.args().size() == 2;

    if (pred.args().get(0).equals(pred.args().get(1))) {
      isModified = true;
      return UConst.one();
    }

    return expr;
  }

  /**
   * Apply function to reduce redundant function
   * e.g. upper(lower(a)) -> upper(a)
   */
  UTerm removeRedundantFunction(UTerm expr) {
    expr = transformSubTerms(expr, this::removeRedundantFunction);

    final UKind kind = expr.kind();
    if (kind != UKind.FUNC) return expr;

    final UFunc func = (UFunc) expr;
    final UName funcName = func.funcName();
    final List<UTerm> arguments = func.args();
    if (arguments.size() == 0) return expr;
    if (arguments.get(0).kind() != UKind.FUNC) return expr;

    switch (funcName.toString().toUpperCase()) {
      case "UPPER", "LOWER" -> {
        assert arguments.size() == 1;
        final UFunc subFunc = (UFunc) arguments.get(0);
        if (Objects.equals(subFunc.funcName().toString().toUpperCase(), "UPPER")
                || Objects.equals(subFunc.funcName().toString().toUpperCase(), "LOWER"))
          return UFunc.mk(UFunc.FuncKind.NON_INT, funcName, subFunc.args());
      }
      default -> {
      }
    }
    return expr;
  }

  /**
   * Apply function to transform expr into result of function.
   */
  UTerm applyFunction(UTerm expr) {
    expr = applyFunctionString(expr);
    expr = applyFunctionDivideAndMinus(expr);
    expr = applyFunctionEqualPred(expr);
    return expr;
  }

  /**
   * Apply string function to transform expr into result of function.
   * e.g. upper('exam') -> 'EXAM'.
   */
  UTerm applyFunctionString(UTerm expr) {
    expr = transformSubTerms(expr, this::applyFunctionString);

    final UKind kind = expr.kind();
    if (kind != UKind.FUNC) return expr;

    final UFunc func = (UFunc) expr;
    final UName funcName = func.funcName();
    final List<UTerm> arguments = func.args();
    if (arguments.size() == 0) return expr;
    // all the function argument must be constants (including strings).
    if (any(arguments, t -> t.kind() != UKind.STRING && t.kind() != UKind.CONST)) return expr;

    switch (funcName.toString().toUpperCase()) {
      case "SUBSTRING" -> {
        final UString str = (UString) arguments.get(0);
        if (arguments.size() == 3) {
          final Integer start = ((UConst) arguments.get(1)).value();
          final Integer length = ((UConst) arguments.get(2)).value();
          String value = str.value();
          value = value.substring(start - 1, start + length - 1);
          isModified = true;
          return UString.mk(value);
        } else if (arguments.size() == 2) {
          final Integer start = ((UConst) arguments.get(1)).value();
          String value = str.value();
          value = value.substring(start - 1);
          isModified = true;
          return UString.mk(value);
        }
        assert false; // should not reach here
      }
      case "UPPER" -> {
        assert arguments.size() == 1;
        final UString str = (UString) arguments.get(0);
        final String value = str.value();
        isModified = true;
        return UString.mk(value.toUpperCase());
      }
      case "LOWER" -> {
        assert arguments.size() == 1;
        final UString str = (UString) arguments.get(0);
        final String value = str.value();
        isModified = true;
        return UString.mk(value.toLowerCase());
      }
      case "CONCAT", "||" -> {
        final List<UString> strings = new ArrayList<>();
        all(arguments, t -> strings.add((UString) t));
        isModified = true;
        return UString.mk(strings.stream().map(UString::value).collect(Collectors.joining("")));
      }
      default -> {
      }
    }

    return expr;
  }

  /**
   * Transform divide and minus function.
   * e.g. divide(a, 1) -> a.
   */
  UTerm applyFunctionDivideAndMinus(UTerm expr) {
    expr = transformSubTerms(expr, this::applyFunctionDivideAndMinus);

    final UKind kind = expr.kind();
    // for multiply and addition cases, try our best to eliminate divide and minus
//    switch (kind) {
//      case MULTIPLY -> {
//        final List<UTerm> divideTerms = filter(expr.subTerms(),
//                t -> t instanceof UFunc func && func.funcName().toString().equalsIgnoreCase("divide"));
//        final List<UTerm> notDivideTerms = filter(expr.subTerms(), t -> !divideTerms.contains(t));
//        final List<UTerm> newSubTerms = new ArrayList<>();
//        loop:
//        for (final UTerm divideTerm : divideTerms) {
//          final UFunc function = (UFunc) divideTerm;
//          for (final UTerm notDivideTerm : notDivideTerms) {
//            if (notDivideTerm.equals(function.args().get(1))) {
//              isModified = true;
//              newSubTerms.add(function.args().get(0));
//              notDivideTerms.remove(notDivideTerm);
//              continue loop;
//            }
//          }
//          newSubTerms.add(divideTerm);
//        }
//        if (!notDivideTerms.isEmpty()) newSubTerms.addAll(notDivideTerms);
//        return UMul.mk(newSubTerms);
//      }
//      case ADD -> {
//        final List<UTerm> minusTerms = filter(expr.subTerms(),
//                t -> t instanceof UFunc func && func.funcName().toString().equalsIgnoreCase("minus"));
//        final List<UTerm> notMinusTerms = filter(expr.subTerms(), t -> !minusTerms.contains(t));
//        final List<UTerm> newSubTerms = new ArrayList<>();
//        loop:
//        for (final UTerm minusTerm : minusTerms) {
//          final UFunc function = (UFunc) minusTerm;
//          for (final UTerm notMinusTerm : notMinusTerms) {
//            if (notMinusTerm.equals(function.args().get(1))) {
//              isModified = true;
//              newSubTerms.add(function.args().get(0));
//              notMinusTerms.remove(notMinusTerm);
//              continue loop;
//            }
//          }
//          newSubTerms.add(minusTerm);
//        }
//        if (!notMinusTerms.isEmpty()) newSubTerms.addAll(notMinusTerms);
//        return UAdd.mk(newSubTerms);
//      }
//    }

    if (kind != UKind.FUNC) return expr;

    final UFunc func = (UFunc) expr;
    final UName funcName = func.funcName();
    final List<UTerm> arguments = func.args();

    switch (funcName.toString().toUpperCase()) {
      case "DIVIDE" -> {
        assert arguments.size() == 2;
        if (arguments.get(1).equals(UConst.one())) {
          isModified = true;
          return arguments.get(0);
        }
        if (arguments.get(1).equals(UConst.nullVal())) {
          isModified = true;
          return UConst.nullVal();
        }
      }
      case "MINUS" -> {
        if (arguments.get(1).equals(UConst.nullVal())) {
          isModified = true;
          return UConst.nullVal();
        }
      }
      default -> {}
    }

    return expr;
  }

  /**
   * Apply function in the context of equal predicate to transform expr into result of function.
   * e.g. a = power(expr, 0.5) => a * a = expr.
   */
  UTerm applyFunctionEqualPred(UTerm expr) {
    expr = transformSubTerms(expr, this::applyFunctionEqualPred);

    final UKind kind = expr.kind();
    if (kind != UKind.PRED) return expr;
    final UPred pred = (UPred) expr;

    if (!pred.isPredKind(UPred.PredKind.EQ)) return expr;

    UTerm firstArg = pred.args().get(0);
    UTerm secondArg = pred.args().get(1);
    boolean handling = true;

    interface funcOperation {
      UTerm[] operation(UTerm src, UTerm tgt);
    }

    funcOperation simpleFuncOperation = (src, tgt) -> {
      final UFunc func = (UFunc) src;
      // handle POWER cases
      if (Objects.equals(func.funcName().toString().toUpperCase(), "POWER")) {
        Integer pow;
        boolean flag;
        if (func.args().get(1).kind() == UKind.CONST) {
          pow = ((UConst) func.args().get(1)).value();
          flag = true;
        } else if (func.args().get(1).kind() == UKind.FUNC) {
          double number = Double.parseDouble(((UFunc) func.args().get(1)).funcName().toString());
          if (number >= 1) {
            flag = true;
            pow = Math.toIntExact(Math.round(number));
          } else {
            flag = false;
            pow = Math.toIntExact(Math.round(1 / number));
          }
        } else {
          throw new IllegalArgumentException("[Exception] Unsupported power value");
        }
        if (pow < 0) throw new IllegalArgumentException("[Exception] Unsupported power value");
        final List<UTerm> newMulTerms = new ArrayList<>();
        for (int i = 0; i < pow; i++) {
          if (flag) {
            newMulTerms.add(func.args().get(0).copy());
          } else {
            newMulTerms.add(tgt);
          }
        }
        if (flag) {
          return new UTerm[]{UMul.mk(newMulTerms), tgt};
        } else {
          return new UTerm[]{func.args().get(0), UMul.mk(newMulTerms)};
        }
      }
      return new UTerm[]{src, tgt};
    };

    while (handling) {
      handling = false;
      UTerm[] result = null;
      // for simple function case: [expr = func]
      if (firstArg.kind() == UKind.FUNC) {
        result = simpleFuncOperation.operation(firstArg, secondArg);
        if (!result[0].equals(firstArg) || !result[1].equals(secondArg)) {
          handling = true;
          firstArg = normalizeExpr(result[0]);
          secondArg = normalizeExpr(result[1]);
        }
      }
      if (secondArg.kind() == UKind.FUNC) {
        result = simpleFuncOperation.operation(secondArg, firstArg);
        if (!result[0].equals(secondArg) || !result[1].equals(firstArg)) {
          handling = true;
          secondArg = normalizeExpr(result[0]);
          firstArg = normalizeExpr(result[1]);
        }
      }
      if (handling) {
        isModified = true;
      }
    }

    return UPred.mkBinary(UPred.PredKind.EQ, firstArg, secondArg, pred.nullSafe());
  }

  /**
   * Remove squash if its body is addition for contradict predicates.
   * This rule is for expanding the possibility for normalization due to distributeAddToMul
   * e.g. ||[a = 1] + [a = 'A']|| -> [a = 1] + [a = 'A']
   */
  UTerm removeSquashOfContradictAddition(UTerm expr) {
    expr = transformSubTerms(expr, this::removeSquashOfContradictAddition);
    if (expr.kind() != UKind.SQUASH || ((USquash) expr).body().kind() != UKind.ADD) return expr;

    final UAdd add = (UAdd) ((USquash) expr).body();

    if (any(add.subTerms(), t -> t.kind() != UKind.PRED)) return expr;

    final List<UPred> predicates = new ArrayList<>();
    all(add.subTerms(), t -> predicates.add((UPred) t));

    if (isContractPredicates(predicates)) {
      isModified = true;
      return add;
    }

    return expr;
  }

  /**
   * ||... + a + a + ...|| -> ||... + a + ...||
   * ||... * a * a * ...|| -> ||... * a * ...||
   */
  UTerm removeDuplicatesInSet(UTerm expr) {
    expr = transformSubTerms(expr, this::removeDuplicatesInSet);
    if (!expr.kind().isUnary()) return expr;

    final UTerm body = expr.subTerms().get(0);
    if (!body.kind().isBinary()) return expr;

    final Set<UTerm> elements = new HashSet<>(body.subTerms());
    if (elements.size() == body.subTerms().size()) return expr;

    final UTerm newBody = remakeTerm(body, new ArrayList<>(elements));
    final List<UTerm> list = new ArrayList<>();
    list.add(newBody);
    return remakeTerm(expr, list);
  }

  /**
   * [|| ... || = 1] -> || ... ||
   * [|| ... || = 0] -> not(...)
   */
  UTerm simplifyUnaryTerm(UTerm expr) {
    expr = transformSubTerms(expr, this::simplifyUnaryTerm);
    if (expr.kind() != UKind.PRED) return expr;

    final UPred pred = (UPred) expr;

    switch (pred.predKind()) {
      case EQ -> {
        // check whether it is [squashTerm = constTerm]
        final UTerm squashTerm = linearFind(pred.args(), a -> a.kind() == UKind.SQUASH);
        final UTerm constTerm = linearFind(pred.args(), a -> a.kind() == UKind.CONST);
        if (squashTerm == null || constTerm == null) break;

        // match the case
        if (constTerm.equals(UConst.one())) {
          // [|| ... || = 1] -> || ... ||
          isModified = true;
          return squashTerm;
        } else if (constTerm.equals(UConst.zero())) {
          // [|| ... || = 0] -> not( ... )
          isModified = true;
          return UNeg.mk(((USquash) squashTerm).body());
        }
      }
      default -> {
      }
    }

    return expr;
  }
}
