package sqlsolver.superopt.uexpr;

import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Constraint;
import sqlsolver.sql.schema.Table;
import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.superopt.uexpr.UExprSupport.transformTerms;
import static sqlsolver.superopt.logic.CASTSupport.schema;

final class UPredImpl implements UPred {
  private PredKind predKind;
  private UName predName;
  private final List<UTerm> arguments;

  /**
   * This member indicates that whether this predicate is null-safe.
   * If it is null-safe, the isNull value cannot be inferred by this predicate, vice versa.
   */
  private final boolean nullSafe;

  public UPredImpl(PredKind predKind, UName predName, List<UTerm> arguments, boolean nullSafe) {
    if (predKind == PredKind.FUNC) assert arguments.size() == 1;
    else assert arguments.size() == 2;

    this.predKind = predKind;
    this.predName = predName;
    this.arguments = arguments;
    this.nullSafe = nullSafe;
  }

  @Override
  public List<UTerm> subTerms() {
    return arguments;
  }

  @Override
  public PredKind predKind() {
    return predKind;
  }

  @Override
  public UName predName() {
    return predName;
  }

  @Override
  public List<UTerm> args() {
    return arguments;
  }

  @Override
  public boolean nullSafe() {
    return nullSafe;
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
    return UPred.mk(predKind, predName, replaced, nullSafe);
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
    if(predKind == PredKind.EQ) {
      return false;
    }
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
    return UPred.mk(predKind, predName, replaced, nullSafe);
  }

