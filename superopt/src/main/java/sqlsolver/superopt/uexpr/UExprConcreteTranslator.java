package sqlsolver.superopt.uexpr;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.*;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.*;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.DateString;
import org.apache.calcite.util.NlsString;
import org.apache.calcite.util.TimeString;
import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.uexpr.normalizer.QueryUExprICRewriter;
import sqlsolver.superopt.uexpr.normalizer.QueryUExprNormalizer;
import sqlsolver.superopt.uexpr.normalizer.ScalarNormalizer;
import sqlsolver.superopt.uexpr.normalizer.ScalarTerm;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.calcite.sql.type.SqlTypeName.*;
import static sqlsolver.common.datasource.DbSupport.MySQL;
import static sqlsolver.common.utils.Commons.coalesce;
import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.calcite.CalciteSupport.*;
import static sqlsolver.superopt.uexpr.UExprSupport.*;

/**
 * This class translate two logical plan into two U-Expressions and
 * further normalize them.
 */
public class UExprConcreteTranslator {
  private static final String VAR_NAME_PREFIX = "x";
  private static final String FREE_VAR_NAME_PREFIX = "y";
  private RelNode p0, p1;
  private Schema schema;
  private final BiMap<VALUESTable, String> VALUESTablesReg;
  private final UExprConcreteTranslationResult result;
  // Options
  private final boolean enableIntegrityConstraintRewrite;
  private final boolean explainsPredicates;

  public record VarSchema(UVar var, List<Value> schema) {
  }

  /**
   * Context of translation of both U-expressions.
   * It aims to enforce the following guarantees:
   * <ul>
   * <li>Two UExpr's summation's boundedVars should not be the same;</li>
   * <li>Two UExpr's constToTuple, which indicates that same unique key tuple;</li>
   * <li>Bound vars matching the same ScalarTerm should be mapped to the same free var.</li>
   * </ul>
   * The context also provides other information (e.g. tuple schema).
   */
  public record Context(Set<UVar> boundVarSet,
                        Map<Integer, VarSchema> constToTuple,
                        Map<ScalarTerm, UVar> scalarToFV,
                        Map<UVar, List<Value>> scalarFVSchema) {
    static Context mk() {
      return new Context(
              new HashSet<>(),
              new HashMap<>(),
              new HashMap<>(),
              new HashMap<>());
    }

    /**
     * Merge "st" with an existing scalar-term-var entry
     * if the entry matches "st" or vice versa,
     * or create a new entry if such entry does not exist.
     *
     * @return the scalar term after merge/creation of entry
     */
    public ScalarTerm addScalarTerm(ScalarTerm st, Supplier<UVar> varSupplier, Supplier<List<Value>> varSchemaSupplier, Map<UVar, List<Value>> varSchemas) {
      for (Map.Entry<ScalarTerm, UVar> entry : new HashSet<>(scalarToFV.entrySet())) {
        final ScalarTerm entrySt = entry.getKey();
        final UVar entryFV = entry.getValue();
        if (entrySt.matches(st, varSchemas) != null) {
          // st is an instance of entrySt
          // do not update entrySt
          return entrySt;
        }
        if (st.matches(entrySt, varSchemas) != null) {
          // entrySt is an instance of st, but the inverse is false
          // generalize entrySt towards st
          scalarToFV.remove(entrySt);
          scalarToFV.put(st, entryFV);
          return st;
        }
      }
      // st is a new scalar term
      final UVar var = varSupplier.get();
      scalarToFV.put(st, var);
      scalarFVSchema.put(var, varSchemaSupplier.get());
      return st;
    }
  }

  UExprConcreteTranslator(RelNode p0, RelNode p1, Schema schema, int tweak) {
    this.p0 = p0;
    this.p1 = p1;
    this.schema = schema;

    this.VALUESTablesReg = HashBiMap.create();
    this.result = new UExprConcreteTranslationResult(p0, p1, schema);
    this.enableIntegrityConstraintRewrite = (tweak & UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE) != 0;
    this.explainsPredicates = (tweak & UEXPR_FLAG_NO_EXPLAIN_PREDICATES) == 0;
    freeVarNameSequence = NameSequence.mkIndexed(FREE_VAR_NAME_PREFIX, 0);
  }

  UExprConcreteTranslator(String sql0, String sql1, Schema baseSchema, int tweak) {
    this.VALUESTablesReg = HashBiMap.create();
    // Deal with sql queries with `VALUES` feature
    final VALUESTableParser parser = new VALUESTableParser(sql0, sql1, baseSchema);
    parser.parse();

    this.result = new UExprConcreteTranslationResult(p0, p1, baseSchema);
    this.enableIntegrityConstraintRewrite = (tweak & UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE) != 0;
    this.explainsPredicates = (tweak & UEXPR_FLAG_NO_EXPLAIN_PREDICATES) == 0;
  }

  UExprConcreteTranslationResult translate() {
    if (p0 == null || p1 == null) return null;

    final QueryTranslator translator0 = new QueryTranslator(p0, false, NameSequence.mkIndexed(VAR_NAME_PREFIX, 0));
    final QueryTranslator translator1 = new QueryTranslator(p1, true, NameSequence.mkIndexed(VAR_NAME_PREFIX, 0));

    // translation context for both expressions in unison
    final Context ctx = Context.mk();

    if (translator0.translate(ctx) && translator1.translate(ctx)) {
//      final UVar newOutVar = UVar.mkBase(UName.mk(VAR_NAME_PREFIX));
//      result.alignOutVar(newOutVar);
      return result;
    } else {
      return null;
    }
  }

  static <T> String getFullName(T col) {
    final Value value = (Value) col;
    if (value.qualification() == null) return value.name();
    else return value.toString();
  }

  public class QueryTranslator {
    private final RelNode plan;
    private final boolean isTargetSide;
    private final Map<String, UVar> auxVars; // Auxiliary variable from outer query.
    public final List<UVar> visibleVars; // visible vars in current scope.
    // for SINGLE_VALUE and scalar queries
    private final Set<ScalarTerm> scalarTerms; // terms representing scalar queries / source of SINGLE_VALUE

    private QueryTranslator(RelNode plan, boolean isTargetSide, NameSequence tupleVarSeq) {
      this.plan = plan;
      this.isTargetSide = isTargetSide;
      this.visibleVars = new ArrayList<>(3);
      this.auxVars = new HashMap<>();
      this.scalarTerms = new HashSet<>();
      // set fresh var name sequence to the new sequence.
      freshVarNameSequence = tupleVarSeq;
    }

    private void alignOutVar(UTerm expr) {
      // Invariant: out var is BASE type
      assert getVisibleVar().is(UVar.VarKind.BASE);
      final UVar freshOutVar = UVar.mkBase(UName.mk(VAR_NAME_PREFIX));

      expr.replaceVarInplace(getVisibleVar(), freshOutVar, false);
      putTupleVarSchema(freshOutVar, getTupleVarSchema(getVisibleVar()));
      pop(visibleVars);
      push(visibleVars, freshOutVar);
    }

    /*
     * Main translation function
     */

    /**
     * translate and normalize the plan.
     */
    private boolean translate(Context ctx) {
      // translate plan into U-exp
      UTerm expr = tr(plan, null);
      if (expr == null) return false;
      for (ScalarTerm term : scalarTerms) {
        expr = UMul.mk(term.toConstraint(), expr);
      }
      alignOutVar(expr);

      // normalize
      expr = UExprSupport.normalizeExpr(expr);
      if (enableIntegrityConstraintRewrite) {
        expr = normalizeWithIntegrityConstraints(expr, ctx.constToTuple);
      } else {
        expr = normalize(expr);
      }
      expr = scalarNormalize(expr, ctx);
      expr = finalNormalize(expr, ctx.boundVarSet);

      // check types
      checkType(expr);

      final UVar outVar = tail(visibleVars);
      assert visibleVars.size() == 1;
      assert auxVars.size() == 0;
      if (!isTargetSide) {
        result.srcExpr = expr;
        result.srcOutVar = outVar;
      } else {
        result.tgtExpr = expr;
        result.tgtOutVar = outVar;
      }
      return true;
    }

    /**
     * Translation functions for each operator.
     */
    private UTerm tr(RelNode node, RelNode father) {
      if (node instanceof Values)
        return trValues(node);
      if (node instanceof TableScan)
        return trInput(node);
      if (node instanceof Filter)
        return trFilter(node);
      if (node instanceof Project)
        return trProj(node);
      if (node instanceof Join)
        return trJoin(node);
      if (node instanceof SetOp)
        return trSetOp(node);
      if (node instanceof Aggregate)
        // father is for single_value case, especially left join single_value case
        return trAgg(node, father);
      if (node instanceof Sort)
        return trSort(node);
      throw new IllegalArgumentException("[Exception]: Unknown Plan Operator");
    }

    /*
     * Helper functions for translation.
     */

    /**
     * Check whether there is null val in arithmetic expression
     */
    private static boolean hasNullInArithmeticExpression(RexNode node) {

      if (node.getKind() == SqlKind.LITERAL && ((RexLiteral) node).getTypeName() == NULL)
        return true;

      // Unary arithmetic
      Function<SqlKind, Boolean> isUnaryArithmetic = (SqlKind kind) ->
              kind == SqlKind.NOT || kind == SqlKind.PLUS_PREFIX || kind == SqlKind.MINUS_PREFIX || kind == SqlKind.IS_TRUE
                      || kind == SqlKind.IS_FALSE || kind == SqlKind.IS_NOT_TRUE || kind == SqlKind.IS_NOT_FALSE || kind == SqlKind.IS_UNKNOWN;

      Function<SqlKind, Boolean> isBinaryArithmetic = (SqlKind kind) ->
              kind == SqlKind.EQUALS || kind == SqlKind.NOT_EQUALS || kind == SqlKind.LESS_THAN || kind == SqlKind.GREATER_THAN
                      || kind == SqlKind.GREATER_THAN_OR_EQUAL || kind == SqlKind.LESS_THAN_OR_EQUAL || kind == SqlKind.MINUS
                      || kind == SqlKind.PLUS || kind == SqlKind.MOD || kind == SqlKind.DIVIDE || kind == SqlKind.TIMES;

      if (isUnaryArithmetic.apply(node.getKind()))
        return hasNullInArithmeticExpression(((RexCall) node).getOperands().get(0));
      if (isBinaryArithmetic.apply(node.getKind()))
        return hasNullInArithmeticExpression(((RexCall) node).getOperands().get(0))
                || hasNullInArithmeticExpression(((RexCall) node).getOperands().get(1));

      return false;
    }

    /**
     * Put the schema of var into the cache.
     */
    public void putTupleVarSchema(UVar var, List<Value> varSchema) {
      assert var.is(UVar.VarKind.BASE);
      if (!isTargetSide) result.setSrcTupleVarSchema(var, varSchema);
      else result.setTgtTupleVarSchema(var, varSchema);
    }

    /**
     * Get the schema of var.
     */
    public List<Value> getTupleVarSchema(UVar var) {
      assert var.is(UVar.VarKind.BASE) || var.is(UVar.VarKind.CONCAT);
      if (var.is(UVar.VarKind.BASE)) {
        return !isTargetSide ? result.srcTupleVarSchemaOf(var) : result.tgtTupleVarSchemaOf(var);
      } else {
        assert any(Arrays.stream(var.args()).toList(), v -> v.is(UVar.VarKind.BASE));
        assert var.args().length >= 2;
        List<Value> schema = getTupleVarSchema(var.args()[0]);
        for (int i = 1; i < var.args().length; ++i) {
          schema = concat(schema, getTupleVarSchema(var.args()[i]));
        }
        return schema;
      }
    }

    public Map<UVar, List<Value>> getSchema() {
      return !isTargetSide ? result.getSrcSchema() : result.getTgtSchema();
    }

    /**
     * Delete a column in the var schema.
     */
    public void deleteTupleVarSchemaByIndexs(UVar var, List<Integer> indexs) {
      assert var.is(UVar.VarKind.BASE);
      final List<Value> varSchema = getTupleVarSchema(var);
      final List<Value> newVarSchema = new ArrayList<>();
      for (int i = 0; i < varSchema.size(); i++) {
        if (indexs.contains(i)) continue;
        newVarSchema.add(varSchema.get(i));
      }
      if (!isTargetSide) result.setSrcTupleVarSchema(var, newVarSchema);
      else result.setTgtTupleVarSchema(var, newVarSchema);
    }

    public UVar getVisibleVar() {
      final UVar var = tail(visibleVars);
      assert var != null;
      return var;
    }

    public UVar mkProjVar(Value value, UVar var) {
      final UName projFullName = UName.mk(getFullName(value));
      if (var.is(UVar.VarKind.BASE)) return UVar.mkProj(projFullName, var);
      else if (var.is(UVar.VarKind.PROJ)) return mkProjVar(value, var.args()[0]);
      // concat var
      for (UVar argVar : var.args()) {
        assert argVar.is(UVar.VarKind.BASE);
        final Value valueCol = getColValByIndexVal(value, getTupleVarSchema(var));
        if (getTupleVarSchema(argVar).contains(valueCol)) {
          assert valueCol != null;
          final UName projTrueName = UName.mk(getIndexStringByInfo(valueCol.qualification(), valueCol.name(), getTupleVarSchema(argVar)));
          return UVar.mkProj(projTrueName, argVar);
        }
      }
      throw new IllegalArgumentException("[Exception] value is not covered by the var's schema");
    }

    public UTerm replaceAllBoundedVars(UTerm expr) {
      expr = transformSubTerms(expr, this::replaceAllBoundedVars);
      if (expr.kind() != UKind.SUMMATION) return expr;

      final Set<UVar> oldVars = new HashSet<>(((USum) expr).boundedVars());
      for (UVar oldVar : oldVars) {
        final UVar newVar = mkFreshBaseVar();
        putTupleVarSchema(newVar, getTupleVarSchema(oldVar));
        expr = expr.replaceVar(oldVar, newVar, true);
      }
      return expr;
    }

    private static UPred.PredKind castBinaryOp2UPredOp(SqlKind opKind) {
      return switch (opKind) {
        case EQUALS -> UPred.PredKind.EQ;
        case NOT_EQUALS -> UPred.PredKind.NEQ;
        case LESS_THAN -> UPred.PredKind.LT;
        case LESS_THAN_OR_EQUAL -> UPred.PredKind.LE;
        case GREATER_THAN -> UPred.PredKind.GT;
        case GREATER_THAN_OR_EQUAL -> UPred.PredKind.GE;
        default -> throw new IllegalArgumentException("Unsupported binary operator: " + opKind);
      };
    }

