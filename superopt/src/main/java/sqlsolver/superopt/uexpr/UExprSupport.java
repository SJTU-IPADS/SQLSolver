package sqlsolver.superopt.uexpr;

import org.apache.calcite.rel.RelNode;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Expression;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.substitution.SubstitutionTranslatorResult;
import sqlsolver.superopt.uexpr.normalizer.UExprPreprocessor;
import sqlsolver.superopt.uexpr.normalizer.UNormalization;
import sqlsolver.superopt.uexpr.normalizer.UNormalizationEnhance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static sqlsolver.common.utils.Commons.coalesce;
import static sqlsolver.common.utils.IterableSupport.all;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.ast.ExprKind.*;
import static sqlsolver.sql.plan.PlanSupport.colIsNullPredicate;
import static sqlsolver.sql.plan.PlanSupport.isSimpleIntArithmeticExpr;

public abstract class UExprSupport {

  // Used flag bits (the most significant bit is on the left):
  // (31-24) _ _ _ _ _ _ _ _
  // (23-16) _ _ _ _ _ _ _ _
  // (15-08) _ _ _ _ _ _ _ _
  // (07-01) _ _ _ x x x x x
  // ("_" are unused, while "x" are used)

  public static final int UEXPR_FLAG_SUPPORT_DEPENDENT_SUBQUERY = 1;
  public static final int UEXPR_FLAG_CHECK_SCHEMA_FEASIBLE = 1 << 1;

  public static final int UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE = 1 << 2;

  // Below are used for concrete plan -> template translation issues
  public static final int UEXPR_FLAG_VERIFY_CONCRETE_PLAN = 1 << 3;

  // decide how to translate predicates like "x IN (a, b, ...)"
  public static final int UEXPR_FLAG_NO_EXPLAIN_PREDICATES = 1 << 4;

  /**
   * New fresh vars' name sequence, it is used in a single translation.
   */
  public static NameSequence freshVarNameSequence = null;

  /**
   * New free vars' name sequence, it is used in a proving process for two sqls, which generate the freeVar.
   */
  public static NameSequence freeVarNameSequence = null;

  private UExprSupport() {
  }

  public static List<UTerm> copyTermList(List<UTerm> exprList) {
    final List<UTerm> copiedList = new ArrayList<>(exprList.size());
    for (UTerm expr : exprList) copiedList.add(expr.copy());

    return copiedList;
  }

  public static boolean isCriticalValue(UTerm subTerm, UTerm expr) {
    // Check whether: subTerm = 0 infers expr = 0
    final UTerm copy = expr.copy();
    return UExprSupport.normalizeExpr(replaceTermRecursive(copy, subTerm, UConst.zero())).equals(UConst.ZERO);
  }

  public static boolean usesTableVar(UTerm expr, String tableName, UVar var) {
    if (expr instanceof UTable table) {
      return table.tableName().toString().equals(tableName)
              && table.var().equals(var);
    } else {
      // recursion
      for (UTerm term : expr.subTerms()) {
        if (usesTableVar(term, tableName, var)) return true;
      }
      return false;
    }
  }

  @Deprecated
  public static UTerm mkBinaryArithmeticPred(Expression expr, UVar var) {
    assert isSimpleIntArithmeticExpr(expr);
    return mkBinaryArithmeticPred0(expr.template(), var);
  }

  private static UTerm mkBinaryArithmeticPred0(SqlNode node, UVar var) {
    if (ColRef.isInstance(node)) return UVarTerm.mk(var.copy());
    if (Literal.isInstance(node)) {
      if (node.$(ExprFields.Literal_Kind) == LiteralKind.INTEGER) {
        final Integer value = (Integer) node.$(ExprFields.Literal_Value);
        return UConst.mk(value);
      } else return null;
    }

    if (Unary.isInstance(node)) {
      final UTerm lhs = mkBinaryArithmeticPred0(node.$(ExprFields.Unary_Expr), var);
      if (lhs == null) return null;
      switch (node.$(ExprFields.Unary_Op)) {
        case NOT:
          return UNeg.mk(lhs);
        default:
          throw new IllegalArgumentException("Unsupported binary operator: " + node.$(ExprFields.Unary_Op));
      }
    }

    if (Binary.isInstance(node)) {
      final UTerm lhs = mkBinaryArithmeticPred0(node.$(ExprFields.Binary_Left), var);
      final UTerm rhs = mkBinaryArithmeticPred0(node.$(ExprFields.Binary_Right), var);
      if (colIsNullPredicate(node)) {
        // `WHERE col IS NULL`
        assert lhs != null && lhs.kind() == UKind.VAR;
        return mkIsNullPred((UVarTerm) lhs);
      }

      if (lhs == null || rhs == null) return null;

      switch (node.$(ExprFields.Binary_Op)) {
        // Logic operators
        case AND:
          return UMul.mk(lhs, rhs);
        case OR:
          return USquash.mk(UAdd.mk(lhs, rhs));
        // Comparison operators
        case EQUAL:
          return UPred.mkBinary(UPred.PredKind.EQ, lhs, rhs);
        case NOT_EQUAL:
          return UPred.mkBinary(UPred.PredKind.NEQ, lhs, rhs);
        case LESS_THAN:
          return UPred.mkBinary(UPred.PredKind.LT, lhs, rhs);
        case LESS_OR_EQUAL:
          return UPred.mkBinary(UPred.PredKind.LE, lhs, rhs);
        case GREATER_THAN:
          return UPred.mkBinary(UPred.PredKind.GT, lhs, rhs);
        case GREATER_OR_EQUAL:
          return UPred.mkBinary(UPred.PredKind.GE, lhs, rhs);
        // Arithmetic operators
        case PLUS:
          return UAdd.mk(lhs, rhs);
        case MULT:
          return UMul.mk(lhs, rhs);
        case DIV:
          if (lhs.kind() == UKind.CONST && rhs.kind() == UKind.CONST) {
            final Integer lhsV = ((UConst) lhs).value(), rhsV = ((UConst) rhs).value();
            if (rhsV * (lhsV / rhsV) == lhsV) return UConst.mk(lhsV / rhsV);
          }
          throw new IllegalArgumentException("Unsupported binary operator: " + node.$(ExprFields.Binary_Op));
        default:
          throw new IllegalArgumentException("Unsupported binary operator: " + node.$(ExprFields.Binary_Op));
      }
    }

    assert false : "Should be a single-param arithmetic Expression";
    return null;
  }

