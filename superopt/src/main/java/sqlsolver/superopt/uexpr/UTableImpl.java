package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class UTableImpl implements UTable {
  private final UName tableName;
  private UVar var;

  UTableImpl(UName tableName, UVar var) {
    this.tableName = tableName;
    this.var = var;
  }

  @Override
  public UName tableName() {
    return tableName;
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
    return UTable.mk(tableName, v);
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
    printer.print(this);
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    return false;
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    return Objects.hash(tableName.hashCode(), var.hashForSort(varHash));
  }

  @Override
  public void sortCommAssocItems() {}

  @Override
  public Set<String> getFVs() {
    return var.getFVs();
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    if (that instanceof UTable table) {
      if (!Objects.equals(tableName, table.tableName())) return false;
      return var.groupSimilarVariables(table.var(), matching);
    }
    return false;
  }

  @Override
  public UTerm copy() {
    return UTable.mk(tableName.copy(), var.copy());
  }

  @Override
  public String toString() {
    return tableName + "(" + var + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof UTable)) return false;

    final UTable that = (UTable) obj;
    return this.tableName.equals(that.tableName()) && this.var.equals(that.var());
  }

  @Override
  public int hashCode() {
    return tableName.hashCode() * 31 + var.hashCode();
  }
}
