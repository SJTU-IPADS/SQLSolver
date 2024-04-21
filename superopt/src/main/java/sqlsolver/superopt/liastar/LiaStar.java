package sqlsolver.superopt.liastar;

import com.microsoft.z3.*;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.util.PrettyBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static sqlsolver.superopt.liastar.LiaOpType.*;


public abstract class LiaStar {

  boolean innerStar = false;

  private static ThreadLocal<Integer> freshVarId = new ThreadLocal<Integer>() {
    public Integer initialValue() {
      return 0;
    }
  };

  private static final Integer dnfTimeout = 2000000;
  private static ThreadLocal<Integer> dnfStartTime  = new ThreadLocal<Integer>() {
    public Integer initialValue() {
      return 0;
    }
  };

  public static void setDnfStartTime() {
    dnfStartTime.set(0);
  }

  public boolean isInStar() {
    return innerStar;
  }

  /**
   * Maintain the "innerStar" field recursively.
   */
  public LiaStar updateInnerStar(boolean innerStar) {
    this.innerStar = innerStar;
    if (this instanceof LiaSumImpl sum) {
      sum.constraints.updateInnerStar(true);
    } else {
      for (LiaStar child : subNodes()) {
        child.updateInnerStar(innerStar);
      }
    }
    return this;
  }

  public LiaStar mergeSameVars() {return this;}

  public static boolean isDnfTimeout() {
    dnfStartTime.set(dnfStartTime.get() + 1);
    return (dnfStartTime.get() > dnfTimeout);
  }

  public static LiaStar deepcopy(LiaStar formula) {
    return formula == null ? null : formula.deepcopy();
  }

  public abstract boolean isLia();

  public abstract LiaStar deepcopy();

  public abstract LiaStar multToBin(int n);

  public abstract LiaStar simplifyMult(Map<LiaStar, String> multToVar);

  public abstract LiaOpType getType();

  public abstract LiaStar mergeMult(Map<LiaMulImpl, LiaVarImpl> multToVar);

  public abstract int embeddingLayers();

  public static String newId() {
    freshVarId.set(freshVarId.get() + 1);
    return String.valueOf(freshVarId.get());
  }

  public static String resetId() {
    freshVarId.set(0);
    return String.valueOf(freshVarId.get());
  }

  public static String newVarName() {
    return "var" + newId();
  }

  /**
   * Collect vars from formulas in a batch.
   */
  public static Set<String> collectVarNames(Collection<LiaStar> formulas) {
    return formulas.stream()
            .map(LiaStar::collectVarNames)
            .reduce(SetSupport::union)
            .orElseGet(HashSet::new);
  }

  /**
   * Collect free vars only.
   * This should be used after removal of parameters except for
   * the case where it is used to collect parameters.
   */
  public abstract Set<String> collectVarNames();

