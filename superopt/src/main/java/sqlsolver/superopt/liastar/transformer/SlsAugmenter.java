package sqlsolver.superopt.liastar.transformer;

import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.superopt.util.VectorSupport.*;
import static sqlsolver.superopt.util.Z3Support.*;

import java.util.*;

import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.liastar.LiaStar;

/**
 * An augmenter starts from an empty semi-linear set (SLS) and augments that SLS towards the
 * specified target.
 */
// TODO: coefficient var names have prefix "sls_lambda_" or "sls_lambda1/2/3_";
//  this requires those names are not used in the target LIA formula
public class SlsAugmenter {
  public static final String MSG_SLS_UNKNOWN = "the corresponding SLS is unknown";

  private final SemiLinearSet sls;
  // index of the newly-created/updated linear set in sls, or -1
  private int updatedLSIndex;
  // the name of vector dimensions; useful to check satisfiability
  private final List<String> slsVector;
  // the target to approach
  private final LiaStar target;
  private final AugmentationFinder finder;

  /**
   * Create an augmenter with a target and a vector of variables that corresponds to dimensions of
   * the SLS.
   *
   * @param slsVector the vector of vars corresponding to SLS dimensions
   * @param target the target
   */
  public SlsAugmenter(List<String> slsVector, LiaStar target) {
    sls = new SemiLinearSet();
    updatedLSIndex = -1;
    this.slsVector = slsVector;
    this.target = target;
    finder = new AugmentationFinder(sls, slsVector, target);
  }

  /**
   * Find a vector that augments the SLS towards target (i.e. a vector outside SLS but satisfying
   * the target) and add it to the SLS. Return false if there is no such vector.
   */
  public boolean augment() {
    // too large SLS
    if (sls.largestLinearSetSize() > slsVector.size()){
      reportUnknownSls();
    }
    // try to find an augmentation vector
    final List<Long> augmentation = finder.find();
    if (augmentation == null) {
      return false;
    }
    // augment SLS with the vector found
    sls.add(new LinearSet(augmentation, new ArrayList<>()));
    updatedLSIndex = sls.size() - 1;
    saturate();
    return true;
  }

  public SemiLinearSet sls() {
    return sls;
  }

  private void reportUnknownSls() {
    throw new RuntimeException(MSG_SLS_UNKNOWN);
  }

  private void saturate() {
    // repeat these operations until convergence
    while (merge() || shiftDown() || offsetDown()) {}
    // convergence
    updatedLSIndex = -1;
  }

  // try to perform a Merge (merge two LS into one)
  // return false if no operation is performed
  private boolean merge() {
    // take the updated LS
    final int index1 = updatedLSIndex;
    // traverse each possible pair of linear sets
    for (int index2 = 0, bound = sls.size(); index2 < bound; index2++) {
      // skip the same LS
      if (index1 == index2) {
        continue;
      }
      if (mergeInto(index1, index2) || mergeInto(index2, index1)) {
        return true;
      }
    }
    return false;
  }

  // try to merge LS_index1 into LS_index2
  private boolean mergeInto(int index1, int index2) {
    // for the linear set (shift1, offsets1) and (shift2, offsets2)
    final LinearSet ls1 = sls.get(index1);
    final List<Long> shift1 = ls1.getShift();
    final LinearSet ls2 = sls.get(index2);
    final List<Long> shift2 = ls2.getShift();
    // if shift2 <= shift1
    if (!constLeAbs(shift2, shift1)) {
      return false;
    }
    // check validity of "forall lambda1, lambda2, lambda3.
    // target(shift2 + lambda1 * offsets1
    // + lambda2 * offsets2
    // + lambda3 * (shift1 - shift2))"
    // where "lambda * offsets" means a linear combination of offsets
    // and lambda is the vector of coefficients
    final List<List<Long>> offsets1 = ls1.getOffsets();
    final List<List<Long>> offsets2 = ls2.getOffsets();
    // construct the formula
    List<LiaStar> argVector = constToLia(shift2);
    final List<String> lambda1 = createVarVector("sls_lambda1", 0, offsets1.size());
    final List<String> lambda2 = createVarVector("sls_lambda2", 0, offsets2.size());
    final String lambda3Name = "sls_lambda3";
    final LiaStar lambda3 = LiaStar.mkVar(false, lambda3Name, Value.TYPE_NAT);
    final List<LiaStar> linearComb1 =
        linearCombination(nameToLia(lambda1, Value.TYPE_NAT), offsets1, argVector.size());
    final List<LiaStar> linearComb2 =
        linearCombination(nameToLia(lambda2, Value.TYPE_NAT), offsets2, argVector.size());
    final List<Long> newOffset = constMinus(shift1, shift2);
    final List<LiaStar> product3 = times(constToLia(newOffset), lambda3);
    argVector = plus(argVector, linearComb1);
    argVector = plus(argVector, linearComb2);
    argVector = plus(argVector, product3);
    final LiaStar toCheckLia = apply(target, slsVector, argVector);
    final Set<String> universalVars = new HashSet<>(lambda1);
    universalVars.addAll(lambda2);
    universalVars.add(lambda3Name);
    // check its validity
    if (isValidLia(toCheckLia, universalVars)) {
      // the two linear sets can be merged.
      // merge them into LS(shift2,offsets1+offsets2+{shift1-shift2})
      sls.remove(ls1);
      sls.remove(ls2);
      updatedLSIndex = sls.size();
      final List<List<Long>> newOffsets = new ArrayList<>();
      union(newOffsets, offsets1);
      union(newOffsets, offsets2);
      union(newOffsets, newOffset);
      sls.add(new LinearSet(shift2, newOffsets));
      return true;
    }
    return false;
  }

