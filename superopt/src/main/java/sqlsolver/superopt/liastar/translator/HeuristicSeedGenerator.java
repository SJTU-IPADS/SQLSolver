package sqlsolver.superopt.liastar.translator;

import static sqlsolver.superopt.liastar.translator.LiaStarTranslatorSupport.*;

import java.util.*;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.superopt.uexpr.USum;
import sqlsolver.superopt.uexpr.UTerm;
import sqlsolver.superopt.uexpr.UVar;

public class HeuristicSeedGenerator {
  public BVM generateSeed(UTerm uexp1, UTerm uexp2, UVar outVar) {
    // preprocess input
    uexp1 = uexp1.copy();
    uexp2 = uexp2.copy();
    List<USum> sums1 = findTopLevelSums(uexp1);
    List<USum> sums2 = findTopLevelSums(uexp2);
    List<UTerm> sums = new ArrayList<>(sums1);
    sums.addAll(sums2);
    Set<Integer> sumIndexSet1 = new HashSet<>();
    Set<Integer> sumIndexSet2 = new HashSet<>();
    appendIntsInRange(sumIndexSet1, 0, sums1.size());
    appendIntsInRange(sumIndexSet2, sums1.size(), sums.size());
    // generate BVM layers
    List<Set<UVar>> result = new ArrayList<>();
    List<UTerm> subSums = new ArrayList<>();
    Set<Integer> subSumIndexSet1 = new HashSet<>();
    Set<Integer> subSumIndexSet2 = new HashSet<>();
    while (!sums.isEmpty()) {
      // generate a BVM layer for current summations "sums"
      result.add(
          generateOneLayer(
              sums, sumIndexSet1, sumIndexSet2, outVar, subSums, subSumIndexSet1, subSumIndexSet2));
      // for next iteration
      sums = subSums;
      sumIndexSet1 = subSumIndexSet1;
      sumIndexSet2 = subSumIndexSet2;
      subSums = new ArrayList<>();
      subSumIndexSet1 = new HashSet<>();
      subSumIndexSet2 = new HashSet<>();
    }
    return new BVM(result);
  }

  private Set<UVar> generateOneLayer(
      List<UTerm> sums,
      Set<Integer> sumIndexSet1,
      Set<Integer> sumIndexSet2,
      UVar outVar,
      List<UTerm> subSums,
      Set<Integer> subSumIndexSet1,
      Set<Integer> subSumIndexSet2) {
    // compute a matching at the current layer
    NameSequence boundVarName = NameSequence.mkIndexed("b", 0);
    final HeuristicBoundVarMatcher matcher = new HeuristicBoundVarMatcher(boundVarName, outVar);
    Set<UVar> result =
        matcher.injectCommonTuple(sums, sumIndexSet1, sumIndexSet2).replacedBoundVars();

    // find summations at the next layer
    final List<USum> subSums1 = new ArrayList<>(), subSums2 = new ArrayList<>();
    for (int i = 0; i < sums.size(); ++i) {
      // For each term, find top-level summations within it
      List<USum> tmpSubSums = findTopLevelSums(sums.get(i));
      // collect those sub-summations & classify them according to on which side they are
      if (sumIndexSet1.contains(i)) subSums1.addAll(tmpSubSums);
      else if (sumIndexSet2.contains(i)) subSums2.addAll(tmpSubSums);
      else assert false;
    }
    subSums.addAll(subSums1);
    subSums.addAll(subSums2);
    appendIntsInRange(subSumIndexSet1, 0, subSums1.size());
    appendIntsInRange(subSumIndexSet2, subSums1.size(), subSums.size());
    return result;
  }
}
