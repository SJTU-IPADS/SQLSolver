package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.Map;
import java.util.Set;

final class ULiaVarImpl implements ULiaVar {
  private UVar var;

  ULiaVarImpl(UVar var) {
    this.var = var;
  }

  @Override
  public UVar var() {
    return var;
  }

  @Override
  public boolean isUsing(UVar var) {
    return this.var.isUsing(var);
  }

  @Override
  public boolean isUsingProjVar(UVar var) {
    return this.var.isUsingProjVar(var);
  }

  @Override
  public UTerm replaceVar(UVar baseVar, UVar repVar, boolean freshVar) {
    final UVar v = var.replaceVar(baseVar, repVar);
    return ULiaVar.mk(v);
  }

  @Override
  public boolean replaceVarInplace(UVar baseVar, UVar repVar, boolean freshVar) {
    final UVar newVar = var.replaceVarInplace(baseVar, repVar);
    if (!newVar.equals(var)) {
      var = newVar;
      return true;
    }
    return false;
  }

  @Override
  public boolean replaceVarInplaceWOPredicate(UVar baseVar, UVar repVar) {
    final UVar newVar = var.replaceVarInplace(baseVar, repVar);
    if (!newVar.equals(var)) {
      var = newVar;
      return true;
    }
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
    printer.print(var);
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    return false;
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    // use the existing hash
    // if absent, rename self to obtain hash
    String key = toString();
    Integer value = varHash.get(key);
    if (value == null) {
      int hash = ("u" + varHash.size()).hashCode();
      varHash.put(key, hash);
    }
    return varHash.get(key);
  }

  @Override
  public void sortCommAssocItems() {}

  @Override
  public Set<String> getFVs() {
    return var.getFVs();
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    if (that instanceof ULiaVar thatVar) {
      return var.groupSimilarVariables(thatVar.var(), matching);
    }
    return false;
  }

  @Override
  public UTerm copy() {
    return new ULiaVarImpl(var.copy());
  }

  @Override
  public String toString() {
    return var.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ULiaVar)) return false;

    final ULiaVar that = (ULiaVar) obj;
    return this.var.equals(that.var());
  }

  @Override
  public int hashCode() {
    return var.hashCode();
  }
}
