package sqlsolver.superopt.uexpr;

import sqlsolver.common.utils.Copyable;
import sqlsolver.superopt.fragment.AggFuncKind;
import sqlsolver.superopt.util.SetMatching;

import java.util.*;

import static sqlsolver.common.utils.ArraySupport.concat;
import static sqlsolver.superopt.uexpr.UVar.VarKind.*;

public interface UVar extends Copyable<UVar> {
  enum VarKind {
    BASE,
    CONCAT,
    PROJ
  }

  UVar replaceVar(UVar baseVar, UVar repVar);

  UVar replaceVarInplace(UVar baseVar, UVar repVar);;

  UName name();

  UVar[] args();

  VarKind kind();

  boolean isUsing(UVar var);

  boolean isUsingProjVar(UVar var);

  UVar copy();

  default boolean is(UVar.VarKind kind) {
    return kind() == kind;
  }

  default boolean isUnaryVar() {
    return kind() == BASE || (kind() == PROJ && args()[0].kind() == BASE);
  }

  static UVar mkBase(UName name) {
    final UVar var = new UVarImpl(VarKind.BASE, name, new UVar[1]);
    var.args()[0] = var;
    return var;
  }

  UName NAME_CONCAT = UName.mk("concat");

  static UName aggregateOutputName(AggFuncKind func) {
    return UName.mk(func.text());
  }

  static UVar mkConcat(UVar v0, UVar v1) {
    final UVar[] lhs = v0.is(PROJ) ? new UVar[] {v0} : v0.args();
    final UVar[] rhs = v1.is(PROJ) ? new UVar[] {v1} : v1.args();
    return new UVarImpl(VarKind.CONCAT, NAME_CONCAT, concat(lhs, rhs));
  }

  static UVar mkConcatRaw(UVar[] vars) {
    return new UVarImpl(VarKind.CONCAT, NAME_CONCAT, vars);
  }

  static UVar mkProj(UName attrName, UVar v) {
    return new UVarImpl(VarKind.PROJ, attrName, new UVar[] {v});
  }

  static Set<UVar> getBaseVars(UVar var) {
    if (var.is(BASE)) return new LinkedHashSet<>(Collections.singleton(var));

    final Set<UVar> baseVars = new LinkedHashSet<>(var.args().length);
    for (UVar arg : var.args()) baseVars.addAll(getBaseVars(arg));
    return baseVars;
  }

  static Set<UVar> getUnaryVars(UVar var) {
    final Set<UVar> unaryVars = new LinkedHashSet<>(var.args().length);
    if (var.isUnaryVar()) unaryVars.add(var);
    else {
      for (UVar arg : var.args()) unaryVars.addAll(getUnaryVars(arg));
    }
    return unaryVars;
  }

  static UVar getSingleBaseVar(UVar var) {
    assert var.isUnaryVar();

    if (var.is(BASE)) return var;
    else return getSingleBaseVar(var.args()[0]);
  }

  static int baseVarComparator(UVar var0, UVar var1) {
    assert var0.is(BASE) && var1.is(BASE);
    return var0.name().toString().compareTo(var1.name().toString());
  }

  static UVar removeNestedProjVar(UVar var) {
    if (var.is(BASE)) return var.copy();
    else if (var.is(PROJ)) {
      assert var.args().length == 1;
      final UVar arg = var.args()[0];
      if (arg.is(BASE)) return var.copy();
      if (arg.is(PROJ)) {
        assert var.args()[0].args().length == 1;
        final UVar newVar = UVar.mkProj(var.name(), arg.args()[0]);
        return removeNestedProjVar(newVar);
      }
    }
    final UVar[] newVars = Arrays.copyOf(var.args(), var.args().length);
    for (int i = 0, bound = var.args().length; i < bound; ++i) {
      final UVar v = removeNestedProjVar(var.args()[i]);
      newVars[i] = v;
    }
    if (Arrays.equals(newVars, var.args())) return var.copy();
    else return new UVarImpl(var.kind(), var.name(), newVars);
  }

  /**
   * @see UTerm#hashForSort
   */
  int hashForSort(Map<String, Integer> varHash);

  /**
   * @see UTerm#getFVs
   */
  Set<String> getFVs();

  /**
   * @see UTerm#groupSimilarVariables
   */
  boolean groupSimilarVariables(UVar that, SetMatching<String> matching);

}