  /*
   * UVar related functions
   */

  /**
   * Make a fresh base var.
   */
  public static UVar mkFreshBaseVar() {
    return UVar.mkBase(UName.mk(freshVarNameSequence.next()));
  }

  /**
   * Make a fresh free var.
   */
  public static UVar mkFreshFreeVar() {
    return UVar.mkBase(UName.mk(freeVarNameSequence.next()));
  }

  /**
   * Remove a column from the bound var containing this column.
   * Specifically, column indices of that bound var in expr are recomputed.
   *
   * @param expr the expression to update
   * @param column a PROJ var representing the column
   * @return the updated expression
   */
  public static UTerm removeSingleColumn(UTerm expr, UVar column) {
    if (column.kind() != UVar.VarKind.PROJ)
      throw new IllegalArgumentException("[Exception] column should be a PROJ var");
    assert column.args().length == 1;
    final UVar tuple = column.args()[0];
    final int index = CalciteSupport.columnNameToIndex(column.name().toString());
    return removeSingleColumn(expr, tuple, index);
  }

  /**
   * Similar to {@link #removeSingleColumn(UTerm, UVar)},
   * but specifies the column by tuple var and column index.
   * @see #removeSingleColumn(UTerm, UVar)
   */
  public static UTerm removeSingleColumn(UTerm expr, UVar tuple, int index) {
    expr = transformSubTerms(expr, t -> removeSingleColumn(t, tuple, index));
    if (expr instanceof UVarTerm vt
            && vt.var().kind() == UVar.VarKind.PROJ
            && vt.var().args().length == 1) {
      final UVar thatTuple = vt.var().args()[0];
      if (thatTuple.equals(tuple)) {
        final int thatIndex = CalciteSupport.columnNameToIndex(vt.var().name().toString());
        if (thatIndex > index) {
          final String colName = CalciteSupport.indexToColumnName(thatIndex - 1);
          return UVarTerm.mk(UVar.mkProj(UName.mk(colName), tuple.copy()));
        }
      }
    }
    return expr;
  }

  /*
   * Null predicate related functions
   */
  public static UTerm mkNotNullPred(UVar var) {
    return UNeg.mk(mkIsNullPred(var));
  }

  public static UTerm mkIsNullPred(UVar var) {
    return UPred.mkFunc(UName.NAME_IS_NULL, var, true);
  }

  public static UTerm mkNotNullPred(UTerm var) {
    return UNeg.mk(mkIsNullPred(var));
  }

  public static UTerm mkIsNullPred(UTerm var) {
    return UPred.mkFunc(UName.NAME_IS_NULL, var, true);
  }

  public static UTerm mkNotNullPred(List<UTerm> terms) {
    return UMul.mk(map(terms, t -> t = mkNotNullPred(t)));
  }

  public static UTerm mkIsNullPred(List<UTerm> terms) {
    return UAdd.mk(map(terms, t -> t = mkIsNullPred(t)));
  }

  public static boolean varIsNotNullPred(UTerm expr) {
    if (expr.kind() != UKind.NEGATION) return false;
    final UTerm body = ((UNeg) expr).body();
    return varIsNullPred(body);
  }

  public static boolean varIsNullPred(UTerm expr) {
    if (expr.kind() != UKind.PRED) return false;

    final UPred pred = (UPred) expr;
    if (pred.isPredKind(UPred.PredKind.FUNC) && UName.NAME_IS_NULL.equals(pred.predName())) {
      assert pred.args().size() == 1;
      return pred.args().get(0).kind().isVarTerm();
    }
    return false;
  }

  public static boolean isNotNullPred(UTerm expr) {
    if (expr.kind() != UKind.NEGATION) return false;
    final UTerm body = ((UNeg) expr).body();
    return isNullPred(body);
  }

  public static boolean isNullPred(UTerm expr) {
    if (expr.kind() != UKind.PRED) return false;

    final UPred pred = (UPred) expr;
    if (pred.isPredKind(UPred.PredKind.FUNC) && UName.NAME_IS_NULL.equals(pred.predName())) {
      assert pred.args().size() == 1;
      return true;
    }
    return false;
  }