  /**
   * Collect free vars only.
   * @see LiaStar#collectVarNames()
   */
  public Set<LiaVarImpl> collectVars() {
    final Set<LiaVarImpl> result = new HashSet<>();
    this.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var) {
        result.add(var);
      } else if (f instanceof LiaSumImpl sum) {
        result.removeIf(p -> sum.innerVector.contains(p.getName()));
      }
      return f;
    });
    return result;
  }

  /** Collect vars, no matter free or bound. */
  public abstract Set<String> collectAllVarNames();

  public Set<LiaVarImpl> collectAllVars() {
    final Set<LiaVarImpl> result = new HashSet<>();
    this.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var) {
        result.add(var);
      }
      return f;
    });
    return result;
  }

  public abstract LiaStar expandStar();

  public LiaStar liaAndConcat(LiaStar[] array) {
    LiaStar res = null;
    for(int i = 0; i < array.length; ++ i) {
      if(array[i] != null) {
        if(res == null)
          res = array[i];
        else
          res = new LiaAndImpl(res, array[i]);
        res.innerStar = true;
      }
    }

    return res;
  }

  public Expr transToSMT(Context ctx, Map<String, Expr> varsName) {
    return transToSMT(ctx, varsName, new HashMap<>());
  }

  public abstract Expr transToSMT(Context ctx, Map<String, Expr> varsName, Map<String, FuncDecl> funcsName);

  final class EstimateResult {
    final Set<String> vars;
    final int n_extra;
    final int m;
    final double a;
    public EstimateResult(Set<String> vars, int n_extra, int m, double a) {
      this.vars = vars;
      this.n_extra = n_extra;
      this.m = m;
      this.a = a;
    }
  }

  public abstract EstimateResult estimate();

  public abstract Expr expandStarWithK(Context ctx, Solver sol, String suffix);

  public static LiaStar mkAnd(boolean isInnerStar, LiaStar a, LiaStar b) {
    if (a == null && b == null) {
      return null;
    }
    LiaStar trueLia = mkTrue(isInnerStar);
    if (a == null || a.equals(trueLia)) {
      return b;
    }
    if (b == null || b.equals(trueLia)) {
      return a;
    }
    LiaStar falseLia = mkFalse(isInnerStar);
    if (a.equals(falseLia) || b.equals(falseLia)) {
      return falseLia;
    }
    LiaStar result = new LiaAndImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaStar mkAndNoStar(LiaStar... terms) {
    LiaStar result = mkTrue(false);
    for (LiaStar term : terms) {
      result = mkAnd(false, result, term);
    }
    return result;
  }

  public static LiaConstImpl mkConst(boolean isInnerStar, long c) {
    LiaConstImpl result = new LiaConstImpl(c);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaStringImpl mkString(boolean isInnerStar, String v) {
    LiaStringImpl result = new LiaStringImpl(v);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaEqImpl mkEq(boolean isInnerStar, LiaStar a, LiaStar b) {
    LiaEqImpl result = new LiaEqImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaStar mkIte(boolean isInnerStar, LiaStar cond, LiaStar op1, LiaStar op2) {
    LiaStar result = new LiaIteImpl(cond, op1, op2);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaLeImpl mkLe(boolean isInnerStar, LiaStar a, LiaStar b) {
    LiaLeImpl result = new LiaLeImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaLtImpl mkLt(boolean isInnerStar, LiaStar a, LiaStar b) {
    LiaLtImpl result = new LiaLtImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaStar mkMul(boolean isInnerStar, LiaStar a, LiaStar b) {
    if (a.isConstV(0) || b.isConstV(0)) {
      return mkConst(isInnerStar, 0);
    }
    if (a.isConstV(1)) {
      return b;
    }
    if (b.isConstV(1)) {
      return a;
    }
    LiaStar result = new LiaMulImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaStar mkNot(boolean isInnerStar, LiaStar a) {
    LiaStar result = new LiaNotImpl(a);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaStar mkOr(boolean isInnerStar, LiaStar a, LiaStar b) {
    if (a == null && b == null) {
      assert false;
      return a;
    }
    LiaStar falseLia = mkFalse(isInnerStar);
    if (a == null || a.equals(falseLia)) {
      return b;
    }
    if (b == null || b.equals(falseLia)) {
      return a;
    }
    LiaStar trueLia = mkTrue(isInnerStar);
    if (a.equals(trueLia) || b.equals(trueLia)) {
      return trueLia;
    }
    LiaStar result = new LiaOrImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaStar mkPlus(boolean isInnerStar, LiaStar a, LiaStar b) {
    if (a.isConstV(0)) {
      return b;
    }
    if (b.isConstV(0)) {
      return a;
    }
    LiaStar result = new LiaPlusImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaDivImpl mkDiv(boolean isInnerStar, LiaStar a, LiaStar b) {
    LiaDivImpl result = new LiaDivImpl(a, b);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaSumImpl mkSum(boolean isInnerStar, List<String> outerVector, List<String> innerVector, LiaStar constraint) {
    LiaSumImpl result = new LiaSumImpl(outerVector, innerVector, constraint);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaFuncImpl mkFunc(boolean isInnerStar, String funcName, LiaStar var, boolean allowsUndefined) {
    LiaFuncImpl result = new LiaFuncImpl(funcName, var, allowsUndefined);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaFuncImpl mkFunc(boolean isInnerStar, String funcName, LiaStar var) {
    LiaFuncImpl result = new LiaFuncImpl(funcName, var);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaFuncImpl mkFunc(boolean isInnerStar, String funcName, List<LiaStar> vars, boolean allowsUndefined) {
    LiaFuncImpl result = new LiaFuncImpl(funcName, vars, allowsUndefined);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaFuncImpl mkFunc(boolean isInnerStar, String funcName, List<LiaStar> vars) {
    LiaFuncImpl result = new LiaFuncImpl(funcName, vars);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaVarImpl mkVar(boolean isInnerStar, String varName, String valueType) {
    LiaVarImpl result = new LiaVarImpl(varName, valueType);
    result.innerStar = isInnerStar;
    return result;
  }

  public static LiaVarImpl mkVar(boolean isInnerStar, String varName) {
    return mkVar(isInnerStar, varName, Value.TYPE_INT);
  }

  public static LiaStar mkTrue(boolean isInnerStar) {
    return mkEq(isInnerStar, mkConst(isInnerStar, 0), mkConst(isInnerStar, 0));
  }

  public static LiaStar mkFalse(boolean isInnerStar) {
    return mkEq(isInnerStar, mkConst(isInnerStar, 1), mkConst(isInnerStar, 0));
  }

  public static LiaStar mkImplies(boolean isInnerStar, LiaStar a, LiaStar b) {
    return mkOr(isInnerStar, mkNot(isInnerStar, a), b);
  }

  public static LiaStar mkIff(boolean isInnerStar, LiaStar a, LiaStar b) {
    return mkAnd(isInnerStar, mkImplies(isInnerStar, a, b), mkImplies(isInnerStar, b, a));
  }

  public static LiaStar mkNeq(boolean isInnerStar, LiaStar a, LiaStar b) {
    return mkNot(isInnerStar, mkEq(isInnerStar, a, b));
  }

  public abstract Set<String> getVars();

  public boolean isConstV(long v) {
    if(this instanceof LiaConstImpl)
      return ((LiaConstImpl) this).getValue() == v;
    else
      return false;
  }

  /** Transform a parameterized Lia formula to its DNF
   * @param activated: set `false` at the beginning
   */
  public static LiaStar paramLiaStar2DNF(LiaStar formula, boolean activated) throws Exception {

    if(isDnfTimeout())
      throw new Exception("dnf timeout");

    if (formula == null) return null;

    // Do recursion from inner to outer (?)
    switch (formula.getType()) {
      // Logic op: OR, AND NOT
      case LOR -> {
        // Do nothing for f1 \/ f2
        final LiaOrImpl or = (LiaOrImpl) formula;
        or.operand1 = paramLiaStar2DNF(or.operand1, activated);
        or.operand2 = paramLiaStar2DNF(or.operand2, activated);
        return simplifyDNF(or);
//        return or;
      }
      case LAND -> {
        final LiaAndImpl and = (LiaAndImpl) formula;
        final boolean innerStar = and.innerStar;
        and.operand1 = paramLiaStar2DNF(and.operand1, activated);
        and.operand2 = paramLiaStar2DNF(and.operand2, activated);
        if (activated) {
          LiaStar newLia = null;
          // if (and.operand1.getType() == LNOT && and.operand2.getType() == LNOT) {
            // ~f1 /\ ~f2 <=> f1 \/ f2
            // final Liastar subOp1 = ((LianotImpl) and.operand1).operand;
            // final Liastar subOp2 = ((LianotImpl) and.operand2).operand;
            // newLia = mkOr(innerStar, subOp1, subOp2);
          // }
          if (and.operand1.getType() == LOR || and.operand2.getType() == LOR) {
            // f /\ (f1 \/ f2) <=> (f /\ f1) \/ (f1 /\ f2)
            final LiaOrImpl or = (LiaOrImpl) (and.operand1.getType() == LOR? and.operand1 : and.operand2);
            final LiaStar other = or == and.operand1 ? and.operand2 : and.operand1; // f
            newLia = mkOr(innerStar, mkAnd(innerStar, other, or.operand1), mkAnd(innerStar, other, or.operand2));
          }
          if (newLia != null) return paramLiaStar2DNF(newLia, activated);
        }
        return simplifyDNF(and);
//        return and;
      }
      case LNOT -> {
        // ~(~f) <=> f
        // ~(f1 /\ f2) <=> ~f1 \/ ~f2
        // ~(f1 \/ f2) <=> ~f1 /\ ~f2
        final LiaNotImpl not = (LiaNotImpl) formula;
        final boolean innerStar = not.innerStar;
        not.operand = paramLiaStar2DNF(not.operand, activated);
        if (activated) {
          LiaStar newLia = null;
          switch (not.operand.getType()) {
            case LNOT -> newLia = ((LiaNotImpl) not.operand).operand;
            case LAND -> {
              final LiaStar subOp1 = ((LiaAndImpl) not.operand).operand1;
              final LiaStar subOp2 = ((LiaAndImpl) not.operand).operand2;
              newLia = mkOr(innerStar, mkNot(innerStar, subOp1), mkNot(innerStar, subOp2));
            }
            case LOR -> {
              final LiaStar subOp1 = ((LiaOrImpl) not.operand).operand1;
              final LiaStar subOp2 = ((LiaOrImpl) not.operand).operand2;
              newLia = mkAnd(innerStar, mkNot(innerStar, subOp1), mkNot(innerStar, subOp2));
            }
          }
          if (newLia != null) return paramLiaStar2DNF(newLia, activated);
        }
        return simplifyDNF(not);
//        return not;
      }
      // Arithmetic ops: EQ, LE, LT
      case LEQ-> {
        // a = ite(b, c, d) <=> (b /\ a = c) \/ (~b /\ a = d)
        final LiaEqImpl eq = (LiaEqImpl) formula;
//        eq.operand1 = paramLiaStar2DNF(eq.operand1, activated);
//        eq.operand2 = paramLiaStar2DNF(eq.operand2, activated);
        final boolean innerStar = eq.innerStar;
        if (activated && (eq.operand1.getType() == LITE || eq.operand2.getType() == LITE)) {
          final LiaIteImpl ite = (LiaIteImpl) (eq.operand1.getType() == LITE ? eq.operand1 : eq.operand2);
          final LiaStar other = ite == eq.operand1 ? eq.operand2 : eq.operand1;

          final LiaAndImpl and1 = (LiaAndImpl)
              mkAnd(innerStar, ite.cond, mkEq(innerStar, other, ite.operand1));
          final LiaAndImpl and2 = (LiaAndImpl)
              mkAnd(innerStar, mkNot(innerStar, ite.cond), mkEq(innerStar, other, ite.operand2));
          final LiaStar or = mkOr(innerStar, and1, and2);
          return or;
//          return paramLiaStar2DNF(or, activated);
        }
        return simplifyDNF(eq);
//        return eq;
      }
      case LLE-> {
        final LiaLeImpl le = (LiaLeImpl) formula;
//        le.operand1 = paramLiaStar2DNF(le.operand1, activated);
//        le.operand2 = paramLiaStar2DNF(le.operand2, activated);
        final boolean innerStar = le.innerStar;
        if (activated && (le.operand1.getType() == LITE || le.operand2.getType() == LITE)) {
          final LiaIteImpl ite = (LiaIteImpl) (le.operand1.getType() == LITE ? le.operand1 : le.operand2);
          final LiaStar other = ite == le.operand1 ? le.operand2 : le.operand1;

          final LiaAndImpl and1 = (LiaAndImpl)
              mkAnd(innerStar, ite.cond, mkLe(innerStar, other, ite.operand1));
          final LiaAndImpl and2 = (LiaAndImpl)
              mkAnd(innerStar, mkNot(innerStar, ite.cond), mkLe(innerStar, other, ite.operand2));
          final LiaStar or = mkOr(innerStar, and1, and2);
          return paramLiaStar2DNF(or, activated);
        }
        return simplifyDNF(le);
//        return le;
      }
      case LLT-> {
        final LiaLtImpl lt = (LiaLtImpl) formula;
//        lt.operand1 = paramLiaStar2DNF(lt.operand1, activated);
//        lt.operand2 = paramLiaStar2DNF(lt.operand2, activated);
        final boolean innerStar = lt.innerStar;
        if (activated && (lt.operand1.getType() == LITE || lt.operand2.getType() == LITE)) {
          final LiaIteImpl ite = (LiaIteImpl) (lt.operand1.getType() == LITE ? lt.operand1 : lt.operand2);
          final LiaStar other = ite == lt.operand1 ? lt.operand2 : lt.operand1;

          final LiaAndImpl and1 = (LiaAndImpl)
              mkAnd(innerStar, ite.cond, mkLt(innerStar, other, ite.operand1));
          final LiaAndImpl and2 = (LiaAndImpl)
              mkAnd(innerStar, mkNot(innerStar, ite.cond), mkLt(innerStar, other, ite.operand2));
          final LiaStar or = mkOr(innerStar, and1, and2);
          return paramLiaStar2DNF(or, activated);
        }
        return simplifyDNF(lt);
//        return lt;
      }
      // Simple terms: PLUS, MULT, ITE
      case LPLUS -> {
        final LiaPlusImpl plus = (LiaPlusImpl) formula;
//        plus.operand1 = paramLiaStar2DNF(plus.operand1, activated);
//        plus.operand2 = paramLiaStar2DNF(plus.operand2, activated);
//        return simplifyDNF(plus);
        return plus;
      }
      case LMULT -> {
        final LiaMulImpl mult = (LiaMulImpl) formula;
//        mult.operand1 = paramLiaStar2DNF(mult.operand1, activated);
//        mult.operand2 = paramLiaStar2DNF(mult.operand2, activated);
//        return simplifyDNF(mult);
        return mult;
      }
      case LITE -> {
        final LiaIteImpl ite = (LiaIteImpl) formula;
//        ite.cond = paramLiaStar2DNF(ite.cond, activated);
//        ite.operand1 = paramLiaStar2DNF(ite.operand1, activated);
//        ite.operand2 = paramLiaStar2DNF(ite.operand2, activated);
//        return simplifyDNF(ite);
        return ite;
//        return ite;
      }
      // SUM
//      case LSUM -> {
//        final LiasumImpl sum = (LiasumImpl) formula;
//        // Judge whether in a param LiaStar.
//        // If not, stop recursion, else, set activated for DNF rewrite
//        if (sum.constraints.isLia()) {
//          final Set<String> varSet = sum.constraints.collectVarSet();
//          if (all(varSet, v -> sum.innerVector.contains(v))) return sum;
//          activated = true;
//        }
//        sum.constraints = paramLiaStar2DNF(sum.constraints, activated);
//        return sum;
//      }
      // Atomic ones: FUNC
      case LFUNC -> {
        assert false;
        return null;
//        final LiaFuncImpl func = (LiaFuncImpl) formula;
//        func.var = paramLiaStar2DNF(func.var, activated);
//        return simplifyDNF(func);
      }
      default -> {
        return formula;
      }
    }
  }

  /**
   * Remove vars coming from outer stars or outermost.
   *
   * @param vars specify the current set of parameters;
   *             in the top-level call, this often refers to the set of outermost vars
   */
  public void removeFVsFromInnerVector(Set<String> vars) {}

  public Set<String> collectParamNames() {
    return new HashSet<>();
  }

  public Set<LiaVarImpl> collectParams() {
    return new HashSet<>();
  }

  public LiaStar removeParameter() {
    return this;
  }

  /**
   * Given "(... v ...) in {(... u ...) | u = ite(p1 /\ p2, e1 * e2, 0) /\ ...}*",
   * if p1 and e1 contain no inner var while p2 and e2 contain inner vars,
   * then p1 and e1 can be "pushed up"
   * (i.e. the formula turns into
   * <br/>
   * "(... v' ...) in {(... u ...) | u = ite(p2, e2, 0) /\ ...}*
   * /\ v = ite(p1, e1 * v', 0)")
   */
  public LiaStar pushUpParameter(Set<String> newVars) {
    return this;
  }

  public LiaStar simplifyIte() {
    return this;
  }

  public static List<LiaStar> decomposeDNF(LiaStar f) {
    if(f.getType() == LiaOpType.LOR) {
      LiaOrImpl tmp = (LiaOrImpl) f;
      List<LiaStar> result = decomposeDNF(tmp.operand1);
      result.addAll(decomposeDNF(tmp.operand2));
      return result;
    } else {
      List<LiaStar> result = new ArrayList<>();
      result.add(f);
      return result;
    }
  }

  public boolean overLap(Set<String> s1, Set<String> s2) {
    for(String s : s1) {
      if(s2.contains(s)) {
        return true;
      }
    }
    return false;
  }

  public static void decomposeConjunction(LiaStar formula, List<LiaStar> literals) {
    if(formula.getType() == LiaOpType.LAND) {
      LiaAndImpl tmp = (LiaAndImpl) formula;
      decomposeConjunction(tmp.operand1, literals);
      decomposeConjunction(tmp.operand2, literals);
    } else {
      literals.add(formula);
    }
  }

  public static boolean isTrue(LiaStar formula) {
    switch(formula.getType()) {
      case LNOT -> {
        LiaNotImpl tmp = (LiaNotImpl) formula;
        return isFalse(tmp.operand);
      }
      case LLT -> {
        LiaLtImpl tmp = (LiaLtImpl) formula;
        LiaStar operand1 = tmp.operand1;
        LiaStar operand2 = tmp.operand2;
        if((operand1 instanceof LiaConstImpl) && (operand2 instanceof LiaConstImpl)) {
          long const1 = ((LiaConstImpl)operand1).value;
          long const2 = ((LiaConstImpl)operand2).value;
          return (const1 < const2);
        }
        break;
      }
      case LLE -> {
        LiaLeImpl tmp = (LiaLeImpl) formula;
        LiaStar operand1 = tmp.operand1;
        LiaStar operand2 = tmp.operand2;
        if(operand1.equals(operand2)) {
          return true;
        }
        if((operand1 instanceof LiaConstImpl) && (operand2 instanceof LiaConstImpl)) {
          long const1 = ((LiaConstImpl)operand1).value;
          long const2 = ((LiaConstImpl)operand2).value;
          return (const1 <= const2);
        }
        break;
      }
      case LEQ -> {
        LiaEqImpl tmp = (LiaEqImpl) formula;
        LiaStar operand1 = tmp.operand1;
        LiaStar operand2 = tmp.operand2;
        return operand1.equals(operand2);
      }
      default -> {
        return false;
      }
    }
    return false;
  }

  public static  boolean isFalse(LiaStar formula) {
    switch(formula.getType()) {
      case LNOT -> {
        LiaNotImpl tmp = (LiaNotImpl) formula;
        return isTrue(tmp.operand);
      }
      case LLT -> {
        LiaLtImpl tmp = (LiaLtImpl) formula;
        LiaStar operand1 = tmp.operand1;
        LiaStar operand2 = tmp.operand2;
        if(operand1.equals(operand2)) {
          return true;
        }
        if((operand1 instanceof LiaConstImpl) && (operand2 instanceof LiaConstImpl)) {
          long const1 = ((LiaConstImpl)operand1).value;
          long const2 = ((LiaConstImpl)operand2).value;
          return !(const1 < const2);
        }
        break;
      }
      case LLE -> {
        LiaLeImpl tmp = (LiaLeImpl) formula;
        LiaStar operand1 = tmp.operand1;
        LiaStar operand2 = tmp.operand2;
        if((operand1 instanceof LiaConstImpl) && (operand2 instanceof LiaConstImpl)) {
          long const1 = ((LiaConstImpl)operand1).value;
          long const2 = ((LiaConstImpl)operand2).value;
          return !(const1 <= const2);
        }
        break;
      }
      case LEQ -> {
        LiaEqImpl tmp = (LiaEqImpl) formula;
        LiaStar operand1 = tmp.operand1;
        LiaStar operand2 = tmp.operand2;
        if((operand1 instanceof LiaConstImpl) && (operand2 instanceof LiaConstImpl)) {
          long const1 = ((LiaConstImpl)operand1).value;
          long const2 = ((LiaConstImpl)operand2).value;
          return !(const1 == const2);
        }
        break;
      }
      default -> {
        return false;
      }
    }
    return false;
  }

  // return null when the conjunction is false
  // return 1 when the conjunction is true
  // Otherwise, return simplified formula
  public static LiaStar simplifyConj(LiaStar formula) {
    List<LiaStar> literals = new ArrayList<>();
    decomposeConjunction(formula, literals);
    LiaStar result = null;
    for(LiaStar lit : literals) {
      if(isFalse(lit)) {
        return null;
      }
      if(lit instanceof LiaNotImpl) {
        LiaNotImpl tmp = (LiaNotImpl) lit;
        if(literals.contains(tmp.operand)) {
          return null;
        }
      }
      if(!isTrue(lit)) {
        result = (result == null) ? lit : mkAnd(formula.innerStar, result, lit);
      }
    }
    return (result == null) ? mkConst(formula.innerStar, 1) : result;
  }

  // return 0 = 0 when formula is true
  // return 0 = 1 when formula is false
  // Otherwise, return simplified formula
  public static LiaStar simplifyDNF(LiaStar formula) {
    List<LiaStar> conjunctions = decomposeDNF(formula);
    LiaStar result = null;
    for(LiaStar conj : conjunctions) {
      LiaStar simpleConj = simplifyConj(conj);
      if(simpleConj instanceof LiaConstImpl) {
        return mkEq(formula.innerStar, mkConst(formula.innerStar, 0), mkConst(formula.innerStar, 0));
      } else if(simpleConj != null) {
        result = (result == null) ? simpleConj : mkOr(formula.innerStar, result, simpleConj);
      }
    }
    return (result == null) ? mkEq(formula.innerStar, mkConst(formula.innerStar, 0), mkConst(formula.innerStar, 1)) : result;
  }

  public static LiaVarImpl renameVar(LiaVarImpl var, String newName) {
    return mkVar(var.innerStar, newName, var.getValueType());
  }

  public static LiaStar replaceVars(LiaStar formula, Set<String> vars) {
    return replaceVars(formula, vars, new HashMap<>());
  }

  public static LiaStar replaceVars(LiaStar formula, Set<String> vars, Map<String, String> replacementMap) {
    if (formula == null) {
      return null;
    }
    return formula.transformPostOrder(f -> {
      if (f instanceof LiaVarImpl var) {
        if (vars.contains(var.varName)) {
          if (replacementMap.containsKey(var.varName)) {
            return renameVar(var, replacementMap.get(var.varName));
          } else {
            final String newName = newVarName();
            replacementMap.put(var.varName, newName);
            return renameVar(var, newName);
          }
        }
      } else if (f instanceof LiaSumImpl sum) {
        final List<String> newOuterVars = new ArrayList<>();
        for (String var : sum.outerVector) {
          if (vars.contains(var)) {
            if (replacementMap.containsKey(var)) {
              newOuterVars.add(replacementMap.get(var));
            } else {
              final String newName = newVarName();
              replacementMap.put(var, newName);
              newOuterVars.add(newName);
            }
          }
        }
        return mkSum(sum.innerStar, newOuterVars, sum.innerVector, sum.constraints);
      }
      return f;
    });
  }

  void decomposePluses(LiaStar formula, Set<LiaStar> items) {
    if(formula instanceof LiaPlusImpl) {
      LiaPlusImpl tmp = (LiaPlusImpl) formula;
      decomposePluses(tmp.operand1, items);
      decomposePluses(tmp.operand2, items);
    } else {
      items.add(formula);
    }
  }


  static void decomposeMults(LiaStar formula, Collection<LiaStar> items) {
    if (formula instanceof LiaMulImpl tmp) {
      decomposeMults(tmp.operand1, items);
      decomposeMults(tmp.operand2, items);
    } else {
      items.add(formula);
    }
  }


  // determine if formula is ite(cond, op, 0) or ite(cond, 0, op)
  // if so, return true, array[0] = cond and array[1] = op
  public static boolean ishasZeroIte(boolean isInnerStar, LiaStar formula, LiaStar[] array) {
    if(formula == null) {
      return false;
    }
    if(!(formula instanceof LiaIteImpl)) {
      return false;
    }
    LiaIteImpl ite = (LiaIteImpl) formula;
    if(ite.operand1.isConstV(0)) {
      array[0] = mkNot(isInnerStar, ite.cond.deepcopy());
      array[1] = ite.operand2.deepcopy();
      return true;
    } else if(ite.operand2.isConstV(0)) {
      array[0] = ite.cond.deepcopy();
      array[1] = ite.operand1.deepcopy();
      return true;
    } else {
      return false;
    }
  }

  public static LiaStar mkConjunction(boolean innerStar, List<LiaStar> conjs) {
    LiaStar result = null;
    for (LiaStar lit : conjs) {
      if (lit != null) {
        result = (result == null) ? lit.deepcopy() : mkAnd(innerStar, result, lit.deepcopy());
      }
    }
    if (result == null) {
      return mkTrue(innerStar);
    } else {
      return result;
    }
  }

  public static LiaStar mkDisjunction(boolean innerStar, List<LiaStar> disjs) {
    LiaStar result = null;
    for (LiaStar lit : disjs) {
      if (lit != null) {
        result = (result == null) ? lit.deepcopy() : mkOr(innerStar, result, lit.deepcopy());
      }
    }
    if (result == null) {
      return mkFalse(innerStar);
    } else {
      return result;
    }
  }

  public static LiaStar allOuterVarEqZero(List<String> outerVector) {
    LiaStar result = null;
    for (String var : outerVector) {
      LiaStar tmp = mkEq(false, mkVar(false, var), mkConst(false, 0));
      result = (result == null) ? tmp : mkAnd(false, result, tmp);
    }
    return result;
  }


  public static LiaStar innerVarEqOuterVar(List<String> outerVector, List<String> innerVector) {
    LiaStar result = null;
    for(int i = 0; i < outerVector.size(); ++ i) {
      String outVar = outerVector.get(i);
      String inVar = innerVector.get(i);
      LiaStar tmp = mkEq(false, mkVar(false, outVar), mkVar(false, inVar));
      result = (result == null) ? tmp : mkAnd(false, result, tmp);
    }
    return result;
  }


  public static LiaStar expandStarWithUnderapp(LiaStar formula, int m) {
    switch (formula.getType()) {
      case LCONST: case LEQ: case LNOT: case LITE: case LLT: case LLE: case LVAR: case LMULT: case LPLUS: {
        return formula;
      }
      case LOR: {
        LiaOrImpl orLia = (LiaOrImpl) formula;
        return mkOr(false, expandStarWithUnderapp(orLia.operand1, m), expandStarWithUnderapp(orLia.operand2, m));
      }
      case LAND: {
        LiaAndImpl andLia = (LiaAndImpl) formula;
        return mkAnd(false, expandStarWithUnderapp(andLia.operand1, m), expandStarWithUnderapp(andLia.operand2, m));
      }
      case LSUM: {
        LiaSumImpl sumLia = (LiaSumImpl) formula;
        LiaStar case1 = allOuterVarEqZero(sumLia.outerVector);
        LiaStar case2Part1 = innerVarEqOuterVar(sumLia.outerVector, sumLia.innerVector);
        LiaStar case2Part2 = expandStarWithUnderapp(sumLia.constraints, m);
        return mkOr(false, case1, mkAnd(false, case2Part1, case2Part2));
      }
      default: {
        assert false;
        return null;
      }
    }
  }

  public static LiaStar constructConjunctions(boolean innerStar, List<LiaStar> formulas) {
    LiaStar result = null;
    for (LiaStar f : formulas) {
      if (f == null) continue;
      result = (result == null) ? f : LiaStar.mkAnd(innerStar, result, f);
    }
    return result;
  }

  public static LiaStar replaceFreeAndBoundVars(LiaStar formula, Map<String, String> oldToNewVars) {
    if (formula == null) {
      return formula;
    }
    switch (formula.getType()) {
      case LEQ -> {
        LiaEqImpl tmp = (LiaEqImpl) formula;
        LiaStar left = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar right = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkEq(tmp.innerStar, left, right);
      }
      case LLE -> {
        LiaLeImpl tmp = (LiaLeImpl) formula;
        LiaStar left = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar right = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkLe(tmp.innerStar, left, right);
      }
      case LLT -> {
        LiaLtImpl tmp = (LiaLtImpl) formula;
        LiaStar left = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar right = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkLt(tmp.innerStar, left, right);
      }
      case LOR -> {
        LiaOrImpl tmp = (LiaOrImpl) formula;
        LiaStar left = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar right = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkOr(tmp.innerStar, left, right);
      }
      case LAND -> {
        LiaAndImpl tmp = (LiaAndImpl) formula;
        LiaStar left = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar right = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkAnd(tmp.innerStar, left, right);
      }
      case LITE -> {
        LiaIteImpl tmp = (LiaIteImpl) formula;
        LiaStar cond = replaceFreeAndBoundVars(tmp.cond, oldToNewVars);
        LiaStar operand1 = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar operand2 = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkIte(tmp.innerStar, cond, operand1, operand2);
      }
      case LNOT -> {
        LiaNotImpl tmp = (LiaNotImpl) formula;
        LiaStar operand = replaceFreeAndBoundVars(tmp.operand, oldToNewVars);
        return LiaStar.mkNot(tmp.innerStar, operand);
      }
      case LVAR -> {
        LiaVarImpl tmp = (LiaVarImpl) formula;
        if (oldToNewVars.containsKey(tmp.varName)) {
          return renameVar(tmp, oldToNewVars.get(tmp.varName));
        }
        return tmp.deepcopy();
      }
      case LMULT -> {
        LiaMulImpl tmp = (LiaMulImpl) formula;
        LiaStar operand1 = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar operand2 = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkMul(tmp.innerStar, operand1, operand2);
      }
      case LPLUS -> {
        LiaPlusImpl tmp = (LiaPlusImpl) formula;
        LiaStar operand1 = replaceFreeAndBoundVars(tmp.operand1, oldToNewVars);
        LiaStar operand2 = replaceFreeAndBoundVars(tmp.operand2, oldToNewVars);
        return LiaStar.mkPlus(tmp.innerStar, operand1, operand2);
      }
      case LSUM -> {
        LiaSumImpl sum = (LiaSumImpl) formula;
        List<String> newOuterVars = new ArrayList<>();
        for (String outerVar : sum.outerVector) {
          newOuterVars.add(oldToNewVars.getOrDefault(outerVar, outerVar));
        }
        List<String> newInnerVars = new ArrayList<>();
        for (String innerVar : sum.innerVector) {
          newInnerVars.add(oldToNewVars.getOrDefault(innerVar, innerVar));
        }
        return LiaStar.mkSum(formula.innerStar, newOuterVars, newInnerVars, replaceFreeAndBoundVars(sum.constraints, oldToNewVars));
      }
      default -> {
        return formula;
      }
    }
  }

  public static LiaStar renameAllVars(LiaStar expr, Set<String> exceptVars) {
    Set<String> allVars = expr.getVars();
    Map<String, String> oldNameToNewName = new HashMap<>();
    for (String oldName : allVars) {
      if (!exceptVars.contains(oldName) && !oldNameToNewName.containsKey(oldName)) {
        oldNameToNewName.put(oldName, newVarName());
      }
    }
    return replaceFreeAndBoundVars(expr, oldNameToNewName);
  }

  public LiaStar subformulaWithoutStar() {
    return this;
  }

  /**
   * Given a unary function, transform the LIA* formula in post-order.
   * The function represents the transformation applied to each formula.
   * The method should create new instances of LIA* formulas
   * to avoid modification of fields.
   * The transformation should make sure that
   * the outermost star is not eliminated
   * (otherwise the "innerStar" flag can be wrong and confusing).
   */
  public abstract LiaStar transformPostOrder(Function<LiaStar, LiaStar> transformer);

  /**
   * Given a binary function, transform the LIA* formula in post-order.
   * The function represents the transformation applied to each formula,
   * while it accepts a second argument indicating the parent formula.
   * The method should create new instances of LIA* formulas
   * to avoid modification of fields.
   * The transformation should make sure that
   * the outermost star is not eliminated
   * (otherwise the "innerStar" flag can be wrong and confusing).
   * @param transformer its first argument is the formula to be transformed,
   *                    and its second argument is the parent formula
   * @param parent the parent formula, which is passed to transformer
   */
  protected abstract LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent);

  /**
   * Transform a LIA* formula, assuming that the formula is top-level.
   * @see #transformPostOrder(BiFunction, LiaStar)
   */
  public LiaStar transformPostOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer) {
    return transformPostOrder(transformer, null);
  }

  /**
   * The pre-order version of <code>transformPostOrder</code>.
   * When a LiaStar object invokes this method,
   * it applies the transformer to itself.
   * If the transformer returns <code>null</code>,
   * the object is not updated and its sub-nodes are recursively transformed.
   * Otherwise, this method returns the return value of transformer
   * and does not transform sub-nodes.
   * @see #transformPostOrder(Function)
   */
  public LiaStar transformPreOrder(Function<LiaStar, LiaStar> transformer) {
    return transformPreOrder((f, parent) -> transformer.apply(f), null);
  }

  /**
   * The pre-order version of <code>transformPostOrder</code>.
   * When a LiaStar object invokes this method,
   * it applies the transformer to itself.
   * If the transformer returns <code>null</code>,
   * the object is not updated and its sub-nodes are recursively transformed.
   * Otherwise, this method returns the return value of transformer
   * and does not transform sub-nodes.
   * @see #transformPostOrder(BiFunction, LiaStar)
   */
  protected LiaStar transformPreOrder(
          BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent) {
    final LiaStar result = transformer.apply(this, parent);
    // "this" is transformed; sub-nodes will not be transformed
    if (result != null) return result;
    // "this" is unchanged; transform sub-nodes
    return transformPreOrderRecursive(transformer, parent);
  }

  /** Handle a null transformation result (i.e. transform sub-nodes). */
  protected abstract LiaStar transformPreOrderRecursive(BiFunction<LiaStar, LiaStar, LiaStar> transformer, LiaStar parent);

  /**
   * The pre-order version of <code>transformPostOrder</code>.
   * When a LiaStar object invokes this method,
   * it applies the transformer to itself.
   * If the transformer returns <code>null</code>,
   * the object is not updated and its sub-nodes are recursively transformed.
   * Otherwise, this method returns the return value of transformer
   * and does not transform sub-nodes.
   * @see #transformPostOrder(BiFunction)
   */
  public LiaStar transformPreOrder(BiFunction<LiaStar, LiaStar, LiaStar> transformer) {
    return transformPreOrder(transformer, null);
  }

  /**
   * Return the list of direct sub-nodes of this node.
   * Note that the returned list can be immutable.
   */
  public List<LiaStar> subNodes() {
    return List.of();
  }

  @Override
  public String toString() {
    PrettyBuilder builder = new PrettyBuilder();
    //builder.setAutoNewLine(50);
    prettyPrint(builder);
    return builder.toString();
  }


  protected static void prettyPrintBinaryOp(PrettyBuilder builder,
                                            LiaStar operand1, LiaStar operand2,
                                            boolean needsParen1, boolean needsParen2,
                                            String op) {
    prettyPrintBinaryOp(builder, operand1, operand2,
            needsParen1, needsParen2, false, op);
  }

  protected static void prettyPrintBinaryOp(PrettyBuilder builder,
                                            LiaStar operand1, LiaStar operand2,
                                            boolean needsParen1, boolean needsParen2,
                                            boolean autoNewLine,
                                            String op) {
    boolean multiLine1 = operand1.isPrettyPrintMultiLine();
    boolean multiLine2 = operand2.isPrettyPrintMultiLine();
    prettyPrintBinaryOp(builder, operand1, operand2,
            needsParen1, needsParen2, multiLine1, multiLine2,
            autoNewLine, op);
  }

  /** autoNewLine: whether auto new line between operands is allowed */
  protected static void prettyPrintBinaryOp(PrettyBuilder builder,
                                            LiaStar operand1, LiaStar operand2,
                                            boolean needsParen1, boolean needsParen2,
                                            boolean multiLine1, boolean multiLine2,
                                            boolean autoNewLine,
                                            String op) {
    if (needsParen1) {
      if (multiLine1) {
        builder.print("(").indent(4).println();
        operand1.prettyPrint(builder);
        builder.indent(-4).println().print(")");
      } else {
        builder.print("(");
        operand1.prettyPrint(builder);
        builder.print(")");
      }
    } else
      operand1.prettyPrint(builder);

    if (multiLine1 && multiLine2) {
      builder.println().println(op);
    } else if (multiLine1) {
      builder.print(op);
    } else if (multiLine2) {
      builder.println(op);
    } else {
      builder.print(op);
    }

    // the only way to trigger auto new line
    if (autoNewLine) {
      builder.setAutoNewLineEnabled(true);
      builder.print("");
      builder.setAutoNewLineEnabled(false);
    }

    if (needsParen2) {
      if (multiLine2) {
        builder.print("(").indent(4).println();
        operand2.prettyPrint(builder);
        builder.indent(-4).println().print(")");
      } else {
        builder.print("(");
        operand2.prettyPrint(builder);
        builder.print(")");
      }
    } else
      operand2.prettyPrint(builder);
  }

  protected abstract void prettyPrint(PrettyBuilder builder);

  protected abstract boolean isPrettyPrintMultiLine();

  public static List<String> newNVarNames(int n) {
    List<String> result = new ArrayList<>();
    for (int i = 0; i < n; ++ i) {
      result.add(newVarName());
    }
    return result;
  }

  public static LiaStar eqNVectors(boolean innerStar, List<String> leftVector, List<List<String>> rightVectors) {
    LiaStar result = null;
    for (int i = 0; i < leftVector.size(); ++ i) {
      LiaVarImpl leftVar = mkVar(innerStar, leftVector.get(i));
      LiaStar rightPlusFormula = null;
      for (int j = 0; j < rightVectors.size(); ++ j) {
        LiaVarImpl rightVar = mkVar(innerStar, rightVectors.get(j).get(i));
        rightPlusFormula = (rightPlusFormula == null) ? rightVar : mkPlus(innerStar, rightPlusFormula, rightVar);
      }
      LiaStar leftEqRight = mkEq(innerStar, leftVar, rightPlusFormula);
      result = (result == null) ? leftEqRight : mkAnd(innerStar, result, leftEqRight);
    }
    return result;
  }

  public static LiaStar calculateUnderApprox(LiaStar formula, int n) {
    LiaStar result = spanLiastarByNVectors(formula, n);
    return expandStarWithUnderapp(result, 1);
  }

  public static LiaStar spanLiastarByNVectors(LiaStar formula, int n) {
    switch (formula.getType()) {
      case LPLUS: case LMULT: case LVAR: case LLT: case LLE: case LEQ: case LDIV: case LCONST: case LSTRING: case LFUNC: {
        return formula;
      }
      case LOR: {
        LiaOrImpl orFormula = (LiaOrImpl) formula;
        orFormula.operand1 = spanLiastarByNVectors(orFormula.operand1, n);
        orFormula.operand2 = spanLiastarByNVectors(orFormula.operand2, n);
        return orFormula;
      }
      case LAND: {
        LiaAndImpl andFormula = (LiaAndImpl) formula;
        andFormula.operand1 = spanLiastarByNVectors(andFormula.operand1, n);
        andFormula.operand2 = spanLiastarByNVectors(andFormula.operand2, n);
        return andFormula;
      }
      case LITE: {
        LiaIteImpl iteFormula = (LiaIteImpl) formula;
        iteFormula.cond = spanLiastarByNVectors(iteFormula.cond, n);
        return iteFormula;
      }
      case LNOT: {
        LiaNotImpl notFormula = (LiaNotImpl) formula;
        notFormula.operand = spanLiastarByNVectors(notFormula.operand, n);
        return notFormula;
      }
      case LSUM: {
        LiaSumImpl sumFormula = (LiaSumImpl) formula;
        List<List<String>> newNameVectors = new ArrayList<>();
        for (int i = 0; i < n; ++ i) {
          newNameVectors.add(newNVarNames(sumFormula.outerVector.size()));
        }
        LiaStar outerEqSumOfNVectors = eqNVectors(sumFormula.innerStar, sumFormula.outerVector, newNameVectors);
        LiaStar newLiasumFormulas = null;
        for (int i = 0; i < n; ++ i) {
          List<String> newOuterNames = newNameVectors.get(i);
          LiaSumImpl tmpSumFormula = (LiaSumImpl) sumFormula.deepcopy();
          tmpSumFormula.outerVector = newOuterNames;
          tmpSumFormula = (LiaSumImpl) renameAllInnerVars(tmpSumFormula);
          tmpSumFormula.constraints = spanLiastarByNVectors(tmpSumFormula.constraints, n);
          newLiasumFormulas = (newLiasumFormulas == null) ? tmpSumFormula : mkAnd(sumFormula.innerStar, newLiasumFormulas, tmpSumFormula);
        }
        LiaStar result = mkAnd(sumFormula.innerStar, outerEqSumOfNVectors, newLiasumFormulas);
        return result;
      }
      default: {
        assert false;
        System.err.println("not support liaterm");
        return null;
      }
    }
  }

  public static LiaStar renameAllInnerVars(LiaStar formula) {
    switch (formula.getType()) {
      case LFUNC: case LSTRING: case LCONST: case LDIV: case LEQ: case LLE: case LLT: case LVAR: case LMULT: case LPLUS: {
         return formula;
      }
      case LOR: {
        LiaOrImpl orFormula = (LiaOrImpl) formula;
        orFormula.operand1 = renameAllInnerVars(orFormula.operand1);
        orFormula.operand2 = renameAllInnerVars(orFormula.operand2);
        return orFormula;
      }
      case LAND: {
        LiaAndImpl andFormula = (LiaAndImpl) formula;
        andFormula.operand1 = renameAllInnerVars(andFormula.operand1);
        andFormula.operand2 = renameAllInnerVars(andFormula.operand2);
        return andFormula;
      }
      case LITE: {
        LiaIteImpl iteFormula = (LiaIteImpl) formula;
        iteFormula.cond = renameAllInnerVars(iteFormula.cond);
        return iteFormula;
      }
      case LNOT: {
        LiaNotImpl notFormula = (LiaNotImpl) formula;
        notFormula.operand = renameAllInnerVars(notFormula.operand);
        return notFormula;
      }
      case LSUM: {
        LiaSumImpl sumFormula = (LiaSumImpl) formula;
        Map<String, String> renameMapping = new HashMap<>();
        for (String innerVarName : sumFormula.innerVector) {
          renameMapping.put(innerVarName, newVarName());
        }
        sumFormula = (LiaSumImpl) replaceFreeAndBoundVars(sumFormula, renameMapping);
        sumFormula.constraints = renameAllInnerVars(sumFormula.constraints);
        return sumFormula;
      }
      default: {
        assert false;
        System.err.println("not support liaterm");
        return null;
      }
    }
  }
}
