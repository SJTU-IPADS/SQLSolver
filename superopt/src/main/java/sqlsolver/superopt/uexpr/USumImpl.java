package sqlsolver.superopt.uexpr;

import sqlsolver.common.utils.ListSupport;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.util.AbstractPrettyPrinter;
import sqlsolver.superopt.util.SetMatching;
import sqlsolver.superopt.util.Timeout;

import java.util.*;

import static sqlsolver.common.utils.Commons.joining;

public record USumImpl(Set<UVar> boundedVars, UTerm body) implements USum {

  @Override
  public boolean isUsing(UVar var) {
    if (!var.is(UVar.VarKind.BASE)) return false;
    return !boundedVars.contains(var) && body.isUsing(var);
  }

  @Override
  public boolean isUsingProjVar(UVar var) {
    if(!var.is(UVar.VarKind.PROJ))  return false;
    return body.isUsingProjVar(var);
  }

  @Override
  public boolean isUsingBoundedVar(UVar var) {
    if (!var.is(UVar.VarKind.BASE)) return false;
    return boundedVars.contains(var);
  }

  @Override
  public UTerm replaceVar(UVar baseVar, UVar repVar, boolean freshVar) {
    // assert baseVar.isUnaryVar() && repVar.isUnaryVar();
    if (freshVar) {
      // Replace a var with a fresh var, `baseVar` and `repVar` should be base var
      assert baseVar.is(UVar.VarKind.BASE) && repVar.is(UVar.VarKind.BASE);
      final UTerm newBody = body.replaceVar(baseVar, repVar, freshVar);
      final Set<UVar> newBoundedVars = new HashSet<>(boundedVars);
      if (isUsingBoundedVar(baseVar)) {
        newBoundedVars.remove(baseVar);
        newBoundedVars.add(repVar);
      }
      return USum.mk(newBoundedVars, newBody);
    } else {
      // Replace a var with an existing known var, `repVar` is either an outer var or using existing bounded vars
      // So do not consider adding `repVar` to boundedVars
      final UTerm newBody = body.replaceVar(baseVar, repVar, freshVar);
      final Set<UVar> newBoundedVars = new HashSet<>(boundedVars);
      return USum.mk(newBoundedVars, newBody);
    }
  }

  @Override
  public boolean replaceVarInplace(UVar baseVar, UVar repVar, boolean freshVar) {
    // assert baseVar.isUnaryVar() && repVar.isUnaryVar();
    if (freshVar) {
      // Replace a var with an existing known var, `repVar` is either an outer var or using existing bounded vars
      // So do not consider adding `repVar` to boundedVars
      assert baseVar.is(UVar.VarKind.BASE) && repVar.is(UVar.VarKind.BASE);
      boolean modified = body.replaceVarInplace(baseVar, repVar, freshVar);
      if (isUsingBoundedVar(baseVar)) {
        boundedVars.remove(baseVar);
        boundedVars.add(repVar);
        modified = true;
      }
      return modified;
    } else {
      // Replace a var with an existing known var, then do not consider adding `repVar` to boundedVars
      return body.replaceVarInplace(baseVar, repVar, freshVar);
    }
  }

  @Override
  public boolean replaceVarInplaceWOPredicate(UVar baseVar, UVar repVar) {
    // assert baseVar.isUnaryVar() && repVar.isUnaryVar();
    // Replace a var with an existing known var, then do not consider adding `repVar` to boundedVars
    return body.replaceVarInplaceWOPredicate(baseVar, repVar);
  }

  @Override
  public UTerm replaceAtomicTermExcept(UTerm baseTerm, UTerm repTerm, UTerm exceptTerm) {
    assert baseTerm.kind().isTermAtomic();
    if (this.equals(exceptTerm)) return this;
    final UTerm replaced = body.replaceAtomicTermExcept(baseTerm, repTerm, exceptTerm);
    final Set<UVar> newBoundedVars = new HashSet<>(boundedVars);
    final USum newSum = USum.mk(newBoundedVars, replaced);
    newSum.removeUnusedBoundedVar();
    return newSum;
  }

