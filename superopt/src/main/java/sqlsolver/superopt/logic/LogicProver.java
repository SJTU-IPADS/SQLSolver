package sqlsolver.superopt.logic;

import com.microsoft.z3.*;
import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.Lazy;
import sqlsolver.common.utils.MapSupport;
import sqlsolver.superopt.constraint.Constraint;
import sqlsolver.superopt.fragment.Proj;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.fragment.Op;
import sqlsolver.superopt.fragment.Symbols;
import sqlsolver.superopt.uexpr.*;

import java.util.*;

import static com.google.common.collect.Sets.difference;
import static sqlsolver.common.utils.ArraySupport.*;
import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.superopt.logic.LogicSupport.PROVER_DISABLE_INTEGRITY_CONSTRAINTS_THEOREM;

class LogicProver {
  private final Substitution rule;
  private final UExprTranslationResult uExprs;
  private final Context z3;
  private final List<BoolExpr> constraints;
  private final Cache cache;

  private final boolean disableIntegrityConstraintsTheorem;

  private int callCount;

  private static ThreadLocal<Integer> tupleId = new ThreadLocal<Integer>() {
    public Integer initialValue() {
      return 0;
    }
  };

  Integer newTupleId() {
    tupleId.set(tupleId.get() + 1);
    return tupleId.get();
  }

  LogicProver(UExprTranslationResult uExprs, Context z3, int tweaks) {
    this.rule = uExprs.rule();
    this.uExprs = uExprs;
    this.z3 = z3;
    this.constraints = new ArrayList<>();
    this.cache = new Cache();
    this.disableIntegrityConstraintsTheorem =
        (tweaks & PROVER_DISABLE_INTEGRITY_CONSTRAINTS_THEOREM) != 0;
  }

