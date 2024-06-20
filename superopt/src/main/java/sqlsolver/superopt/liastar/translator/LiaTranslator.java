package sqlsolver.superopt.liastar.translator;

import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.liastar.LiaConstImpl;
import sqlsolver.superopt.liastar.LiaIteImpl;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.uexpr.*;

import java.util.*;

import static sqlsolver.superopt.liastar.translator.LiaStarTranslatorSupport.translateFunc;
import static sqlsolver.superopt.uexpr.UExprSupport.isPredOfVarArg;

/**
 * A translator that turns a U-expression into a LIA formula.
 * Summations are replaced with vars and no stars are generated.
 */
public class LiaTranslator {
  private final NameSequence liaVarName;
  private final UTerm uexp;
  private final Map<UVar, List<Value>> varSchema;

  public static LiaStar translate(UTerm uexp, Map<UVar, List<Value>> varSchema) {
    return new LiaTranslator(uexp, varSchema).translate(new HashMap<>(), new HashMap<>());
  }

  public static LiaStar translate(UTerm uexp, Map<UVar, List<Value>> varSchema,
                                  Map<UVar, String> varMap, Map<USum, String> sumVarMap) {
    return new LiaTranslator(uexp, varSchema).translate(varMap, sumVarMap);
  }

  public static LiaStar translate(UTerm uexp, Map<UVar, List<Value>> varSchema, NameSequence liaVarName,
                                  Map<UVar, String> varMap, Map<USum, String> sumVarMap) {
    return new LiaTranslator(uexp, varSchema, liaVarName).translate(varMap, sumVarMap);
  }

  public LiaTranslator(UTerm uexp, Map<UVar, List<Value>> varSchema) {
    this(uexp, varSchema, NameSequence.mkIndexed("u", 0));
  }

  public LiaTranslator(UTerm uexp, Map<UVar, List<Value>> varSchema, NameSequence liaVarName) {
    this.liaVarName = liaVarName;
    this.uexp = uexp;
    this.varSchema = varSchema;
  }

  public LiaStar translate(Map<UVar, String> varMap, Map<USum, String> sumVarMap) {
    final UTerm expWithSumReplaced = LiaStarTranslatorSupport.replaceSummations(sumVarMap, new HashSet<>(), uexp, liaVarName);
    return translateRecursive(expWithSumReplaced, varMap);
  }

  private String newLiaVarName() {
    return liaVarName.next();
  }