  // try to perform a ShiftDown (decrease shift vector with an offset vector)
  // return false if no operation is performed
  private boolean shiftDown() {
    // take the updated LS
    final int index = updatedLSIndex;
    // for the linear set (shift, offsets) and offset in offsets
    // if offset <= shift
    // check validity of "forall lambda. target(shift - offset + lambda * offsets)"
    // where "lambda * offsets" means a linear combination of offsets
    // and lambda is the vector of coefficients
    final LinearSet ls = sls.get(index);
    final List<Long> shift = ls.getShift();
    final List<List<Long>> offsets = ls.getOffsets();
    for (List<Long> offset : offsets) {
      // we need an offset vector "<=" the shift vector
      if (!constLeAbs(offset, shift)) {
        continue;
      }
      // construct "target(shift - offset + lambda * offsets)"
      List<LiaStar> argVector = constToLia(constMinus(shift, offset));
      final List<String> lambda = createVarVector("sls_lambda", 0, offsets.size());
      final List<LiaStar> linearComb =
          linearCombination(nameToLia(lambda, Value.TYPE_NAT), offsets, argVector.size());
      argVector = plus(argVector, linearComb);
      final LiaStar toCheckLia = apply(target, slsVector, argVector);
      final Set<String> universalVars = new HashSet<>(lambda);
      // check its validity
      if (isValidLia(toCheckLia, universalVars)) {
        // offset is a desired vector
        // replace LS(shift,offsets) with LS(shift-offset,offsets)
        sls.remove(ls);
        updatedLSIndex = sls.size();
        sls.add(new LinearSet(constMinus(shift, offset), offsets));
        return true;
      }
    }
    // no offset vector meets the demand
    return false;
  }

  // try to perform a OffsetDown (decrease offset of a linear set)
  // return false if no operation is performed
  private boolean offsetDown() {
    // take the updated LS
    int index = updatedLSIndex;
    // for the linear set (shift, offsets) and offset1,offset2 in offsets
    // if offset2 <= offset1
    // check validity of "forall lambda. target(shift + lambda * offsets')"
    // where "lambda * offsets'" means a linear combination of offsets',
    // lambda is the vector of coefficients,
    // and offsets' = offsets \ {offset1} + {offset1-offset2}
    final LinearSet ls = sls.get(index);
    final List<Long> shift = ls.getShift();
    final List<List<Long>> offsets = ls.getOffsets();
    // TODO: O(N^2) -> O(N)?
    for (int i = 0, bound = offsets.size(); i < bound; i++) {
      for (int j = 0; j < bound; j++) {
        // skip the same offset vector
        if (i == j) {
          continue;
        }
        // offset2 should be "<=" offset1
        final List<Long> offset1 = offsets.get(i);
        final List<Long> offset2 = offsets.get(j);
        if (!constLeAbs(offset2, offset1)) {
          continue;
        }
        // construct "target(shift + lambda * offsets')"
        // where offsets' = offsets \ {offset1} + {offset1-offset2}
        final List<List<Long>> offsetsPrime = new ArrayList<>(offsets);
        offsetsPrime.remove(i);
        final List<Long> newOffset = constMinus(offset1, offset2);
        if (!offsetsPrime.contains(newOffset)) {
          offsetsPrime.add(newOffset);
        }
        final List<String> lambda = createVarVector("sls_lambda", 0, offsetsPrime.size());
        final List<LiaStar> linearComb =
            linearCombination(nameToLia(lambda, Value.TYPE_NAT), offsetsPrime, shift.size());
        final List<LiaStar> argVector = plus(constToLia(shift), linearComb);
        final LiaStar toCheckLia = apply(target, slsVector, argVector);
        final Set<String> universalVars = new HashSet<>(lambda);
        // check its validity
        if (isValidLia(toCheckLia, universalVars)) {
          // (shift,offsets') is the desired linear set
          sls.remove(ls);
          updatedLSIndex = sls.size();
          sls.add(new LinearSet(shift, offsetsPrime));
          return true;
        }
      }
    }
    // no pair of offset vectors meets the demand
    return false;
  }
}