  VerificationResult proveEqNotNeedLia() {
    if (LogicSupport.isFastRejected(uExprs)) return VerificationResult.NEQ;

    trConstraints();
    final UTerm srcTerm = uExprs.sourceExpr(), tgtTerm = uExprs.targetExpr();
    try {
      return proveEq0NotNeedLia(srcTerm, tgtTerm);
    } catch (Exception e) {
//      e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  VerificationResult proveEq() {
    if (LogicSupport.isFastRejected(uExprs)) return VerificationResult.NEQ;

    trConstraints();
    // master: the side with more bounded variables, or the source side if the numbers are equal
    // slave: the side with less bounded variables, or the target side if the numbers are equal
    final UTerm srcTerm = uExprs.sourceExpr(), tgtTerm = uExprs.targetExpr();

    return proveEq0(
        LogicSupport.getMaster(srcTerm, tgtTerm), LogicSupport.getSlave(srcTerm, tgtTerm));
  }

  ArithExpr myTrUtermNotNeedLia(UTerm exp) throws Exception {
    UKind kind = exp.kind();
    switch(kind) {
      case MULTIPLY:
      case ADD: {
        List<UTerm> subterms = exp.subTerms();
        ArithExpr result = null;
        for (UTerm term : subterms) {
          ArithExpr tmp = myTrUtermNotNeedLia(term);
          if (result == null) {
            result = tmp;
          } else if (kind == UKind.ADD) {
            result = z3.mkAdd(result, tmp);
          } else if (kind == UKind.MULTIPLY) {
            result = z3.mkMul(result, tmp);
          }
        }
        return result;
      }
      case PRED: case TABLE: {
        BoolExpr cond = (BoolExpr) trAtom(exp, true);
        return (ArithExpr) z3.mkITE(cond, one(), zero());
      }
      case SQUASH: case NEGATION: {
        BoolExpr cond = trAsSet(exp);
        return (ArithExpr) z3.mkITE(cond, one(), zero());
      }
      default: {
       throw new Exception("no such out op");
      }
    }
  }

  UTerm makeBoundVarUnique(UTerm expr, HashSet<String> tupleNames) {
    UKind kind = expr.kind();
    switch (kind) {
      case ADD : case MULTIPLY: {
        ArrayList<UTerm> subterms = new ArrayList<>();
        for(UTerm t : expr.subTerms()) {
          UTerm tmp = makeBoundVarUnique(t, tupleNames);
          subterms.add(tmp);
        }
        return (kind == UKind.ADD) ? UAdd.mk(subterms) : UMul.mk(subterms);
      }
      case SUMMATION: {
        USum sumExp = (USum) expr;
        HashSet<UVar> boundVars = new HashSet<>(sumExp.boundedVars());
        UTerm body = sumExp.body();
        for(UVar v : sumExp.boundedVars()) {
          String name = v.toString();
          if(tupleNames.contains(name)) {
            UVar newTuple = UVar.mkBase(UName.mk("tt"+newTupleId()));
            boundVars.remove(v);
            boundVars.add(newTuple);
            body.replaceVarInplace(v, newTuple, false);
            SchemaDesc desc =  uExprs.schemaOf(v);
            uExprs.setVarSchema(newTuple, desc);
          } else {
            tupleNames.add(name);
          }
        }

        body = makeBoundVarUnique(body, tupleNames);
        return USum.mk(boundVars, body);
      }
      case NEGATION: {
        UTerm body = makeBoundVarUnique(((UNeg)expr).body(), tupleNames);
        return UNeg.mk(body);
      }
      case SQUASH: {
        UTerm body = makeBoundVarUnique(((USquash)expr).body(), tupleNames);
        return USquash.mk(body);
      }
      default: {
        return expr;
      }
    }
  }

  private void trConstraints() {
    final Symbols srcSide = rule._0().symbols();
    final Set<String> touched = new HashSet<>();

    for (var tableSym : srcSide.symbolsOf(sqlsolver.superopt.fragment.Symbol.Kind.TABLE)) {
      final String tableName = uExprs.tableNameOf(tableSym);
      if (touched.add(tableName)) trTableBasic(tableName);
    }
    touched.clear();

    for (var attrsSym : srcSide.symbolsOf(sqlsolver.superopt.fragment.Symbol.Kind.ATTRS)) {
      final String attrsName = uExprs.attrsNameOf(attrsSym);
      if (touched.add(attrsName)) trAttrsBasic(attrsName);
    }
    touched.clear();

    for (Constraint c : rule.constraints().ofKind(Constraint.Kind.TableEq)) {
      if (c.symbols()[0].ctx() == srcSide && c.symbols()[1].ctx() == srcSide) trTableEq(c);
    }

    for (Constraint c : rule.constraints().ofKind(Constraint.Kind.AttrsEq)) {
      if (c.symbols()[0].ctx() == srcSide && c.symbols()[1].ctx() == srcSide) trAttrsEq(c);
    }

    for (Constraint c : rule.constraints().ofKind(Constraint.Kind.AttrsSub)) {
      trAttrSub(c);
    }

    trOutVarEq(uExprs.sourceOutVar(), uExprs.targetOutVar());


    if (!disableIntegrityConstraintsTheorem) {
      for (Constraint c : rule.constraints().ofKind(Constraint.Kind.NotNull)) {
        if (touched.add(uExprs.tableNameOf(c.symbols()[0]) + uExprs.attrsNameOf(c.symbols()[1])))
          trNotNull(c);
      }
      touched.clear();

      for (Constraint c : rule.constraints().ofKind(Constraint.Kind.Unique)) {
        if (touched.add(uExprs.tableNameOf(c.symbols()[0]) + uExprs.attrsNameOf(c.symbols()[1])))
          trUnique(c);
      }
      touched.clear();

      for (Constraint c : rule.constraints().ofKind(Constraint.Kind.Reference)) {
        final String t0 = uExprs.tableNameOf(c.symbols()[0]);
        final String a0 = uExprs.attrsNameOf(c.symbols()[1]);
        final String t1 = uExprs.tableNameOf(c.symbols()[2]);
        final String a1 = uExprs.attrsNameOf(c.symbols()[3]);
        if (touched.add(t0 + a0 + t1 + a1)) trReferences(c);
      }
      touched.clear();
    }

  }

  private void trTableBasic(String tableName) {
    final FuncDecl tableFunc = tableFunc(tableName);
    final Expr tuple = z3.mkConst("x", tupleSort());
    final Expr[] vars = new Expr[] {tuple};
    final Expr body = z3.mkGe((ArithExpr) tableFunc.apply(tuple), zero());
    final BoolExpr assertion = mkForall(vars, body);
    constraints.add(assertion);
  }

  private void trAttrsBasic(String attrsName) {
    final FuncDecl projFunc = projFunc(attrsName);
    final Expr tuple = z3.mkConst("x", tupleSort());
    final IntExpr schema = z3.mkIntConst("s");
    final Expr[] vars = new Expr[] {tuple, schema};
    final BoolExpr projIsNull = z3.mkEq(projFunc.apply(schema, tuple), nullTuple());
    final BoolExpr tupleIsNull = z3.mkEq(tuple, nullTuple());
    final BoolExpr body = z3.mkImplies(tupleIsNull, projIsNull);
    final BoolExpr assertion = mkForall(vars, body);
    constraints.add(assertion);
  }

  private void trAttrSub(Constraint c) {
    if (c.symbols()[1].kind() == sqlsolver.superopt.fragment.Symbol.Kind.TABLE) return;

    final int schema = getSchema(c.symbols()[1]);
    Op op = uExprs.rule()._0().symbols().ownerOf(c.symbols()[1]);
    if(!(op instanceof Proj))
      return;
    final var sourceAttrs = ((Proj) op).attrs();

    final FuncDecl outerProj = projFunc(uExprs.attrsDescOf(c.symbols()[0]).name().toString());
    final FuncDecl innerProj = projFunc(uExprs.attrsDescOf(sourceAttrs).name().toString());
    final IntNum outerSchema = z3.mkInt(schema);
    final IntExpr innerSchema = z3.mkIntConst("s");

    final Expr tuple = z3.mkConst("x", tupleSort());
    final Expr[] vars = {tuple, innerSchema};
    final BoolExpr assertion =
        mkForall(
            vars,
            z3.mkEq(
                outerProj.apply(outerSchema, innerProj.apply(innerSchema, tuple)),
                outerProj.apply(innerSchema, tuple)));

    constraints.add(assertion);
  }

  private void trTableEq(Constraint c) {
    final int schema0 = getSchema(c.symbols()[0]);
    final int schema1 = getSchema(c.symbols()[1]);
    if (schema0 == schema1) return;

    final IntNum s0 = z3.mkInt(schema0), s1 = z3.mkInt(schema1);
    final Expr tuple = z3.mkConst("x", tupleSort());
    final Expr[] vars = new Expr[] {tuple};
    for (var attrSym : rule._0().symbols().symbolsOf(sqlsolver.superopt.fragment.Symbol.Kind.ATTRS)) {
      final FuncDecl projFunc = projFunc(uExprs.attrsDescOf(attrSym).name().toString());
      final BoolExpr assertion = z3.mkEq(projFunc.apply(s0, tuple), projFunc.apply(s1, tuple));
      constraints.add(mkForall(vars, assertion));
    }
  }

  private void trAttrsEq(Constraint c) {
    final int schema0 = getSchema(rule.constraints().sourceOf(c.symbols()[0]));
    final int schema1 = getSchema(rule.constraints().sourceOf(c.symbols()[1]));
    if (schema0 == schema1) return;

    final IntNum s0 = z3.mkInt(schema0), s1 = z3.mkInt(schema1);
    final FuncDecl projFunc = projFunc(uExprs.attrsDescOf(c.symbols()[0]).name().toString());
    final Expr tuple = z3.mkConst("x", tupleSort());
    final Expr[] vars = new Expr[] {tuple};
    final BoolExpr body = z3.mkEq(projFunc.apply(s0, tuple), projFunc.apply(s1, tuple));
    final BoolExpr assertion = mkForall(vars, body);
    constraints.add(assertion);
  }

  private void trNotNull(Constraint c) {
    final String tableName = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String attrsName = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final FuncDecl tableFunc = tableFunc(tableName);
    final FuncDecl projFunc = projFunc(attrsName);
    final Expr tuple = z3.mkConst("x", tupleSort());
    final IntNum schema = z3.mkInt(getSchema(c.symbols()[0]));
    final BoolExpr p0 = z3.mkGt((ArithExpr) tableFunc.apply(tuple), zero());
    final BoolExpr p1 = z3.mkNot(mkIsNull(projFunc.apply(schema, tuple)));

    final Expr[] vars = new Expr[] {tuple};
    final Expr body = z3.mkIff(p0, p1);

    final BoolExpr assertion = mkForall(vars, body);
    constraints.add(assertion);
  }

  private void trUnique(Constraint c) {
    final String tableName = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String attrsName = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final FuncDecl projFunc = projFunc(attrsName);
    final FuncDecl tableFunc = tableFunc(tableName);

    final Expr xTuple = z3.mkConst("x", tupleSort());
    final Expr[] vars0 = new Expr[] {xTuple};
    final BoolExpr body0 = z3.mkLe((ArithExpr) tableFunc.apply(xTuple), one());
    final BoolExpr assertion0 = mkForall(vars0, body0);
    constraints.add(assertion0);

    final Expr yTuple = z3.mkConst("y", tupleSort());
    final IntNum schema = z3.mkInt(getSchema(c.symbols()[0]));
    final BoolExpr b0 = z3.mkGt((ArithExpr) tableFunc.apply(xTuple), zero());
    final BoolExpr b1 = z3.mkGt((ArithExpr) tableFunc.apply(yTuple), zero());
    final BoolExpr b2 = z3.mkEq(projFunc.apply(schema, xTuple), projFunc.apply(schema, yTuple));
    final BoolExpr b4 = z3.mkEq(xTuple, yTuple);
    final Expr[] vars1 = new Expr[] {xTuple, yTuple};
    final BoolExpr body1 = z3.mkImplies(z3.mkAnd(b0, b1, b2), b4);
    final BoolExpr assertion1 = mkForall(vars1, body1);
    constraints.add(assertion1);
  }

  private void trReferences(Constraint c) {
    final String tableName0 = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String tableName1 = uExprs.tableDescOf(c.symbols()[2]).term().tableName().toString();
    final String attrsName0 = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final String attrsName1 = uExprs.attrsDescOf(c.symbols()[3]).name().toString();
    final int schema0 = getSchema(c.symbols()[0]);
    final int schema1 = getSchema(c.symbols()[2]);

    final FuncDecl tableFunc0 = tableFunc(tableName0), tableFunc1 = tableFunc(tableName1);
    final FuncDecl projFunc0 = projFunc(attrsName0), projFunc1 = projFunc(attrsName1);
    final IntNum zero = zero();

    final Expr xTuple = z3.mkConst("x", tupleSort()), yTuple = z3.mkConst("y", tupleSort());
    final IntNum xSchema = z3.mkInt(schema0), ySchema = z3.mkInt(schema1);
    final Expr xProj = projFunc0.apply(xSchema, xTuple), yProj = projFunc1.apply(ySchema, yTuple);
    final BoolExpr b0 = z3.mkGt((ArithExpr) tableFunc0.apply(xTuple), zero);
    final BoolExpr b1 = z3.mkNot(mkIsNull(xProj));
    final BoolExpr b2 = z3.mkGt((ArithExpr) tableFunc1.apply(yTuple), zero);
    final BoolExpr b3 = z3.mkNot(mkIsNull(yProj));
    final BoolExpr b4 = z3.mkEq(xProj, yProj);
    final Expr[] innerVars = new Expr[] {yTuple}, outerVars = new Expr[] {xTuple};
    final BoolExpr body = z3.mkImplies(z3.mkAnd(b0, b1), mkExists(innerVars, z3.mkAnd(b2, b3, b4)));
    final BoolExpr assertion = mkForall(outerVars, body);

    constraints.add(assertion);
  }

  private void trOutVarEq(UVar var0, UVar var1) {
    final Set<UVar> baseVars0 = UVar.getBaseVars(var0);
    final Set<UVar> baseVars1 = UVar.getBaseVars(var1);
    assert baseVars0.size() == baseVars1.size();
    for (var pair : zip(baseVars0, baseVars1)) {
      constraints.add(z3.mkEq(trVar(pair.getLeft()), trVar(pair.getRight())));
    }
  }

  private VerificationResult proveEq0NotNeedLia(UTerm exp1, UTerm exp2) throws Exception {
    HashSet<String> tupleNames = new HashSet<>();
    UTerm newExp1 = exp1; // makeBoundVarUnique(exp1.copy(), tupleNames);
    UTerm newExp2 = exp2; //makeBoundVarUnique(exp2.copy(), tupleNames);
    ArithExpr arithExp1 = myTrUtermNotNeedLia(newExp1);
    ArithExpr arithExp2 = myTrUtermNotNeedLia(newExp2);
    final Solver solver = z3.mkSolver();
    solver.add(constraints.toArray(BoolExpr[]::new));
    return trResult(check(solver, z3.mkNot(z3.mkEq(arithExp1, arithExp2))));
  }

  private VerificationResult proveEq0(UTerm masterTerm, UTerm slaveTerm) {
    final UTerm masterBody = LogicSupport.getBody(masterTerm);
    final UTerm slaveBody = LogicSupport.getBody(slaveTerm);
    final Set<UVar> masterVars = LogicSupport.getBoundedVars(masterTerm);
    final Set<UVar> slaveVars = LogicSupport.getBoundedVars(slaveTerm);
    final Solver solver = z3.mkSolver();
    solver.add(constraints.toArray(BoolExpr[]::new));

    // simple case: E = E' or Sum{x}(E) = Sum{x}(E') ==> tr(E) = tr(E')
    if (masterVars.size() == slaveVars.size()) {
      final ArithExpr masterFormula = trAsBag(masterBody);
      final ArithExpr slaveFormula = trAsBag(slaveBody);
      return trResult(check(solver, z3.mkNot(z3.mkEq(masterFormula, slaveFormula))));
    }

    /*
     complex cases: Sum{x}(X) = Sum{x,y}(Y * Z), where Z is the term using y.
     Need to apply Theorem 5.2.

     The target is to prove P is valid:
     P := (X != Y => (X = 0 /\ Sum{y}(Z(y)) = 0))     | Q0
          /\ (X = Y => (X = 0 \/ Sum{y}(Z(y) = 1)))   | Q1
     We need to prove Q0 and Q1 are both valid.

     To prove Q0's validity, we just need to prove the validity of
       (X != Y => X = 0) /\ (X != Y /\ X = 0 /\ Y != 0 => Sum{y}(Z(y)) = 0)
     so we prove
       q0: X != Y /\ X != 0 is unsat
       q1: X != Y /\ X = 0 /\ Y != 0 /\ Sum{y}(Z(y)) != 0 is unsat

     To prove Q1's validity, we need to prove
          X = Y /\ X != 0 /\ Sum{y}(Z(y)) != 1 is unsat
     Sum{y}(Z(y)) != 1 can be broken into (q2 \/ q3 \/ q4), where
       q2: \forall y. Z(y) = 0
       q3: \exists y. Z(y) > 1
       q4: \exist y,y'. y != y' /\ Z(y) = 1 /\ Z(y') = 1
       We prove (X = Y /\ X != 0 /\ q2), (X = Y /\ X != 0 /\ q3) and (X = Y /\ X != 0 /\ q4) are all unsat.

     There will be 5 SMT invocations in total.
    */

    Status answer;
    final HashSet<UVar> diffVars = new HashSet<>(difference(masterVars, slaveVars));
    final var pair = separateFactors(masterBody, diffVars);
    //noinspection UnnecessaryLocalVariable
    final UTerm exprX = slaveBody, exprY = pair.getLeft(), exprZ = pair.getRight();
    final ArithExpr valueX = trAsBag(exprX);
    final ArithExpr valueY = trAsBag(exprY);
    final BoolExpr boolX = trAsSet(exprX);
    final BoolExpr boolY = trAsSet(exprY);
    final BoolExpr boolZ = trAsSet(exprZ);

    // q0: X != Y /\ X != 0
    final BoolExpr eqXY = z3.mkEq(valueX, valueY);
    final BoolExpr neqXY = z3.mkNot(eqXY);
    answer = check(solver, neqXY, boolX);
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q1: X != Y /\ X = 0 /\ Y != 0 /\ (\exists y. Z(y) != 0) (
    // where X != Y can be collapsed,
    // \exists y. Z(y) can be simplified as Z != 0 (existential quantifier elimination)
    final Expr[] ys = map(diffVars, this::trVar, Expr.class);
    answer = check(solver, z3.mkNot(boolX), boolY, boolZ);
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q2: X = Y /\ X != 0 /\ \forall y. Z(y) = 0
    final BoolExpr eqZ0 = mkForall(ys, z3.mkNot(boolZ));
    answer = check(solver, eqXY, boolX, eqZ0);
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q3: X = Y /\ X != 0 /\ Z(y) > 1
    final ArithExpr valueZ = trAsBag(exprZ);
    answer = check(solver, eqXY, boolX, z3.mkGt(valueZ, one()));
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q4: X = Y /\ X != 0 /\ Z(y) != 0 /\ Z(y') != 0 /\ y != y'
    final FuncDecl funcZ = z3.mkFuncDecl("Z", map(ys, Expr::getSort, Sort.class), z3.getBoolSort());
    solver.add(mkForall(ys, z3.mkEq(funcZ.apply(ys), boolZ)));
    final Expr[] ys1 = map(diffVars, it -> trVar(UVar.mkBase(UName.mk(it + "_"))), Expr.class);
    solver.add(mkOr(generate(ys.length, i -> z3.mkNot(z3.mkEq(ys[i], ys1[i])), BoolExpr.class)));
    answer = check(solver, eqXY, boolX, (BoolExpr) funcZ.apply(ys), (BoolExpr) funcZ.apply(ys1));

    return trResult(answer);
  }

  // Translate a u-expression, must not be summation.
  private ArithExpr trAsBag(UTerm uExpr) {
    final UKind kind = uExpr.kind();
    assert kind != UKind.SUMMATION;
    if (kind.isTermAtomic()) {
      return (ArithExpr) trAtom(uExpr, false);

    } else if (kind == UKind.MULTIPLY) {
      if (uExpr.subTerms().isEmpty()) return one();

      final List<BoolExpr> bools = new ArrayList<>(uExpr.subTerms().size());
      final List<ArithExpr> nums = new ArrayList<>(uExpr.subTerms().size());
      for (UTerm term : uExpr.subTerms()) {
        if (term.kind() == UKind.TABLE) nums.add((ArithExpr) trAtom(term, false));
        else if (term.kind() == UKind.ADD) nums.add(trAsBag(term));
        else if (term.kind() == UKind.MULTIPLY) nums.add(trAsBag(term));
        else bools.add(trAsSet(term));
      }
      final ArithExpr[] factors = nums.toArray(ArithExpr[]::new);
      if (bools.isEmpty()) return mkMul(factors);
      else {
        final BoolExpr[] preconditions = bools.toArray(BoolExpr[]::new);
        return (ArithExpr) z3.mkITE(mkAnd(preconditions), mkMul(factors), zero());
      }

    } else if (kind == UKind.ADD) {
      return z3.mkAdd(map(uExpr.subTerms(), this::trAsBag, ArithExpr.class));

    } else if (kind.isUnary()) {
      final BoolExpr e = trAsSet(uExpr.subTerms().get(0));
      return (ArithExpr)
          (kind == UKind.SQUASH ? z3.mkITE(e, one(), zero()) : z3.mkITE(e, zero(), one()));

    } else {
      throw new IllegalArgumentException("unknown term");
    }
  }

  private BoolExpr mkAnd(BoolExpr[] preconditions) {
    if (preconditions.length == 0) return z3.mkBool(true);
    else if (preconditions.length == 1) return preconditions[0];
    else return z3.mkAnd(preconditions);
  }

  private BoolExpr mkOr(BoolExpr[] preconditions) {
    if (preconditions.length == 0) return z3.mkBool(true);
    else if (preconditions.length == 1) return preconditions[0];
    else return z3.mkOr(preconditions);
  }

  private ArithExpr mkMul(ArithExpr[] factors) {
    if (factors.length == 0) return one();
    else if (factors.length == 1) return factors[0];
    else return z3.mkMul(factors);
  }

  // Translate a u-expression inside squash/negation.
  private BoolExpr trAsSet(UTerm uExpr) {
    final UKind kind = uExpr.kind();

    if (kind.isTermAtomic()) {
      return (BoolExpr) trAtom(uExpr, true);

    } else if (kind.isBinary()) {
      if (uExpr.subTerms().isEmpty()) return z3.mkBool(true);

      final BoolExpr[] es = map(uExpr.subTerms(), this::trAsSet, BoolExpr.class);
      return kind == UKind.MULTIPLY ? mkAnd(es) : z3.mkOr(es);

    } else if (kind == UKind.SUMMATION) {
      final USum sum = (USum) uExpr;
      final BoolExpr body = trAsSet(sum.body());
      final Expr[] vars = map(sum.boundedVars(), this::trVar, Expr.class);
      return mkExists(vars, body);

    } else if (kind.isUnary()) {
      final BoolExpr e = trAsSet(uExpr.subTerms().get(0));
      return kind == UKind.SQUASH ? e : z3.mkNot(e);

    } else {
      throw new IllegalArgumentException("unknown term");
    }
  }

  private Expr trVar(UVar var) {
    final UVar.VarKind kind = var.kind();
    final String name = var.name().toString();
    if (kind == UVar.VarKind.BASE) return z3.mkConst(name, tupleSort());
    if (kind == UVar.VarKind.CONCAT) {
      final FuncDecl concatFunc = mkConcatFunc(var.args().length);
      final Expr[] args = map(var.args(), this::trVar, Expr.class);
      return concatFunc.apply(args);
    }
    if (kind == UVar.VarKind.PROJ) {
      assert var.args().length == 1;
      final FuncDecl projFunc = projFunc(name);
      final int schema = getSchema(var.args()[0]);
      final Expr arg = trVar(var.args()[0]);
      return projFunc.apply(z3.mkInt(schema), arg);
    }
    throw new IllegalArgumentException("unknown var");
  }

  private Expr trAtom(UTerm atom, boolean asBool) {
    assert atom.kind().isTermAtomic();
    if (atom.kind() == UKind.TABLE) {
      final ArithExpr e = trTableAtom((UTable) atom);
      return asBool ? z3.mkGt(e, zero()) : e;

    } else if (atom.kind() == UKind.PRED) {
      final BoolExpr e = trPredAtom((UPred) atom);
      return asBool ? e : z3.mkITE(e, one(), zero());

    } else {
      throw new IllegalArgumentException("unknown atom");
    }
  }

  private ArithExpr trTableAtom(UTable tableTerm) {
    final String tableName = tableTerm.tableName().toString();
//    final String varName = tableTerm.var().name().toString();
    final FuncDecl func = tableFunc(tableName);
//    final Expr var = z3.mkConst(varName, tupleSort());
    final Expr var = trVar(tableTerm.var());
    return (ArithExpr) func.apply(var);
  }

  private BoolExpr trPredAtom(UPred predTerm) {
    // final UVar var = predTerm.var();
    // final UVar.VarKind kind = var.kind();
    // assert kind == UVar.VarKind.FUNC || kind == UVar.VarKind.EQ;

    final UPred.PredKind predKind = predTerm.predKind();
    // assert predKind == UPred.PredKind.FUNC || predKind == UPred.PredKind.EQ;

    switch (predKind) {
      case FUNC: {
        assert predTerm.args().size() == 1 && UExprSupport.isPredOfVarArg(predTerm);
        final UVar var = UExprSupport.getPredVarArgs(predTerm).get(0);
        final String funcName = predTerm.predName().toString();
        final Expr arg = trVar(var);

        if (UName.FUNC_IS_NULL_NAME.equals(funcName)) return mkIsNull(arg);
        else return (BoolExpr) predFunc(funcName).apply(arg);
      }
      case GT: case EQ: case GE: case LE: case LT: case NEQ: {
        assert predTerm.args().size() == 2;
        if(!UExprSupport.isPredOfVarArg(predTerm)) return null;
        final List<UVar> vars = UExprSupport.getPredVarArgs(predTerm);
        final Expr lhs = trVar(vars.get(0));
        final Expr rhs = trVar(vars.get(1));
        return switch(predKind) {
          case GT -> z3.mkGt((ArithExpr) lhs, (ArithExpr) rhs);
          case EQ -> z3.mkEq(lhs, rhs);
          case GE -> z3.mkGe((ArithExpr) lhs, (ArithExpr) rhs);
          case LE -> z3.mkLe((ArithExpr) lhs, (ArithExpr) rhs);
          case LT -> z3.mkLt((ArithExpr) lhs, (ArithExpr) rhs);
          case NEQ -> z3.mkNot(z3.mkEq(lhs, rhs));
          default -> null;
        };
      }
      default: {
        return null;
      }
    }
  }

  private IntNum one() {
    return cache.one.get();
  }

  private IntNum zero() {
    return cache.zero.get();
  }

  private Sort tupleSort() {
    return cache.tupleSort.get();
  }

  private Expr nullTuple() {
    return cache.nullTuple.get();
  }

  private FuncDecl tableFunc(String name) {
    return cache.tableFuncs.get(name);
  }

  private FuncDecl projFunc(String name) {
    return cache.projFuncs.get(name);
  }

  private FuncDecl predFunc(String name) {
    return cache.predFuncs.get(name);
  }

  private BoolExpr mkIsNull(Expr var) {
    return z3.mkEq(var, nullTuple());
  }

  private Quantifier mkForall(Expr[] vars, Expr body) {
    return z3.mkForall(vars, body, 1, null, null, null, null);
  }

  private Quantifier mkExists(Expr[] vars, Expr body) {
    return z3.mkExists(vars, body, 1, null, null, null, null);
  }

  private int getSchema(sqlsolver.superopt.fragment.Symbol sym) {
    final SchemaDesc desc = uExprs.schemaOf(sym);
//    assert desc.components.length == 1;
    return desc.components[0];
  }

  private int getSchema(UVar var) {
    final SchemaDesc desc = uExprs.schemaOf(var);
    assert desc.components.length == 1;
    return desc.components[0];
  }

  private VerificationResult trResult(Status res) {
    if (res == Status.UNSATISFIABLE) return VerificationResult.EQ;
    else if (res == Status.SATISFIABLE) return VerificationResult.NEQ;
    else return VerificationResult.UNKNOWN;
  }

  private Status check(Solver solver, BoolExpr... exprs) {
    LogicSupport.incrementNumInvocations();
    solver.push();
    solver.add(exprs);
    final Status res = solver.check();
    if (LogicSupport.dumpFormulas) {
      System.out.println("==== Begin of Snippet-" + (++callCount) + " ====");
      System.out.println(solver);
      System.out.println("(check-sat)");
      System.out.println("==== End of Snippet-" + callCount + " ====");
      System.out.println("==> Result: " + res);
      return res;
    }
    solver.pop();
    return res;
    // return solver.check(exprs);
  }

  private static Pair<UTerm, UTerm> separateFactors(UTerm mul, Set<UVar> vars) {
    final List<UTerm> factors = mul.subTerms();
    final List<UTerm> termsY = new ArrayList<>(factors.size());
    final List<UTerm> termsZ = new ArrayList<>(factors.size());
    for (UTerm term : factors) {
      if (any(vars, term::isUsing)) termsZ.add(term);
      else termsY.add(term);
    }
    return Pair.of(UMul.mk(termsY), UMul.mk(termsZ));
  }

  private FuncDecl mkConcatFunc(int arity) {
    final Sort[] argSorts = repeat(tupleSort(), arity);
    return z3.mkFuncDecl("concat" + arity, argSorts, tupleSort());
  }

  private class Cache {
    private final Lazy<IntNum> zero = Lazy.mk(() -> z3.mkInt(0));
    private final Lazy<IntNum> one = Lazy.mk(() -> z3.mkInt(1));
    private final Lazy<Sort> tupleSort = Lazy.mk(() -> z3.mkUninterpretedSort("Tuple"));
    private final Lazy<Expr> nullTuple = Lazy.mk(() -> z3.mkConst("Null", tupleSort.get()));
    private final Lazy<Sort[]> projFuncArgSorts =
        Lazy.mk(() -> new Sort[] {z3.getIntSort(), tupleSort.get()});
    private final Map<String, FuncDecl> tableFuncs =
        MapSupport.mkLazy(it -> z3.mkFuncDecl(it, tupleSort.get(), z3.getIntSort()));
    private final Map<String, FuncDecl> projFuncs =
        MapSupport.mkLazy(it -> z3.mkFuncDecl(it, projFuncArgSorts.get(), tupleSort.get()));
    private final Map<String, FuncDecl> predFuncs =
        MapSupport.mkLazy(it -> z3.mkFuncDecl(it, tupleSort.get(), z3.getBoolSort()));
  }
}
