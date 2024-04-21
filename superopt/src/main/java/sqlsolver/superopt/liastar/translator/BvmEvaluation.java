package sqlsolver.superopt.liastar.translator;

import static sqlsolver.superopt.liastar.translator.LiaStarTranslatorSupport.*;

import java.util.*;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.superopt.uexpr.UName;
import sqlsolver.superopt.uexpr.USum;
import sqlsolver.superopt.uexpr.UTerm;
import sqlsolver.superopt.uexpr.UVar;
import sqlsolver.superopt.util.Bag;

/** Utilities for evaluating BVM (i.e. see how good a BVM is). */
public class BvmEvaluation {
  /**
   * Given two U-expressions, see how similar they are under a specific BVM. The score is the
   * "distance" between both U-expression trees under the same BVM. The lower this score is, the
   * more similar they are.
   *
   * @param bvm the BVM to use
   * @param uexp1 the first U-expression
   * @param uexp2 the second U-expression
   * @return the similarity score
   */
  public static int similarityScore(BVM bvm, UTerm uexp1, UTerm uexp2) {
    // TODO: maybe it can be polished further
    // apply the BVM
    final List<UVar> commonVars = new ArrayList<>();
    for (int i = 0, bound = bvm.matching.size(); i < bound; i++) {
      commonVars.add(UVar.mkBase(UName.mk("b" + i)));
    }
    uexp1 = apply(bvm, uexp1, commonVars);
    uexp2 = apply(bvm, uexp2, commonVars);
    // calculate difference
    return greedyDiffBetweenSums(findTopLevelSums(uexp1), findTopLevelSums(uexp2));
  }

  // difference between t1 and t2 with regard to v
  // i.e. compare their subterms that contain v
  public static int diffBetweenVarOccurrencesInTerms(UVar v, UTerm t1, UTerm t2) {
    t1 = getOutermostMultiArgOrAtomTerm(t1);
    t2 = getOutermostMultiArgOrAtomTerm(t2);
    Set<UTerm> set1 = collectRelatedSubTermSet(t1, v);
    Set<UTerm> set2 = collectRelatedSubTermSet(t2, v);
    Bag<Integer> hashes1 = mapSubTermsToHashes(t1);
    Bag<Integer> hashes2 = mapSubTermsToHashes(t2);
    int overlap = SetSupport.intersect(set1, set2).size();
    int diffHash1 = Bag.minus(hashes1, hashes2).size();
    int diffHash2 = Bag.minus(hashes2, hashes1).size();
    return diffHash1 + diffHash2 - overlap;
  }

  private static int diffOfHangingSum(UTerm t) {
    t = getOutermostMultiArgOrAtomTerm(t);
    Bag<Integer> hashes = mapSubTermsToHashes(t);
    return hashes.size();
  }

  // out-of-place replacement (i.e. does not change the original uexp)
  // replace vars in uexp with commonVars according to bvm
  private static UTerm apply(BVM bvm, UTerm uexp, List<UVar> commonVars) {
    int i = 0;
    for (Set<UVar> layer : bvm.matching) {
      UVar commonVar = commonVars.get(i);
      for (UVar bv : layer) {
        // out-of-place replacement
        // replace body & bound var
        uexp = uexp.replaceVar(bv, commonVar, true);
      }
      i++;
    }
    return uexp;
  }

  private static Bag<Integer> mapSubTermsToHashes(UTerm term) {
    Bag<Integer> hashes = new Bag<>();
    for (UTerm subTerm : term.subTerms()) {
      hashes.add(subTerm.hashForSort());
    }
    return hashes;
  }

  // calculate diff between two groups of summations using greedy algorithm
  private static int greedyDiffBetweenSums(List<USum> sums1, List<USum> sums2) {
    int result = 0;
    sums2 = new ArrayList<>(sums2);
    for (USum sum1 : sums1) {
      // for each summation in sums1
      // select its matching summation in sums2 in a greedy manner
      if (sums2.isEmpty()) {
        // sums1 have more sums than sums2
        // compute diff for each remaining sum in sums1
        result += sums1.stream().map(BvmEvaluation::diffOfHangingSum).reduce(Integer::sum).get();
        return result;
      }
      int minDiff = Integer.MAX_VALUE;
      USum matchingSum = null;
      for (USum sum2 : sums2) {
        int diff = diffBetweenSum(sum1, sum2);
        if (diff < minDiff) {
          minDiff = diff;
          matchingSum = sum2;
        }
      }
      assert matchingSum != null;
      sums2.remove(matchingSum);
      // the total diff is the sum of diff between matching summations
      result += minDiff;
    }
    if (!sums2.isEmpty()) {
      // sums2 have more sums than sums1
      // compute diff for each remaining sum in sums2
      result += sums2.stream().map(BvmEvaluation::diffOfHangingSum).reduce(Integer::sum).get();
    }
    return result;
  }

  // calculate diff between a pair of summations
  private static int diffBetweenSum(USum sum1, USum sum2) {
    assert !sum1.boundedVars().isEmpty();
    return sum1.boundedVars().stream()
        .map(v -> diffBetweenVarOccurrencesInTerms(v, sum1, sum2))
        .reduce(Integer::sum)
        .get();
  }
}