  @Override
  public UTerm replaceAtomicTerm(UTerm baseTerm, UTerm repTerm) {
    assert baseTerm.kind().isTermAtomic();
    final UTerm replaced = body.replaceAtomicTerm(baseTerm, repTerm);
    final Set<UVar> newBoundedVars = new HashSet<>(boundedVars);
    final USum newSum = USum.mk(newBoundedVars, replaced);
    newSum.removeUnusedBoundedVar();
    return newSum;
  }

  @Override
  public void prettyPrint(AbstractPrettyPrinter printer) {
    final StringBuilder builder = new StringBuilder("\u2211");
    final List<String> vars = ListSupport.map(boundedVars, UVar::toString);
    vars.sort(String::compareTo);
    joining("{", ",", "}(", false, vars, builder);

    String prefix = builder.toString();
    printer.print(prefix);

    int indent = prefix.length();
    printer.indent(indent);
    body.prettyPrint(printer);
    printer.indent(-indent);
    printer.print(')');
  }

  @Override
  public boolean isPrettyPrintMultiLine() {
    return body.isPrettyPrintMultiLine();
  }

  @Override
  public int hashForSort(Map<String, Integer> varHash) {
    int[] hashes = new int[2];
    hashes[0] = ("USum{" + boundedVars.size() + "}").hashCode();
    hashes[1] = body().hashForSort(varHash);
    return Arrays.hashCode(hashes);
  }

  @Override
  public void sortCommAssocItems() {
    body.sortCommAssocItems();
  }

  @Override
  public Set<String> getFVs() {
    Set<String> fvs = body.getFVs();
    for (UVar var : boundedVars) {
      fvs.remove(var.toString());
    }
    return fvs;
  }

  @Override
  public boolean groupSimilarVariables(UTerm that, SetMatching<String> matching) {
    if (that instanceof USum sum) {
      if (boundedVars.size() != sum.boundedVars().size()) return false;
      return body.groupSimilarVariables(sum.body(), matching);
    }
    return false;
  }

  public UTerm replaceTerm(UTerm baseTerm, UTerm repTerm) {
    if (body instanceof UMul) {
      final UTerm replaced = ((UMul) body).replaceTerm(baseTerm, repTerm);
      final Set<UVar> newBoundedVars = new HashSet<>(boundedVars);
      final USum newSum = USum.mk(newBoundedVars, replaced);
      newSum.removeUnusedBoundedVar();
      return newSum;
    } else {
      return this;
    }
  }

  public UTerm addMulSubTerm(UTerm newTerm) {
    if (body instanceof UMul) {
      ((UMul) body).addFactor(newTerm);
    } else {
      assert false;
    }
    return this;
  }

  @Override
  public boolean removeBoundedVar(UVar var) {
    if (body.isUsing(var)) return false;
    return boundedVars().remove(var);
  }

  public boolean removeBoundedVarForce(UVar var) {
    return boundedVars().remove(var);
  }

  public boolean addBoundedVarForce(UVar var) {
    return boundedVars().add(var);
  }

  @Override
  public void removeUnusedBoundedVar() {
    boundedVars.removeIf(v -> !body.isUsing(v));
  }

  @Override
  public UTerm copy() {
    final UTerm copyBody = body.copy();
    return new USumImpl(new HashSet<>(boundedVars), copyBody);
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("\u2211");
    final List<String> vars = ListSupport.map(boundedVars, UVar::toString);
    vars.sort(String::compareTo);
    joining("{", ",", "}", false, vars, builder);
    builder.append('(').append(body).append(')');
    return builder.toString();
  }

  boolean checkSameSum(int cur, List<UVar> boundedVars, UTerm body, HashSet<UVar> thatBoundVars, UTerm thatBody) {
    if (cur == boundedVars.size()) return body.equals(thatBody);

    final UVar curVar = boundedVars.get(cur);
    final UVar newVar = UVar.mkBase(UName.mk(LiaStar.newVarName()));
    body = body.replaceVar(curVar, newVar, true);

    for (UVar v: thatBoundVars) {
      final HashSet<UVar> tmpVars = new HashSet<>(thatBoundVars);
      tmpVars.remove(v);
      final UTerm tmpThatBody = thatBody.replaceVar(v, newVar, true);
      final boolean result = checkSameSum(cur + 1, boundedVars, body, tmpVars, tmpThatBody);
      if (result) return true;
    }
    return false;
  }

