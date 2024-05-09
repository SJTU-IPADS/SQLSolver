package sqlsolver.superopt.liastar.transformer;

import static sqlsolver.superopt.util.VectorSupport.*;

import java.util.*;

import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.liastar.LiaAndImpl;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.LiaVarImpl;
import sqlsolver.superopt.util.Timeout;
import sqlsolver.superopt.util.Z3Support;

/**
 * Transform plain LIA* formula (without nested stars, parameters and non-linear terms) into an
 * equivalent LIA formula. The LIA* formula should be in this form: g /\ (v,_) in {w|f}*. In case of
 * exceptions, the resulting LIA formula is an over-approximation of the original LIA* formula.
 */
public class LiaTransformer {
  /**
   * Transform plain LIA* formula (without nested stars, parameters and non-linear terms) into an
   * equivalent LIA formula. The LIA* formula should be given in this form: (v, _) in {w | f /\ s}*,
   * where (v, _) and w have the same number of dimensions, f does not contain formulas from
   * expanding stars, and s contains formulas from expanding stars. An external constraint g may
   * accelerate the transformation: if the LIA* formula contradicts g, then this method returns a
   * LIA formula representing "false". Inaccurate partition between f and s may affect proof
   * ability. If transformation into an equivalent LIA formula fails, return an over-approximation.
   *
   * @param g the external constraint
   * @param f the part of constraint that has no formula from expanding stars; <code>null</code>
   *     indicates absence of this part
   * @param s the part of constraint that contains formulas from expanding stars; <code>null</code>
   *     indicates absence of this part
   * @return equivalent or over-approximation of "(v, _) in {w | f /\ s}*" under the constraint g
   *     (i.e. a LIA formula L such that "g /\ L" iff (or if) "g /\ (v, _) in {w | f /\ s}*")
   */
  public LiaStar transform(LiaStar g, List<String> v, List<String> w, LiaStar f, LiaStar s) {
    // non-integer values have corresponding integer representation
    // so replace non-integer vars with integer vars
    g = replaceNonIntegers(g);
    f = replaceNonIntegers(f);
    s = replaceNonIntegers(s);
    // fast routine: detect contradiction
    final LiaStar constraint = LiaStar.mkAnd(true, f, s);
    if (contradicts(g, v, w, constraint)) {
      return LiaStar.mkFalse(g.isInStar());
    }
    // regular routine: compute equivalent LIA
    // transform into equivalent LIA
    try {
      return transformEquivalent(g, v, w, constraint);
    } catch (RuntimeException e) {
      Timeout.bypassTimeout(e);
    }
    // if fails, transform into over-approximation
    return transformOverApprox(g, v, w, f, s);
  }

