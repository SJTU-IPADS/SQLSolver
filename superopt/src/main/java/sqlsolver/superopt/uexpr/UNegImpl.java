package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

record UNegImpl(UTerm body) implements UNeg {
  @Override
  public boolean isUsing(UVar var) {
    return body.isUsing(var);
  }

  @Override
  public boolean isUsingProjVar(UVar var) {
    return body.isUsingProjVar(var);
  }

  @Override
  public UTerm replaceVar(UVar baseVar, UVar repVar, boolean freshVar) {
    final UTerm e = body.replaceVar(baseVar, repVar, freshVar);
    return UNeg.mk(e);
  }

  @Override
  public boolean replaceVarInplace(UVar baseVar, UVar repVar, boolean freshVar) {
    return body.replaceVarInplace(baseVar, repVar, freshVar);
  }

  @Override
  public boolean replaceVarInplaceWOPredicate(UVar baseVar, UVar repVar) {
    return body.replaceVarInplaceWOPredicate(baseVar, repVar);
  }

  @Override
  public UTerm replaceAtomicTermExcept(UTerm baseTerm, UTerm repTerm, UTerm exceptTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(exceptTerm)) return this;
    final UTerm replaced = body.replaceAtomicTermExcept(baseTerm, repTerm, exceptTerm);
    return new UNegImpl(replaced);
  }

  @Override
  public UTerm replaceAtomicTerm(UTerm baseTerm, UTerm repTerm) {
    assert baseTerm.kind().isTermAtomic();
    final UTerm replaced = body.replaceAtomicTerm(baseTerm, repTerm);
    return new UNegImpl(replaced);
  }

  @Override
  public void prettyPrint(AbstractPrettyPrinter printer) {
    printer.print("not(");
    printer.indent(4);
    body.prettyPrint(printer);
    printer.indent(-4);
    printer.print(")");
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    return body.isPrettyPrintMultiLine();
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    return Objects.hash(body.hashForSort(varHash), "-");
  }

  @Override
  public void sortCommAssocItems() {
    body.sortCommAssocItems();
  }

  @Override
  public Set<String> getFVs() {
    return body.getFVs();
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    if (that instanceof UNeg neg) {
      return body.groupSimilarVariables(neg.body(), matching);
    }
    return false;
  }

  @Override
  public UTerm copy() {
    final UTerm copyBody = body.copy();
    return new UNegImpl(copyBody);
  }

  @Override
  public String toString() {
    return "not(" + body + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof UNeg)) return false;

    final UNeg that = (UNeg) obj;
    return this.body.equals(that.body());
  }

  @Override
  public int hashCode() {
    return body.hashCode();
  }
}