    private boolean cannotEnumerateProjSummation(UTerm expr, UVar baseVar) {
      if (expr.kind() == UKind.TABLE && expr.isUsing(baseVar)) return true;
      if (expr.kind() == UKind.SUMMATION) return true;

      return any(expr.subTerms(), t -> cannotEnumerateProjSummation(t, baseVar));
    }

    private boolean usingUString(UTerm expr) {
      if (expr.kind() == UKind.STRING) return true;
      List<UTerm> subTerms = expr.subTerms();
      for (UTerm subTerm : subTerms) {
        if (usingUString(subTerm))
          return true;
      }
      return false;
    }

    private Set<TupleInstance> searchTupleInstances(UTerm expr, UVar baseVar) {
      final Set<TupleInstance> tupleInstances = new HashSet<>();
      for (UTerm subTerm : expr.subTerms())
        tupleInstances.addAll(searchTupleInstances(subTerm, baseVar));

      if (expr.kind() != UKind.MULTIPLY && expr.kind() != UKind.PRED) return tupleInstances;

      assert baseVar.is(UVar.VarKind.BASE);
      final List<Value> tupleSchema = getTupleVarSchema(baseVar);
      final List<UTerm> predicates = expr.kind() == UKind.MULTIPLY ? expr.subTermsOfKind(UKind.PRED) : new ArrayList<>(List.of(expr));
      final List<Integer> tupleValues = new ArrayList<>();
      for (Value value : tupleSchema) {
        boolean match = false;
        final UVarTerm targetProjVar = UVarTerm.mk(mkProjVar(value, baseVar));
        for (UTerm subTerm : predicates) {
          final UPred pred = (UPred) subTerm;
          if (pred.isPredKind(UPred.PredKind.EQ)) {
            assert pred.args().size() == 2;
            final UTerm arg0 = pred.args().get(0), arg1 = pred.args().get(1);
            if ((arg0.equals(targetProjVar) && arg1.kind() == UKind.CONST) ||
                    (arg1.equals(targetProjVar) && arg0.kind() == UKind.CONST)) {
              final UConst constVal = (UConst) (arg1.kind() == UKind.CONST ? arg1 : arg0);
              tupleValues.add(constVal.value());
              match = true;
              break;
            }
          } else if (varIsNullPred(pred) && getIsNullPredVar(pred).equals(targetProjVar.var())) {
            tupleValues.add(null);
            match = true;
            break;
          }
        }
        if (!match) return tupleInstances;
      }
      assert tupleValues.size() == tupleSchema.size();
      tupleInstances.add(TupleInstance.mk(tupleValues));

      return tupleInstances;
    }

    private UTerm propagateNullVar(UTerm expr, UVarTerm nullVarTerm) {
      expr = transformSubTerms(expr, e -> propagateNullVar(e, nullVarTerm));
      if (expr.kind() != UKind.PRED) return expr;

      final UPred pred = (UPred) expr;
      if (varIsNullPred(pred) && getIsNullPredVar(pred).equals(nullVarTerm.var())) return UConst.one();
      if (pred.isPredKind(UPred.PredKind.EQ)) {
        final UTerm arg0 = pred.args().get(0), arg1 = pred.args().get(1);
        if (!arg0.equals(nullVarTerm) && !arg1.equals(nullVarTerm)) return expr;
        final UTerm targetVarTerm = arg0.equals(nullVarTerm) ? arg0 : arg1;
        final UTerm otherTerm = targetVarTerm == arg0 ? arg1 : arg0;
        if (otherTerm.kind() == UKind.CONST) return UConst.zero();
        else if (otherTerm.kind() == UKind.VAR) return mkIsNullPred((UVarTerm) otherTerm);
      }

      return expr;
    }

    private void extractColRef(RexNode node, List<RexNode> results) {
      if (node.getKind() == SqlKind.INPUT_REF) {
        results.add(node);
        return;
      }
      if (node instanceof RexCall rexCall) {
        if (rexCall.getOperator().getKind().name().equalsIgnoreCase("CASE")) return;
        for (final RexNode child : rexCall.getOperands()) {
          extractColRef(child, results);
        }
      }
    }

    private UTerm mkProjEqCond(List<Value> outputs, List<RexNode> projList, UVar outVar, UVar inVar) {
      assert outputs.size() == projList.size();

      final List<UTerm> eqs = new ArrayList<>();
      for (int i = 0, bound = outputs.size(); i < bound; ++i) {
        final UVar outProjVar = mkProjVar(outputs.get(i), outVar);
        final ComposedUTerm projTargetTerm = mkValue(projList.get(i), inVar);
        if (projTargetTerm == null) return null;
        // handle scalar query, e.g. SELECT (SELECT EMP0.DEPTNO FROM EMP AS EMP0 WHERE EMP0.EMPNO < 20) AS D FROM EMP AS EMP
        if (projTargetTerm.getSubQueryOutVar() != null) {
          final UVar subQueryProjVar = mkProjVar(getValueByIndex(0), projTargetTerm.getSubQueryOutVar());
          projTargetTerm.replaceVar(subQueryProjVar, outProjVar);
          assert (projTargetTerm.toPredUTerm().kind() == UKind.SUMMATION);
          // here we assert that scalar query is a simple summation like \sum{t1} (R(t1) * p(t1) * [t = a(t1)])
          // fullSum is \sum{t1} (R(t1) * p(t1) * [t = a(t1)]), while smallSum is \sum{t1} (R(t1) * p(t1))
          final USum fullSum = (USum) projTargetTerm.toPredUTerm();
          final USum smallSum = USum.mk(fullSum.boundedVars(), remakeTerm(fullSum.body(), filter(fullSum.body().subTerms(),
                  t -> !t.isUsingProjVar(outProjVar))));
          final UTerm term1 = UMul.mk(USquash.mk(fullSum), UPred.mkBinary(UPred.PredKind.EQ, smallSum, UConst.one()));
          final UTerm term2 = UMul.mk(mkIsNullPred(outProjVar), UNeg.mk(smallSum));
          eqs.add(UAdd.mk(term1, term2));
          continue;
        }
        final ComposedUTerm outProjVarTerm = ComposedUTerm.mk(UVarTerm.mk(outProjVar));
        final UTerm freeProjTerm = ComposedUTerm.doComparatorOp(outProjVarTerm, projTargetTerm, SqlKind.EQUALS, true);
        eqs.add(freeProjTerm);
      }
      return UMul.mk(eqs);
    }

    private UTerm mkIsNullPredForAllAttrs(UVar base) {
      final List<Value> schema = getValueListBySize(getTupleVarSchema(base).size());
      final List<UTerm> subTerms = new ArrayList<>();
      for (Value v : schema) {
        final UVar projVar = mkProjVar(v, base);
        subTerms.add(mkIsNullPred(UVarTerm.mk(projVar)));
      }
      assert !subTerms.isEmpty();
      return subTerms.size() == 1 ? subTerms.get(0) : UMul.mk(subTerms);
    }

    /**
     * Translation of scalar subquery (common in mkValue cases).
     */
    private UTerm[] getScalarQueryTerm(UTerm child, UVar scalarVisibleVar) {
      final UVar newVar = mkFreshBaseVar();
      putTupleVarSchema(newVar, getTupleVarSchema(scalarVisibleVar));
      assert scalarVisibleVar != null && getTupleVarSchema(scalarVisibleVar).size() == 1;
      final UVar projScalarVar = mkProjVar(getValueByIndex(0), scalarVisibleVar);
      final UVar projNewVar = mkProjVar(getValueByIndex(0), newVar);
      final USum littleSum = USum.mk(new HashSet<>(Set.of(scalarVisibleVar)), UMul.mk(child));
      final USum bigSum = USum.mk(new HashSet<>(Set.of(scalarVisibleVar)), UMul.mk(child,
              UPred.mkBinary(UPred.PredKind.EQ, projNewVar, projScalarVar, true)));
      final UTerm[] result = {UAdd.mk(UMul.mk(UPred.mkBinary(UPred.PredKind.EQ, replaceAllBoundedVars(littleSum), UConst.one()), USquash.mk(bigSum)),
              UMul.mk(mkIsNullPred(projNewVar), UNeg.mk(replaceAllBoundedVars(littleSum)))), UVarTerm.mk(newVar)};
      return result;
    }