  // expr can be a new UTerm after replacing nullvar in it with NULL
  public static boolean canReplaceNullVar(UTerm expr, UVar nullVar) {
    if (!expr.isUsing(nullVar))
      return true;
    switch (expr.kind()) {
      case NEGATION: {
        return canReplaceNullVar(((UNeg) expr).body(), nullVar);
      }
      case SQUASH: {
        return canReplaceNullVar(((USquash) expr).body(), nullVar);
      }
      case MULTIPLY:
      case ADD: {
        for (UTerm subterm : expr.subTerms()) {
          if (!canReplaceNullVar(subterm, nullVar)) {
            return false;
          }
        }
        return true;
      }
      case PRED: {
        return canReplaceNullVarPred((UPred) expr, nullVar);
      }
      case VAR: {
        return expr.isUsing(nullVar);
      }
      default: {
        return false;
      }
    }
  }

  public static UTerm afterReplaceNullVar(UTerm expr, UVar nullVar) {
    if (!expr.isUsing(nullVar))
      return expr;
    switch (expr.kind()) {
      case NEGATION: {
        UTerm newBody = afterReplaceNullVar(((UNeg) expr).body(), nullVar);
        if (newBody == null) {
          return null;
        } else if (newBody instanceof UConst) {
          int val = ((UConst) newBody).value();
          return (val == 0) ? UConst.one() : UConst.zero();
        } else {
          return UNeg.mk(newBody);
        }
      }
      case SQUASH: {
        UTerm newBody = afterReplaceNullVar(((USquash) expr).body(), nullVar);
        if (newBody == null) {
          return null;
        } else if (newBody instanceof UConst) {
          int val = ((UConst) newBody).value();
          return (val == 0) ? UConst.zero() : UConst.one();
        } else {
          return USquash.mk(newBody);
        }
      }
      case MULTIPLY:
      case ADD: {
        ArrayList<UTerm> newSubTerms = new ArrayList<>();
        for (UTerm subterm : expr.subTerms()) {
          UTerm newSubTerm = afterReplaceNullVar(subterm, nullVar);
          if (newSubTerm == null)
            return null;
          newSubTerms.add(newSubTerm);
        }
        return (expr.kind() == UKind.MULTIPLY) ? UMul.mk(newSubTerms) : UAdd.mk(newSubTerms);
      }
      case PRED: {
        return afterReplaceNullVarPred((UPred) expr, nullVar);
      }
      case VAR: {
        return null;
      }
      default: {
        return expr;
      }
    }
  }

  public static boolean canReplaceNullVarPred(UPred pred, UVar nullVar) {
    switch (pred.predKind()) {
      case FUNC: {
        return isNullPred(pred);
      }
      case EQ:
      case GE:
      case GT:
      case LE:
      case LT: {
        assert pred.args().size() == 2;
        UTerm left = pred.args().get(0);
        UTerm right = pred.args().get(1);
        if (canReplaceNullVar(left, nullVar) && canReplaceNullVar(right, nullVar)) {
          return true;
        } else if (left instanceof UVarTerm && left.isUsing(nullVar) && right instanceof UConst) {
          return true;
        } else if (left instanceof UConst && right instanceof UVarTerm && right.isUsing(nullVar)) {
          return true;
        } else {
          return false;
        }
      }
      default: {
        return false;
      }
    }
  }

  public static UTerm afterReplaceNullVarCmpPred(UTerm left, UTerm right, UPred.PredKind kind) {
    switch (kind) {
      case EQ: {
        if (left == null && right == null) {
          return UConst.one();
        } else if (left == null) {
          if (right instanceof UConst) {
            return UConst.zero();
          }
          if (right instanceof UVarTerm) {
            ArrayList<UTerm> args = new ArrayList<>();
            args.add(right);
            return UPred.mk(UPred.PredKind.FUNC, UName.NAME_IS_NULL, args, true);
          }
          return null;
        } else if (right == null) {
          if (left instanceof UConst) {
            return UConst.zero();
          }
          if (left instanceof UVarTerm) {
            ArrayList<UTerm> args = new ArrayList<>();
            args.add(left);
            return UPred.mk(UPred.PredKind.FUNC, UName.NAME_IS_NULL, args, true);
          }
          return null;
        } else {
          return null;
        }
      }
      case LT:
      case GT: {
        if (left == null && right == null) {
          return UConst.zero();
        } else if (left == null || right == null) {
          return UConst.zero();
        } else {
          return null;
        }
      }
      default: {
        return null;
      }
    }
  }

  public static UTerm afterReplaceNullVarPred(UPred pred, UVar nullVar) {
    UPred.PredKind kind = pred.predKind();
    switch (kind) {
      case FUNC: {
        assert isNullPred(pred);
        return UConst.one();
      }
      case EQ:
      case GE:
      case GT:
      case LE:
      case LT: {
        assert pred.args().size() == 2;
        UTerm left = pred.args().get(0);
        UTerm right = pred.args().get(1);
        if (canReplaceNullVar(left, nullVar) && canReplaceNullVar(right, nullVar)) {
          UTerm leftVal = afterReplaceNullVar(left, nullVar);
          UTerm rightVal = afterReplaceNullVar(right, nullVar);
          UTerm newPred = afterReplaceNullVarCmpPred(leftVal, rightVal, kind);
          return newPred == null ? pred : newPred;
        } else if (left instanceof UVarTerm && left.isUsing(nullVar) && right instanceof UConst) {
          return UConst.zero();
        } else if (left instanceof UConst && right instanceof UVarTerm && right.isUsing(nullVar)) {
          return UConst.zero();
        } else {
          return pred;
        }
      }
      default: {
        return pred;
      }
    }
  }


