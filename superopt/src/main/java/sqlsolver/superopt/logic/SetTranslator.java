package sqlsolver.superopt.logic;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.superopt.uexpr.PredefinedFunctions.*;
import static sqlsolver.superopt.util.Z3Support.*;

import com.microsoft.z3.*;
import java.util.*;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.translator.LiaTranslator;
import sqlsolver.superopt.uexpr.*;

/**
 * Translate U-expressions into FOL formulas. It aims at U-expressions whose summations are always
 * within squash/negation.
 */
public abstract class SetTranslator {
  public record Config(String varMode, boolean forcesArithExpr) {
    public static final String VAR_MODE_TUPLE_AS_VAR = "tuple as var";
    public static final String VAR_MODE_COLUMN_AS_VAR = "column as var";
  }

  public static SetTranslator mk(Config config, TranslatorContext ctx, UTerm term) {
    switch (config.varMode) {
      case Config.VAR_MODE_TUPLE_AS_VAR -> {
        return new TupleVarSetTranslator(config, ctx, term);
      }
      case Config.VAR_MODE_COLUMN_AS_VAR -> {
        return new ColumnVarSetTranslator(config, ctx, term);
      }
      default -> throw new UnsupportedOperationException("unknown var mode");
    }
  }

  public static class TranslatorContext {
    // z3 var/func declarations, etc.
    final Context z3;
    // z3 statements (e.g. constraints, assertions, etc.)
    final Solver solver;
    // z3 var definitions (for each $i(x))
    final Map<UTerm, Expr> termVarMap;
    // z3 function definitions
    final Map<String, FuncDecl> funcs;
    // used to generate new var names
    final NameSequence varNameSeq;
    // schema info of each var
    final Map<UVar, List<Value>> varSchema;
    // table schema (ICs)
    final Schema tableSchema;
    // BVs in each summation layer (inner layers in the front)
    // existBVs[i] is the set of z3 vars corresponding to sumBVs[i]
    // except that the last set in existBVs is the set of free z3 vars
    final LinkedList<Set<UVar>> sumBVs;
    final LinkedList<Set<Expr>> existBVs;

    public TranslatorContext(Context z3, Map<UVar, List<Value>> varSchema, Schema tableSchema) {
      this.z3 = z3;
      solver = z3.mkSolver();
      termVarMap = new HashMap<>();
      funcs = new HashMap<>();
      varNameSeq = NameSequence.mkIndexed("u", 0);
      this.varSchema = varSchema;
      this.tableSchema = tableSchema;
      sumBVs = new LinkedList<>();
      existBVs = new LinkedList<>();
      existBVs.add(new HashSet<>());
    }

    public Solver getSolver() {
      return solver;
    }

    Expr newVar(Sort sort) {
      return z3.mkConst(varNameSeq.next(), sort);
    }

    void enterSumLayer(Set<UVar> bvs) {
      sumBVs.addFirst(bvs);
      existBVs.addFirst(new HashSet<>());
    }

    Set<Expr> exitSumLayer() {
      sumBVs.removeFirst();
      return existBVs.removeFirst();
    }

    void bindZ3BV(Expr z3BV, UVar var) {
      // find the corresponding sum layer
      Set<Expr> targetLayer = null;
      for (Pair<Set<UVar>, Set<Expr>> pair : zip(sumBVs, existBVs)) {
        if (any(pair.getLeft(), var::isUsing)) {
          targetLayer = pair.getRight();
          break;
        }
      }
      if (targetLayer == null) targetLayer = existBVs.getLast();
      // bind z3BV to it
      targetLayer.add(z3BV);
    }

    private boolean isNotNullColumn(UVar var, LinkedList<UTerm> path) {
      if (var.kind() != UVar.VarKind.PROJ) return false;
      assert var.args().length == 1;
      // find which table(s) the tuple belongs to
      final List<String> tableNames = belongsToWhichTable(var.args()[0], path);
      // check if the column is NOT NULL
      for (String tableName : tableNames) {
        // if the column is NOT NULL in one table, then it is NOT NULL
        final int colIndex = CalciteSupport.columnNameToIndex(var.name().toString());
        final String colName =
            tableSchema.table(tableName).columns().stream().toList().get(colIndex).name();
        if (any(
            tableSchema.table(tableName).constraints(),
            ic ->
                ic.kind() == ConstraintKind.NOT_NULL
                    && ic.columns().size() == 1
                    && ic.columns().get(0).name().equals(colName))) return true;
      }
      return false;
    }

