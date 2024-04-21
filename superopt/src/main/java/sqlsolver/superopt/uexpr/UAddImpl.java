package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.CommAssocUexpUtils;
import sqlsolver.superopt.util.SetMatching;

import java.util.*;

import static sqlsolver.common.utils.Commons.joining;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.superopt.uexpr.UExprSupport.transformTerms;

final class UAddImpl implements UAdd {
  private final List<UTerm> factors;

  UAddImpl(List<UTerm> factors) {
    this.factors = factors;
  }

  static UAdd mk(UTerm e0, UTerm e1) {
    final List<UTerm> factors = new ArrayList<>(e0.subTerms().size() + e1.subTerms().size());
    addFactor(factors, e0);
    addFactor(factors, e1);
    return new UAddImpl(factors);
  }

  static UAdd mk(UTerm e0, UTerm e1, UTerm... others) {
    final int sum = Arrays.stream(others).map(UTerm::subTerms).mapToInt(List::size).sum();
    final List<UTerm> factors =
        new ArrayList<>(e0.subTerms().size() + e1.subTerms().size() + sum + 1);
    addFactor(factors, e0);
    addFactor(factors, e1);
    for (UTerm factor : others) addFactor(factors, factor);
    return new UAddImpl(factors);
  }

  private static void addFactor(List<UTerm> factors, UTerm factor) {
    if (factor.kind() == UKind.ADD) factors.addAll(factor.subTerms());
    else factors.add(factor);
  }

  @Override
  public List<UTerm> subTerms() {
    return factors;
  }

  @Override
  public boolean isUsing(UVar var) {
    return any(factors, it -> it.isUsing(var));
  }

  @Override
  public boolean isUsingProjVar(UVar var) {
    return any(factors, it -> it.isUsingProjVar(var));
  }

  @Override
  public UTerm replaceVar(UVar baseVar, UVar repVar, boolean freshVar) {
    final List<UTerm> replaced = transformTerms(factors, t -> t.replaceVar(baseVar, repVar, freshVar));
    return new UAddImpl(replaced);
  }

  @Override
  public boolean replaceVarInplace(UVar baseVar, UVar repVar, boolean freshVar) {
    boolean modified = false;
    for (UTerm factor : factors) {
      if (factor.replaceVarInplace(baseVar, repVar, freshVar)) modified = true;
    }
    return modified;
  }


  @Override
  public boolean replaceVarInplaceWOPredicate(UVar baseVar, UVar repVar) {
    boolean modified = false;
    for (UTerm factor : factors) {
      if (factor.replaceVarInplaceWOPredicate(baseVar, repVar)) modified = true;
    }
    return modified;
  }

  @Override
  public UTerm replaceAtomicTermExcept(UTerm baseTerm, UTerm repTerm, UTerm exceptTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(exceptTerm)) return this;
    final List<UTerm> replaced = transformTerms(factors, t -> t.replaceAtomicTermExcept(baseTerm, repTerm, exceptTerm));
    return new UAddImpl(replaced);
  }

  @Override
  public UTerm replaceAtomicTerm(UTerm baseTerm, UTerm repTerm) {
    assert baseTerm.kind().isTermAtomic();
    final List<UTerm> replaced = transformTerms(factors, t -> t.replaceAtomicTerm(baseTerm, repTerm));
    return new UAddImpl(replaced);
  }

  @Override
  public void prettyPrint(AbstractPrettyPrinter printer) {
    int bound = factors.size();
    if (bound == 0) return;
    factors.get(0).prettyPrint(printer);
    for (int i = 1; i < bound; i++) {
      printer.println();
      printer.print("+ ");
      printer.indent(2);
      factors.get(i).prettyPrint(printer);
      printer.indent(-2);
    }
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    if (factors.size() > 1) return true;
    if (factors.size() == 0) return false;
    return factors.get(0).isPrettyPrintMultiLine();
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    int size = factors.size();
    int[] hashes = new int[size + 1];
    for (int i = 0; i < size; i++) {
      hashes[i] = factors.get(i).hashForSort(varHash);
    }
    hashes[size] = "+".hashCode();
    return Arrays.hashCode(hashes);
  }

  @Override
  public void sortCommAssocItems() {
    for (UTerm term : factors) {
      term.sortCommAssocItems();
    }
    factors.sort(Comparator.comparingInt(UTerm::hashForSort));
  }

  @Override
  public Set<String> getFVs() {
    Set<String> fvs = new HashSet<>();
    for (UTerm factor : factors) {
      fvs.addAll(factor.getFVs());
    }
    return fvs;
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    if (that instanceof UAdd add) {
      return CommAssocUexpUtils.groupSimilarVariables(factors, add.subTerms(), matching);
    }
    return false;
  }

  @Override
  public UTerm copy() {
    List<UTerm> copies = new ArrayList<>(factors);
    for (int i = 0, bound = factors.size(); i < bound; i++) {
      final UTerm copiedFactor = factors.get(i).copy();
      copies.set(i, copiedFactor);
    }
    return new UAddImpl(copies);
  }

  @Override
  public String toString() {
    if (factors.size() == 0) return "";
    else if (factors.size() == 1) return factors.get(0).toString();
    return joining(" + ", factors);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof UAdd)) return false;

    final UAdd that = (UAdd) obj;
    if (this.subTerms().size() != that.subTerms().size()) return false;
    for (UTerm thisTerm : subTerms()) {
      boolean commonTerm = false;
      for (UTerm thatTerm : that.subTerms()) {
        if (thisTerm.equals(thatTerm)) {
          commonTerm = true;
          break;
        }
      }
      if (commonTerm == false) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return new HashSet<>(factors).hashCode();
  }
}
