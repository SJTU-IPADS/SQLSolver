package sqlsolver.superopt.uexpr;

import sqlsolver.common.utils.Copyable;
import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.PrettyBuilder;
import sqlsolver.superopt.util.PrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static sqlsolver.common.utils.IterableSupport.all;
import static sqlsolver.common.utils.ListSupport.filter;

/** A U-expr. */
public interface UTerm extends Copyable<UTerm> {

  /**
   * Whether term always evaluates to a natural number.
   */
  static boolean returnsNatural(UTerm term) {
    final UKind kind = term.kind();
    if (kind == UKind.SQUASH
            || kind == UKind.NEGATION
            || kind == UKind.PRED
            || kind == UKind.TABLE)
      return true;
    if (kind == UKind.SUMMATION || kind.isBinary())
      return all(term.subTerms(), UTerm::returnsNatural);
    if (kind == UKind.CONST && ((UConst) term).value() >= 0)
      return true;
    return false;
  }

  UKind kind();

  List<UTerm> subTerms();

  default List<UTerm> subTermsOfKind(UKind kind) {
    return filter(subTerms(), term -> term.kind() == kind);
  }

  boolean isUsing(UVar var);

  boolean isUsingProjVar(UVar var);

  default boolean isUsingTerm(UTerm term) {
    if (this.equals(term)) return true;
    for (final UTerm subTerm : subTerms()) {
      if (subTerm.isUsingTerm(term)) return true;
    }
    return false;
  }

  UTerm replaceVar(UVar baseVar, UVar repVar, boolean freshVar);

  boolean replaceVarInplace(UVar baseVar, UVar repVar, boolean freshVar);


  boolean replaceVarInplaceWOPredicate(UVar baseVar, UVar repVar);

  UTerm replaceAtomicTermExcept(UTerm baseTerm, UTerm repTerm, UTerm exceptTerm);

  UTerm replaceAtomicTerm(UTerm baseTerm, UTerm repTerm);

  default boolean tupleProjectedFromFuncParam(UVar outVar) {
    return (kind() == UKind.VAR) && isUsing(outVar);
  }

  default void prettyPrint() {
    prettyPrint(new PrettyPrinter());
  }

  default String toPrettyString() {
    PrettyBuilder builder = new PrettyBuilder();
    prettyPrint(builder);
    return builder.toString();
  }

  void prettyPrint(AbstractPrettyPrinter printer);

  boolean isPrettyPrintMultiLine();

  /**
   * Hash of the structure excluding concrete variable names.
   * @param varHash the map which records hash code assigned to existing variables
   */
  int hashForSort(Map<String, Integer> varHash);

  default int hashForSort() {
    return hashForSort(new HashMap<>());
  }

  void sortCommAssocItems();

  /**
   * FVs (free variables): unbound base variables (xN) or lia* variables (uN)
   */
  Set<String> getFVs();

  /**
   * Try to match bound variables between two summations.
   * Instead of enumerating all possible matching,
   * For each bound variable, it rules out bound variables in the other summation that cannot match.
   * This function assumes that comm and assoc items in mult/add/eq/neq have been sorted in their structure hash order (i.e. <code>hashForSort</code>),
   * and bound variables do not share the same name.
   * @param that the other UTerm which may represent a (part of) summation body
   * @param matching a list of pairs, where each pair contains two sets of variable names
   * so that each variable in the first set may only match variables in the second set
   * @return whether matching is possible
   */
  boolean groupSimilarVariables(UTerm that, SetMatching<String> matching);

}