    private List<String> belongsToWhichTable(UVar tuple, LinkedList<UTerm> path) {
      final List<String> result = new ArrayList<>();
      final List<UTable> tables = findTableTerm(tuple, path.getLast());
      for (UTable table : tables) {
        final String tableName = table.tableName().toString();
        if (belongsToTable(tuple, path, tableName)) result.add(tableName);
      }
      return result;
    }

    private List<UTable> findTableTerm(UVar var, UTerm term) {
      // find table(var) in term
      if (term instanceof UTable table && table.var().equals(var)) return List.of(table);
      final List<UTable> result = new ArrayList<>();
      for (UTerm sub : term.subTerms()) {
        result.addAll(findTableTerm(var, sub));
      }
      return result;
    }

    private boolean belongsToTable(UVar tuple, LinkedList<UTerm> path, String tableName) {
      // whether assigning 0 to the table term
      //   makes any term on the path become a constant
      for (UTerm term : path) {
        final UTable tableTerm = UTable.mk(UName.mk(tableName), tuple.copy());
        if (UExprSupport.normalizeExpr(term.replaceAtomicTerm(tableTerm, UConst.zero())).kind() == UKind.CONST)
          return true;
      }
      return false;
    }
  }

  protected final Config config;
  protected final TranslatorContext ctx;
  private final UTerm term;

  protected SetTranslator(Config config, TranslatorContext ctx, UTerm term) {
    this.config = config;
    this.ctx = ctx;
    this.term = term;
  }

  /** Translate the preset U-expression into an FOL arithmetic expression. */
  public ArithExpr translate() {
    final Expr result = translateRecursive(term, new LinkedList<>(), false);
    if (result instanceof ArithExpr arithResult) {
      return arithResult;
    } else if (result instanceof BoolExpr boolResult) {
      return (ArithExpr) ctx.z3.mkITE(boolResult, ctx.z3.mkInt(1), ctx.z3.mkInt(0));
    }
    throw new UnsupportedOperationException(
        "expressions beyond Arithmetic/Bool types are not supported");
  }

  protected String notNullVarOf(String var) {
    return var + "_notnull";
  }

  // whether a term must be binary (term = 0 \/ term = 1)
  private boolean isBinaryValue(UTerm term) {
    final LiaStar termLia = LiaTranslator.translate(term, ctx.varSchema);
    final LiaStar zero = LiaStar.mkConst(false, 0);
    final LiaStar one = LiaStar.mkConst(false, 1);
    final LiaStar toCheck =
        LiaStar.mkOr(false, LiaStar.mkEq(false, termLia, zero), LiaStar.mkEq(false, termLia, one));
    try {
      return isValidLia(toCheck);
    } catch (Throwable e) {
      // non-integral values are considered non-binary
      return false;
    }
  }