  @Override
  public UTerm replaceAtomicTerm(UTerm baseTerm, UTerm repTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(baseTerm)) return repTerm.copy();
    final List<UTerm> replaced = transformTerms(arguments, t -> t.replaceAtomicTerm(baseTerm, repTerm));
    return UPred.mk(predKind, predName, replaced, nullSafe);
  }

  @Override
  public void prettyPrint(AbstractPrettyPrinter printer) {
    printer.print("[");
    printer.indent(1);
    if (isUnaryPred()) {
      // only one single arg
      assert arguments.size() == 1;
      printer.print(predName).print("(");
      int indent = predName.toString().length() + 1;
      printer.indent(indent);
      arguments.get(0).prettyPrint(printer);
      printer.print(")");
      printer.indent(-indent);
    } else {
      assert arguments.size() == 2;
      UTerm left = arguments.get(0), right = arguments.get(1);
      boolean multiLineLeft = left.isPrettyPrintMultiLine();
      boolean multiLineRight = right.isPrettyPrintMultiLine();
      left.prettyPrint(printer);
      if (multiLineLeft && multiLineRight) {
        printer.println().println(predName);
      } else if (multiLineLeft) {
        printer.println().print(predName).print(" ");
      } else if (multiLineRight) {
        printer.print(" ").println(predName);
      } else {
        printer.print(" ").print(predName).print(" ");
      }
      right.prettyPrint(printer);
    }
    printer.print("]");
    printer.indent(-1);
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    if (isUnaryPred()) {
      assert arguments.size() == 1;
      return arguments.get(0).isPrettyPrintMultiLine();
    } else {
      assert arguments.size() == 2;
      return arguments.get(0).isPrettyPrintMultiLine()
              || arguments.get(1).isPrettyPrintMultiLine();
    }
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    if (isUnaryPred()) {
      assert arguments.size() == 1;
      return Objects.hash(arguments.get(0).hashForSort(varHash),
              predKind);
    } else {
      assert arguments.size() == 2;
      return Objects.hash(arguments.get(0).hashForSort(varHash),
              arguments.get(1).hashForSort(varHash), predKind, predName);
    }
  }

  @Override
  public void sortCommAssocItems() {
    for (UTerm term : arguments) {
      term.sortCommAssocItems();
    }
    if (predKind == PredKind.EQ || predKind == PredKind.NEQ) {
      arguments.sort(Comparator.comparingInt(UTerm::hashForSort));
    }
  }

  @Override
  public Set<String> getFVs() {
    Set<String> fvs = arguments.get(0).getFVs();
    if (!isUnaryPred()) fvs.addAll(arguments.get(1).getFVs());
    return fvs;
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    if (that instanceof UPredImpl pred) {
      if (arguments.size() != pred.arguments.size()
              || predKind != pred.predKind
              || !Objects.equals(predName, pred.predName))
        return false;
      if (isUnaryPred()) {
        return arguments.get(0).groupSimilarVariables(pred.arguments.get(0), matching);
      } else {
        int hash1 = arguments.get(0).hashForSort();
        int hash2 = arguments.get(1).hashForSort();
        if ((pred.isPredKind(PredKind.EQ) || pred.isPredKind(PredKind.NEQ))
                && hash1 == hash2) {
          // symmetric predicate
          return matching.match(getFVs(), that.getFVs());
        } else {
          return arguments.get(0).groupSimilarVariables(pred.arguments.get(0), matching)
                  && arguments.get(1).groupSimilarVariables(pred.arguments.get(1), matching);
        }
      }
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
    return UPred.mk(predKind, predName.copy(), copies, nullSafe);
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(arguments.size() * 4);
    builder.append("[");
    if (isUnaryPred()) {
      // only one single arg
      assert arguments.size() == 1;
      builder.append(predName).append("(").append(arguments.get(0)).append(")");
    } else {
      assert arguments.size() == 2;
      builder.append(arguments.get(0));
      builder.append(" ").append(predName).append(" ");
      builder.append(arguments.get(1));
    }
    builder.append("]");
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof UPred)) return false;

    UPred that = (UPred) obj;
    if (that.predKind() == PredKind.EQ) {
      UTerm lhs = that.args().get(0);
      UTerm rhs = that.args().get(1);
      if(lhs instanceof UPred && rhs instanceof UConst && ((UConst) rhs).value() == 1)
        that = (UPred) lhs;
      if(rhs instanceof UPred && lhs instanceof UConst && ((UConst) lhs).value() == 1)
        that = (UPred) rhs;
    }

    if(predKind == PredKind.EQ) {
      UTerm lhs = args().get(0);
      UTerm rhs = args().get(1);
      if(lhs instanceof UPred && rhs instanceof UConst && ((UConst) rhs).value() == 1)
        return lhs.equals(that);
      if(rhs instanceof UPred && lhs instanceof UConst && ((UConst) lhs).value() == 1)
        return rhs.equals(that);
    }

    if (predKind != that.predKind() || !predName.equals(that.predName())) return false;

    if (isPredKind(PredKind.FUNC)) {
      assert arguments.size() == 1 && that.args().size() == 1;
      return arguments.get(0).equals(that.args().get(0));
    } else {
      assert arguments.size() == 2 && that.args().size() == 2;
      if (isPredKind(PredKind.EQ) || isPredKind(PredKind.NEQ)) // [t1 = t2] equals to [t2 = t1]
        return new HashSet<>(arguments).equals(new HashSet<>(that.args()));
      else return arguments.equals(that.args());
    }
  }

  @Override
  public int hashCode() {
    return predKind.hashCode() * 31 * 31 + predName.hashCode() * 31 + new HashSet<>(arguments).hashCode();
  }

  // 1 true 0 false -1 unknown
  public int isTruePred(UTerm expr) {
    switch (predKind) {
      case EQ : {
        UTerm v1 = arguments.get(0);
        UTerm v2 = arguments.get(1);
        if (v1.toString().equals(v2.toString()))
          return 1;
        if (v1 instanceof UConst && v2 instanceof UConst) {
          if(((UConst) v1).value() != ((UConst) v2).value()) {
            return 0;
          }
        }
        return -1; // unknown
      }
      case FUNC: {
        if (predName.equals(UName.NAME_IS_NULL)) {
          UTerm v1 = arguments.get(0);
          if(v1.kind() != UKind.VAR)
            return -1;
          final List<Constraint> notNulls = new ArrayList<>();
          for (Table table : schema.tables()) {
            table.constraints(ConstraintKind.NOT_NULL).forEach(notNulls::add);
          }
          UVar arg = ((UVarTerm) v1).var();

          if(expr == null) return -1;
//          System.out.println("constraint: " + notNulls);
          for(final Constraint notNull : notNulls) {
            for(final Column column : notNull.columns()) {
              if(arg.name().toString().equals(column.tableName()+"."+column.name())) {
                if (arg.args().length == 1) {
                  if(expr.toString().contains(column.tableName()+"("+arg.args()[0]+")"))
                    return 0;
                  else {
                    //natural congruence
                    String columnStr = column.tableName()+"."+column.name();
                    String first_matcher = columnStr+"\\("+arg.args()[0]+"\\)";
                    String second_matcher = columnStr+"\\(x\\d+\\)";
                    Pattern pattern1 = Pattern.compile("\\["+first_matcher+"\s"+"="+"\s"+second_matcher+"\\]");
                    Pattern pattern2 = Pattern.compile("\\["+second_matcher+"\s"+"="+"\s"+first_matcher+"\\]");
                    Matcher matcher1 = pattern1.matcher(expr.toString());
                    Matcher matcher2 = pattern2.matcher(expr.toString());
                    while(matcher1.find()) {
                      Pattern pattern = Pattern.compile("x\\d+");
                      Matcher matcher = pattern.matcher(matcher1.group());
                      while(matcher.find()) {
                        if(!Objects.equals(arg.args()[0].toString(), matcher.group())
                                && expr.toString().contains(column.tableName()+"("+matcher.group()+")"))
                          return 0;
                      }
                    }
                    while(matcher2.find()) {
                      Pattern pattern = Pattern.compile("x\\d+");
                      Matcher matcher = pattern.matcher(matcher2.group());
                      while(matcher.find()) {
                        if(!Objects.equals(arg.args()[0].toString(), matcher.group())
                                && expr.toString().contains(column.tableName()+"("+matcher.group()+")"))
                          return 0;
                      }
                    }
                  }
                }
              }
            }
          }
        } else {
          return -1;
        }
      }
      default: return -1; // unknown
    }
  }
}