  public static UVar getIsNullPredVar(UPred pred) {
    assert varIsNullPred(pred);
    final UTerm nullArg = pred.args().get(0);
    return ((UVarTerm) nullArg).var();
  }

  /*
   * Const/Var predicate related functions
   */
  public static boolean isPredOfVarStringArg(UPred pred) {
    // check whether arguments of this pred are UVarTerms
    // i.e. check whether this pred only takes tuple Vars as input
    final List<UTerm> args = pred.args();
    return all(args, arg -> arg.kind().isVarTerm() || arg.kind() == UKind.STRING);
  }

  public static boolean isPredOfVarConstArg(UPred pred) {
    // check whether arguments of this pred are UVarTerms
    // i.e. check whether this pred only takes tuple Vars as input
    final List<UTerm> args = pred.args();
    return all(args, arg -> arg.kind().isVarTerm() || arg.kind() == UKind.CONST);
  }

  public static boolean isPredOfVarArg(UPred pred) {
    // check whether arguments of this pred are UVarTerms
    // i.e. check whether this pred only takes tuple Vars as input
    final List<UTerm> args = pred.args();
    return all(args, arg -> arg.kind().isVarTerm());
  }

  public static boolean isPredOfVarPredArg(UPred pred) {
    // check whether arguments of this pred are UVarTerms
    // i.e. check whether this pred only takes tuple Vars as input
    final List<UTerm> args = pred.args();
    return all(args, arg -> arg.kind().isVarTerm() || arg.kind() == UKind.PRED);
  }

  public static List<UVar> getPredVarArgs(UPred pred) {
    assert isPredOfVarArg(pred);
    final List<UTerm> args = pred.args();
    final List<UVar> varArgs = new ArrayList<>(args.size());
    for (UTerm arg : args) {
      varArgs.add(((UVarTerm) arg).var());
    }
    return varArgs;
  }

  /**
   * eq tuple vars congruence searching functions
   */
  // Get equivalent UVars in a UMul's sub-terms, e.g. `[a0(t0) = a1(t1)]` -> eq class {`a0(t0)`, `a1(t1)`}
  public static NaturalCongruence<UVar> getEqVarCongruenceInTermsOfMul(UTerm mulContext) {
    assert mulContext.kind() == UKind.MULTIPLY;
    final NaturalCongruence<UVar> varEqClass = NaturalCongruence.mk();
    for (UTerm subTerm : mulContext.subTermsOfKind(UKind.PRED)) {
      final UPred pred = (UPred) subTerm;
      if (pred.isPredKind(UPred.PredKind.EQ) && isPredOfVarArg(pred)) {
        final List<UVar> eqPredVars = getPredVarArgs(pred);
        assert eqPredVars.size() == 2;
        final UVar varArg0 = eqPredVars.get(0), varArg1 = eqPredVars.get(1);
        varEqClass.putCongruent(varArg0, varArg1);
      }
    }
    return varEqClass;
  }

  /**
   * Get equivalent UVars/UStrings in a UMul's sub-terms
   * e.g. `[a0(t0) = a1(t1)]` -> eq class {`a0(t0)`, `a1(t1)`}
   * e.g. `[a0(t0) = 'a']` -> eq class {`a0(t0)`, 'a'}
   */
  public static NaturalCongruence<UTerm> getEqVarStringCongruenceInTermsOfMul(UTerm mulContext) {
    return getEqCongruenceInTermsOfMul(mulContext, UExprSupport::isPredOfVarStringArg);
  }

  /**
   * Get equivalent UVars/UConsts in a UMul's sub-terms
   * e.g. `[a0(t0) = a1(t1)]` -> eq class {`a0(t0)`, `a1(t1)`}
   * e.g. `[a0(t0) = 10]` -> eq class {`a0(t0)`, 10}
   */
  public static NaturalCongruence<UTerm> getEqVarConstCongruenceInTermsOfMul(UTerm mulContext) {
    return getEqCongruenceInTermsOfMul(mulContext, UExprSupport::isPredOfVarConstArg);
  }

  /**
   * Get equivalent UVars/UConsts in a UMul's sub-terms
   * e.g. `[a0(t0) = a1(t1)]` -> eq class {`a0(t0)`, `a1(t1)`}
   * e.g. `[a0(t0) = 10]` -> eq class {`a0(t0)`, 10}
   */
  public static NaturalCongruence<UTerm> getEqVarConstStringPredCongruenceInTermsOfMul(UTerm mulContext) {
    return getEqCongruenceInTermsOfMul(mulContext, pred -> isPredOfVarConstArg(pred)
            || isPredOfVarStringArg(pred)
            || isPredOfVarPredArg(pred));
  }

  /**
   * Get equivalent UVars/UConsts in a UMul's sub-terms
   * e.g. `[a0(t0) = a1(t1)]` -> eq class {`a0(t0)`, `a1(t1)`}
   * e.g. `[a0(t0) = 10]` -> eq class {`a0(t0)`, 10}
   * NOTE: this function only consider critical value for ctx!
   */
  public static NaturalCongruence<UTerm> getEqVarConstCongruenceInTermsOfMulCritical(UTerm mulContext, UTerm ctx) {
    return getEqCongruenceInTermsOfMul(mulContext, pred -> isPredOfVarConstArg(pred) && isCriticalValue(pred, ctx));
  }

