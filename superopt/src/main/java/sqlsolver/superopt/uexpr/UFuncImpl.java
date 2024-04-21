package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.*;

import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.superopt.uexpr.UExprSupport.transformTerms;

final public class UFuncImpl implements UFunc {
  private FuncKind funcKind;
  private UName funcName;
  private final List<UTerm> arguments;

  public UFuncImpl(FuncKind funcKind, UName funcName, List<UTerm> arguments) {
    this.funcKind = funcKind;
    this.funcName = funcName;
    this.arguments = arguments;
  }

  @Override
  public List<UTerm> subTerms() {
        return arguments;
    }

  @Override
  public FuncKind funcKind() {
        return funcKind;
    }

  @Override
  public UName funcName() {
        return funcName;
    }

  @Override
  public List<UTerm> args() {
        return arguments;
    }

  @Override
  public boolean isUsing(UVar var) {
        return any(arguments, arg -> arg.isUsing(var));
    }

  @Override
  public boolean isUsingProjVar(UVar var) {
        return any(arguments, arg -> arg.isUsingProjVar(var));
    }

  @Override
  public UTerm replaceVar(UVar baseVar, UVar repVar, boolean freshVar) {
    final List<UTerm> replaced = transformTerms(arguments, t -> t.replaceVar(baseVar, repVar, freshVar));
    return UFunc.mk(funcKind, funcName, replaced);
  }

  @Override
  public boolean replaceVarInplace(UVar baseVar, UVar repVar, boolean freshVar) {
    boolean modified = false;
    for (UTerm arg : arguments) {
      if (arg.replaceVarInplace(baseVar, repVar, freshVar)) modified = true;
    }
      return modified;
  }

  @Override
  public boolean replaceVarInplaceWOPredicate(UVar baseVar, UVar repVar) {
    boolean modified = false;
    for (UTerm arg : arguments) {
      if (arg.replaceVarInplaceWOPredicate(baseVar, repVar)) modified = true;
    }
    return modified;
  }

  @Override
  public UTerm replaceAtomicTermExcept(UTerm baseTerm, UTerm repTerm, UTerm exceptTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(exceptTerm)) return this;
    if (this.equals(baseTerm)) return repTerm.copy();
    final List<UTerm> replaced = transformTerms(arguments, t -> t.replaceAtomicTermExcept(baseTerm, repTerm, exceptTerm));
    return UFunc.mk(funcKind, funcName, replaced);
  }

  @Override
  public UTerm replaceAtomicTerm(UTerm baseTerm, UTerm repTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(baseTerm)) return repTerm.copy();
    final List<UTerm> replaced = transformTerms(arguments, t -> t.replaceAtomicTerm(baseTerm, repTerm));
    return UFunc.mk(funcKind, funcName, replaced);
  }

  @Override
  public void prettyPrint(AbstractPrettyPrinter printer) {
    printer.print(funcName);
    printer.print("(");
    int indent = funcName.toString().length() + 1;
    printer.indent(indent);
    for (int i = 0; i < arguments.size(); i++) {
      UTerm arg = arguments.get(i);
      if (arg.isPrettyPrintMultiLine()) printer.println();
      arg.prettyPrint(printer);
      if (i < arguments.size() - 1) {
        printer.print(", ");
      }
      if (arg.isPrettyPrintMultiLine()) printer.println();
    }
    printer.indent(-indent);
    printer.print(")");
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    for (UTerm arg : arguments) {
      if (arg.isPrettyPrintMultiLine()) return true;
    }
    return false;
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    int size = arguments.size();
    int[] hashes = new int[size + 2];
    for (int i = 0; i < size; i++) {
      hashes[i] = arguments.get(i).hashForSort(varHash);
    }
    hashes[size] = funcKind.hashCode();
    hashes[size + 1] = funcName.hashCode();
    return Arrays.hashCode(hashes);
  }

  @Override
  public void sortCommAssocItems() {
    for (UTerm term : arguments) {
      term.sortCommAssocItems();
    }
  }

  @Override
  public Set<String> getFVs() {
    Set<String> fvs = new HashSet<>();
    for (UTerm arg : arguments) {
      fvs.addAll(arg.getFVs());
    }
    return fvs;
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    if (that instanceof UFunc func) {
      int size = arguments.size();
      if (funcKind != func.funcKind()
              || !Objects.equals(funcName, func.funcName())
              || size != func.args().size())
        return false;
      List<UTerm> thatArgs = func.args();
      for (int i = 0; i < size; i++) {
        if (!arguments.get(i).groupSimilarVariables(thatArgs.get(i), matching))
          return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public UTerm copy() {
    List<UTerm> copies = new ArrayList<>(arguments);
    for (int i = 0, bound = arguments.size(); i < bound; i++) {
      final UTerm copiedFactor = arguments.get(i).copy();
      copies.set(i, copiedFactor);
    }
    return UFunc.mk(funcKind, funcName.copy(), copies);
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(arguments.size() * 5);
    builder.append(funcName);
    builder.append("(");
    for(int i = 0; i < arguments.size(); i++) {
      if(i == arguments.size() - 1) {
        builder.append(arguments.get(i));
      } else {
        builder.append(arguments.get(i)).append(", ");
      }
    }
    builder.append(")");
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof UFunc)) return false;

    UFunc that = (UFunc) obj;
    if (funcKind != that.funcKind() || !funcName.equals(that.funcName())) return false;
    if(arguments.size() != that.args().size()) return false;

    assert arguments.size() == that.args().size();
    return arguments.equals(that.args());
  }

  @Override
  public int hashCode() {
    return funcKind.hashCode() * 31 * 31 + funcName.hashCode() * 31 + new HashSet<>(arguments).hashCode();
  }
}
