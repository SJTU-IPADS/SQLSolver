package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UStringImpl implements UString {

  private final String value;

  private UStringImpl(String value) {
        this.value = value;
    }

  static UString mkVal(String v) {
        return new UStringImpl(v);
    }
  static UString mkNull() {
        return new UStringImpl("");
    }

  @Override
  public String value() {
        return value;
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
    printer.print("'").print(value).print("'");
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    return false;
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    return value.hashCode();
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
        return new UStringImpl(value);
    }

  @Override
  public String toString() {
        return "'" + String.valueOf(value) + "'";
    }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof UString)) return false;

    final UString that = (UString) obj;
    return Objects.equals(that.value(), this.value);
  }

  @Override
  public int hashCode() {
        return value.hashCode();
    }
}