  private LiaStar replaceNonIntegers(LiaStar formula) {
    if (formula == null) {
      return LiaStar.mkTrue(true);
    }
    return formula.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var && !Value.isIntegralType(var.getValueType())) {
        return LiaStar.mkVar(var.isInStar(), var.getName(), Value.TYPE_INT);
      }
      return f;
    });
  }

  // whether "g /\ (v, _) in {w | f}*" is UNSAT
  private boolean contradicts(LiaStar g, List<String> v, List<String> w, LiaStar f) {
    // "g(v) /\ (v, _) in {(w1, w2) | f(w1, w2)}*" is UNSAT
    // if all of these are true:
    // (1) g(0) is UNSAT
    // (2) forall w1. (exists w2. f(w1, w2)) -> g(w1) is UNSAT
    // (3) forall x, y. g(x) is UNSAT /\ g(y) is UNSAT -> g(x + y) is UNSAT

    // Condition 1: "not g(0)" is valid (i.e. always true)
    final LiaStar cond1 = LiaStar.mkNot(false, apply(g, v, constToLia(constZero(v.size()))));
    if (!Z3Support.isValidLia(cond1)) {
      return false;
    }

    // Condition 2: forall w1,w2,x. f(w1, w2) -> not g(w1)
    // x are the variables in g besides w1
    final int vSize = v.size();
    final List<String> w1 = w.subList(0, vSize);
    final LiaStar cond2Csq = LiaStar.mkNot(false, apply(g, v, nameToLia(w1)));
    final LiaStar cond2 = LiaStar.mkImplies(false, f, cond2Csq);
    if (!Z3Support.isValidLia(cond2)) {
      return false;
    }

    // Condition 3: forall x,y,z3. exists z1,z2.
    //   g(x) \/ g(y) \/ not g(x + y)
    // z1,z2,z3 are vars in the 1st/2nd/3rd occurrence of g besides args (x/y/x+y)
    // TODO: (assumption) g does not contain var names like "contra_..."
    final List<String> x = createVarVector("contra_x", 0, vSize);
    final List<String> y = createVarVector("contra_y", 0, vSize);
    // rename vars in g to z1,z2,z3
    final Set<String> usedVarNames = g.collectVarNames();
    final Map<String, String> renameMap1 = new HashMap<>();
    final Map<String, String> renameMap2 = new HashMap<>();
    final Map<String, String> renameMap3 = new HashMap<>();
    final LiaStar g1 = g.deepcopy().transformPostOrder(fm -> renameVarsBesides(fm, v, renameMap1, usedVarNames));
    final LiaStar g2 = g.deepcopy().transformPostOrder(fm -> renameVarsBesides(fm, v, renameMap2, usedVarNames));
    final LiaStar g3 = g.deepcopy().transformPostOrder(fm -> renameVarsBesides(fm, v, renameMap3, usedVarNames));
    // construct condition 3
    final List<LiaStar> cond3Parts = new ArrayList<>();
    cond3Parts.add(apply(g1, v, nameToLia(x)));
    cond3Parts.add(apply(g2, v, nameToLia(y)));
    cond3Parts.add(LiaStar.mkNot(false, apply(g3, v, plus(nameToLia(x), nameToLia(y)))));
    final LiaStar cond3 = LiaStar.mkDisjunction(false, cond3Parts);
    return Z3Support.isValidLia(cond3, cond3Parts.get(2).collectVarNames());
  }

  // rename vars in f except those in "besides"
  // "used" records used var names so vars are not renamed to them
  private LiaStar renameVarsBesides(LiaStar f, Collection<String> besides, Map<String, String> renameMap, Collection<String> used) {
    if (f instanceof LiaVarImpl var) {
      final String oldName = var.getName();
      // exclude vars in "besides"
      if (besides.contains(oldName)) {
        return f;
      }
      // find existing mapping and rename
      final String renamedVar = renameMap.get(oldName);
      if (renamedVar != null) return LiaStar.mkVar(f.isInStar(), renamedVar);
      // find an unused name
      int count = 1;
      String newName;
      do {
        newName = oldName + "_" + count++;
      } while (used.contains(newName));
      // update mapping and rename
      renameMap.put(oldName, newName);
      used.add(newName);
      return LiaStar.mkVar(f.isInStar(), newName);
    }
    return f;
  }

  private LiaStar transformEquivalent(LiaStar g, List<String> v, List<String> w, LiaStar f) {
    return transformSls(g, v, w, f);
  }

  private LiaStar transformOverApprox(
          LiaStar g, List<String> v, List<String> w, LiaStar f, LiaStar s) {
    // make implication from f /\ s
    LiaStar overApprox = makeImplications(v, w, LiaStar.mkAnd(true, f, s));
    // transform part of f /\ s into SLS then LIA
    if (s != null) {
      // transform f into SLS then LIA
      try {
        overApprox = LiaStar.mkAnd(true, overApprox, transformSls(g, v, w, f));
      } catch (RuntimeException e) {
        Timeout.bypassTimeout(e);
        // if fails, transform only part of f related to v into SLS then LIA
        try {
          overApprox =
              LiaStar.mkAnd(
                  true,
                  overApprox,
                  transformSls(g, v, w, removeUnrelatedTerms(f, w.subList(0, v.size()))));
        } catch (RuntimeException e1) {
          Timeout.bypassTimeout(e1);
        }
      }
    } else {
      // when s is absent, transforming f is redundant
      // transform only part of f related to v
      try {
        overApprox =
            LiaStar.mkAnd(
                true,
                overApprox,
                transformSls(g, v, w, removeUnrelatedTerms(f, w.subList(0, v.size()))));
      } catch (RuntimeException e) {
        Timeout.bypassTimeout(e);
      }
    }
    return overApprox;
  }

  private LiaStar removeUnrelatedTerms(LiaStar f, List<String> vars) {
    // destruct f into terms connected by AND
    final List<LiaStar> terms = new ArrayList<>();
    if (f instanceof LiaAndImpl and) {
      and.flatten(terms);
    } else {
      terms.add(f);
    }
    // only collect related terms
    final Set<String> varSet = new HashSet<>(vars);
    LiaStar result = LiaStar.mkTrue(true);
    for (final LiaStar term : terms) {
      if (SetSupport.intersects(term.collectVarNames(), varSet)) {
        result = LiaStar.mkAnd(true, result, term);
      }
    }
    return result;
  }

  // compute SLS for "(v, _) in {w | f}*" under constraint g
  private LiaStar transformSls(LiaStar g, List<String> v, List<String> w, LiaStar f) {
    // SLS of the under-approximation
    final List<String> slsVector = w.subList(0, v.size());
    final SlsAugmenter underApprox = new SlsAugmenter(slsVector, f);
    // Maintain under-approx u and (optional) over-approx o
    // Repeat:
    // 1. weaken u
    // 2. (optional; do not do this when u has been SAT) strengthen o
    // until g/\o becomes UNSAT (return 1 = 0)
    // or u becomes over-approximation (return u)
    while (true) {
      // try to weaken u
      final boolean b = underApprox.augment();
      if (!b) {
        // no augmentation is found
        // u has become over-approximation
        final LiaStar underLia = underApprox.sls().toStarLia(slsVector);
        return apply(underLia, slsVector, nameToLia(v));
      }
      // TODO: (optional) strengthen over-approximation o
      //  if g/\o is UNSAT, return "1 = 0"
    }
  }

  // Given "(v,_) in {w|f}*",
  // it tries to compute its over-approximation by making implications.
  private LiaStar makeImplications(List<String> v, List<String> w, LiaStar f) {
    // generate over-approximation of "(v,_) in {w|f}*"
    // i.e. make implications from that LIA* formula
    final LiaStar conclusionVarEq = implyVarEq(v, w, f);
    final LiaStar conclusionNonZeroImplication = implyZeroImplication(v, w, f);
    final LiaStar conclusionVarNonNegative = implyVarNonNegative(v, w, f);
    return LiaStar.mkAnd(false,
            LiaStar.mkAnd(false, conclusionVarEq, conclusionNonZeroImplication),
            conclusionVarNonNegative);
  }

  // make implications like "v1 = v2" from f
  private LiaStar implyVarEq(List<String> v, List<String> w, LiaStar f) {
    LiaStar result = LiaStar.mkTrue(false);
    for (int i = 0; i < v.size(); i++) {
      final String outVar1 = v.get(i);
      final String inVar1 = w.get(i);
      for (int j = i + 1; j < v.size(); j++) {
        final String outVar2 = v.get(j);
        final String inVar2 = w.get(j);
        // equivalence among dimensions of w implies
        // equivalence among dimensions of v
        try {
          if (impliesVarEq(f, inVar1, inVar2)) {
            final LiaStar outEq =
                LiaStar.mkEq(false, LiaStar.mkVar(false, outVar1), LiaStar.mkVar(false, outVar2));
            result = LiaStar.mkAnd(false, result, outEq);
          }
        } catch (RuntimeException e) {
          Timeout.bypassTimeout(e);
        }
      }
    }
    return result;
  }

  // whether f implies v1 = v2
  private boolean impliesVarEq(LiaStar f, String v1, String v2) {
    final LiaStar v1EqV2 = LiaStar.mkEq(false, LiaStar.mkVar(false, v1), LiaStar.mkVar(false, v2));
    final LiaStar toCheck = LiaStar.mkImplies(false, f, v1EqV2);
    return Z3Support.isValidLia(toCheck);
  }

  // make implications like "v1 = 0 -> v2 = 0" from f
  // some vars are always zero without conditions
  private LiaStar implyZeroImplication(List<String> v, List<String> w, LiaStar f) {
    LiaStar result = LiaStar.mkTrue(false);
    for (int i = 0; i < v.size(); i++) {
      final String outVar1 = v.get(i);
      final String inVar1 = w.get(i);
      // find if inVar1 is always 0
      // if so, outVar1 is also always 0
      if (impliesZero(f, inVar1)) {
        final LiaStar isZero =
                LiaStar.mkEq(false, LiaStar.mkVar(false, outVar1), LiaStar.mkConst(false, 0));
        result = LiaStar.mkAnd(false, result, isZero);
        continue;
      }
      // find if inVar1 is 0 under certain conditions
      for (int j = 0; j < v.size(); j++) {
        if (i == j) {
          continue;
        }
        final String outVar2 = v.get(j);
        final String inVar2 = w.get(j);
        // For each pair of dimensions w1 and w2 in w,
        // if w1 = 0 -> w2 = 0 and w1 is always non-negative,
        // then for its corresponding dimensions v1 and v2 in v,
        // we have v1 = 0 -> v2 = 0.
        try {
          if (impliesNonNegative(inVar1, f) && impliesZeroImplication(f, inVar1, inVar2)) {
            final LiaStar nonZeroW1 =
                LiaStar.mkEq(false, LiaStar.mkVar(false, outVar1), LiaStar.mkConst(false, 0));
            final LiaStar nonZeroW2 =
                LiaStar.mkEq(false, LiaStar.mkVar(false, outVar2), LiaStar.mkConst(false, 0));
            final LiaStar nonZeroImplication = LiaStar.mkImplies(false, nonZeroW1, nonZeroW2);
            result = LiaStar.mkAnd(false, result, nonZeroImplication);
          }
        } catch (RuntimeException e) {
          Timeout.bypassTimeout(e);
        }
      }
    }
    return result;
  }

  // whether f implies "v = 0"
  private boolean impliesZero(LiaStar f, String v) {
    final LiaStar isZero =
            LiaStar.mkEq(false, LiaStar.mkVar(false, v), LiaStar.mkConst(false, 0));
    final LiaStar toCheck = LiaStar.mkImplies(false, f, isZero);
    return Z3Support.isValidLia(toCheck);
  }

  // whether f implies "v1 = 0 -> v2 = 0"
  private boolean impliesZeroImplication(LiaStar f, String v1, String v2) {
    final LiaStar isZeroV1 =
            LiaStar.mkEq(false, LiaStar.mkVar(false, v1), LiaStar.mkConst(false, 0));
    final LiaStar isZeroV2 =
            LiaStar.mkEq(false, LiaStar.mkVar(false, v2), LiaStar.mkConst(false, 0));
    final LiaStar zeroImplication = LiaStar.mkImplies(false, isZeroV1, isZeroV2);
    final LiaStar toCheck = LiaStar.mkImplies(false, f, zeroImplication);
    return Z3Support.isValidLia(toCheck);
  }

  private LiaStar implyVarNonNegative(List<String> v, List<String> w, LiaStar f) {
    LiaStar result = LiaStar.mkTrue(false);
    for (int i = 0; i < v.size(); i++) {
      final String outVar = v.get(i);
      final String inVar = w.get(i);
      // if "f -> inVar >= 0", then "outVar >= 0"
      if (impliesNonNegative(inVar, f)) {
        final LiaStar outVarNN = LiaStar.mkLe(false, LiaStar.mkConst(false, 0), LiaStar.mkVar(false, outVar));
        result = LiaStar.mkAnd(false, result, outVarNN);
      }
    }
    return result;
  }

  // whether f -> v >= 0
  private boolean impliesNonNegative(String v, LiaStar f) {
    final LiaStar varNN = LiaStar.mkLe(false, LiaStar.mkConst(false, 0), LiaStar.mkVar(false, v));
    final LiaStar toCheck = LiaStar.mkImplies(false, f, varNN);
    return Z3Support.isValidLia(toCheck);
  }
}