    /**
     * Translation for the aggregate operators.
     * The isNullResult indicates that whether the agg function is operated on empty set.
     */
    private UTerm mkAggOutVarEqCond(
            List<Value> outputs,
            List<AggregateCall> aggList,
            UVar outVar,
            UVar inVar,
            UTerm groupByTerm,
            boolean isGroupByEmpty,
            boolean isNullResult) {
      // The isNullResult indicates that whether the agg function is operated on empty set.
      assert outputs.size() == aggList.size();

      final List<UTerm> subTerms = new ArrayList<>();
      for (int i = 0, bound = outputs.size(); i < bound; ++i) {
        final Value value = outputs.get(i);
        final AggregateCall aggExpr = aggList.get(i);

        // correspond to tr in thesis
        final UVar outProjVar = mkProjVar(value, outVar);
        List<Value> aggValueRefs = getValueListByIndexes(aggExpr.getArgList(), getTupleVarSchema(inVar));
        boolean isCountStar = aggExpr.getArgList().size() == 0;
        if (isCountStar)
          aggValueRefs = getValueListBySize(getTupleVarSchema(inVar).size()); // Special case for `count(*)`

        final List<UVar> aggProjVars = map(aggValueRefs, v -> mkProjVar(v, inVar));

        final SqlKind aggFunc = aggExpr.getAggregation().getKind();
        final boolean dedupFlag = aggExpr.isDistinct();
        if (aggFunc == null) return null;
        UTerm aggTerm;
        // different aggregate functions return different values given at least one non-NULL input
        switch (aggFunc) {
          case COUNT -> {
            if (dedupFlag) {
              final UVar newBaseVar = mkFreshBaseVar();
              final List<Value> projValueList = getValueListBySize(aggValueRefs.size());
              putTupleVarSchema(newBaseVar, aggValueRefs);
              final List<UVar> newProjVars = map(projValueList, v -> mkProjVar(v, newBaseVar));
              final List<UTerm> innerEqPredList =
                      map(
                              zip(aggProjVars, newProjVars),
                              pair -> UPred.mkBinary(UPred.PredKind.EQ, pair.getLeft(), pair.getRight(), true));
              final UTerm innerEqs = UMul.mk(innerEqPredList);
              final UTerm innerNotNulls = UMul.mk(map(aggProjVars, UExprSupport::mkNotNullPred));
              final UTerm innerSummationBody = UMul.mk(groupByTerm, innerEqs, innerNotNulls);
              final UTerm innerSummation = USum.mk(UVar.getBaseVars(inVar), innerSummationBody);
              final UTerm outerSummation = USum.mk(UVar.getBaseVars(newBaseVar), USquash.mk(innerSummation));
              aggTerm = UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(outProjVar), outerSummation, true);
            } else if (!isCountStar) {
              final UTerm notNulls = UMul.mk(map(aggProjVars, UExprSupport::mkNotNullPred));
              final UTerm countSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNulls));
              aggTerm = UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(outProjVar), countSummation, true);
            } else {
              final UTerm countSummation = USum.mk(UVar.getBaseVars(inVar), groupByTerm);
              aggTerm = UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(outProjVar), countSummation, true);
            }
          }
          case SUM -> {
            assert aggProjVars.size() == 1;
            final UVar aggProjVar = aggProjVars.get(0);
            if (dedupFlag) {
              assert aggValueRefs.size() == 1;
              final UVar newBaseVar = mkFreshBaseVar();
              final List<Value> projValueList = getValueListBySize(aggValueRefs.size());
              putTupleVarSchema(newBaseVar, aggValueRefs);
              final UVar newProjVar = mkProjVar(projValueList.get(0), newBaseVar);
              final UTerm innerEqPred = UPred.mkBinary(UPred.PredKind.EQ, aggProjVar, newProjVar, true);
              final UTerm innerEqs = UMul.mk(innerEqPred);
              final UTerm innerSummationBody = UMul.mk(groupByTerm, innerEqs);
              final UTerm innerSummation = USum.mk(UVar.getBaseVars(inVar), innerSummationBody);
              final UTerm outerNotNull = UMul.mk(mkNotNullPred(newProjVar));
              final UTerm outerMultiplyBody = UMul.mk(UVarTerm.mk(newProjVar), outerNotNull, USquash.mk(innerSummation));
              final UTerm outerSummation = USum.mk(UVar.getBaseVars(newBaseVar), outerMultiplyBody);
              aggTerm = UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(outProjVar), outerSummation, true);
            } else {
              final UTerm sumProjVarTerm = UVarTerm.mk(aggProjVar);
              final UTerm notNull = mkNotNullPred(aggProjVar);
              final UTerm sumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, sumProjVarTerm));
              aggTerm = UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(outProjVar), sumSummation, true);
            }
            if (isNullResult) aggTerm = mkIsNullPred(UVarTerm.mk(outProjVar));
          }
          case AVG -> {
            assert aggProjVars.size() == 1;
            if (dedupFlag) {
              assert aggValueRefs.size() == 1;
              // COUNT DISTINCT
              final UVar newBaseVar = mkFreshBaseVar();
              final List<Value> projValueList = getValueListBySize(aggValueRefs.size());
              putTupleVarSchema(newBaseVar, aggValueRefs);
              final List<UVar> newCountProjVars = map(projValueList, v -> mkProjVar(v, newBaseVar));
              final List<UTerm> innerEqPredList =
                      map(
                              zip(aggProjVars, newCountProjVars),
                              pair -> UPred.mkBinary(UPred.PredKind.EQ, pair.getLeft(), pair.getRight(), true));
              final UTerm innerCountEqs = UMul.mk(innerEqPredList);
              final UTerm innerCountNotNulls = UMul.mk(map(aggProjVars, UExprSupport::mkNotNullPred));
              final UTerm innerCountSummationBody = UMul.mk(groupByTerm, innerCountEqs, innerCountNotNulls);
              final UTerm innerCountSummation = USum.mk(UVar.getBaseVars(inVar), innerCountSummationBody);
              final UTerm outerCountSummation = USum.mk(UVar.getBaseVars(newBaseVar), USquash.mk(innerCountSummation));

              // SUM DISTINCT
              final UVar newSumBaseVar = mkFreshBaseVar();
              putTupleVarSchema(newSumBaseVar, aggValueRefs);
              final UVar newProjVar = mkProjVar(projValueList.get(0), newSumBaseVar);
              final UTerm innerSumEqPred = UPred.mkBinary(UPred.PredKind.EQ, aggProjVars.get(0), newProjVar, true);
              final UTerm innerSumEqs = UMul.mk(innerSumEqPred);
              final UTerm innerSumSummationBody = UMul.mk(groupByTerm, innerSumEqs);
              final UTerm innerSumSummation = USum.mk(UVar.getBaseVars(inVar), innerSumSummationBody);
              final UTerm outerSumNotNull = UMul.mk(mkNotNullPred(newProjVar));
              final UTerm outerSumMultiplyBody = UMul.mk(UVarTerm.mk(newProjVar), outerSumNotNull, USquash.mk(innerSumSummation));
              final UTerm outerSumSummation = USum.mk(UVar.getBaseVars(newSumBaseVar), outerSumMultiplyBody);

              aggTerm =
                      UPred.mkBinary(
                              UPred.PredKind.EQ, UMul.mk(UVarTerm.mk(outProjVar), outerCountSummation), outerSumSummation, true);
            } else {
              final UTerm avgProjVar = UVarTerm.mk(aggProjVars.get(0));
              final UTerm notNull = mkNotNullPred(aggProjVars.get(0));
              final UTerm sumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar));
              final UTerm countSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm.copy(), notNull.copy()));
              aggTerm =
                      UPred.mkBinary(
                              UPred.PredKind.EQ, UMul.mk(UVarTerm.mk(outProjVar), countSummation), sumSummation, true);
            }
            if (isNullResult) aggTerm = mkIsNullPred(UVarTerm.mk(outProjVar));
          }
          case MAX, MIN -> {
            assert aggProjVars.size() == 1;
            final UVar maxminProjVar = aggProjVars.get(0);
            final UPred.PredKind predKind = aggFunc == SqlKind.MAX ? UPred.PredKind.GT : UPred.PredKind.LT;
            final UTerm notSumPred = UPred.mkBinary(predKind, maxminProjVar, outProjVar, true);
            final UTerm notSum = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notSumPred, mkNotNullPred(maxminProjVar)));
            final UTerm squashSumPred = UPred.mkBinary(UPred.PredKind.EQ, maxminProjVar, outProjVar, true);
            final UTerm squashSum = UMul.mk(USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm.copy(), squashSumPred)), mkNotNullPred(outProjVar));
            aggTerm = UMul.mk(UNeg.mk(notSum), USquash.mk(squashSum));
            if (isNullResult) aggTerm = mkIsNullPred(UVarTerm.mk(outProjVar));
          }
          case VAR_POP -> {
            // var_pop = (sum(t * t) - sum(t) * sum(t) / count(*)) / count(*)
            assert aggProjVars.size() == 1 && !dedupFlag;
            final UTerm avgProjVar = UVarTerm.mk(aggProjVars.get(0));
            final UTerm notNull = mkNotNullPred(aggProjVars.get(0));
            final UTerm sumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar));
            final UTerm squareSumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar, avgProjVar));
            final UTerm countSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm.copy(), notNull.copy()));
            final UTerm firstDivideTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(UMul.mk(sumSummation, sumSummation), countSummation)));
            final UTerm minusTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_MINUS), new ArrayList<>(List.of(squareSumSummation, firstDivideTerm)));
            final UTerm targetTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(minusTerm, countSummation)));

            aggTerm =
                    UPred.mkBinary(
                            UPred.PredKind.EQ, UVarTerm.mk(outProjVar), targetTerm, true);
          }
          case VAR_SAMP -> {
            // var_samp = (sum(t * t) - sum(t) * sum(t) / count(*)) / case when count(*) = 1 then null else count(*) - 1
            assert aggProjVars.size() == 1 && !dedupFlag;
            final UTerm avgProjVar = UVarTerm.mk(aggProjVars.get(0));
            final UTerm notNull = mkNotNullPred(aggProjVars.get(0));
            final UTerm sumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar));
            final UTerm squareSumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar, avgProjVar));
            final UTerm countSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm.copy(), notNull.copy()));
            final UTerm firstDivideTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(UMul.mk(sumSummation, sumSummation), countSummation)));
            final UTerm minusTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_MINUS), new ArrayList<>(List.of(squareSumSummation, firstDivideTerm)));

            final UTerm caseWhenTerm = UAdd.mk(UMul.mk(UPred.mkBinary(UPred.PredKind.EQ, countSummation, UConst.one()), mkIsNullPred(outProjVar)),
                    UMul.mk(UNeg.mk(UPred.mkBinary(UPred.PredKind.EQ, countSummation, UConst.one())), UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(outProjVar), UFunc.mk(UFunc.FuncKind.INTEGER,
                            UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(minusTerm, UFunc.mk(UFunc.FuncKind.INTEGER,
                                    UName.mk(PredefinedFunctions.NAME_MINUS), new ArrayList<>(List.of(countSummation, UConst.one())))))))));

            aggTerm = caseWhenTerm;
          }
          case STDDEV_POP -> {
            // stddev_pop = power((sum(t * t) - sum(t) * sum(t)) / count(*) / count(*), 0.5)
            assert aggProjVars.size() == 1 && !dedupFlag;
            final UTerm avgProjVar = UVarTerm.mk(aggProjVars.get(0));
            final UTerm notNull = mkNotNullPred(aggProjVars.get(0));
            final UTerm sumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar));
            final UTerm squareSumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar, avgProjVar));
            final UTerm countSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm.copy(), notNull.copy()));
            final UTerm firstDivideTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(UMul.mk(sumSummation, sumSummation), countSummation)));
            final UTerm minusTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_MINUS), new ArrayList<>(List.of(squareSumSummation, firstDivideTerm)));
            final UTerm targetTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(minusTerm, countSummation)));

            aggTerm =
                    UPred.mkBinary(
                            UPred.PredKind.EQ, UMul.mk(UVarTerm.mk(outProjVar), UVarTerm.mk(outProjVar)), targetTerm, true);
          }
          case STDDEV_SAMP -> {
            // stddev_samp = power((sum(t * t) - sum(t) * sum(t) / count(*)) / case when count(*) = 1 then null else count(*) - 1, 0.5)
            assert aggProjVars.size() == 1 && !dedupFlag;
            final UTerm avgProjVar = UVarTerm.mk(aggProjVars.get(0));
            final UTerm notNull = mkNotNullPred(aggProjVars.get(0));
            final UTerm sumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar));
            final UTerm squareSumSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm, notNull, avgProjVar, avgProjVar));
            final UTerm countSummation = USum.mk(UVar.getBaseVars(inVar), UMul.mk(groupByTerm.copy(), notNull.copy()));
            final UTerm firstDivideTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(UMul.mk(sumSummation, sumSummation), countSummation)));
            final UTerm minusTerm = UFunc.mk(UFunc.FuncKind.INTEGER,
                    UName.mk(PredefinedFunctions.NAME_MINUS), new ArrayList<>(List.of(squareSumSummation, firstDivideTerm)));

            final UTerm caseWhenTerm = UAdd.mk(UMul.mk(UPred.mkBinary(UPred.PredKind.EQ, countSummation, UConst.one()), mkIsNullPred(outProjVar)),
                    UMul.mk(UNeg.mk(UPred.mkBinary(UPred.PredKind.EQ, countSummation, UConst.one())), UPred.mkBinary(UPred.PredKind.EQ, UMul.mk(UVarTerm.mk(outProjVar), UVarTerm.mk(outProjVar)),
                            UFunc.mk(UFunc.FuncKind.INTEGER, UName.mk(PredefinedFunctions.NAME_DIVIDE), new ArrayList<>(List.of(minusTerm, UFunc.mk(UFunc.FuncKind.INTEGER,
                                    UName.mk(PredefinedFunctions.NAME_MINUS), new ArrayList<>(List.of(countSummation, UConst.one())))))))));

            aggTerm = caseWhenTerm;
          }
          case SINGLE_VALUE -> {
            assert aggProjVars.size() == 1 && !dedupFlag;
            // append single-row constraint
            final ScalarTerm st = ScalarTerm.mk(inVar.copy(), getTupleVarSchema(inVar), groupByTerm.copy());
            assert st != null;
            scalarTerms.add(st);
            // generate U-exp
            final UTerm summation = USum.mk(UVar.getBaseVars(inVar), groupByTerm);
            final UTerm squashSummation = USquash.mk(USum.mk(UVar.getBaseVars(inVar),
                    UMul.mk(groupByTerm,
                            UPred.mkBinary(UPred.PredKind.EQ, outProjVar, aggProjVars.get(0)))));
            final UTerm term1 = UMul.mk(UPred.mkBinary(UPred.PredKind.EQ, summation.copy(), UConst.one()), squashSummation);
            final UTerm term2 = UMul.mk(mkIsNullPred(outProjVar), UNeg.mk(summation.copy()));
            return UAdd.mk(term1, term2);
          }
          default -> {
            return null;
          }
        }
        // aggregate functions (except COUNT) returns NULL given empty/all-NULL input
        if (aggFunc != SqlKind.COUNT && !isNullResult) {
          assert aggProjVars.size() == 1 && aggValueRefs.size() == 1;
          final UVar aggProjVar = aggProjVars.get(0);
          final Value aggValueRef = aggValueRefs.get(0);
          final UTerm existsNotNull =
              USum.mk(
                  UVar.getBaseVars(inVar), UMul.mk(groupByTerm.copy(), mkNotNullPred(aggProjVar)));
          // if GROUP BY list is empty or aggregated column is nullable,
          // we need to do case analysis
          if (isGroupByEmpty || !aggValueRef.isNotNull()) {
            // case 1: there exists non-NULL input
            if (aggFunc != SqlKind.MAX && aggFunc != SqlKind.MIN)
              // aggTerm of MAX,MIN is enough to imply "existsNotNull"
              aggTerm = UMul.mk(aggTerm, USquash.mk(existsNotNull));
            // case 2: there is no non-NULL input
            aggTerm =
                UAdd.mk(aggTerm, UMul.mk(mkIsNullPred(outProjVar), UNeg.mk(existsNotNull.copy())));
          }
          // otherwise, groupTerm implies case 1
        }
        // rename bound vars
        aggTerm = replaceAllBoundedVars(aggTerm);
        subTerms.add(aggTerm);
      }
      return UMul.mk(subTerms);
    }

    /**
     * Translate in subquery equal conditions.
     */
    private UTerm mkInSubEqCond(List<ComposedUTerm> lhsExprTerms, UVar rhsVar) {
      final List<Value> rhsVarSchema = getValueListBySize(getTupleVarSchema(rhsVar).size());
      assert lhsExprTerms.size() == rhsVarSchema.size();

      final List<UTerm> eqs = new ArrayList<>();
      for (int i = 0, bound = lhsExprTerms.size(); i < bound; ++i) {
        final ComposedUTerm lhsExprTerm = lhsExprTerms.get(i);
        final UVar rhsProjVar = mkProjVar(rhsVarSchema.get(i), rhsVar);
        final ComposedUTerm rhsProjVarComposedTerm = ComposedUTerm.mk(UVarTerm.mk(rhsProjVar));
        final UTerm eqCond = ComposedUTerm.doComparatorOp(lhsExprTerm, rhsProjVarComposedTerm, SqlKind.EQUALS, false);
        eqs.add(eqCond);
      }
      assert !eqs.isEmpty();
      return eqs.size() == 1 ? eqs.get(0) : UMul.mk(eqs);
    }

    /**
     * Check whether a call matched translation for OR into IN_OP cases.
     * Specifically, E=C1 OR E=C2 OR ... E=CN -> E IN (C1,C2,...,CN).
     */
    private boolean isOrIn(RexCall call) {
      if (call.getKind() != SqlKind.OR) return false;
      if (call.getOperands().size() <= 1) return false;
      // for every argument, it should be a = CONST.
      if (any(call.getOperands(), o -> !(o instanceof RexCall rexCall)
              || rexCall.getOperands().size() != 2
              || rexCall.getOperator().getKind() != SqlKind.EQUALS)) return false;

      RexNode firstArgument = null;
      for (final RexNode operand : call.getOperands()) {
        final RexCall rexCall = (RexCall) operand;
        final RexNode op0 = rexCall.getOperands().get(0);
        final RexNode op1 = rexCall.getOperands().get(1);
        if (firstArgument == null) firstArgument = op0;
        if (!Objects.equals(firstArgument, op0)) return false;
        if (!(op1 instanceof RexLiteral)) return false;
      }
      return true;
    }

    /**
     * Make a UTerm that corresponds to two UVar equivalence:
     * x = y => for all column of x and y, construct the equivalence.
     * NOTE: two UVar should have same schema size.
     *
     * @param var1 the first considering var.
     * @param var2 the second considering var.
     * @return a U-expression term that indicates that two equivalent var.
     */
    private UTerm mkEqualVar(UVar var1, UVar var2, boolean nullSafe) {
      if (getTupleVarSchema(var1).size() != getTupleVarSchema(var2).size())
        throw new IllegalArgumentException("[Exception] two var should have same schema");

      int totalSize = getTupleVarSchema(var1).size();
      final List<UTerm> results = new ArrayList<>();

      for (int i = 0; i < totalSize; i++) {
        results.add(UPred.mkBinary(UPred.PredKind.EQ,
                mkProjVar(CalciteSupport.getValueByIndex(i), var1),
                mkProjVar(CalciteSupport.getValueByIndex(i), var2),
                nullSafe));
      }
      return UMul.mk(results);
    }

    /*
     * Sub translation function
     */

    /**
     * Translate a predicate into a U-expression.
     * Need to consider NULL value carefully.
     */
    private UTerm mkPredicate(RexNode exprCtx, UVar baseVar) {
      return UExprSupport.normalizeExpr(mkPredicate0(exprCtx, baseVar));
    }

    private UTerm mkPredicate0(RexNode exprCtx, UVar baseVar) {
      final SqlKind exprKind = exprCtx.getKind();
      switch (exprKind) {
        case INPUT_REF -> {
          // ColRef case
          final RexInputRef inputRef = (RexInputRef) exprCtx;
          final Value param = Value.mk(null, inputRef.getName());
          final UVar projVar = mkProjVar(param, baseVar);
          return UMul.mk(UVarTerm.mk(projVar), mkNotNullPred(projVar));
        }
        case LITERAL -> {
          // Literal case
          final RexLiteral literal = (RexLiteral) exprCtx;
          final RelDataType type = literal.getType();
          switch (type.getSqlTypeName()) {
            case BOOLEAN -> {
              final Boolean value = (Boolean) literal.getValue();
              return Boolean.TRUE.equals(value) ? UConst.one() : UConst.zero();
            }
            case NULL -> {
              return UConst.zero();
            }
            default ->
                    throw new IllegalArgumentException("[Exception] Unsupported literal value type: " + type.getSqlTypeName());
          }
        }
        case NOT, PLUS_PREFIX, MINUS_PREFIX, IS_TRUE, IS_FALSE,
                IS_NOT_TRUE, IS_NOT_FALSE, IS_UNKNOWN -> {
          // Unary case, except for null case
          final RexCall call = (RexCall) exprCtx;
          assert call.getOperands().size() == 1;
          final UTerm lhs = mkPredicate0(call.getOperands().get(0), baseVar);
          // The inputRefs extract all the columns of child, construct the null related terms
          // NOTE: this method will incur false negative/positive like ((a + b) IS NULL) IS NULL
          final List<RexNode> inputRefs = new ArrayList<>();
          final List<UTerm> inputRefTerms = new ArrayList<>();
          extractColRef(call.getOperands().get(0), inputRefs);
          for (final RexNode inputRef : inputRefs) {
            inputRefTerms.add(mkValue(inputRef, baseVar).toPredUTerm());
          }

          switch (exprKind) {
            case NOT -> {
              return UNeg.mk(lhs);
            }
            case PLUS_PREFIX, MINUS_PREFIX -> {
              return lhs;
            }
            case IS_TRUE -> {
              // NULL IS TRUE -> FALSE
              // return notnull(lhs) * [lhs != 0] => notnull(lhs) is transformed to notnull(a) * notnull(b) * ...
              if (inputRefTerms.size() != 0)
                return UMul.mk(UPred.mkBinary(UPred.PredKind.NEQ, lhs, UConst.zero(), true), mkNotNullPred(inputRefTerms));
              return UMul.mk(UPred.mkBinary(UPred.PredKind.NEQ, lhs, UConst.zero(), true));
            }
            case IS_FALSE -> {
              // NULL IS FALSE -> FALSE
              // return notnull(lhs) * [lhs = 0] => notnull(lhs) is transformed to notnull(a) * notnull(b) * ...
              if (inputRefTerms.size() != 0)
                return UMul.mk(UPred.mkBinary(UPred.PredKind.EQ, lhs, UConst.zero(), true), mkNotNullPred(inputRefTerms));
              return UMul.mk(UPred.mkBinary(UPred.PredKind.EQ, lhs, UConst.zero(), true));
            }
            case IS_NOT_TRUE -> {
              // NULL IS NOT TRUE -> TRUE
              // return || isnull(lhs) + notnull(lhs) * [lhs = 0] ||
              if (inputRefTerms.size() != 0)
                return USquash.mk(UAdd.mk(mkIsNullPred(inputRefTerms),
                        UMul.mk(mkNotNullPred(inputRefTerms), UPred.mkBinary(UPred.PredKind.EQ, lhs, UConst.zero(), true))));
              if (lhs.equals(UConst.nullVal())) {
                return UConst.one();
              }
              return UPred.mkBinary(UPred.PredKind.EQ, lhs, UConst.zero(), true);
            }
            case IS_NOT_FALSE -> {
              // NULL IS NOT FALSE -> TRUE
              // return || isnull(lhs) + notnull(lhs) * [lhs != 0] ||
              if (inputRefTerms.size() != 0)
                return USquash.mk(UAdd.mk(mkIsNullPred(inputRefTerms),
                        UMul.mk(mkNotNullPred(inputRefTerms), UPred.mkBinary(UPred.PredKind.NEQ, lhs, UConst.zero(), true))));
              if (lhs.equals(UConst.nullVal())) {
                return UConst.one();
              }
              return UPred.mkBinary(UPred.PredKind.NEQ, lhs, UConst.zero(), true);
            }
            default -> throw new IllegalArgumentException("[Exception] Unsupported unary operator: " + exprKind);
          }
        }
        case IS_NULL, IS_NOT_NULL -> {
          // Unary null case
          final RexCall call = (RexCall) exprCtx;
          assert call.getOperands().size() == 1;

          final List<RexNode> inputRefs = new ArrayList<>();
          final List<UTerm> inputRefTerms = new ArrayList<>();
          // specially not handle case for CASE in IsNull()
          if (call.getOperands().get(0).getKind() != SqlKind.CASE) {
            extractColRef(call.getOperands().get(0), inputRefs);
            for (final RexNode inputRef : inputRefs) {
              inputRefTerms.add(mkValue(inputRef, baseVar).toPredUTerm());
            }
          }

          final ComposedUTerm lhs = mkValue(call.getOperands().get(0), baseVar);
          final UTerm result = ComposedUTerm.doComparatorOp(lhs, ComposedUTerm.mk(UConst.nullVal()), SqlKind.EQUALS, true);
          switch (exprKind) {
            case IS_NULL -> {
              if (!inputRefTerms.isEmpty()) {
                return mkIsNullPred(inputRefTerms);
              }
              return result;
            }
            case IS_NOT_NULL -> {
              if (!inputRefTerms.isEmpty()) {
                return mkNotNullPred(inputRefTerms);
              }
              return UNeg.mk(result);
            }
          }
        }
        case AND, OR -> {
          // Binary logic case
          final RexCall call = (RexCall) exprCtx;
          final List<UTerm> argTerms = new ArrayList<>();
          // for translating multiple OR into IN_OP cases.
          if (exprKind == SqlKind.OR
                  && !explainsPredicates
                  && isOrIn(call)) {
            final List<RexNode> firstArguments = new ArrayList<>();
            final List<RexLiteral> secondArguments = new ArrayList<>();
            for (final RexNode operand : call.getOperands()) {
              final RexCall rexCall = (RexCall) operand;
              firstArguments.add(rexCall.getOperands().get(0));
              secondArguments.add((RexLiteral) rexCall.getOperands().get(1));
            }
            secondArguments.sort(Comparator.comparing(RexNode::toString));
            final List<UTerm> arguments = new ArrayList<>();
            arguments.add(mkValue(firstArguments.get(0), baseVar).toPredUTerm());
            all(secondArguments, literal -> arguments.add(mkValue(literal, baseVar).toPredUTerm()));
            String funcName = PredefinedFunctions.instantiateFamilyFunc(PredefinedFunctions.NAME_IN_LIST, arguments.size());
            UTerm in = UFunc.mk(UFunc.FuncKind.INTEGER, UName.mk(funcName), arguments);
            return UPred.mkBinary(UPred.PredKind.GT, in, UConst.zero());
          }
          // other cases.
          for (final RexNode operand : call.getOperands()) {
            final UTerm argTerm = mkPredicate0(operand, baseVar);
            if (argTerm == null) return null;
            argTerms.add(argTerm);
          }
          switch (exprKind) {
            case AND -> {
              // ... AND NULL -> 0
              if (any(argTerms, t -> t.equals(UConst.nullVal()))) return UConst.zero();
              return UMul.mk(argTerms);
            }
            case OR -> {
              // NULL OR NULL -> 0
              if (all(argTerms, t -> t.equals(UConst.nullVal()))) return UConst.zero();
              // ... OR NULL -> ...
              final List<UTerm> newArgTerms = new ArrayList<>();
              for (final UTerm argTerm : argTerms) {
                if (!argTerm.equals(UConst.nullVal())) {
                  newArgTerms.add(argTerm);
                }
              }
              return USquash.mk(UAdd.mk(newArgTerms));
            }
            default -> throw new IllegalArgumentException("[Exception] Unsupported binary operator: " + exprKind);
          }
        }
        case EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN,
                GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL -> {
          // Binary comparison case
          if (hasNullInArithmeticExpression(exprCtx))
            return UConst.zero();

          final RexCall call = (RexCall) exprCtx;
          assert call.getOperands().size() == 2;

          // only for those terms like [a = b] where a and b are columns
          final List<RexNode> inputRefs = new ArrayList<>();
          final List<UTerm> inputRefTerms = new ArrayList<>();
          extractColRef(call.getOperands().get(0), inputRefs);
          for (final RexNode inputRef : inputRefs) {
            inputRefTerms.add(mkValue(inputRef, baseVar).toPredUTerm());
          }

          ComposedUTerm lhs = null, rhs = null;

          // process minus
          if (call.getOperands().get(0).getKind() == SqlKind.MINUS) {
            // `a - b = c` => `a = b + c`
            final ComposedUTerm lhsLhs0 =
                    mkValue(((RexCall) (call.getOperands().get(0))).getOperands().get(0), baseVar);
            final ComposedUTerm lhsRhs0 =
                    mkValue(((RexCall) (call.getOperands().get(0))).getOperands().get(1), baseVar);
            final ComposedUTerm rhs0 = mkValue(call.getOperands().get(1), baseVar);
            if (lhsLhs0 == null || lhsRhs0 == null || rhs0 == null) return null;
            lhs = lhsLhs0;
            rhs = ComposedUTerm.doArithmeticOp(lhsRhs0, rhs0, SqlKind.PLUS);
          } else if (call.getOperands().get(1).getKind() == SqlKind.MINUS) {
            // `a = b - c` => `a + c = b`
            final ComposedUTerm rhsLhs0 =
                    mkValue(((RexCall) (call.getOperands().get(1))).getOperands().get(0), baseVar);
            final ComposedUTerm rhsRhs0 =
                    mkValue(((RexCall) (call.getOperands().get(1))).getOperands().get(1), baseVar);
            final ComposedUTerm lhs0 = mkValue(call.getOperands().get(0), baseVar);
            if (rhsLhs0 == null || rhsRhs0 == null || lhs0 == null) return null;
            lhs = ComposedUTerm.doArithmeticOp(rhsRhs0, lhs0, SqlKind.PLUS);
            rhs = rhsLhs0;
          }

          // common situation
          if (lhs == null && rhs == null) {
            lhs = mkValue(call.getOperands().get(0), baseVar);
            rhs = mkValue(call.getOperands().get(1), baseVar);
          }

          // handle scalar query
          if (call.getOperands().get(0).getKind() == SqlKind.SCALAR_QUERY
                  || call.getOperands().get(1).getKind() == SqlKind.SCALAR_QUERY) {
            if (call.getOperands().get(0).getKind() == SqlKind.SCALAR_QUERY
                    && call.getOperands().get(1).getKind() != SqlKind.SCALAR_QUERY) {
              final UTerm[] results = getScalarQueryTerm(lhs.toPredUTerm(), lhs.getSubQueryOutVar());
              final UTerm scalarTerm = results[0];
              final UVar newVar = ((UVarTerm) results[1]).var();
              lhs = ComposedUTerm.mk(scalarTerm);
              return USquash.mk(USum.mk(new HashSet<>(Set.of(newVar)), UMul.mk(lhs.toPredUTerm(),
                      ComposedUTerm.doComparatorOp(lhs, rhs, exprKind, false))));
            }
            if (call.getOperands().get(1).getKind() == SqlKind.SCALAR_QUERY
                    && call.getOperands().get(0).getKind() != SqlKind.SCALAR_QUERY) {
              final UTerm[] results = getScalarQueryTerm(rhs.toPredUTerm(), rhs.getSubQueryOutVar());
              final UTerm scalarTerm = results[0];
              final UVar newVar = ((UVarTerm) results[1]).var();
              rhs = ComposedUTerm.mk(scalarTerm);
              return USquash.mk(USum.mk(new HashSet<>(Set.of(newVar)), UMul.mk(rhs.toPredUTerm(),
                      ComposedUTerm.doComparatorOp(lhs, rhs, exprKind, false))));
            }
            // for both scalar subquery case
            final UTerm[] leftResults = getScalarQueryTerm(lhs.toPredUTerm(), lhs.getSubQueryOutVar());
            final UTerm[] rightResults = getScalarQueryTerm(rhs.toPredUTerm(), rhs.getSubQueryOutVar());
            final UTerm leftScalarTerm = leftResults[0];
            final UVar leftNewVar = ((UVarTerm) leftResults[1]).var();
            final UTerm rightScalarTerm = rightResults[0];
            final UVar rightNewVar = ((UVarTerm) rightResults[1]).var();
            final UVar projLeftNewVar = mkProjVar(getValueByIndex(0), leftNewVar);
            final UVar projRightNewVar = mkProjVar(getValueByIndex(0), rightNewVar);
            final List<UTerm> mulBody = new ArrayList<>();
            mulBody.add(leftScalarTerm);
            mulBody.add(rightScalarTerm);
            mulBody.add(ComposedUTerm.doComparatorOp(ComposedUTerm.mk(UVarTerm.mk(projLeftNewVar)),
                    ComposedUTerm.mk(UVarTerm.mk(projRightNewVar)),
                    exprKind,
                    false));
            mulBody.add(mkNotNullPred(projLeftNewVar));
            mulBody.add(mkNotNullPred(projRightNewVar));
            // normally here requires a squash, but left join also needs to add a squash
            // because we don't add squash in left join, here we also don't add squash
            return USum.mk(new HashSet<>(Set.of(leftNewVar, rightNewVar)), UMul.mk(mulBody));

          }


          if (lhs == null || rhs == null) return null;
          if (!inputRefTerms.isEmpty()) {
            return UMul.mk(ComposedUTerm.doComparatorOp(lhs, rhs, exprKind, false), mkNotNullPred(inputRefTerms));
          }
          return ComposedUTerm.doComparatorOp(lhs, rhs, exprKind, false);
        }
        case LIKE -> {
          // LIKE operator case
          final RexCall call = (RexCall) exprCtx;
          assert call.getOperands().size() == 2;
          final ComposedUTerm lhsComposedUTerm = mkValue(call.getOperands().get(0), baseVar);
          final ComposedUTerm rhsComposedUTerm = mkValue(call.getOperands().get(1), baseVar);
          // if the case only contain alphabet, construct EQ. e.g. t.a LIKE 'abC' -> [t.a = 'abC']
          // else construct the unresolved function
          if (lhsComposedUTerm == null || rhsComposedUTerm == null) return null;
          if (rhsComposedUTerm.toPredUTerm() instanceof UString string) {
            String regex = "^[0-9a-zA-Z]+$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(string.value());
            if (matcher.matches()) {
              return UPred.mkBinary(UPred.PredKind.EQ, lhsComposedUTerm.toPredUTerm(), rhsComposedUTerm.toPredUTerm(), false);
            } else {
              final UName funcName = UName.mk(PredefinedFunctions.NAME_LIKE);
              final UFunc func = UFunc.mk(UFunc.FuncKind.STRING, funcName, new ArrayList<>(List.of(lhsComposedUTerm.toPredUTerm(), rhsComposedUTerm.toPredUTerm())));
              return UPred.mkBinary(UPred.PredKind.LT, UConst.zero(), func);
            }
          }
        }
        case CASE -> {
          // CASE operator case
          final List<UTerm> whenConds = new ArrayList<>();
          final List<UTerm> thenExprs = new ArrayList<>();
          final RexCall call = (RexCall) exprCtx;
          assert call.getOperands().size() % 2 == 1;

          // construct whenConds and thenExprs
          int i = 0;
          for (; i < call.getOperands().size() - 1; i++) {
            if (i % 2 == 0) {
              // when conditions
              final UTerm whenCond = mkPredicate0(call.getOperands().get(i), baseVar);
              if (whenCond == null) return null;
              whenConds.add(whenCond);
            } else {
              // then exprs
              final UTerm thenExpr = mkPredicate0(call.getOperands().get(i), baseVar);
              if (thenExpr == null) return null;
              thenExprs.add(thenExpr);
            }
          }

          // construct elseExpr
          final UTerm elseExpr = mkPredicate0(call.getOperands().get(i), baseVar);
          if (elseExpr == null) return null;

          // construct UTerm of CASE WHEN ... THEN ... ELSE ... END
          assert whenConds.size() == thenExprs.size();
          final List<UTerm> caseWhenTerms = new ArrayList<>();
          for (int x = 0; x < whenConds.size(); x++) {
            final List<UTerm> preCondList = new ArrayList<>();
            for (int y = 0; y < x; y++)
              preCondList.add(UNeg.mk(whenConds.get(y).copy()));
            preCondList.add(whenConds.get(x).copy());
            final UTerm preCond = preCondList.size() == 1 ? preCondList.get(0) : UMul.mk(preCondList);
            caseWhenTerms.add(UMul.mk(preCond, thenExprs.get(x)));
          }
          final List<UTerm> elsePreCondList = new ArrayList<>();
          whenConds.forEach(c -> elsePreCondList.add(UNeg.mk(c.copy())));
          final UTerm elsePreCond = elsePreCondList.size() == 1 ? elsePreCondList.get(0) : UMul.mk(elsePreCondList);
          caseWhenTerms.add(UMul.mk(elsePreCond, elseExpr));

          return UAdd.mk(caseWhenTerms);
        }
        case EXISTS -> {
          // EXISTS case
          final RexSubQuery rexSubQuery = (RexSubQuery) exprCtx;

          final UVar lhsVisibleVar = baseVar;
          assert lhsVisibleVar != null;
          final UTerm rhs = tr(rexSubQuery.rel, null);
          if (rhs == null) return null;

          // Store lhs visible var and free var only, rhs vars are no longer visible.
          final UVar rhsVisibleVar = pop(visibleVars);
          assert rhsVisibleVar != null;

          return USquash.mk(USum.mk(UVar.getBaseVars(rhsVisibleVar), rhs));
        }
        case SEARCH -> {
          final RexNode expandSearch = RexUtil.expandSearch(new RexBuilder(JAVA_TYPE_FACTORY), null, exprCtx);
          return mkPredicate0(expandSearch, baseVar);
        }
        case IN -> {
          // IN case
          final RexSubQuery rexSubQuery = (RexSubQuery) exprCtx;

          final UVar lhsVisibleVar = baseVar;
          assert lhsVisibleVar != null;
          final UTerm rhs = tr(rexSubQuery.rel, null);
          if (rhs == null) return null;

          // Store lhs visible var and free var only, rhs vars are no longer visible.
          final UVar rhsVisibleVar = pop(visibleVars);
          assert rhsVisibleVar != null;

          final List<ComposedUTerm> lhsExprTerms = new ArrayList<>();
          for (final RexNode operand : rexSubQuery.getOperands()) {
            final ComposedUTerm term = mkValue(operand, lhsVisibleVar);
            if (term == null) return null;
            lhsExprTerms.add(term);
          }

          final UTerm eqCond = mkInSubEqCond(lhsExprTerms, rhsVisibleVar);
          final UTerm decoratedRhs = USum.mk(UVar.getBaseVars(rhsVisibleVar), UMul.mk(eqCond, rhs));
          return USquash.mk(decoratedRhs);
        }
      }
      throw new IllegalArgumentException("[Exception] Unsupported expr kind: " + exprKind);
    }

    /**
     * Translate a value into U-expression.
     */
    private ComposedUTerm mkValue(RexNode node, UVar baseVar) {
      final SqlKind nodeKind = node.getKind();
      switch (nodeKind) {
        case INPUT_REF -> {
          // ColRef case
          final RexInputRef inputRef = (RexInputRef) node;
          final Value param = Value.mk(null, inputRef.getName());
          final UVar projVar = mkProjVar(param, baseVar);
          return ComposedUTerm.mk(UVarTerm.mk(projVar));
        }
        case FIELD_ACCESS -> {
          // FIELD_ACCESS case, generally for those subquery who access outer column
          final RexFieldAccess fieldAccess = (RexFieldAccess) node;
          final int field = fieldAccess.getField().getIndex();
          final Value param = Value.mk(null, indexToColumnName(field));
          final UVar projVar = mkProjVar(param, auxVars.get(((RexCorrelVariable) fieldAccess.getReferenceExpr()).getName()));
          return ComposedUTerm.mk(UVarTerm.mk(projVar));
        }
        case LITERAL -> {
          // Literal case
          final RexLiteral literal = (RexLiteral) node;
          final RelDataType type = literal.getType();
          final SqlTypeName typeName;
          if (NULL.equals(literal.getTypeName())) {
            // NULL literals override the data type
            typeName = NULL;
          } else {
            typeName = type.getSqlTypeName();
          }
          switch (typeName) {
            case INTEGER, INTERVAL_DAY, BIGINT -> {
              // Interval_day here consider milliseconds
              final BigDecimal bigDecimal = (BigDecimal) literal.getValue();
              if (bigDecimal == null) return ComposedUTerm.mk(UConst.nullVal());
              final int value = bigDecimal.intValue();
              return ComposedUTerm.mk(UConst.mk(value));
            }
            case NULL -> {
              return ComposedUTerm.mk(UConst.nullVal());
            }
            case BOOLEAN -> {
              final Boolean value = (Boolean) literal.getValue();
              if (value == null) return ComposedUTerm.mk(UConst.nullVal());
              return ComposedUTerm.mk(UConst.mk(value ? 1 : 0));
            }
            case VARCHAR, CHAR -> {
              final NlsString nlsValue = (NlsString) literal.getValue();
              if (nlsValue == null) return ComposedUTerm.mk(UConst.nullVal());
              final String value = nlsValue.getValue();
              if (value == null) return ComposedUTerm.mk(UConst.nullVal());
              return ComposedUTerm.mk(UString.mk(value));
            }
            case DECIMAL -> {
              // for the Integer case, it will be handled by INTEGER,
              // so this case should handle fractional type
              final double value = ((BigDecimal) literal.getValue()).doubleValue();
              // if it is zero, then return 0
              if (value == 0) {
                return ComposedUTerm.mk(UConst.zero());
              }
              // Integer case, return integer
              if (value == Math.floor(value)) {
                return ComposedUTerm.mk(UConst.mk((int) Math.floor(value)));
              }
              // otherwise, treat it as an unresolved function
              final UName funcName = UName.mk(String.valueOf(value));
              return ComposedUTerm.mk(ComposedUTerm.mkFuncCall(UFunc.FuncKind.STRING, funcName, new ArrayList<>()));
            }
            case DATE -> {
              final DateString value = literal.getValueAs(DateString.class);
              assert value != null;
              final UName funcName = UName.mk("Date");
              return ComposedUTerm.mk(ComposedUTerm.mkFuncCall(UFunc.FuncKind.STRING, funcName, new ArrayList<>(List.of(ComposedUTerm.mk(UString.mk(value.toString()))))));
            }
            case TIMESTAMP -> {
              final TimeString value = literal.getValueAs(TimeString.class);
              if (value == null) return ComposedUTerm.mk(UConst.nullVal());
              return ComposedUTerm.mk(UString.mk(value.toString()));
            }
            case SYMBOL -> {
              final String value = literal.getValue().toString();
              assert value != null;
              return ComposedUTerm.mk(UString.mk(value));
            }
            default ->
                    throw new IllegalArgumentException("[Exception] Unsupported literal value type: " + type.getSqlTypeName());
          }
        }
        case AND, OR -> {
          // Binary logic case
          final RexCall call = (RexCall) node;

          final Stack<ComposedUTerm> argTerms = new Stack<>();
          for (final RexNode operand : call.getOperands()) {
            final ComposedUTerm argTerm = mkValue(operand, baseVar);
            if (argTerm == null) return null;
            argTerms.push(argTerm);
          }

          // Treat the argTerms as a stack, pop every 2 items to process
          while (argTerms.size() > 1) {
            final ComposedUTerm lhs = argTerms.pop();
            final ComposedUTerm rhs = argTerms.pop();
            argTerms.push(ComposedUTerm.doLogicOp(lhs, rhs, nodeKind));
          }
          assert argTerms.size() == 1;
          return argTerms.get(0);
        }
        case EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN,
                GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL -> {
          // Binary comparison case
          final RexCall call = (RexCall) node;
          assert call.getOperands().size() == 2;
          final ComposedUTerm lhs = mkValue(call.getOperands().get(0), baseVar);
          final ComposedUTerm rhs = mkValue(call.getOperands().get(1), baseVar);
          if (lhs == null || rhs == null) return null;
          return ComposedUTerm.mk(ComposedUTerm.doComparatorOp(lhs, rhs, nodeKind, false));
        }
        case MINUS, PLUS, MOD, DIVIDE, TIMES -> {
          // Binary arithmetic case
          final RexCall call = (RexCall) node;
          assert call.getOperands().size() == 2;
          final ComposedUTerm lhs = mkValue(call.getOperands().get(0), baseVar);
          final ComposedUTerm rhs = mkValue(call.getOperands().get(1), baseVar);
          if (lhs == null || rhs == null) return null;
          return ComposedUTerm.doArithmeticOp(lhs, rhs, nodeKind);
        }
        case NOT, PLUS_PREFIX, MINUS_PREFIX, IS_TRUE, IS_FALSE,
                IS_NOT_TRUE, IS_NOT_FALSE, IS_NULL, IS_NOT_NULL, IS_UNKNOWN -> {
          // Unary case
          final RexCall call = (RexCall) node;
          assert call.getOperands().size() == 1;
          final ComposedUTerm lhs = mkValue(call.getOperands().get(0), baseVar);
          if (lhs == null) return null;
          return ComposedUTerm.doUnaryOp(lhs, nodeKind);
        }
        case CASE -> {
          // CASE operator case
          final List<UTerm> whenConds = new ArrayList<>();
          final List<ComposedUTerm> thenExprs = new ArrayList<>();
          final RexCall call = (RexCall) node;
          assert call.getOperands().size() % 2 == 1;

          // construct whenConds and thenExprs
          int i = 0;
          for (; i < call.getOperands().size() - 1; i++) {
            if (i % 2 == 0) {
              // when conditions
              final UTerm whenCond = mkPredicate0(call.getOperands().get(i), baseVar);
              if (whenCond == null) return null;
              whenConds.add(whenCond);
            } else {
              // then exprs
              final ComposedUTerm thenExpr = mkValue(call.getOperands().get(i), baseVar);
              if (thenExpr == null) return null;
              thenExprs.add(thenExpr);
            }
          }

          // construct elseExpr
          final ComposedUTerm elseExpr = mkValue(call.getOperands().get(i), baseVar);
          if (elseExpr == null) return null;

          // construct UTerm of CASE WHEN ... THEN ... ELSE ... END
          assert whenConds.size() == thenExprs.size();
          final ComposedUTerm caseWhen = ComposedUTerm.mk();
          for (int x = 0; x < whenConds.size(); x++) {
            final List<UTerm> preCondList = new ArrayList<>();
            for (int y = 0; y < x; y++)
              preCondList.add(UNeg.mk(whenConds.get(y).copy()));
            preCondList.add(whenConds.get(x).copy());
            final UTerm preCond = preCondList.size() == 1 ? preCondList.get(0) : UMul.mk(preCondList);
            caseWhen.appendTermPair(preCond, thenExprs.get(x).toPredUTerm());
          }
          final List<UTerm> elsePreCondList = new ArrayList<>();
          whenConds.forEach(c -> elsePreCondList.add(UNeg.mk(c.copy())));
          UTerm elsePreCond = null;
          switch (elsePreCondList.size()) {
            case 0: {
              ArrayList<UTerm> args = new ArrayList<>();
              args.add(UConst.ZERO);
              args.add(UConst.ZERO);
              elsePreCond = UPred.mk(UPred.PredKind.EQ, UName.mk("="), args, true);
              break;
            }
            case 1: {
              elsePreCond = elsePreCondList.get(0);
              break;
            }
            default: {
              elsePreCond = UMul.mk(elsePreCondList);
            }
          }
          caseWhen.appendTermPair(elsePreCond, elseExpr.toPredUTerm());
          return caseWhen;
        }
        case IN, EXISTS -> {
          // IN, EXIST case
          return ComposedUTerm.mk(mkPredicate(node, baseVar));
        }
        case CAST -> {
          // CAST operator case
          final RexCall call = (RexCall) node;
          assert call.getOperands().size() == 1;
          // just directly return the child node
          return mkValue(call.getOperands().get(0), baseVar);
        }
        case SCALAR_QUERY -> {
          // SCALAR_QUERY case
          final RexSubQuery rexSubQuery = (RexSubQuery) node;

          final UVar lhsVisibleVar = tail(visibleVars);
          assert lhsVisibleVar != null;
          final UTerm rhs = tr(rexSubQuery.rel, null);
          if (rhs == null) return null;
          final UVar scalarVar = pop(visibleVars);
          assert scalarVar != null;
          final List<Value> scalarFreeSchema = CalciteSupport.getValueListBySize(getTupleVarSchema(scalarVar).size());
          assert scalarFreeSchema.size() == 1;
          // append single-row constraint
          final ScalarTerm st = ScalarTerm.mk(scalarVar.copy(), getTupleVarSchema(scalarVar), rhs.copy());
          assert st != null;
          scalarTerms.add(st);

          return ComposedUTerm.mk(rhs, scalarVar);
        }
        case EXTRACT -> {
          // EXTRACT case
          final RexCall call = (RexCall) node;
          assert call.getOperands().size() == 2;
          final ComposedUTerm rhs = mkValue(call.getOperands().get(1), baseVar);
          if (call.getOperands().get(0) instanceof RexLiteral literal) {
            return ComposedUTerm.mk(ComposedUTerm.mkFuncCall(UFunc.FuncKind.STRING,
                    UName.mk(literal.getValue().toString().toLowerCase()),
                    new ArrayList<>(List.of(rhs))));
          }
        }
        case OTHER_FUNCTION, OTHER, TRIM, ROW -> {
          // OTHER_FUNCTION case: unresolved function
          final RexCall function = (RexCall) node;
          final UName funcName = UName.mk(function.getOperator().getName());
          final List<ComposedUTerm> arguments = new ArrayList<>();
          for (final RexNode argument : function.getOperands()) {
            arguments.add(mkValue(argument, baseVar));
          }
          return ComposedUTerm.mk(ComposedUTerm.mkFuncCall(UFunc.FuncKind.STRING, funcName, arguments));
        }
      }
      throw new IllegalArgumentException("[Exception] Unsupported value kind: " + nodeKind);
    }

    /**
     * Translation for Values node.
     * The corresponding node in the calcite plan is LogicalValues.
     */
    private UTerm trValues(RelNode node) {
      final Values values = (Values) node;

      final ImmutableList<ImmutableList<RexLiteral>> literals = values.getTuples();
      assert literals.size() >= 1;
      final UVar baseVar = mkFreshBaseVar();
      final List<Value> projValueList = getValueListBySize(literals.get(0).size());
      assert projValueList.size() == values.getRowType().getFieldList().size();
      zip(projValueList, values.getRowType().getFieldList(), (value, typeField) -> value.setType(typeField.getType().getSqlTypeName().getName()));

      putTupleVarSchema(baseVar, projValueList);
      push(visibleVars, baseVar);

      final List<UTerm> addFactors = new ArrayList<>();
      for (ImmutableList<RexLiteral> tuple : literals) {
        final List<UTerm> mulFactors = new ArrayList<>();
        for (int i = 0, bound = tuple.size(); i < bound; ++i) {
          final UVar projVar = mkProjVar(projValueList.get(i), baseVar);
          final RexLiteral value = tuple.get(i);
          UTerm valueTerm = null;
          if (value.getValue() == null) mulFactors.add(mkIsNullPred(UVarTerm.mk(projVar)));
          else {
            switch (value.getTypeName()) {
              case CHAR -> {
                valueTerm = UString.mk(value.getValueAs(NlsString.class).getValue());
              }
              case DECIMAL -> {
                valueTerm = UConst.mk(value.getValueAs(BigDecimal.class).intValue());
              }
              case BOOLEAN -> {
                valueTerm = value.getValueAs(Boolean.class) ? UConst.one() : UConst.zero();
              }
              default -> {
                throw new IllegalArgumentException("[Exception]: Unknown Value Type");
              }
            }
            mulFactors.add(UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(projVar), valueTerm, true));
          }
        }
        addFactors.add(UMul.mk(mulFactors));
      }

      if (addFactors.size() == 0) return UConst.zero();
      else return addFactors.size() == 1 ? addFactors.get(0) : UAdd.mk(addFactors);
    }

    /**
     * Translation for Input node.
     * The corresponding node in the calcite plan is LogicalTableScan.
     */
    private UTerm trInput(RelNode node) {
      final TableScan input = (TableScan) node;
      assert (input.getTable().getQualifiedName().size() == 1);
      final String tableName = input.getTable().getQualifiedName().get(0);

      final UVar baseVar = mkFreshBaseVar();
      final List<Value> schemas = getValueListByCalciteTable(input.getTable(), schema);
      assert schemas.size() == input.getRowType().getFieldList().size();
      zip(schemas, input.getRowType().getFieldList(), (value, typeField) -> value.setType(typeField.getType().getSqlTypeName().getName()));
      putTupleVarSchema(baseVar, schemas);
      push(visibleVars, baseVar);


      return UTable.mk(UName.mk(tableName), baseVar);
    }

    /**
     * Translation for Filter node.
     * The corresponding node in the calcite plan is LogicalFilter.
     */
    private UTerm trFilter(RelNode relNode) {
      final UTerm predecessor = tr(relNode.getInput(0), relNode);
      if (predecessor == null) return null;

      final Filter filter = (Filter) relNode;
      final RexNode predExpr = filter.getCondition();
      final Set<CorrelationId> variableSet = filter.getVariablesSet();
      assert variableSet.size() <= 1;

      final UVar visibleVar = getVisibleVar();
      assert visibleVar != null;

      // if there is a correlated variable, it must be an auxVar
      if (variableSet.size() == 1) {
        CorrelationId variable = variableSet.iterator().next();
        auxVars.put(variable.getName(), visibleVar);
      }
      final UTerm pred = mkPredicate(predExpr, visibleVar);

      // delete the corresponding correlated variable in auxVars
      if (variableSet.size() == 1) {
        CorrelationId variable = variableSet.iterator().next();
        auxVars.remove(variable.getName());
      }
      if (pred == null) return null;

      return UMul.mk(predecessor, pred);
    }

    /**
     * Translation for Proj node.
     * The corresponding node in the calcite plan is LogicalProject.
     */
    private UTerm trProj(RelNode relNode) {
      final UTerm predecessor = tr(relNode.getInput(0), relNode);
      if (predecessor == null) return null;

      final Project proj = (Project) relNode;
      final UVar visibleVar = pop(visibleVars);
      assert visibleVar != null;

      final List<Value> outputValues = getValueListByIndexExpr(proj.getProjects(), getTupleVarSchema(visibleVar));
      final List<Value> projValueList = getValueListBySize(outputValues.size());
      UVar outVar = mkFreshBaseVar();
      // outputValues store the schema, while projValueList indicates the index
      putTupleVarSchema(outVar, outputValues);
      push(visibleVars, outVar);

      /*
       * Generate the summation body
       */

      final UTerm eqCond = mkProjEqCond(projValueList, proj.getProjects(), outVar, visibleVar);
      if (eqCond == null) return null;

      // Case 1. Common summation introduced by Proj, since input expr is not from VALUES table
      // when visible var is not baseVar, use this case
      if (all(UVar.getBaseVars(visibleVar), v -> cannotEnumerateProjSummation(predecessor, v))
              || !visibleVar.is(UVar.VarKind.BASE)) {
        return USum.mk(UVar.getBaseVars(visibleVar), UMul.mk(eqCond, predecessor));
      }

      // Case 2. Input expr is from VALUES table: build a template to expand summation with finite tuples
      final UTerm normalizedPredecessor = UExprSupport.normalizeExpr(predecessor);
      final UTerm template = UMul.mk(eqCond, normalizedPredecessor);
      final Set<TupleInstance> tupleInstances = searchTupleInstances(normalizedPredecessor, visibleVar);
      final List<UTerm> instantiatedTerms = new ArrayList<>();
      for (TupleInstance tupleInstance : tupleInstances) {
        UTerm instantiation = template.copy();
        for (var pair : zip(getTupleVarSchema(visibleVar), tupleInstance.values())) {
          final Value schemaAttr = pair.getLeft();
          final Integer val = pair.getRight();
          final UVarTerm projVarTerm = UVarTerm.mk(mkProjVar(schemaAttr, visibleVar));
          if (val != null) instantiation = instantiation.replaceAtomicTerm(projVarTerm, UConst.mk(val));
          else instantiation = propagateNullVar(instantiation, projVarTerm); // Null value of tuple
        }
        instantiatedTerms.add(instantiation);
      }

      UTerm res;
      if (instantiatedTerms.size() == 0) {
        if (usingUString(template)) {
          res = USum.mk(UVar.getBaseVars(visibleVar), template).copy();
          return res;
        }
        res = UConst.zero();
      } else res = instantiatedTerms.size() == 1 ? instantiatedTerms.get(0) : UAdd.mk(instantiatedTerms);

      return res;
    }

    /**
     * Translation for Join node.
     * The corresponding node in the calcite plan is LogicalJoin.
     */
    private UTerm trJoin(RelNode relNode) {
      final UTerm lhs = tr(relNode.getInput(0), relNode);
      final UTerm rhs = tr(relNode.getInput(1), relNode);
      if (lhs == null || rhs == null) return null;

      final Join join = (Join) relNode;
      final JoinRelType joinKind = join.getJoinType();
      final UVar rhsVisibleVar = pop(visibleVars);
      final UVar lhsVisibleVar = pop(visibleVars);
      assert rhsVisibleVar != null && lhsVisibleVar != null;

      final UVar outVisibleVar = UVar.mkConcat(lhsVisibleVar, rhsVisibleVar);
      push(visibleVars, outVisibleVar);

      final RexNode joinCond = join.getCondition();
      assert joinCond != null;

      UTerm joinCondTerm = null;
      if (joinCond != null) {
        joinCondTerm = mkPredicate(joinCond, outVisibleVar);
      } else {
        ArrayList<UTerm> args = new ArrayList<>();
        args.add(UConst.one());
        args.add(UConst.one());
        joinCondTerm = UPred.mk(UPred.PredKind.EQ, UName.mk("="), args, false);
      }
      if (joinCondTerm == null) return null;

      final UTerm innerJoinBody = UMul.mk(lhs, rhs, joinCondTerm);
      if (joinKind == JoinRelType.INNER) return innerJoinBody;

      // Left Join
      if (joinKind == JoinRelType.LEFT) {
        UTerm newSum = USum.mk(UVar.getBaseVars(rhsVisibleVar), UMul.mk(rhs, joinCondTerm).copy());
        newSum = replaceAllBoundedVars(newSum);
        // if rhs is scalar query, multiply another term : [summation <= 1]
        if (checkRelNodeScalar(relNode.getInput(1))) {
          // scalar query is a single multiplication with an addition
          // the term is like [1 = \sum{t1}E] * || \sum{t1}(E * [t = a(t1)] || + IsNull(t) * not(\sum{t1}E)
          // the targetSum here is getting the \sum{t1}E in [1 = \sum{t1}E]
          final UPred targetPred = (UPred) filter(rhs.subTerms().get(0).subTerms().get(0).subTerms(), t -> t instanceof UPred pred
                  && pred.isPredKind(UPred.PredKind.EQ)
                  && any(pred.args(), a -> a.equals(UConst.one()))).get(0);
          final USum targetSum = (USum) filter(targetPred.args(), a -> a.kind() == UKind.SUMMATION).get(0);
          final UMul lJoinBody = UMul.mk(lhs.copy(),
                  mkIsNullPredForAllAttrs(rhsVisibleVar),
                  UNeg.mk(newSum), UPred.mkBinary(UPred.PredKind.LE, targetSum, UConst.one()));
          return UAdd.mk(innerJoinBody, lJoinBody);
        }
        final UMul lJoinBody = UMul.mk(lhs.copy(), mkIsNullPredForAllAttrs(rhsVisibleVar), UNeg.mk(newSum));
        return UAdd.mk(innerJoinBody, lJoinBody);
      }
      // Right Join
      if (joinKind == JoinRelType.RIGHT) {
        UTerm newSum = USum.mk(UVar.getBaseVars(lhsVisibleVar), UMul.mk(lhs, joinCondTerm).copy());
        newSum = replaceAllBoundedVars(newSum);
        // if lhs is scalar query, multiply another term : [summation <= 1]
        // the same as LEFT JOIN
        if (checkRelNodeScalar(relNode.getInput(0))) {
          final UPred targetPred = (UPred) filter(lhs.subTerms().get(0).subTerms().get(0).subTerms(), t -> t instanceof UPred pred
                  && pred.isPredKind(UPred.PredKind.EQ)
                  && any(pred.args(), a -> a.equals(UConst.one()))).get(0);
          final USum targetSum = (USum) filter(targetPred.args(), a -> a.kind() == UKind.SUMMATION).get(0);
          final UMul rJoinBody = UMul.mk(rhs.copy(),
                  mkIsNullPredForAllAttrs(lhsVisibleVar),
                  UNeg.mk(newSum), UPred.mkBinary(UPred.PredKind.LE, targetSum, UConst.one()));
          return UAdd.mk(innerJoinBody, rJoinBody);
        }
        final UMul rJoinBody = UMul.mk(rhs.copy(), mkIsNullPredForAllAttrs(lhsVisibleVar), UNeg.mk(newSum));
        return UAdd.mk(innerJoinBody, rJoinBody);
      }
      // Full Join
      if (joinKind == JoinRelType.FULL) {
        UTerm newSumLJoin = USum.mk(UVar.getBaseVars(rhsVisibleVar), UMul.mk(rhs, joinCondTerm).copy());
        newSumLJoin = replaceAllBoundedVars(newSumLJoin);
        final UTerm lJoinBody = UMul.mk(lhs.copy(), mkIsNullPredForAllAttrs(rhsVisibleVar), UNeg.mk(newSumLJoin));

        UTerm newSumRJoin = USum.mk(UVar.getBaseVars(lhsVisibleVar), UMul.mk(lhs, joinCondTerm).copy());
        newSumRJoin = replaceAllBoundedVars(newSumRJoin);
        final UTerm rJoinBody = UMul.mk(rhs.copy(), mkIsNullPredForAllAttrs(lhsVisibleVar), UNeg.mk(newSumRJoin));

        return UAdd.mk(innerJoinBody, lJoinBody, rJoinBody);
      }

      throw new IllegalArgumentException("[Exception] Unsupported join type: " + joinKind);
    }

    /**
     * Translation for SetOp node.
     * Only consider Union, Intersect and Except here.
     */
    private UTerm trSetOp(RelNode relNode) {
      final UTerm lhs = tr(relNode.getInput(0), relNode);
      final UTerm rhs = tr(relNode.getInput(1), relNode);
      if (lhs == null || rhs == null) return null;

      final UVar rhsVisibleVar = pop(visibleVars);
      final UVar lhsVisibleVar = pop(visibleVars);
      assert rhsVisibleVar != null && lhsVisibleVar != null;
      assert lhsVisibleVar.kind() == rhsVisibleVar.kind()
              && lhsVisibleVar.args().length == rhsVisibleVar.args().length
              : "different visible var types of SetOp's children";

      final List<Value> lhsSchema = getValueListBySize(getTupleVarSchema(lhsVisibleVar).size());
      final List<Value> rhsSchema = getValueListBySize(getTupleVarSchema(rhsVisibleVar).size());
      assert lhsSchema.size() == rhsSchema.size() : "Schemas of SetOp's children are not aligned";
      // Replace each projVar in rhs with corresponding projVar of lhs
      // Make lhs visible var to be output visible var
      for (var schemaPair : zip(lhsSchema, rhsSchema)) {
        final UVar lhsProjVar = mkProjVar(schemaPair.getLeft(), lhsVisibleVar);
        final UVar rhsProjVar = mkProjVar(schemaPair.getRight(), rhsVisibleVar);
        rhs.replaceVarInplace(rhsProjVar, lhsProjVar, false);
      }
      for (int i = 0, bound = lhsVisibleVar.args().length; i < bound; i++) {
        rhs.replaceVarInplace(rhsVisibleVar.args()[i], lhsVisibleVar.args()[i], false);
      }
      push(visibleVars, lhsVisibleVar);

      if (relNode instanceof Union union) {
        UTerm result = UAdd.mk(lhs, rhs);
        if (!union.all) result = USquash.mk(result);
        return result;
      }

      if (relNode instanceof Intersect intersect) {
        if (!intersect.all) return USquash.mk(UMul.mk(lhs, rhs));
        else return UAdd.mk(UMul.mk(lhs, USquash.mk(rhs)), UMul.mk(USquash.mk(lhs.copy()), rhs.copy()));
      }

      if (relNode instanceof Minus except) {
        UTerm result = UMul.mk(lhs, UNeg.mk(rhs));
        if (!except.all) return USquash.mk(result);
        return result;
      }

      throw new IllegalArgumentException("[Exception] Unknown set operator");
    }

    /**
     * Translation for Aggregation node.
     * The corresponding node in the calcite plan is LogicalAggregate.
     * HINT: The default calcite plan doesn't have the exception case like:
     * <p>
     * <code>SELECT a, AGG(expr) FROM T GROUP BY a, b
     * <br/>it will be transformed to:<br/>
     * SELECT S.a1, S.a3 FROM (SELECT a AS a1, b AS a2, AGG(expr) AS a3 FROM T GROUP BY a, b) AS S</code>
     */
    private UTerm trAgg(RelNode relNode, RelNode father) {
      final UTerm predecessor = tr(relNode.getInput(0), relNode);
      if (predecessor == null) return null;

      final Aggregate agg = (Aggregate) relNode;
      final UVar visibleVar = pop(visibleVars);
      assert visibleVar != null;

      final UVar outBaseVar = mkFreshBaseVar();
      final List<Value> outputValues = getValueListByAgg(agg.getGroupSet().asList(), agg.getAggCallList(), getTupleVarSchema(visibleVar));
      final List<Value> projValueList = getValueListBySize(outputValues.size());
      assert outputValues.size() == agg.getRowType().getFieldList().size();
      zip(outputValues, agg.getRowType().getFieldList(), (value, typeField) -> value.setType(typeField.getType().getSqlTypeName().getName()));
      putTupleVarSchema(outBaseVar, outputValues);
      push(visibleVars, outBaseVar);

      final List<AggregateCall> aggregateExprs = agg.getAggCallList();
      final List<Value> aggregateOutputs = projValueList.subList(agg.getGroupSet().asList().size(), projValueList.size());
      final List<RexNode> commonProjExprs = getRexInputRefByIndexs(agg.getGroupSet().asList());
      final List<Value> commonProjOutputs = projValueList.subList(0, agg.getGroupSet().asList().size());

      /*
       * TODO:  add an extra Proj if:
       *  1. groupBy lists is different from select list
       *  2. If `HAVING count(*) > 1` but `count(*)` does not exists in select list
       */


      final List<UTerm> subTerms = new ArrayList<>(3);
      // Part1: Eq-conditions for common projected columns in select list
      final UTerm commonProjBody = commonProjExprs.isEmpty() ?
              predecessor :
              UMul.mk(predecessor, mkProjEqCond(commonProjOutputs, commonProjExprs, outBaseVar, visibleVar));
      if (!commonProjExprs.isEmpty()) {
        final UTerm commonProjTerm = USquash.mk(USum.mk(UVar.getBaseVars(visibleVar), commonProjBody));
        subTerms.add(commonProjTerm);
      }

      // Part2: Eq-conditions for Aggregated result in select list
      if (!aggregateExprs.isEmpty()) {
        final boolean isNullResult = normalize(commonProjBody.copy()).equals(UConst.zero());
        final UTerm aggregateTerm =
                mkAggOutVarEqCond(aggregateOutputs,
                        aggregateExprs,
                        outBaseVar,
                        visibleVar,
                        commonProjBody.copy(),
                        commonProjExprs.isEmpty(),
                        isNullResult);
        if (aggregateTerm == null) return null;
        subTerms.add(aggregateTerm);
      }

      // Part3: There is no need to handle HAVING due to calcite's rewrite
      return UMul.mk(subTerms);
    }

    /**
     * Translation for Sort node.
     * The corresponding node in the calcite plan is LogicalSort.
     * This translation only hack for fetch 0 cases.
     */
    private UTerm trSort(RelNode relNode) {
      final UTerm predecessor = tr(relNode.getInput(0), relNode);
      if (predecessor == null) return null;

      final Sort sort = (Sort) relNode;
      if (sort.fetch instanceof RexLiteral rexLiteral
              && rexLiteral.getValueAs(BigDecimal.class) != null
              && rexLiteral.getValueAs(BigDecimal.class).intValue() == 0) {
        return UConst.zero();
      }
      throw new IllegalArgumentException("[Exception] Unsupported Sort Case");
    }

    /*
     * U-expr Normalization and rewriting functions
     */

    private UTerm normalize(UTerm expr) {
      return new QueryUExprNormalizer(expr, schema, this).normalizeTerm();
    }

    private UTerm scalarNormalize(UTerm expr, Context ctx) {
      return new ScalarNormalizer(expr, schema, this, ctx).normalizeTerm();
    }

    private UTerm finalNormalize(UTerm expr, Set<UVar> boundVarSet) {
      return new QueryUExprNormalizer(expr, schema, this).finalNormalizeTerm(boundVarSet);
    }

    private UTerm normalizeWithIntegrityConstraints(UTerm expr, Map<Integer, VarSchema> constToTuple) {
      return new QueryUExprICRewriter(expr, schema, this, constToTuple).normalizeTerm();
    }

    /*
     * Type checking
     */

    /**
     * Check var types in a U-expression.
     */
    private void checkType(UTerm expr) {
      if (expr instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) {
        final UTerm lhs = pred.args().get(0);
        final UTerm rhs = pred.args().get(1);
        final UVarTerm vt1 = isSingleVarTermSet(collectVarTerms(lhs));
        final UVarTerm vt2 = isSingleVarTermSet(collectVarTerms(rhs));
        if (vt1 != null && isIntegerExpr(rhs)
                && !isVarFromTable(vt1.var().args()[0])) {
          updateValueType(vt1.var(), Value.TYPE_INT);
        } else if (vt2 != null && isIntegerExpr(lhs)
                && !isVarFromTable(vt2.var().args()[0])) {
          updateValueType(vt2.var(), Value.TYPE_INT);
        }
      }
      // recursion
      for (UTerm sub : expr.subTerms()) {
        checkType(sub);
      }
    }

    private boolean isVarFromTable(UVar var) {
      assert var.kind() == UVar.VarKind.BASE;
      final List<Value> schema = getTupleVarSchema(var);
      return all(schema, v -> v.qualification() != null);
    }
    
    // return the single PROJ var term in the set,
    // or null if there is zero or multiple terms or non-PROJ var in the set
    private UVarTerm isSingleVarTermSet(Set<UVarTerm> set) {
      if (set.size() != 1) return null;
      final UVarTerm vt = set.stream().toList().get(0);
      if (vt.var().kind() == UVar.VarKind.PROJ) return vt;
      return null;
    }

    // return var terms that expr contains
    private Set<UVarTerm> collectVarTerms(UTerm expr) {
      if (expr instanceof UVarTerm vt) {
        return new HashSet<>(Set.of(vt));
      }
      final Set<UVarTerm> result = new HashSet<>();
      for (UTerm sub : expr.subTerms()) {
        result.addAll(collectVarTerms(sub));
      }
      return result;
    }

    private boolean isIntegerExpr(UTerm expr) {
      switch (expr.kind()) {
        case CONST, PRED, SQUASH, NEGATION, TABLE -> {
          return true;
        }
        case STRING -> {
          return false;
        }
        case VAR -> {
          final UVarTerm vt = (UVarTerm) expr;
          final UVar var = vt.var();
          if (var.kind() != UVar.VarKind.PROJ) return false;
          final int index = columnNameToIndex(var.name().toString());
          final Value value = getTupleVarSchema(var.args()[0]).get(index);
          return Value.isIntegralType(value.type());
        }
      }
      return all(expr.subTerms(), this::isIntegerExpr);
    }

    private void updateValueType(UVar var, String valueType) {
      assert var.kind() == UVar.VarKind.PROJ;
      final int index = columnNameToIndex(var.name().toString());
      final UVar tuple = var.args()[0];
      final List<Value> schema = new ArrayList<>(getTupleVarSchema(tuple));
      final Value value = schema.get(index).copy();
      value.setType(valueType);
      schema.set(index, value);
      putTupleVarSchema(tuple, schema);
    }

  }

  /**
   * Inner-classes data structures used for U-expr translation
   */
  static class ComposedUTerm {
    // (p1 /\ v1) \/ (p2 /\ v2) \/ ...
    private final List<Pair<UTerm, UTerm>> preCondAndValues;

    private boolean subQuery = false;

    private UVar subQueryOutVar = null;

    ComposedUTerm(List<Pair<UTerm, UTerm>> preCondAndValues) {
      this.preCondAndValues = preCondAndValues;
    }

    ComposedUTerm(UTerm preCond, UTerm value) {
      this.preCondAndValues = new ArrayList<>();
      preCondAndValues.add(Pair.of(preCond, value));
    }

    ComposedUTerm(UTerm value) {
      this.preCondAndValues = new ArrayList<>();
      preCondAndValues.add(Pair.of(UConst.one(), value));
    }

    ComposedUTerm(UTerm value, UVar subQueryOutVar) {
      this.preCondAndValues = new ArrayList<>();
      preCondAndValues.add(Pair.of(UConst.one(), value));
      this.subQuery = true;
      this.subQueryOutVar = subQueryOutVar;
    }

    ComposedUTerm() {
      this.preCondAndValues = new ArrayList<>();
    }

    void appendTermPair(UTerm preCond, UTerm value) {
      this.preCondAndValues.add(Pair.of(preCond, value));
    }

    UTerm toPredUTerm() {
      // return [p1] * v1 + [p2] * v2 + ...
      final List<UTerm> subTerms = new ArrayList<>();
      for (var pair : preCondAndValues) {
        subTerms.add(UMul.mk(pair.getLeft(), pair.getRight()).copy());
      }

      return UExprSupport.normalizeExpr(UAdd.mk(subTerms));
    }

    static ComposedUTerm doUnaryOp(ComposedUTerm pred0, SqlKind opKind) {
      final List<Pair<UTerm, UTerm>> pair = new ArrayList<>();
      for (var pair0 : pred0.preCondAndValues) {
        final UTerm preCond = pair0.getLeft();
        final UTerm value = pair0.getRight();
        final UTerm finalValue;
        switch (opKind) {
          case MINUS_PREFIX -> {
            if (value.kind() == UKind.CONST) {
              final int V = ((UConst) value).value();
              finalValue = UConst.mk(-V);
            } else throw new IllegalArgumentException("[Exception] Unsupported unary operator: " + opKind);
          }
          case PLUS_PREFIX -> {
            if (value.kind() == UKind.CONST) {
              final int V = ((UConst) value).value();
              finalValue = UConst.mk(V);
            } else throw new IllegalArgumentException("[Exception] Unsupported unary operator: " + opKind);
          }
          case NOT -> {
            if (value.kind() == UKind.CONST && value.equals(UConst.nullVal())) {
              finalValue = value.copy();
            } else {
              finalValue = UNeg.mk(value.copy());
            }
          }
          case IS_TRUE -> {
            // [value = TRUE]
            finalValue = UPred.mkBinary(UPred.PredKind.EQ, value, UConst.one(), true);
          }
          case IS_FALSE -> {
            // [value = FALSE]
            finalValue = UPred.mkBinary(UPred.PredKind.EQ, value, UConst.zero(), true);
          }
          case IS_NOT_TRUE -> {
            // not([value = TRUE])
            finalValue = UNeg.mk(UPred.mkBinary(UPred.PredKind.EQ, value, UConst.one(), true));
          }
          case IS_NOT_FALSE -> {
            // not([value = FALSE])
            finalValue = UNeg.mk(UPred.mkBinary(UPred.PredKind.EQ, value, UConst.zero(), true));
          }
          case IS_NULL -> {
            // isNull(value)
            finalValue = mkIsNullPred(value);
          }
          case IS_NOT_NULL -> {
            // notNull(value)
            finalValue = mkNotNullPred(value);
          }
          default -> throw new IllegalArgumentException("[Exception] Unsupported unary operator: " + opKind);
        }
        pair.add(Pair.of(preCond, finalValue));
      }
      return ComposedUTerm.mk(pair);
    }

    static ComposedUTerm doArithmeticOp(ComposedUTerm pred0, ComposedUTerm pred1, SqlKind opKind) {
      assert opKind == SqlKind.TIMES
              || opKind == SqlKind.DIVIDE
              || opKind == SqlKind.MOD
              || opKind == SqlKind.PLUS
              || opKind == SqlKind.MINUS;
      final List<Pair<UTerm, UTerm>> pair = new ArrayList<>();
      for (var pair0 : pred0.preCondAndValues) {
        for (var pair1 : pred1.preCondAndValues) {
          final UTerm preCond0 = pair0.getLeft(), preCond1 = pair1.getLeft();
          final UTerm value0 = pair0.getRight(), value1 = pair1.getRight();
          final UTerm combinedPreCond;
          if (preCond0.equals(UConst.ONE)) combinedPreCond = preCond1.copy();
          else if (preCond1.equals(UConst.ONE)) combinedPreCond = preCond0.copy();
          else combinedPreCond = UMul.mk(preCond0.copy(), preCond1.copy());
          final UTerm combinedValue;
          switch (opKind) {
            case PLUS -> combinedValue = UAdd.mk(value0.copy(), value1.copy());
            case TIMES -> combinedValue = UMul.mk(value0.copy(), value1.copy());
            case MINUS -> {
              if (value0.kind() == UKind.CONST && value1.kind() == UKind.CONST) {
                final Integer lhsV = ((UConst) value0).value(), rhsV = ((UConst) value1).value();
                if (lhsV - rhsV >= 0)
                  combinedValue = UConst.mk(lhsV - rhsV);
                else
                  throw new IllegalArgumentException("[Exception] Unsupported minus operator with result less than 0");
              } else {
                UName funcName = UName.mk(PredefinedFunctions.NAME_MINUS);
                combinedValue = UFunc.mk(UFunc.FuncKind.INTEGER, funcName, new ArrayList<>(List.of(value0, value1)));
              }
            }
            case DIVIDE -> {
              if (value0.kind() == UKind.CONST && value1.kind() == UKind.CONST) {
                final Integer lhsV = ((UConst) value0).value(), rhsV = ((UConst) value1).value();
                if (rhsV * (lhsV / rhsV) == lhsV)
                  combinedValue = UConst.mk(lhsV / rhsV);
                else
                  throw new IllegalArgumentException("[Exception] Unsupported divide operator with result's exception");
              } else {
                UName funcName = UName.mk(PredefinedFunctions.NAME_DIVIDE);
                combinedValue = UFunc.mk(UFunc.FuncKind.INTEGER, funcName, new ArrayList<>(List.of(value0, value1)));
              }
            }
            default -> throw new IllegalArgumentException("[Exception] Unsupported binary operator: " + opKind);
          }
          pair.add(Pair.of(combinedPreCond, combinedValue));
        }
      }

      return ComposedUTerm.mk(pair);
    }

    static Pair<UTerm, UTerm> convertDivisionToProduct(UTerm value0, UTerm value1) {
      // If value0 is a / b and value1 is c / d,
      // then convert "a / b = c / d" to "a * d = c * b"
      // TODO: keep track of whether a/b/c/d is an integer
      final UTerm dividend0, divisor0, dividend1, divisor1;
      if (value0 instanceof UFunc func0
              && PredefinedFunctions.NAME_DIVIDE.equals(func0.funcName().toString())) {
        if (any(func0.args(), a -> a.equals(UConst.nullVal()))) return Pair.of(value0, value1);
        dividend0 = func0.args().get(0);
        divisor0 = func0.args().get(1);
      } else {
        dividend0 = value0;
        divisor0 = null;
      }
      if (value1 instanceof UFunc func1
              && PredefinedFunctions.NAME_DIVIDE.equals(func1.funcName().toString())) {
        if (any(func1.args(), a -> a.equals(UConst.nullVal()))) return Pair.of(value0, value1);
        dividend1 = func1.args().get(0);
        divisor1 = func1.args().get(1);
      } else {
        dividend1 = value1;
        divisor1 = null;
      }
      final UTerm left = divisor1 == null ? dividend0 : UMul.mk(dividend0, divisor1);
      final UTerm right = divisor0 == null ? dividend1 : UMul.mk(dividend1, divisor0);
      return Pair.of(left, right);
    }

    static UTerm doComparatorOp(ComposedUTerm pred0, ComposedUTerm pred1, SqlKind opKind, boolean isMkProjEqCond) {
      assert opKind.belongsTo(SqlKind.COMPARISON);
      final List<UTerm> subTerms = new ArrayList<>();
      // For each combination of pre-conditions, construct comparison between two values
      for (var pair0 : pred0.preCondAndValues) {
        for (var pair1 : pred1.preCondAndValues) {
          final UTerm preCond0 = pair0.getLeft(), preCond1 = pair1.getLeft();
          UTerm value0 = pair0.getRight(), value1 = pair1.getRight();
          // handle division
          if (SqlKind.EQUALS.equals(opKind)
                  && !value0.equals(UConst.NULL)
                  && !value1.equals(UConst.NULL)) {
            Pair<UTerm, UTerm> newValues = convertDivisionToProduct(value0, value1);
            value0 = newValues.getLeft();
            value1 = newValues.getRight();
          }
          // combine pre-conditions
          final UTerm combinedPreCond;
          if (preCond0.equals(UConst.ONE)) combinedPreCond = preCond1.copy();
          else if (preCond1.equals(UConst.ONE)) combinedPreCond = preCond0.copy();
          else combinedPreCond = UMul.mk(preCond0.copy(), preCond1.copy());
          // construct comparison of two values
          final UTerm compareTerm;
          if (isMkProjEqCond) {
            // Used in `mkProjEqCond` for tuple's attribute mapping
            if (value0.equals(UConst.NULL)) compareTerm = mkIsNullPred(value1.copy());
            else if (value1.equals(UConst.NULL)) compareTerm = mkIsNullPred(value0.copy());
            else compareTerm = UPred.mkBinary(UPred.PredKind.EQ, value0.copy(), value1.copy(), true);
          } else {
            // But `mkInSubEqCond` uses code here, since attrs of In sub-query should be not NULL
            if (value0.equals(UConst.NULL) || value1.equals(UConst.NULL))
              compareTerm = UConst.zero();
            else {
              final UPred.PredKind uPredOp = QueryTranslator.castBinaryOp2UPredOp(opKind);
              final UTerm comp = UPred.mkBinary(uPredOp, value0.copy(), value1.copy(), false);
              if (uPredOp == UPred.PredKind.EQ && value0.kind() == UKind.VAR && value1.kind() == UKind.VAR)
                compareTerm = UMul.mk(comp, mkNotNullPred(value0.copy()));
              else compareTerm = comp;
            }
          }
          subTerms.add(combinedPreCond.equals(UConst.ONE) ? compareTerm : UMul.mk(combinedPreCond, compareTerm));
        }
      }
      return UAdd.mk(subTerms);
    }

    static ComposedUTerm doLogicOp(ComposedUTerm pred0, ComposedUTerm pred1, SqlKind opKind) {
      assert opKind == SqlKind.AND || opKind == SqlKind.OR;
      final List<Pair<UTerm, UTerm>> pair = new ArrayList<>();
      for (var pair0 : pred0.preCondAndValues) {
        for (var pair1 : pred1.preCondAndValues) {
          final UTerm preCond0 = pair0.getLeft(), preCond1 = pair1.getLeft();
          final UTerm value0 = pair0.getRight(), value1 = pair1.getRight();
          final UTerm combinedPreCond;
          if (preCond0.equals(UConst.ONE)) combinedPreCond = preCond1.copy();
          else if (preCond1.equals(UConst.ONE)) combinedPreCond = preCond0.copy();
          else combinedPreCond = UMul.mk(preCond0.copy(), preCond1.copy());
          final UTerm combinedValue;
          switch (opKind) {
            case OR -> combinedValue = USquash.mk(UAdd.mk(value0.copy(), value1.copy()));
            case AND -> combinedValue = UMul.mk(value0.copy(), value1.copy());
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + opKind);
          }
          pair.add(Pair.of(combinedPreCond, combinedValue));
        }
      }

      return ComposedUTerm.mk(pair);
    }

    static UTerm mkFuncCall(UFunc.FuncKind funcKind, UName funcName, List<ComposedUTerm> arguments) {
      List<UTerm> funcArguments = new ArrayList<>();
      for (ComposedUTerm composedArg : arguments) {
//        assert composedArg.preCondAndValues.size() == 1;
//        final UTerm left = composedArg.preCondAndValues.get(0).getLeft();
//        assert left.equals(UConst.ONE);
        funcArguments.add(composedArg.toPredUTerm());
      }
      return UFunc.mk(funcKind, funcName, funcArguments);
    }

    UVar getSubQueryOutVar() {
      if (this.subQuery) return subQueryOutVar;
      return null;
    }

    void replaceVar(UVar baseVar, UVar repVar) {
      for (Pair<UTerm, UTerm> pair : this.preCondAndValues) {
        pair.getRight().replaceVarInplace(baseVar, repVar, false);
      }
    }

    static ComposedUTerm mk(List<Pair<UTerm, UTerm>> preCondAndValues) {
      return new ComposedUTerm(preCondAndValues);
    }

    static ComposedUTerm mk(UTerm preCond, UTerm value) {
      return new ComposedUTerm(preCond, value);
    }

    static ComposedUTerm mk(UTerm value) {
      return new ComposedUTerm(value);
    }

    static ComposedUTerm mk(UTerm value, UVar subQueryOutVar) {
      return new ComposedUTerm(value, subQueryOutVar);
    }

    static ComposedUTerm mk() {
      return new ComposedUTerm();
    }
  }

  static class VALUESTable {
    final Map<TupleInstance, Integer> tupleMultiSet;
    final List<String> schema;

    VALUESTable(List<TupleInstance> tuples, List<String> schema) {
      this.tupleMultiSet = new HashMap<>();
      this.schema = schema;
      for (TupleInstance tuple : tuples)
        tupleMultiSet.put(tuple, tupleMultiSet.getOrDefault(tuple, 0) + 1);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      VALUESTable valuesTable = (VALUESTable) o;
      return Objects.equals(tupleMultiSet, valuesTable.tupleMultiSet) && Objects.equals(schema, valuesTable.schema);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tupleMultiSet, schema);
    }

    static VALUESTable parse(String str, List<String> defaultSchema) {
      final List<TupleInstance> tuples = new ArrayList<>();
      final List<String> schema = new ArrayList<>();
      // `str` = (VALUES (1, 2), (3, 4)) or (VALUES)
      String data = str.substring(7, str.length() - 1); // (1, 2), (3, 4)
      data = data.replaceAll(" ", ""); // (1,2),(3,4)
      if (data.isEmpty()) // (VALUES) is an empty table
        return new VALUESTable(tuples, coalesce(defaultSchema, schema));

      final String[] tupleList = data.substring(1, data.length() - 1).split("\\),\\(");
      for (String tuple : tupleList) {
        // tuple = 1,2
        final List<String> valueStrs = Arrays.stream(tuple.split(",")).toList();
        final List<Integer> values = new ArrayList<>(valueStrs.size());
        for (String vs : valueStrs) {
          // If vs is a string, wrapped by '', or NULL value
          if (!Character.isDigit(vs.charAt(0))) {
            if ("NULL".equals(vs)) values.add(null);
            else if ("TRUE".equals(vs)) values.add(1);
            else if ("FALSE".equals(vs)) values.add(0);
            else values.add(vs.substring(1, vs.length() - 1).hashCode());
          } else values.add(Integer.parseInt(vs));
        }
        tuples.add(new TupleInstance(values));
      }
      if (tuples.size() > 0) {
        final int schemaLength = tuples.get(0).size();
        for (int i = 0; i < schemaLength; ++i)
          schema.add("expr$" + i);
      }
      return new VALUESTable(tuples, coalesce(defaultSchema, schema));
    }
  }

  class VALUESTableParser {
    private final NameSequence tableSeq;
    private final String sql0, sql1;
    private final Schema baseSchema;

    VALUESTableParser(String sql0, String sql1, Schema baseSchema) {
      this.sql0 = sql0;
      this.sql1 = sql1;
      this.tableSeq = NameSequence.mkIndexed("r", 0);
      this.baseSchema = baseSchema;
    }

    void parse() {
      final String modifiedSql0 = registerVALUESTable(sql0);
      final String modifiedSql1 = registerVALUESTable(sql1);
      schema = initSchema(baseSchema);
//      p0 = parsePlan(modifiedSql0, schema);
//      p1 = parsePlan(modifiedSql1, schema);
    }

    private String registerVALUESTable(String sql) {
      // `SELECT * FROM VALUES()` -> `SELECT * FROM R`, R: [expr$0, expr$1, ...]
      // find patterns of `(VALUES  (30, 3))`
      sql = sql.replaceAll("ROW", "");
      while (sql.contains("(VALUES")) {
        final int start = sql.indexOf("(VALUES");
        int count = 0, idx0 = start;
        for (int bound = sql.length(); idx0 < bound; ++idx0) {
          if (sql.charAt(idx0) == '(') ++count;
          if (sql.charAt(idx0) == ')') {
            --count;
            if (count == 0) break;
          }
        }
        if (idx0 == sql.length()) {
          assert false : "wrong pattern of VALUES()";
          return null;
        }
        final String VALUESInfo = sql.substring(start, idx0 + 1); // `(VALUES  (30, 3))`
        // For case of `(VALUES) as t(col, ..)` -> r0 as t, and pick schema info in t(col, ..)
        idx0++;
        List<String> schema = null;
        if (idx0 < sql.length() && sql.substring(idx0).trim().toUpperCase().startsWith("AS")) {
          final int aliasStart = sql.toUpperCase().indexOf("AS", idx0) + 3;
          final int aliasEnd;
          int idx1 = aliasStart;
          while (idx1 < sql.length() && (sql.charAt(idx1) != ' ' && sql.charAt(idx1) != '('))
            idx1++;
          final String aliasName = sql.substring(aliasStart, idx1);

          while (idx1 < sql.length() && sql.charAt(idx1) == ' ')
            idx1++;
          if (idx1 < sql.length() && sql.charAt(idx1) == '(') {
            while (idx1 < sql.length() && sql.charAt(idx1) != '(')
              idx1++;
            aliasEnd = sql.indexOf(")", idx1) + 1;
            final String schemaStr = sql.substring(idx1, aliasEnd);
            schema = Arrays.stream(schemaStr.substring(1, schemaStr.length() - 1).split(", ")).toList();
            final String aliasAll = sql.substring(aliasStart, aliasEnd);
            sql = sql.replace(aliasAll, aliasName);
          }
        }
        final VALUESTable valuesTable = VALUESTable.parse(VALUESInfo, schema);
        if (VALUESTablesReg.get(valuesTable) == null)
          VALUESTablesReg.put(valuesTable, tableSeq.next());
        sql = sql.replace(VALUESInfo, VALUESTablesReg.get(valuesTable));
      }
      return sql;
    }

    private Schema initSchema(Schema baseSchema) {
      StringBuilder builder = new StringBuilder();
      for (String table : VALUESTablesReg.values()) {
        builder.append("CREATE TABLE `").append(table).append("`(\n");
        final List<String> columns = VALUESTablesReg.inverse().get(table).schema;
        for (String col : columns) {
          builder.append("`").append(col).append("` int");
          if (columns.indexOf(col) == columns.size() - 1) builder.append("\n");
          else builder.append(",\n");
        }
        if (columns.isEmpty()) {
          builder.append("`expr$0` int").append("\n");
        }
        builder.append(");\n");
      }
      // Combine with baseSchema
      if (baseSchema != null)
        builder.append(baseSchema.toDdl(baseSchema.dbType(), builder));

      return Schema.parse(MySQL, builder.toString());
    }
  }

  record TupleInstance(List<Integer> values) {
    Integer getValue(int index) {
      if (values == null || index >= values.size()) return null;
      return values.get(index);
    }

    int size() {
      if (values == null) return 0;
      return values.size();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TupleInstance that = (TupleInstance) o;
      return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(values);
    }

    static TupleInstance mk(List<Integer> values) {
      return new TupleInstance(values);
    }
  }
}