  private LiaStar translateRecursive(UTerm exp, Map<UVar, String> varMap) {
    switch (exp.kind()) {
      case SUMMATION -> {
        // should not have summation!"
        assert false;
        return null;
      }
      case ADD -> {
        LiaStar result = null;
        final List<UTerm> subt = exp.subTerms();

        int constVal = 0;
        int i = 0;
        for (i = 0; i < subt.size(); ++i) {
          UTerm cur = subt.get(i);
          if (cur instanceof UConst) constVal = constVal + ((UConst) cur).value();
          else break;
        }
        if (i == subt.size()) return LiaStar.mkConst(false, constVal);

        for (UTerm t : subt) {
          final LiaStar curLia = translateRecursive(t, varMap);
          if (curLia instanceof LiaConstImpl) {
            if (((LiaConstImpl) curLia).getValue() == 0) continue;
          }
          if (result == null) {
            result = curLia;
          } else {
            if (result instanceof LiaIteImpl) result = ((LiaIteImpl) result).plusIte(curLia);
            else if (curLia instanceof LiaIteImpl) result = ((LiaIteImpl) curLia).plusIte(result);
            else result = LiaStar.mkPlus(false, result, curLia);
          }
        }
        return (result == null) ? LiaStar.mkConst(false, 0) : result;
      }
      case MULTIPLY -> {
        LiaStar result = null;
        final List<UTerm> subt = exp.subTerms();
        for (UTerm t : subt) {
          final LiaStar curLia = translateRecursive(t, varMap);
          if (curLia instanceof LiaConstImpl) {
            long value = ((LiaConstImpl) curLia).getValue();
            if (value == 0) return LiaStar.mkConst(false, 0);
            if (value == 1) continue;
          }
          if (result == null) result = curLia;
          else {
            if (result instanceof LiaIteImpl) result = ((LiaIteImpl) result).MultIte(curLia);
            else if (curLia instanceof LiaIteImpl) result = ((LiaIteImpl) curLia).MultIte(result);
            else result = LiaStar.mkMul(false, result, curLia);
          }
        }
        return (result == null) ? LiaStar.mkConst(false, 1) : result;
      }
      case NEGATION -> {
        final UTerm body = ((UNeg) exp).body();
        final LiaStar cond =
                LiaStar.mkEq(false, translateRecursive(body, varMap), LiaStar.mkConst(false, 0));
        return LiaStar.mkIte(false, cond, LiaStar.mkConst(false, 1), LiaStar.mkConst(false, 0));
      }
      case SQUASH -> {
        final UTerm body = ((USquash) exp).body();
        final LiaStar cond =
                LiaStar.mkEq(false, translateRecursive(body, varMap), LiaStar.mkConst(false, 0));
        return LiaStar.mkIte(false, cond, LiaStar.mkConst(false, 0), LiaStar.mkConst(false, 1));
      }
      case CONST -> {
        return LiaStar.mkConst(false, ((UConst) exp).value());
      }
      case TABLE -> {
        final UTable table = (UTable) exp;
        final String tblName = table.tableName().toString();
        final UVar tblVar = table.var();
        final LiaStar lVar = LiaStar.mkVar(false, varMap.computeIfAbsent(tblVar, v -> newLiaVarName()));
        return LiaStar.mkFunc(false, tblName, lVar, true);
        // TODO: table term >= 0
      }
      case PRED -> {
        if (exp instanceof ULiaVar) return LiaStar.mkVar(false, exp.toString());
        final UPred pred = (UPred) exp;
        if (pred.isPredKind(UPred.PredKind.FUNC) && isPredOfVarArg(pred)) {
          // Case 1. [p(a(t))]
          final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
          assert pArgs.size() == 1;
          final UVar predVar = pArgs.get(0); // a(t)
          final String predName = pred.predName().toString();
          final LiaStar lVar = LiaStar.mkVar(false, varMap.computeIfAbsent(predVar, v -> newLiaVarName()));
          final LiaStar funcLia = LiaStar.mkFunc(false, predName, lVar);
          return LiaStar.mkIte(
                  false,
                  LiaStar.mkEq(false, funcLia, LiaStar.mkConst(false, 0)),
                  LiaStar.mkConst(false, 0),
                  LiaStar.mkConst(false, 1));
        } else if (pred.isBinaryPred()) {
          // Case 2. [U-expr0 <binary op> U-expr1]
          final LiaStar liaVar0 = translateRecursive(pred.args().get(0), varMap);
          final LiaStar liaVar1 = translateRecursive(pred.args().get(1), varMap);

          if (liaVar0 instanceof LiaConstImpl && liaVar1 instanceof LiaConstImpl) {
            long value0 = ((LiaConstImpl) liaVar0).getValue();
            long value1 = ((LiaConstImpl) liaVar1).getValue();
            boolean b =
                    switch (pred.predKind()) {
                      case EQ -> value0 == value1;
                      case NEQ -> value0 != value1;
                      case LE -> value0 <= value1;
                      case LT -> value0 < value1;
                      case GE -> value0 >= value1;
                      case GT -> value0 > value1;
                      default -> throw new IllegalArgumentException("unsupported predicate in Uexpr.");
                    };
            return b ? LiaStar.mkConst(false, 1) : LiaStar.mkConst(false, 0);
          }

          final LiaStar target =
                  switch (pred.predKind()) {
                    case EQ -> LiaStar.mkEq(false, liaVar0, liaVar1);
                    case NEQ -> LiaStar.mkNot(false, LiaStar.mkEq(false, liaVar0, liaVar1));
                    case LE -> LiaStar.mkLe(false, liaVar0, liaVar1);
                    case LT -> LiaStar.mkLt(false, liaVar0, liaVar1);
                    case GE -> LiaStar.mkLe(false, liaVar1, liaVar0);
                    case GT -> LiaStar.mkLt(false, liaVar1, liaVar0);
                    default -> throw new IllegalArgumentException("unsupported predicate in Uexpr.");
                  };
          return LiaStar.mkIte(false, target, LiaStar.mkConst(false, 1), LiaStar.mkConst(false, 0));
        } else {
          throw new UnsupportedOperationException("unsupported UPred var type.");
        }
      }
      case VAR -> {
        final UVar uVar = ((UVarTerm) exp).var();
        final String varName = varMap.computeIfAbsent(uVar, v -> newLiaVarName());
        // decide var type
        final String type;
        assert uVar.kind() != null;
        if (uVar.kind() == UVar.VarKind.PROJ) {
          // type of $N(x) is determined by schema of x
          assert uVar.args().length == 1;
          final List<Value> schema = varSchema.get(uVar.args()[0]);
          assert schema != null;
          final int colIndex = CalciteSupport.columnNameToIndex(uVar.name().toString());
          type = schema.get(colIndex).type();
        } else {
          if (uVar.args().length == 1) {
            // type of a simple single-column tuple is type of that column
            final List<Value> schema = varSchema.get(uVar.args()[0]);
            assert schema != null;
            assert schema.size() == 1;
            type = schema.get(0).type();
          } else {
            // unsupported
            throw new UnsupportedOperationException("var occurrence " + uVar);
          }
        }
        return LiaStar.mkVar(false, varName, type);
      }
      case STRING -> {
        return LiaStar.mkString(false, ((UString) exp).value());
      }
      case FUNC -> {
        final UFunc func = (UFunc) exp;
        final String funcName = func.funcName().toString();
        final List<LiaStar> liaOps = new ArrayList<>();
        final List<UTerm> args = exp.subTerms();
        for (UTerm arg : args) {
          LiaStar liaOp = translateRecursive(arg, varMap);
          liaOps.add(liaOp);
        }
        return translateFunc(false, funcName, liaOps);
      }
      default -> {
        throw new IllegalArgumentException("unsupported Uexpr type.");
      }
    }
  }
}