  /**
   * Get equivalent UTerms in a UMul's sub-terms based on EQ predicates.
   * The specified filter ignores sub-terms that make it return false.
   */
  public static NaturalCongruence<UTerm> getEqCongruenceInTermsOfMul(UTerm mulContext, Function<UPred, Boolean> filter) {
    assert mulContext.kind() == UKind.MULTIPLY;
    final NaturalCongruence<UTerm> varEqClass = NaturalCongruence.mk();
    for (UTerm subTerm : mulContext.subTermsOfKind(UKind.PRED)) {
      final UPred pred = (UPred) subTerm;
      if (pred.isPredKind(UPred.PredKind.EQ) && filter.apply(pred)) {
        final List<UTerm> eqPredTerms = pred.args();
        assert eqPredTerms.size() == 2;
        final UTerm termArg0 = eqPredTerms.get(0), termArg1 = eqPredTerms.get(1);
        varEqClass.putCongruent(termArg0, termArg1);
      }
    }
    return varEqClass;
  }

  /**
   * Get equivalent UTerms in a UMul's sub-terms based on EQ and isNull predicates.
   * The specified filter ignores sub-terms that make it return false.
   */
  public static NaturalCongruence<UTerm> getEqIsNullCongruenceInTermsOfMul(UTerm mulContext, Function<UPred, Boolean> filter) {
    assert mulContext.kind() == UKind.MULTIPLY;
    final NaturalCongruence<UTerm> varEqClass = NaturalCongruence.mk();
    for (UTerm subTerm : mulContext.subTermsOfKind(UKind.PRED)) {
      final UPred pred = (UPred) subTerm;
      if (pred.isPredKind(UPred.PredKind.EQ) && filter.apply(pred)) {
        final List<UTerm> eqPredTerms = pred.args();
        assert eqPredTerms.size() == 2;
        final UTerm termArg0 = eqPredTerms.get(0), termArg1 = eqPredTerms.get(1);
        varEqClass.putCongruent(termArg0, termArg1);
      }
      if (isNullPred(pred) && filter.apply(pred)) {
        final List<UTerm> isNullPredTerms = pred.args();
        assert isNullPredTerms.size() == 1;
        final UTerm termArg0 = isNullPredTerms.get(0);
        varEqClass.putCongruent(UConst.nullVal(), termArg0);
      }
    }
    return varEqClass;
  }

  // Get equivalent UVars in a UMul's sub-terms, e.g. `[a0(t0) = a1(t1)]` -> eq class {`a0(t0)`, `a1(t1)`}
  public static void getEqVarCongruenceInTermsOfMul(NaturalCongruence<UVar> varEqClass, UTerm mulContext) {
    assert mulContext.kind() == UKind.MULTIPLY;
    for (UTerm subTerm : mulContext.subTermsOfKind(UKind.PRED)) {
      final UPred pred = (UPred) subTerm;
      if (pred.isPredKind(UPred.PredKind.EQ) && isPredOfVarArg(pred)) {
        final List<UVar> eqPredVars = getPredVarArgs(pred);
        assert eqPredVars.size() == 2;
        final UVar varArg0 = eqPredVars.get(0), varArg1 = eqPredVars.get(1);
        varEqClass.putCongruent(varArg0, varArg1);
      }
    }
  }

  public static void getEqVarCongruenceInTermsOfPred(NaturalCongruence<UVarTerm> varEqClass, UTerm predContext) {
    assert predContext.kind() == UKind.PRED && ((UPred) predContext).isPredKind(UPred.PredKind.EQ);
    final UPred pred = (UPred) predContext;
    if (pred.isPredKind(UPred.PredKind.EQ)) {
      final List<UTerm> eqPredVars = pred.args();
      assert eqPredVars.size() == 2;
      final UTerm varArg0 = eqPredVars.get(0), varArg1 = eqPredVars.get(1);
      if (varArg0 instanceof UVarTerm && varArg1 instanceof UVarTerm)
        varEqClass.putCongruent((UVarTerm) varArg0, (UVarTerm) varArg1);
    }
  }

  // Get equivalent UVars from anywhere of a UTerm (used to find UVars of equivalent schemas and do normalizations)
  public static NaturalCongruence<UVar> getSchemaEqVarCongruence(UTerm expr) {
    final NaturalCongruence<UVar> varEqClass = NaturalCongruence.mk();
    getSchemaEqVarCongruence0(expr, varEqClass);
    return varEqClass;
  }

  private static void getSchemaEqVarCongruence0(UTerm expr, NaturalCongruence<UVar> varEqClass) {
    if (expr.kind() == UKind.PRED) {
      final UPred pred = (UPred) expr;
      if (pred.isPredKind(UPred.PredKind.EQ) && isPredOfVarArg(pred)) {
        final List<UVar> eqPredArgs = getPredVarArgs(pred);
        assert eqPredArgs.size() == 2;
        final UVar varArg0 = eqPredArgs.get(0), varArg1 = eqPredArgs.get(1);
        varEqClass.putCongruent(varArg0, varArg1);
      }
    }
    for (UTerm subTerm : expr.subTerms()) getSchemaEqVarCongruence0(subTerm, varEqClass);
  }

