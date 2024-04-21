package sqlsolver.superopt.util;

import sqlsolver.superopt.uexpr.UTerm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilities for U-expressions like UAdd and UMul.
 */
public class CommAssocUexpUtils {

  // lefts and rights are the matching pair of groups
  private static boolean matchGroups(List<UTerm> lefts, List<UTerm> rights, SetMatching<String> matching) {
    if (lefts.size() == 1) {
      // precise (single) match
      return lefts.get(0).groupSimilarVariables(rights.get(0), matching);
    } else {
      // group match
      Set<String> ls = new HashSet<>();
      for (UTerm item : lefts) {
        ls.addAll(item.getFVs());
      }
      Set<String> rs = new HashSet<>();
      for (UTerm item : rights) {
        rs.addAll(item.getFVs());
      }
      return matching.match(ls, rs);
    }
  }

  public static boolean groupSimilarVariables(List<UTerm> factors, List<UTerm> thatFactors, SetMatching<String> matching) {
    int size = factors.size();
    if (size != thatFactors.size()) return false;
    // factors with the same structure hash are grouped together
    List<UTerm> lefts = new ArrayList<>(), rights = new ArrayList<>();
    int lastHash = -1;
    for (int i = 0; i < size; i++) {
      UTerm left = factors.get(i);
      UTerm right = thatFactors.get(i);
      int lHash = left.hashForSort();
      int rHash = left.hashForSort();
      if (lHash != rHash) return false;
      if (i > 0 && lastHash != lHash) {
        // match between the current groups
        if (!matchGroups(lefts, rights, matching)) return false;
        // a new group
        lefts = new ArrayList<>();
        rights = new ArrayList<>();
      }
      // add the new factors into the groups
      lefts.add(left);
      rights.add(right);
      lastHash = lHash;
    }
    // match the last pair of groups
    return matchGroups(lefts, rights, matching);
  }

}
