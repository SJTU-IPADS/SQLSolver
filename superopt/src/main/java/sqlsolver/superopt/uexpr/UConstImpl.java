package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class UConstImpl implements UConst {

  private final int value;

  private UConstImpl(int value) {
    this.value = value;
  }

  static UConst mkVal(int v) {
    return new UConstImpl(v);
  }

  static UConst mkNull() {
    return new UConstImpl(Integer.MIN_VALUE);
  }

  @Override
  public int value() {
    return value;
  }

  @Override
  public boolean isZeroOneVal() {
    return value == 0 || value == 1;
  }

  @Override
  public boolean replaceVarInplaceWOPredicate(UVar baseVar, UVar repVar) {
    return false;
  }

  @Override
  public UTerm replaceAtomicTermExcept(UTerm baseTerm, UTerm repTerm, UTerm exceptTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(exceptTerm)) return this;
    if (this.equals(baseTerm)) return repTerm.copy();
    return this.copy();
  }

  @Override
  public UTerm replaceAtomicTerm(UTerm baseTerm, UTerm repTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(baseTerm)) return repTerm.copy();
    return this.copy();
  }

  @Override
  public void prettyPrint(AbstractPrettyPrinter printer) {
    printer.print(value);
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    return false;
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    return Integer.hashCode(value);
  }

  @Override
  public void sortCommAssocItems() {}

  @Override
  public Set<String> getFVs() {
    return new HashSet<>();
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    return true;
  }

  @Override
  public UTerm copy() {
    return new UConstImpl(value);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof UConst)) return false;

    final UConst that = (UConst) obj;
    return that.value() == this.value;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(value);
  }
}
