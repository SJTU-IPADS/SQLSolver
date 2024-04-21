package sqlsolver.superopt.liastar.translator;

import java.util.*;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.uexpr.*;

import static sqlsolver.superopt.uexpr.PredefinedFunctions.DIVIDE;

public class LiaStarTranslatorSupport {
  public static LiaStar translateFunc(boolean innerStar, String funcName, List<LiaStar> liaOps) {
    final int arity = liaOps.size();
    if (DIVIDE.contains(funcName, arity)) {
      // division has a dedicated LIA expression type
      return LiaStar.mkDiv(innerStar, liaOps.get(0), liaOps.get(1));
    }
    return LiaStar.mkFunc(innerStar, funcName, liaOps);
  }

  /** Traverse down all possible paths till the end of path or a summation. */
  public static List<USum> findTopLevelSums(UTerm uexp) {
    final List<USum> list = new ArrayList<>();
    if (uexp instanceof USum sum) {
      list.add(sum);
      return list;
    }
    for (UTerm sub : uexp.subTerms()) {
      list.addAll(findTopLevelSums(sub));
    }
    return list;
  }

  /**
   * Given a list of terms ("list") and two sets ("indices1" and "indices2") that include references
   * (integers) to those terms, replace summations in the terms with lia vars, output the mapping to
   * two lists ("subSums" and "subVars") and two new sets ("subSumIndices1" and "subSumIndices2") so
   * that each summation that occurs in a term referenced by "indicesN" is in "subSumIndicesN".
   */
  public static void replaceSumsInList(
      List<UTerm> list, List<UTerm> subSums, List<String> subVars, NameSequence liaVarName) {
    Map<USum, String> sumMap = new HashMap<>();
    list.replaceAll(uexp -> replaceSummations(sumMap, new HashSet<>(), uexp, liaVarName));
    // convert map into two lists (keys and values)
    for (Map.Entry<USum, String> entry : sumMap.entrySet()) {
      subSums.add(entry.getKey());
      subVars.add(entry.getValue());
    }
  }

  public static UTerm replaceSummations(
      Map<USum, String> sumMap, Set<USum> sums, UTerm uexp, NameSequence liaVarName) {
    switch (uexp.kind()) {
      case SUMMATION:
        sums.add((USum) uexp);
        if (sumMap.containsKey(uexp)) return ULiaVar.mk(sumMap.get(uexp));
        final String sumName = liaVarName.next();
        final ULiaVar newVar = ULiaVar.mk(sumName);
        sumMap.put((USum) uexp, sumName);
        // TODO: summations may be non-integral
        return newVar;
      case MULTIPLY, ADD, PRED, FUNC:
        {
          final List<UTerm> subTerms = uexp.subTerms();
          final List<UTerm> newSubTerms = new ArrayList<>();
          for (UTerm t : subTerms) {
            newSubTerms.add(replaceSummations(sumMap, sums, t, liaVarName));
          }
          return switch (uexp.kind()) {
            case MULTIPLY -> UMul.mk(newSubTerms);
            case ADD -> UAdd.mk(newSubTerms);
            case FUNC -> UFunc.mk(
                ((UFunc) uexp).funcKind(), ((UFunc) uexp).funcName(), newSubTerms);
            default -> UPred.mk(
                ((UPred) uexp).predKind(), ((UPred) uexp).predName(), newSubTerms, false);
          };
        }
      case NEGATION:
        return UNeg.mk(replaceSummations(sumMap, sums, ((UNeg) uexp).body(), liaVarName));
      case SQUASH:
        return USquash.mk(replaceSummations(sumMap, sums, ((USquash) uexp).body(), liaVarName));
      default:
        return uexp;
    }
  }

  /** Copy terms from a list to another. */
  public static void copyUExprList(List<UTerm> src, List<UTerm> dst) {
    for (UTerm sum : src) {
      dst.add(sum.copy());
    }
  }

  /**
   * Append integers within a specific range into a collection.
   *
   * @param collection the collection to append
   * @param start the lower bound of range (inclusive)
   * @param end the upper bound of range (exclusive)
   */
  public static void appendIntsInRange(Collection<Integer> collection, int start, int end) {
    for (int i = start; i < end; i++) {
      collection.add(i);
    }
  }

  public static UTerm getOutermostMultiArgOrAtomTerm(UTerm term) {
    int argCount = term.subTerms().size();
    if (argCount > 1 || argCount == 0) return term;
    return getOutermostMultiArgOrAtomTerm(term.subTerms().get(0));
  }

  public static Set<UTerm> collectRelatedSubTermSet(UTerm term, UVar v) {
    Set<UTerm> terms = new HashSet<>();
    collectRelatedSubTerms(term, v, terms);
    return terms;
  }

  private static void collectRelatedSubTerms(UTerm term, UVar v, Collection<UTerm> collection) {
    for (UTerm subTerm : term.subTerms()) {
      if (subTerm.isUsing(v)) {
        collection.add(subTerm);
      }
    }
  }
}
