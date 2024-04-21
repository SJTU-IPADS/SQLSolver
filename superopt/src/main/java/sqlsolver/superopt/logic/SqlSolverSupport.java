package sqlsolver.superopt.logic;

import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.superopt.uexpr.*;

import java.util.*;

import static sqlsolver.common.utils.SetSupport.filter;

public class SqlSolverSupport {
  public static List<Set<UVar>> buildEqRelation(Set<Pair<UVar, UVar>> eqVarPairs) {
    List<Set<UVar>> eqRels = new ArrayList<>();
    for (Pair<UVar, UVar> pair : eqVarPairs) {
      UVar v1 = pair.getLeft();
      UVar v2 = pair.getRight();
      Set<UVar> targetSet = null;
      for (int i = 0; i < eqRels.size(); ++i) {
        Set<UVar> eqRel = eqRels.get(i);
        if (eqRel.contains(v1) || eqRel.contains(v2)) {
          if (targetSet == null) {
            targetSet = eqRel;
          } else {
            targetSet.addAll(eqRel);
            eqRel.clear();
          }
          targetSet.add(v1.copy());
          targetSet.add(v2.copy());
        }
      }
      if (targetSet == null) {
        Set<UVar> eqRel = new HashSet<>();
        eqRel.add(v1.copy());
        eqRel.add(v2.copy());
        eqRels.add(eqRel);
      }
    }

    List<Set<UVar>> result = new ArrayList<>();
    for (Set<UVar> s : eqRels) {
      if (!s.isEmpty()) {
        result.add(s);
      }
    }
    return result;
  }

  public static boolean hasFreeTuple(UVar v, Set<UVar> freeTuples) {
    switch (v.kind()) {
      case PROJ -> {
        for (UVar arg : v.args()) {
          if (hasFreeTuple(arg, freeTuples)) {
            return true;
          }
        }
        return false;
      }
      case BASE -> {
        return freeTuples.contains(v);
      }
      default -> {
        return false;
      }
    }
  }
}