  @Override
  public Set<String> getBoundVarNames() {
    Set<String> names = new HashSet<>();
    for (UVar var : boundedVars) {
      names.add(var.toString());
    }
    return names;
  }

  private static Set<UVar> strings2Vars(Set<String> names) {
    Set<UVar> vars = new HashSet<>();
    for (String name : names) {
      vars.add(UVar.mkBase(UName.mk(name)));
    }
    return vars;
  }

  private static SetMatching<UVar> strings2BoundVars(SetMatching<String> matching, Set<String> leftBoundVarNames, Set<String> rightBoundVarNames) {
    SetMatching<UVar> varMatching = new SetMatching<>();
    for (Set<String>[] pair : matching) {
      Set<String> lnames = new HashSet<>(pair[0]);
      Set<String> rnames = new HashSet<>(pair[1]);
      // eliminate non-bound variables
      lnames.retainAll(leftBoundVarNames);
      rnames.retainAll(rightBoundVarNames);
      // convert to UVar
      Set<UVar> s1 = strings2Vars(lnames), s2 = strings2Vars(rnames);
      if (!varMatching.match(s1, s2)) return null;
    }
    return varMatching;
  }

  // match s1[depth] with elements in s2
  private static boolean tryMatchInPair(int depth, SetMatching<UVar> matching, UTerm t1, UTerm t2,
                                        int depthInPair, List<UVar> s1, Set<UVar> s2) {
    if (depthInPair == s1.size()) return tryMatch(depth + 1, matching, t1, t2);
    final UVar curVar = s1.get(depthInPair);
    final UVar newVar = UVar.mkBase(UName.mk(LiaStar.newVarName()));
    t1 = t1.replaceVar(curVar, newVar, true);
    for (UVar v2 : s2) {
      Set<UVar> tmps2 = new HashSet<>(s2);
      tmps2.remove(v2);
      final UTerm tmpt2 = t2.replaceVar(v2, newVar, true);
      if (tryMatchInPair(depth, matching, t1, tmpt2, depthInPair + 1, s1, tmps2))
        return true;
    }
    return false;
  }

  private static boolean tryMatch(int depth, SetMatching<UVar> matching, UTerm t1, UTerm t2) {
    Timeout.checkTimeout();
    // border of search
    if (depth == matching.size()) return t1.equals(t2);
    // search for different matching within matching[depth]
    Set<UVar>[] pair = matching.get(depth);
    return tryMatchInPair(depth, matching, t1, t2, 0,
            new ArrayList<>(pair[0]), pair[1]);
  }

  private static boolean fastEquals(USum sum1, USum sum2) {
    sum1 = (USum) sum1.copy();
    sum2 = (USum) sum2.copy();
    sum1.sortCommAssocItems();
    sum2.sortCommAssocItems();
    // match variables in groups
    SetMatching<String> matching = new SetMatching<>();
    if (!sum1.body().groupSimilarVariables(sum2.body(), matching)) return false;
    // enumerate matching
    SetMatching<UVar> varMatching = strings2BoundVars(matching, sum1.getBoundVarNames(), sum2.getBoundVarNames());
    if (varMatching == null) return false;
    return tryMatch(0, varMatching, sum1.body().copy(), sum2.body().copy());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof final USum that)) return false;
    HashSet<UVar> thatBoundVars = new HashSet<>(that.boundedVars());

    if (boundedVars.size() != thatBoundVars.size()) return false;

    if (boundedVars.size() > 4) return fastEquals(this, that);

    UTerm thatBody = that.body().copy();
    if (boundedVars.equals(thatBoundVars) && body.equals(thatBody)) return true;

    return checkSameSum(0, new ArrayList<>(boundedVars), body.copy(), thatBoundVars, thatBody);
  }

  @Override
  public int hashCode() {
    // make hash of different summations as distinct as possible
    // since invoking equals among large summations is extremely costly
    final USum sum = (USum) copy();
    sum.sortCommAssocItems();
    return sum.hashForSort();
  }
}
