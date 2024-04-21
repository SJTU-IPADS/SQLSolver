package sqlsolver.superopt.uexpr.normalizer;

import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.uexpr.*;

import java.util.ArrayList;
import java.util.List;

import static sqlsolver.superopt.uexpr.UExprSupport.transformSubTerms;

public class UExprPreprocessor {

  /* ================================ *
   * Merge OR operands: ||[P]+[Q]||
   * ================================ */

  /**
   * Under certain circumstances, P OR Q -> P'
   */
  private UTerm mergeOrOperands(UTerm uexp) {
    uexp = mergeOrOperandsGT0(uexp);
    return uexp;
  }

  private UTerm argOfPositivePred(UTerm uexp) {
    if (uexp instanceof UPred pred) {
      if (pred.isPredKind(UPred.PredKind.GT)
              && pred.args().get(1).equals(UConst.ZERO))
        return pred.args().get(0);
      if (pred.isPredKind(UPred.PredKind.LT)
              && pred.args().get(0).equals(UConst.ZERO))
        return pred.args().get(1);
    }
    return null;
  }

  /**
   * If A and B are non-negative, then
   * ||[A > 0] + [B > 0]|| -> [A + B > 0]
   * ||[A > 0] + [B > 0] + ...|| -> ||[A + B > 0] + ...||
   */
  private UTerm mergeOrOperandsGT0(UTerm uexp) {
    uexp = transformSubTerms(uexp, this::mergeOrOperandsGT0);
    if (uexp instanceof USquash squash) {
      UTerm body = squash.body();
      if (body instanceof UAdd add) {
        List<UTerm> newTerms = new ArrayList<>();
        List<UTerm> posTerms = new ArrayList<>();
        // traverse sub-terms of the addition
        for (UTerm term : add.subTerms()) {
          UTerm arg = argOfPositivePred(term);
          if (arg != null) {
            // positive predicates will be merged
            posTerms.add(arg);
          } else {
            // the rest remains the same
            newTerms.add(term);
          }
        }
        if (posTerms.isEmpty()) return uexp;
        newTerms.add(UPred.mkBinary(UPred.PredKind.GT,
                  UAdd.mk(posTerms), UConst.zero()));
        if (newTerms.size() == 1) return newTerms.get(0);
        return USquash.mk(UAdd.mk(newTerms));
      }
    }
    return uexp;
  }

  /* ================================ *
   * Top-level interface
   * ================================ */

  public UTerm preprocess(UTerm uexp) {
    uexp = mergeOrOperands(uexp);
    return uexp;
  }

}