  /**
   * Get equivalent UTerms in a recursive context (only consider critical kind) based on EQ predicates.
   * The specified filter ignores sub-terms that make it return false.
   * NOTE: this function is more efficient than the another implementation of getEqCongruenceRecursive.
   */
  public static void getEqCongruenceRecursive(UTerm context,
                                              Function<UPred, Boolean> filter,
                                              NaturalCongruence<UTerm> eqClass,
                                              boolean considerNull) {
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR, ADD, NEGATION -> {
      }
      case PRED -> {
        final UPred pred = (UPred) context;
        if (pred.isPredKind(UPred.PredKind.EQ) && filter.apply(pred)) {
          final List<UTerm> eqPredTerms = pred.args();
          assert eqPredTerms.size() == 2;
          final UTerm termArg0 = eqPredTerms.get(0), termArg1 = eqPredTerms.get(1);
          eqClass.putCongruent(termArg0, termArg1);
        }
        if (considerNull && isNullPred(pred) && filter.apply(pred)) {
          final List<UTerm> isNullPredTerms = pred.args();
          assert isNullPredTerms.size() == 1;
          final UTerm termArg0 = isNullPredTerms.get(0);
          eqClass.putCongruent(UConst.nullVal(), termArg0);
        }
      }
      case MULTIPLY, SUMMATION, SQUASH  -> {
        for (final UTerm subTerm : context.subTerms()) {
          getEqCongruenceRecursive(subTerm, filter, eqClass, considerNull);
        }
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + context.kind());
    }
  }

  /**
   * Get equivalent UTerms in a recursive context (only consider critical kind) based on EQ predicates.
   * The specified filter ignores sub-terms that make it return false.
   * Only consider the term that is const related to context.
   */
  public static void getEqCongruenceRecursive(UTerm context,
                                              Function<UPred, Boolean> filter,
                                              NaturalCongruence<UTerm> eqClass,
                                              UTerm fullContext,
                                              boolean considerNull) {
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR -> {
      }
      case PRED -> {
        if (!isCriticalValue(context, fullContext)) return;
        final UPred pred = (UPred) context;
        if (pred.isPredKind(UPred.PredKind.EQ) && filter.apply(pred)) {
          final List<UTerm> eqPredTerms = pred.args();
          assert eqPredTerms.size() == 2;
          final UTerm termArg0 = eqPredTerms.get(0), termArg1 = eqPredTerms.get(1);
          eqClass.putCongruent(termArg0, termArg1);
        }
        if (considerNull && isNullPred(pred) && filter.apply(pred)) {
          final List<UTerm> isNullPredTerms = pred.args();
          assert isNullPredTerms.size() == 1;
          final UTerm termArg0 = isNullPredTerms.get(0);
          eqClass.putCongruent(UConst.nullVal(), termArg0);
        }
      }
      case MULTIPLY, SUMMATION, SQUASH, ADD, NEGATION  -> {
        for (final UTerm subTerm : context.subTerms()) {
          getEqCongruenceRecursive(subTerm, filter, eqClass, fullContext, considerNull);
        }
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + context.kind());
    }
  }

  /**
   * Reason about pre-defined functions (e.g. like_op)
   */
  public static UTerm preprocessExpr(UTerm expr) {
    return new UExprPreprocessor().preprocess(expr);
  }

  /*
   * U-expression recursive functions
   */
  /**
   * Replace repTerm with tgtTerm in the recursive context.
   */
  public static UTerm replaceTermRecursive(UTerm context, UTerm tgtTerm, UTerm repTerm) {
    if (context.equals(tgtTerm)) return repTerm;

    final List<UTerm> newSubTerms = new ArrayList<>();

    for (final UTerm subTerm : context.subTerms()) {
      newSubTerms.add(replaceTermRecursive(subTerm, tgtTerm, repTerm));
    }

    return remakeTerm(context, newSubTerms);
  }

  /**
   * Replace repTerm with tgtTerm in the recursive context (only consider critical kind).
   */
  public static void replaceTermRecursiveCritical(UTerm context, UTerm tgtTerm, UTerm repTerm) {
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR, ADD, NEGATION, PRED -> {
      }
      case MULTIPLY, SUMMATION, SQUASH -> {
        for (final UTerm subTerm : context.subTerms()) {
          replaceTermRecursiveCritical(subTerm, tgtTerm, repTerm);
        }

        for (int i = 0; i < context.subTerms().size(); i++) {
          if (context.subTerms().get(i).equals(tgtTerm)) {
            context.subTerms().set(i, repTerm);
          }
        }
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + context.kind());
    }
  }

  public static void getTargetUExprRecursive(UTerm context, Function<UTerm, Boolean> filter, List<UTerm> result, UTerm fullContext) {
    if (filter.apply(context) && isCriticalValue(context, fullContext)) result.add(context);
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR, PRED -> {
      }
      case MULTIPLY, SUMMATION, SQUASH, ADD, NEGATION -> {
        for (final UTerm subTerm : context.subTerms()) {
          getTargetUExprRecursive(subTerm, filter, result, fullContext);
        }
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + context.kind());
    }
  }

  public static void getTargetUExprRecursive(UTerm context, Function<UTerm, Boolean> filter, List<UTerm> result) {
    if (filter.apply(context)) result.add(context);
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR, ADD, NEGATION, PRED -> {
      }
      case MULTIPLY, SUMMATION, SQUASH -> {
        for (final UTerm subTerm : context.subTerms()) {
          getTargetUExprRecursive(subTerm, filter, result);
        }
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + context.kind());
    }
  }

  /**
   * Get bounded vars in a recursive context (only consider critical kind).
   * The specified filter ignores sub-terms that make it return false.
   */
  public static void getBoundedVarsRecursive(UTerm context, Set<UVar> result, UTerm fullContext) {
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR, PRED -> {
      }
      case MULTIPLY, SUMMATION, SQUASH, ADD, NEGATION -> {
        if (context.kind() == UKind.SUMMATION && isCriticalValue(context, fullContext)) result.addAll(((USum) context).boundedVars());
        for (final UTerm subTerm : context.subTerms()) {
          getBoundedVarsRecursive(subTerm, result, fullContext);
        }
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + context.kind());
    }
  }

  /**
   * Transform null terms to isNull format.
   * If the target term is invalid, return null.
   * e.g. [a = NULL] => isNull(a)
   */
  public static UTerm transformNullTerm(UTerm term) {
    if (!term.isUsingTerm(UConst.nullVal())) return term;
    switch (term.kind()) {
      case CONST -> {
        if (term.equals(UConst.nullVal())) return UConst.nullVal();
      }
      case STRING, TABLE, VAR -> {
      }
      case PRED -> {
        // if subTerm has transformed the null
        List<UTerm> newSubTerms = transformTerms(term.subTerms(), UExprSupport::transformNullTerm);
        if (any(newSubTerms, Objects::isNull)) return null;
        newSubTerms = transformTerms(newSubTerms, t -> t.isUsingTerm(UConst.nullVal()) ? t : normalizeExpr(t));
        term = remakeTerm(term, newSubTerms);
        // if the argument contains null
        final UPred pred = (UPred) term;
        if (!pred.args().contains(UConst.nullVal())) return term;
        if (isNullPred(pred)) {
          if (pred.args().get(0).equals(UConst.nullVal())) return UConst.one();
        }
        if (pred.isPredKind(UPred.PredKind.EQ)) {
          final UTerm arg0 = pred.args().get(0);
          final UTerm arg1 = pred.args().get(1);
          if (arg0.equals(arg1)) return UConst.one();
          if (arg0.equals(UConst.nullVal())) return mkIsNullPred(arg1);
          if (arg1.equals(UConst.nullVal())) return mkIsNullPred(arg0);
        }
        if (pred.isBinaryPred()) return UConst.zero();
      }
      case MULTIPLY, SUMMATION, SQUASH, ADD, NEGATION, FUNC -> {
        List<UTerm> newSubTerms = transformTerms(term.subTerms(), UExprSupport::transformNullTerm);
        if (any(newSubTerms, Objects::isNull)) return null;
        final List<UTerm> newSubTermsWithoutNullConst = filter(newSubTerms, t -> !t.isUsingTerm(UConst.nullVal()));
        // In MULTIPLY/ADD, NULL-free part can be normalized
        if ((term.kind() == UKind.MULTIPLY || term.kind() == UKind.ADD)
                && !newSubTermsWithoutNullConst.isEmpty()) {
          final UTerm newTermWithoutNullConst = normalizeExpr(remakeTerm(term, newSubTermsWithoutNullConst));
          // 0*e = 0
          if (term.kind() == UKind.MULTIPLY
                  && newTermWithoutNullConst.equals(UConst.zero()))
            return UConst.zero();
          // merge normalized part and NULL part
          newSubTerms = filter(newSubTerms, t -> t.isUsingTerm(UConst.nullVal()));
          if (newTermWithoutNullConst.kind() == term.kind()) {
            // the same kind of terms can be merged
            // e.g. (a*b)*(c*d) = (a*b*c*d)
            newSubTerms.addAll(newTermWithoutNullConst.subTerms());
          } else {
            // a term of different kind is seen as a whole
            // e.g. (a*b)*(c+d) = (a*b*(c+d))
            newSubTerms.add(newTermWithoutNullConst);
          }
        }
        // 0 * null -> 0 in uexpr, but null + 0 -> null, so we can only eliminate addition with nulls
        if (term.kind() == UKind.ADD
                && any(newSubTerms, t -> t.equals(UConst.nullVal()))) {
          return UConst.nullVal();
        }
        return remakeTerm(term, newSubTerms);
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + term.kind());
    }
    return null;
  }

  /**
   * get TargetUExpr in arithmetic context.
   */
  public static void getTargetUExprRecursiveArithmetic(UTerm context, Function<UTerm, Boolean> filter, List<UTerm> result) {
    if (filter.apply(context)) result.add(context);
    switch (context.kind()) {
      case CONST, STRING, TABLE, FUNC, VAR, NEGATION, PRED, SUMMATION, SQUASH -> {
      }
      case MULTIPLY, ADD -> {
        for (final UTerm subTerm : context.subTerms()) {
          getTargetUExprRecursiveArithmetic(subTerm, filter, result);
        }
      }
      default -> throw new IllegalArgumentException("[Exception] Unsupported U-expression kind: " + context.kind());
    }
  }

  /*
   * U-expression predicate related functions
   */

  /**
   * Check whether every predicate is contradicted.
   * Contradicted: there are at most one predicate in these predicates can equal to one.
   * e.g. These predicates are contradicted: [x = 1], [x = 2], [x = "ABC"]
   * NOTE: the version of this function here is for HACKING, and it only considers equal predicate now.
   */
  public static boolean isContractPredicates(List<UPred> predicates) {
    // hack version
    if (any(predicates, p -> !p.isPredKind(UPred.PredKind.EQ))) return false;

    // only consider the form that [f(VAR) = CONST]
    UTerm lastTerm = null;
    final List<UTerm> constTerms = new ArrayList<>();
    for (final UPred predicate : predicates) {
      assert predicate.args().size() == 2;
      UTerm targetTerm = null;
      UTerm constTerm = null;

      final UTerm firstArg = predicate.args().get(0);
      final UTerm secondArg = predicate.args().get(1);

      if (firstArg.kind() == UKind.STRING || firstArg.kind() == UKind.CONST) {
        constTerm = firstArg;
        targetTerm = secondArg;
      }
      else if (secondArg.kind() == UKind.STRING || secondArg.kind() == UKind.CONST) {
        constTerm = secondArg;
        targetTerm = firstArg;
      }

      if (lastTerm == null) lastTerm = targetTerm;
      // only consider the case that has same var
      if (targetTerm == null || !lastTerm.equals(targetTerm)) return false;

      if (firstArg.kind() == UKind.STRING || firstArg.kind() == UKind.CONST) constTerm = firstArg;
      else if (secondArg.kind() == UKind.STRING || secondArg.kind() == UKind.CONST) constTerm = secondArg;

      if (constTerm == null) return false;
      constTerms.add(constTerm);
    }

    // if all const terms are not equal, return true.
    for (int i = 0; i < constTerms.size(); i++) {
      for (int j = i + 1; j < constTerms.size(); j++) {
        if (constTerms.get(i).equals(constTerms.get(j))) return false;
      }
    }

    return true;
  }

  /*
   * U-expression normalization functions
   */
  public static UTerm normalizeExpr(UTerm expr) {
    return new UNormalization(expr).normalizeTerm();
  }

  public static UTerm normalizeExprEnhance(UTerm expr) {
    return new UNormalizationEnhance(expr).normalizeTerm();
  }

  static boolean checkNormalForm(UTerm expr) {
    return UNormalization.isNormalForm(expr);
  }

  /**
   * Apply transformation to each term in `terms`.
   *
   * <p>Returns the original `terms` if each term are not changed (or changed in-place). Otherwise,
   * a new list.
   */
  static List<UTerm> transformTerms(List<UTerm> terms, Function<UTerm, UTerm> transformation) {
    List<UTerm> copies = null;
    for (int i = 0, bound = terms.size(); i < bound; i++) {
      final UTerm subTerm = terms.get(i);
      final UTerm modifiedSubTerm = transformation.apply(subTerm);
      if (modifiedSubTerm != subTerm) {
        if (copies == null) copies = new ArrayList<>(terms);
        copies.set(i, modifiedSubTerm);
      }
    }

    return coalesce(copies, terms);
  }

  public static UTerm remakeTerm(UTerm template, List<UTerm> subTerms) {
    if (subTerms == template.subTerms()) return template;

    // should not reach here by design
    return switch (template.kind()) {
      case CONST, TABLE, VAR, STRING -> template;
      case PRED ->
              UPred.mk(((UPred) template).predKind(), ((UPred) template).predName(), subTerms, ((UPred) template).nullSafe());
      case FUNC -> UFunc.mk(((UFunc) template).funcKind(), ((UFunc) template).funcName(), subTerms);
      case ADD -> UAdd.mk(subTerms);
      case MULTIPLY -> UMul.mk(subTerms);
      case NEGATION -> UNeg.mk(subTerms.get(0));
      case SQUASH -> USquash.mk(subTerms.get(0));
      case SUMMATION -> USum.mk(((USum) template).boundedVars(), subTerms.get(0));
    };
  }

  /**
   * Apply transformation to each sub term of `term`.
   *
   * <p>Returns the original `terms` if each term are not changed (or changed in-place). Otherwise,
   * a new list.
   */
  public static UTerm transformSubTerms(UTerm expr, Function<UTerm, UTerm> transformation) {
    return remakeTerm(expr, transformTerms(expr.subTerms(), transformation));
  }

  /**
   * Functions to get translated U-exprs
   */
  public static UExprTranslationResult translateToUExpr(Substitution rule) {
    return new UExprTranslator(rule, 0, null).translate();
  }

  public static UExprTranslationResult translateToUExpr(Substitution rule, int tweaks) {
    return new UExprTranslator(rule, tweaks, null).translate();
  }

  public static UExprTranslationResult translateToUExpr
          (Substitution rule, int tweaks, SubstitutionTranslatorResult extraInfo) {
    return new UExprTranslator(rule, tweaks, extraInfo).translate();
  }

  public static UExprConcreteTranslationResult translateQueryWithVALUESToUExpr
          (String sql0, String sql1, Schema baseSchema, int tweaks) {
    // Process query with `VALUES` feature
    return new UExprConcreteTranslator(sql0, sql1, baseSchema, tweaks).translate();
  }

  public static UExprConcreteTranslationResult translateQueryToUExpr
          (RelNode plan0, RelNode plan1, Schema schema, int tweaks) {
    // Process concrete queries translation
    return new UExprConcreteTranslator(plan0, plan1, schema, tweaks).translate();
  }
}