  // isUnderSet: whether term is considered to be squashed.
  //   Example 1: a,b in ||a+b|| can be considered to be squashed
  //     since ||a+b|| = ||(||a||)+(||b||)||;
  //   Example 2: a,b in [a=b] are considered not to be squashed
  //     even if there is a squash outside the predicate;
  //   Example 3: whether a,b in a*b are considered to be squashed
  //     is determined by its environment (i.e. whether isUnderSet is true).
  //   The term type and the value of "isUnderSet" decides whether each direct subterm
  //   can be squashed without changing the term's value.
  private Expr translateRecursive(UTerm term, LinkedList<UTerm> path, boolean isUnderSet) {
    final UTerm parent = path.isEmpty() ? null : path.getFirst();
    final LinkedList<UTerm> newPath = new LinkedList<>(path);
    newPath.addFirst(term);
    // if term must be binary, we may add squash to it
    // but the type of returned expr (determined only by "isUnderSet") does not change
    final boolean isUnderSetFinal = isUnderSet || isBinaryValue(term);
    // translate (assign arithResult or boolResult according to term type)
    ArithExpr arithResult = null;
    BoolExpr boolResult = null;
    switch (term.kind()) {
      case CONST -> {
        arithResult = ctx.z3.mkInt(((UConst) term).value());
      }
      case STRING -> {
        // todo: does mkString return a constant or var?
        // strings cannot be cast to other type
        return ctx.z3.mkString(((UString) term).value());
      }
      case TABLE -> {
        arithResult = (ArithExpr) trTable((UTable) term, vt -> trVarTermWithCheck(vt, term));
      }
      case PRED -> {
        final UPred pred = (UPred) term;
        // PRED resets "isUnderSet"
        final List<Expr> argExps = map(pred.args(), arg -> translateRecursive(arg, newPath, false));
        switch (pred.predKind()) {
          case EQ -> {
            boolResult = ctx.z3.mkEq(argExps.get(0), argExps.get(1));
          }
          case NEQ -> {
            boolResult = ctx.z3.mkNot(ctx.z3.mkEq(argExps.get(0), argExps.get(1)));
          }
          case LT -> {
            boolResult = ctx.z3.mkLt((ArithExpr) argExps.get(0), (ArithExpr) argExps.get(1));
          }
          case LE -> {
            boolResult = ctx.z3.mkLe((ArithExpr) argExps.get(0), (ArithExpr) argExps.get(1));
          }
          case GT -> {
            boolResult = ctx.z3.mkGt((ArithExpr) argExps.get(0), (ArithExpr) argExps.get(1));
          }
          case GE -> {
            boolResult = ctx.z3.mkGe((ArithExpr) argExps.get(0), (ArithExpr) argExps.get(1));
          }
          case FUNC -> {
            final String funcName = pred.predName().toString();
            boolResult =
                (BoolExpr) assemblePredefinedFunction(funcName, argExps, ctx.z3, ctx.funcs);
          }
        }
        // not-null constraints
        for (Pair<UTerm, Expr> pair : zip(pred.args(), argExps)) {
          final UTerm arg = pair.getLeft();
          if (!(arg instanceof UVarTerm vt)) continue;
          final UVar var = vt.var();
          if (ctx.isNotNullColumn(var, newPath)) continue;
          // for each nullable var, append not-null constraint and z3 var def
          final Expr varExp = pair.getRight();
          final String notNullVarName = notNullVarOf(varExp.toString());
          final BoolExpr notNullVar = ctx.z3.mkBoolConst(notNullVarName);
          ctx.bindZ3BV(notNullVar, var);
          boolResult = ctx.z3.mkAnd(boolResult, notNullVar);
        }
      }
      case FUNC -> {
        final UFunc func = (UFunc) term;
        final String funcName = func.funcName().toString();
        final List<UTerm> args = func.args();
        final int arity = args.size();
        if (!isPredefinedFunction(funcName, arity)) {
          throw new UnsupportedOperationException(
              "Function " + funcName + "(" + arity + " args) is unknown");
        }
        // FUNC resets "isUnderSet"
        final List<Expr> z3Args = map(args, arg -> translateRecursive(arg, newPath, false));
        final Expr result = assemblePredefinedFunction(funcName, z3Args, ctx.z3, ctx.funcs);
        if (result instanceof ArithExpr arithExpr) {
          arithResult = arithExpr;
        } else {
          // not support casting other types of functions
          return result;
        }
      }
      case VAR -> {
        final Expr result = trVarTermWithCheck((UVarTerm) term, parent);
        if (result instanceof ArithExpr arithExpr) {
          arithResult = arithExpr;
        } else {
          return result;
        }
      }
      case MULTIPLY -> {
        if (config.forcesArithExpr) {
          final List<Expr> args =
              map(term.subTerms(), t -> translateRecursive(t, newPath, isUnderSetFinal));
          arithResult = ctx.z3.mkMul(args.toArray(new ArithExpr[0]));
        } else {
          if (!isUnderSetFinal) {
            final List<Expr> args =
                map(term.subTerms(), t -> translateRecursive(t, newPath, false));
            arithResult = ctx.z3.mkMul(args.toArray(new ArithExpr[0]));
          } else {
            final List<Expr> args = map(term.subTerms(), t -> translateRecursive(t, newPath, true));
            boolResult = ctx.z3.mkAnd(args.toArray(new BoolExpr[0]));
          }
        }
      }
      case ADD -> {
        if (config.forcesArithExpr) {
          final List<Expr> args =
              map(term.subTerms(), t -> translateRecursive(t, newPath, isUnderSetFinal));
          arithResult = ctx.z3.mkAdd(args.toArray(new ArithExpr[0]));
        } else {
          if (!isUnderSetFinal) {
            final List<Expr> args =
                map(term.subTerms(), t -> translateRecursive(t, newPath, false));
            arithResult = ctx.z3.mkAdd(args.toArray(new ArithExpr[0]));
          } else {
            final List<Expr> args = map(term.subTerms(), t -> translateRecursive(t, newPath, true));
            boolResult = ctx.z3.mkOr(args.toArray(new BoolExpr[0]));
          }
        }
      }
      case SUMMATION -> {
        if (!isUnderSetFinal)
          throw new UnsupportedOperationException(
              "summations outside squash/negation are not supported");
        final USum sum = (USum) term;
        ctx.enterSumLayer(sum.boundedVars());
        if (config.forcesArithExpr) {
          final ArithExpr bodyExpr = (ArithExpr) translateRecursive(sum.body(), newPath, true);
          boolResult = ctx.z3.mkNot(ctx.z3.mkEq(bodyExpr, ctx.z3.mkInt(0)));
        } else {
          boolResult = (BoolExpr) translateRecursive(sum.body(), newPath, true);
        }
        // collect actual z3 bound vars
        final Set<Expr> bvs = ctx.exitSumLayer();
        boolResult =
            ctx.z3.mkExists(bvs.toArray(new Expr[0]), boolResult, 0, null, null, null, null);
      }
      case NEGATION -> {
        final UNeg neg = (UNeg) term;
        if (config.forcesArithExpr) {
          boolResult = ctx.z3.mkEq(translateRecursive(neg.body(), newPath, true), ctx.z3.mkInt(0));
        } else {
          boolResult = (BoolExpr) translateRecursive(neg.body(), newPath, true);
          boolResult = ctx.z3.mkNot(boolResult);
        }
      }
      case SQUASH -> {
        final USquash squash = (USquash) term;
        if (config.forcesArithExpr) {
          boolResult =
              ctx.z3.mkEq(translateRecursive(squash.body(), newPath, true), ctx.z3.mkInt(0));
          boolResult = ctx.z3.mkNot(boolResult);
        } else {
          boolResult = (BoolExpr) translateRecursive(squash.body(), newPath, true);
        }
      }
    }
    // cast between arith expr and bool expr if necessary
    // the final type to return is determined by "isUnderSet"
    if (arithResult != null) {
      if (!config.forcesArithExpr && isUnderSet) {
        // a -> ~(a=0)
        return ctx.z3.mkNot(ctx.z3.mkEq(arithResult, ctx.z3.mkInt(0)));
      }
      // a
      return arithResult;
    } else if (boolResult != null) {
      if (config.forcesArithExpr || !isUnderSet) {
        // p -> ite(p,1,0)
        return ctx.z3.mkITE(boolResult, ctx.z3.mkInt(1), ctx.z3.mkInt(0));
      }
      // p
      return boolResult;
    }
    return null;
  }

  private Expr trVarTermWithCheck(UVarTerm vt, UTerm parent) {
    // check:
    // hack: a var should only be directly under predicates or table
    //   unless this var is NOT NULL
    if (parent == null || parent.kind() != UKind.PRED && parent.kind() != UKind.TABLE)
      throw new UnsupportedOperationException(
          "vars not as direct children of a predicate/table are not supported");
    // translate
    final Expr result = trVarTerm(vt);
    // bind z3 var
    final UVar var = vt.var();
    ctx.bindZ3BV(result, var);
    return result;
  }

  protected ValueType toValueType(String typeName) {
    return PredefinedFunctions.ValueType.getValueTypeByString(typeName);
  }

  protected abstract Expr trTable(UTable table, Function<UVarTerm, Expr> trVarTerm);

  protected abstract Expr trVarTerm(UVarTerm vt);
}
