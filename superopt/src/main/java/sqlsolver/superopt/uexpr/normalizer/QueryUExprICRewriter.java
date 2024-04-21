package sqlsolver.superopt.uexpr.normalizer;

import sqlsolver.common.utils.Congruence;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Constraint;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.Table;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.liastar.translator.LiaTranslator;
import sqlsolver.superopt.uexpr.*;
import sqlsolver.superopt.util.Z3Support;

import java.util.*;
import java.util.function.Function;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.filter;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.sql.calcite.CalciteSupport.*;
import static sqlsolver.superopt.uexpr.UKind.*;
import static sqlsolver.superopt.uexpr.UExprSupport.*;
import static sqlsolver.superopt.uexpr.UExprConcreteTranslator.VarSchema;

/**
 * This class provides the integrity constraint normalization for U-Expression.
 */
public class QueryUExprICRewriter extends UNormalization {
  private final Schema schema;

  private static final String VAR_NAME_PREFIX = "y";
  private final List<UVar> icFreshVars;

  private final List<UVar> boundedStackVars;

  private UVar replaceVarOneRecord;

  private final NameSequence tupleVarSeq;

  private final Map<Integer, VarSchema> constToTuple;

  private static int selectedIC = -1;
  private static boolean hasIC = true;

  public static void selectIC(int index) {
    selectedIC = index;
  }

  public static int selectedIC() {
    return selectedIC;
  }

  public static void setHasIC(boolean val) {
    hasIC = val;
  }

  public static boolean hasIC() {
    return hasIC;
  }

  public UVar mkFreshICRewriterBaseVar() {
    UVar newVar = UVar.mkBase(UName.mk(tupleVarSeq.next()));
    return newVar;
  }

  public List<UVar> getIcFreshVars() {
    return this.icFreshVars;
  }

  public QueryUExprICRewriter(UTerm expr,
                              Schema schema,
                              UExprConcreteTranslator.QueryTranslator translator,
                              Map<Integer, VarSchema> constToTuple) {
    super(expr, translator);
    this.schema = schema;
    this.icFreshVars = new ArrayList<>();
    this.boundedStackVars = new ArrayList<>();
    this.replaceVarOneRecord = null;
    this.tupleVarSeq = NameSequence.mkIndexed(VAR_NAME_PREFIX, 0);
    this.constToTuple = constToTuple;
  }

  @Override
  public UTerm normalizeTerm() {
    do {
      expr = new QueryUExprNormalizer(expr, schema, translator).normalizeTerm();
      isModified = false;

      // applyForeign may add notNull terms, should be processed before applyNotNull
      expr = performNormalizeRule(this::applyForeign);
      expr = performNormalizeRule(this::applyNotNull);
      expr = performNormalizeRule(this::applyPrimary);
      expr = performNormalizeRule(this::applyUnique);
    } while (isModified);
    // delay free var generation rule, because it will break some rules' application
    do {
      expr = new QueryUExprNormalizer(expr, schema, translator).normalizeTerm();
      isModified = false;

      expr = performNormalizeRule(this::applyUniqueDelayed);
    } while (isModified);
    return expr;
  }

  @Override
  protected UTerm performNormalizeRule(Function<UTerm, UTerm> transformation) {
    expr = transformation.apply(expr);
    expr = new QueryUExprNormalizer(expr, schema, translator).normalizeTerm();
    return expr;
  }

  /*
   * Helper functions.
   */

  /**
   * If the expr is a isnull pred on a projVar in the given projVars, return 0.
   */
  private UTerm removeNotNullByProjVars(UTerm expr, List<UVar> projVars) {
    expr = transformSubTerms(expr, t -> removeNotNullByProjVars(t, projVars));
    if (!isNullPred(expr)) return expr;

    final UPred isNull = (UPred) expr;
    assert isNull.subTerms().size() == 1;

    if (any(projVars, t -> mkIsNullPred(t).equals(isNull))) {
      isModified = true;
      return UConst.zero();
    }

    return expr;
  }

  /**
   * Get the single column proj var under the  constraint of targetVar in the context of a table.
   * e.g. T(x) with constraint PRIMARY T.$0 -> return [$0(x)]
   */
  private List<UVar> getProjVarsOfTableSingleColumn(UVar targetVar, String tableName, List<Constraint> constraints) {
    final List<UVar> results = new ArrayList<>();

    for (final Constraint constraint : constraints) {
      if (constraint.columns().size() != 1) continue;
      final Column column = constraint.columns().get(0);
      if (Objects.equals(column.tableName(), tableName)) {
        final String projString = getIndexStringByInfo(tableName, column.name(), translator.getTupleVarSchema(targetVar));
        results.add(UVar.mkProj(UName.mk(projString), targetVar));
      }
    }

    return results;
  }

  /**
   * Check whether pred is eq pred who has target and the eq term doesn't have the baseVars of target.
   */
  private boolean isEqVarFreeTerm(UTerm pred, UVarTerm target) {
    // Check whether `pred` is like [a(t) = e] for `target` a(t)
    if (pred.kind() != UKind.PRED || !((UPred) pred).isPredKind(UPred.PredKind.EQ)) return false;

    assert ((UPred) pred).args().size() == 2;
    final UTerm predArg0 = ((UPred) pred).args().get(0), predArg1 = ((UPred) pred).args().get(1);
    if (!predArg0.equals(target) && !predArg1.equals(target)) return false;

    final UTerm otherTerm = predArg0.equals(target) ? predArg1 : predArg0;
    return all(UVar.getBaseVars(target.var()), v -> !otherTerm.isUsing(v));
  }

  /**
   * Check whether pred is eq pred who has target and the eq term is actually a const (UConst or UString).
   */
  private boolean isEqVarConstTerm(UTerm pred, UVarTerm target) {
    // Check whether `pred` is like [a(t) = e] for `target` a(t)
    if (pred.kind() != UKind.PRED || !((UPred) pred).isPredKind(UPred.PredKind.EQ)) return false;

    assert ((UPred) pred).args().size() == 2;
    final UTerm predArg0 = ((UPred) pred).args().get(0), predArg1 = ((UPred) pred).args().get(1);
    if (!predArg0.equals(target) && !predArg1.equals(target)) return false;

    final UTerm otherTerm = predArg0.equals(target) ? predArg1 : predArg0;
    return otherTerm.kind() == CONST
            || otherTerm.kind() == STRING
            || ((otherTerm instanceof UVarTerm varTerm)
                && varTerm.var().is(UVar.VarKind.PROJ)
                && varTerm.var().isUsing(translator.getVisibleVar()));
  }

  /*
   * Normalize functions
   */

  /**
   * NotNull constraint rules.
   */
  private UTerm applyNotNull(UTerm expr) {
    final List<Constraint> notNulls = new ArrayList<>();
    if (selectedIC < 0) {
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.NOT_NULL).forEach(notNulls::add);
      }
    } else {
      // select the selectedIC-th nonempty IC
      int nonemptyICIndex = 0;
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.NOT_NULL).forEach(notNulls::add);
        if (!notNulls.isEmpty() && nonemptyICIndex < selectedIC) {
          notNulls.clear();
          nonemptyICIndex = nonemptyICIndex + 1;
        } else if (!notNulls.isEmpty()) {
          break;
        }
      }
      hasIC = !notNulls.isEmpty();
    }

    expr = applyNotNullRemoveNotNull(expr, notNulls);
    return expr;
  }

  /**
   * Foreign constraint rules.
   */
  private UTerm applyForeign(UTerm expr) {
    final List<Constraint> primarys = new ArrayList<>();
    final List<Constraint> foreigns = new ArrayList<>();
    for (Table table : schema.tables()) {
      table.constraints(ConstraintKind.PRIMARY).forEach(primarys::add);
      table.constraints(ConstraintKind.FOREIGN).forEach(foreigns::add);
    }

    expr = applyForeignRemoveRedundantBoundVar(expr, primarys, foreigns);
    return expr;
  }

  /**
   * Unique constraint rules.
   */
  private UTerm applyUnique(UTerm expr) {
    final List<Constraint> uniques = new ArrayList<>();
    if (selectedIC < 0) {
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.UNIQUE).forEach(uniques::add);
      }
    } else {
      // select the selectedIC-th nonempty IC
      int nonemptyICIndex = 0;
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.UNIQUE).forEach(uniques::add);
        if (!uniques.isEmpty() && nonemptyICIndex < selectedIC) {
          uniques.clear();
          nonemptyICIndex = nonemptyICIndex + 1;
        } else if (!uniques.isEmpty()) {
          break;
        }
      }
      hasIC = !uniques.isEmpty();
    }
    Collections.reverse(uniques);

    expr = applyUniqueAddSquash(expr, uniques);
    return expr;
  }

  /**
   * Unique constraint rules.
   */
  private UTerm applyUniqueDelayed(UTerm expr) {
    final List<Constraint> uniques = new ArrayList<>();
    if (selectedIC < 0) {
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.UNIQUE).forEach(uniques::add);
      }
    } else {
      // select the selectedIC-th nonempty IC
      int nonemptyICIndex = 0;
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.UNIQUE).forEach(uniques::add);
        if (!uniques.isEmpty() && nonemptyICIndex < selectedIC) {
          uniques.clear();
          nonemptyICIndex = nonemptyICIndex + 1;
        } else if (!uniques.isEmpty()) {
          break;
        }
      }
      hasIC = !uniques.isEmpty();
    }
    Collections.reverse(uniques);

    expr = applyUniqueReplaceBoundedVarFreeVar(expr, uniques, constToTuple);
    return expr;
  }

  /**
   * Primary constraint rules.
   */
  private UTerm applyPrimary(UTerm expr) {
    final List<Constraint> primaries = new ArrayList<>();
    if (selectedIC < 0) {
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.PRIMARY).forEach(primaries::add);
      }
    } else {
      // select the selectedIC-th nonempty IC
      int nonemptyICIndex = 0;
      for (Table table : schema.tables()) {
        table.constraints(ConstraintKind.PRIMARY).forEach(primaries::add);
        if (!primaries.isEmpty() && nonemptyICIndex < selectedIC) {
          primaries.clear();
          nonemptyICIndex = nonemptyICIndex + 1;
        } else if (!primaries.isEmpty()) {
          break;
        }
      }
      hasIC = !primaries.isEmpty();
    }

    expr = applyPrimaryReplaceBoundedVar(expr, primaries);
    expr = applyPrimaryImplyTupleEq(expr, primaries);
    return expr;
  }

  /**
   * For multiply terms `expr`, consider $0(x) which indicates the not_null column. <p/>
   * Delete the NotNull($0(x)) in it.
   */
  private UTerm applyNotNullRemoveNotNull(UTerm expr, List<Constraint> notnulls) {
    expr = transformSubTerms(expr, t -> applyNotNullRemoveNotNull(t, notnulls));
    if (expr.kind() != UKind.MULTIPLY) return expr;

    final List<UTerm> tables = new ArrayList<>();
    getTargetUExprRecursive(expr, t -> t.kind() == TABLE, tables);
    for (final UTerm subTerm : tables) {
      final UTable table = (UTable) subTerm;
      final UName tableName = table.tableName();
      final UVar tableVar = table.var();

      // notnulls constraint have exactly one column.
      final List<String> relatedColumns = new ArrayList<>();
      all(filter(notnulls,
                      t -> Objects.equals(t.columns().get(0).tableName(), tableName.toString())),
              t -> relatedColumns.add(t.columns().get(0).name()));
      final List<UVar> relatedProjVars = new ArrayList<>();
      // traverse relatedColumns to add relatedProjVar.
      for (final String relatedColumn : relatedColumns) {
        final String projString = getIndexStringByInfo(tableName.toString(), relatedColumn, translator.getTupleVarSchema(tableVar));
        if (projString == null) continue;
        final UName projName = UName.mk(projString);
        final UVar projVar = UVar.mkProj(projName, tableVar);
        relatedProjVars.add(projVar);
      }
      final NaturalCongruence<UTerm> congruence = NaturalCongruence.mk();
      getEqCongruenceRecursive(expr, UExprSupport::isPredOfVarArg, congruence, expr, false);
      final List<UVar> toRemoveNotNullProjVars = new ArrayList<>();
      all(relatedProjVars, v ->
              toRemoveNotNullProjVars.addAll(map(congruence.eqClassOf(UVarTerm.mk(v)), t -> ((UVarTerm) t).var())));
      expr = removeNotNullByProjVars(expr, toRemoveNotNullProjVars);
    }

    return expr;
  }

  /**
   * \sum{..., x, y, ...} (R(x) * [$i(x) = $j(y)] * T(y)). <p/>
   * Suppose R's primary key is $i(x) and $j(y) is T's foreign key，delete R(x) and replace $i(x) with any term in congruence,
   * and add notnull pred for every var in $i(x)'s congruence，delete x finally. <p/>
   * The condition is that the terms related to x only use R(x) and $i(x) in the summation.
   */
  private UTerm applyForeignRemoveRedundantBoundVar(UTerm expr,
                                                    List<Constraint> primarys,
                                                    List<Constraint> foreigns) {
    expr = transformSubTerms(expr, t -> applyForeignRemoveRedundantBoundVar(t, primarys, foreigns));
    if (expr.kind() != UKind.SUMMATION || ((USum) expr).body().kind() != MULTIPLY) return expr;

    final USum summation = (USum) expr;
    UMul multiply = (UMul) summation.body();
    final Set<UVar> boundedVars = summation.boundedVars();

    final List<UTerm> tables = new ArrayList<>();
    final List<UTerm> notTableTerms = new ArrayList<>();
    getTargetUExprRecursive(multiply, t -> t.kind() == TABLE, tables);
    getTargetUExprRecursive(multiply, t -> t.kind() != TABLE, notTableTerms);

    for (final UVar boundedVar : boundedVars) {
      // check whether this boundedVar satisfy the condition.
      // first check whether this boundedVar is in a table.
      if (all(tables, t -> !t.isUsing(boundedVar))) continue;
      final List<UTerm> boundedVarRelatedTables = filter(tables, t -> t.isUsing(boundedVar));
      // should only consider one table case
      assert boundedVarRelatedTables.size() == 1;
      final UTable relatedTable = (UTable) boundedVarRelatedTables.get(0);
      // get the primary key column of this table.
      final List<Constraint> tableRelatedPrimaries = filter(primarys, t -> t.columns().size() == 1
              && Objects.equals(t.columns().get(0).tableName(), relatedTable.tableName().toString()));
      if (tableRelatedPrimaries.isEmpty()) continue;
      assert tableRelatedPrimaries.size() == 1;
      final Column tableRelatedPrimaryColumn = tableRelatedPrimaries.get(0).columns().get(0);

      // second check whether only R(x) and $i(x) are used.
      final String projString = getIndexStringByInfo(relatedTable.tableName().toString(),
              tableRelatedPrimaryColumn.name(),
              translator.getTupleVarSchema(boundedVar));
      if (projString == null) continue;
      final UVar primaryProjVar = UVar.mkProj(UName.mk(projString), boundedVar);
      // here check only the $i(x) is used.
      if (any(notTableTerms, t -> t.isUsing(boundedVar) && !t.isUsingProjVar(primaryProjVar))) continue;
      // get the foreign key column of this table.
      final List<Constraint> tableRelatedForeigns = filter(foreigns, t -> t.columns().size() == 1
              && Objects.equals(t.refTable().name(), relatedTable.tableName().toString())
              && t.refColumns().size() == 1
              && t.refColumns().get(0).equals(tableRelatedPrimaryColumn));
      // get the congruence of primaryProjVar
      NaturalCongruence<UTerm> congruence = NaturalCongruence.mk();
      getEqCongruenceRecursive(multiply, UExprSupport::isPredOfVarArg, congruence, multiply, false);
      final List<UVar> eqVars = map(congruence.eqClassOf(UVarTerm.mk(primaryProjVar)), t -> ((UVarTerm) t).var());

      // third check any other boundedVar satisfy the condition.
      final List<UVar> otherBoundedVars = filter(boundedVars, t -> !t.equals(boundedVar));
      for (final UVar otherBoundedVar : otherBoundedVars) {
        if (all(tables, t -> !t.isUsing(otherBoundedVar))) continue;
        final List<UTerm> otherBoundedVarRelatedTables = filter(tables, t -> t.isUsing(otherBoundedVar));
        // should only consider one table case
        assert otherBoundedVarRelatedTables.size() == 1;
        final UTable otherRelatedTable = (UTable) otherBoundedVarRelatedTables.get(0);
        for (final Constraint tableRelatedForeign : tableRelatedForeigns) {
          // if this constraint is not this otherBoundedVar's table
          if (!Objects.equals(tableRelatedForeign.columns().get(0).tableName(), otherRelatedTable.tableName().toString()))
            continue;
          final String foreignProjString = getIndexStringByInfo(otherRelatedTable.tableName().toString(),
                  tableRelatedForeign.columns().get(0).name(),
                  translator.getTupleVarSchema(otherBoundedVar));
          if (foreignProjString == null) continue;
          final UVar foreignProjVar = UVar.mkProj(UName.mk(foreignProjString), otherBoundedVar);
          if (!eqVars.contains(foreignProjVar)) continue;
          // modify the expr here
          isModified = true;
          final List<UTerm> newSubTerms = new ArrayList<>();
          multiply = (UMul) multiply.replaceAtomicTerm(boundedVarRelatedTables.get(0), UConst.one())
                  .replaceAtomicTerm(UVarTerm.mk(primaryProjVar), UVarTerm.mk(foreignProjVar));
          newSubTerms.add(multiply);
          // should add notnulls
          for (final UVar eqVar : eqVars) {
            if (eqVar.equals(primaryProjVar)) continue;
            newSubTerms.add(mkNotNullPred(eqVar));
          }
          return USum.mk(new HashSet<>(otherBoundedVars), UMul.mk(newSubTerms));
        }
      }

    }

    return expr;
  }

  /**
   * Apply unique constraints to add squash on terms.
   */
  public UTerm applyUniqueAddSquash(UTerm expr, List<Constraint> uniques) {
    expr = applyUniqueAddSquashInner(expr, uniques, true);
    expr = applyUniqueAddSquashOuter(expr, uniques);
    return expr;
  }

  /**
   * Apply unique constraint to add squash in a summation.
   * \sum_{t}(R(t) * g(t)) -> \sum_{t}||R(t) * g(t)||, g(t) does not contain [a(t) = e]
   */
  public UTerm applyUniqueAddSquashInner(UTerm expr, List<Constraint> uniques, boolean consider) {
    boolean thisConsider = (expr.kind() == UKind.MULTIPLY
            || expr.kind() == UKind.ADD) ? consider : !expr.kind().isUnary();
    expr = transformSubTerms(expr, t -> applyUniqueAddSquashInner(t, uniques, thisConsider));
    if (expr.kind() != UKind.SUMMATION || !consider) return expr;

    final USum summation = (USum) expr;
    final List<UTerm> subTerms = summation.body().subTerms();
    final Set<UTerm> squashedTerms = new HashSet<>(summation.body().subTerms().size());

    for (Constraint unique : uniques) {
      if (unique.columns().size() > 1) {
        continue;
      }
      final Column column = unique.columns().get(0);
      final UName tableName = UName.mk(column.tableName());
      for (UVar boundedVar : summation.boundedVars()) {
        final UTerm tableTerm =
                linearFind(subTerms, t -> !squashedTerms.contains(t) && t.equals(UTable.mk(tableName, boundedVar)));
        final String projString = getIndexStringByInfo(column.tableName(), column.name(), translator.getTupleVarSchema(boundedVar));
        if (projString == null) continue;
        final UVarTerm projVarTerm = UVarTerm.mk(UVar.mkProj(UName.mk(projString), boundedVar));
        final UTerm predTerm =
                linearFind(subTerms, t -> !squashedTerms.contains(t) && isEqVarFreeTerm(t, projVarTerm));
        if (tableTerm == null) continue;

        if (predTerm != null) continue;
        squashedTerms.addAll(filter(subTerms, t -> t.isUsing(boundedVar) && !(t.kind() == UKind.VAR)));
      }
    }


    if (!squashedTerms.isEmpty()) {
      isModified = true;
      summation.body().subTerms().removeAll(squashedTerms);
      final UTerm newSquashBody = UMul.mk(new ArrayList<>(squashedTerms));
      final USquash newSquash = USquash.mk(newSquashBody);
      summation.body().subTerms().add(newSquash);
    }
    return summation;
  }

  /**
   * Apply unique constraint to add squash on a summation.
   * \sum{t}([a(t) = e] * R(t) * f(t)) -> || \sum{t}([a(t) = e] * R(t) * f(t)) ||, if a(t) is unique.
   */
  public UTerm applyUniqueAddSquashOuter(UTerm expr, List<Constraint> uniques) {
    if (expr.kind().isUnary()) return expr;
    expr = transformSubTerms(expr, t -> applyUniqueAddSquashOuter(t, uniques));
    if (expr.kind() != UKind.SUMMATION) return expr;

    final USum summation = (USum) expr;
    final List<UTerm> subTerms = summation.body().subTerms();
    final Set<UTerm> squashedTerms = new HashSet<>(summation.body().subTerms().size());
    final Set<UVar> squashedVars = new HashSet<>(summation.boundedVars().size());

    for (Constraint unique : uniques) {
      if (unique.columns().size() > 1) {
        continue;
      }
      final Column column = unique.columns().get(0);
      final UName tableName = UName.mk(column.tableName());
      for (UVar boundedVar : filter(summation.boundedVars(), v -> !squashedVars.contains(v))) {
        final UTerm tableTerm =
                linearFind(subTerms, t -> !squashedTerms.contains(t) && t.equals(UTable.mk(tableName, boundedVar)));
        final String projString = getIndexStringByInfo(column.tableName(), column.name(), translator.getTupleVarSchema(boundedVar));
        if (projString == null) continue;
        final UVarTerm projVarTerm = UVarTerm.mk(UVar.mkProj(UName.mk(projString), boundedVar));
        final UTerm predTerm =
                linearFind(subTerms, t -> !squashedTerms.contains(t) && isEqVarFreeTerm(t, projVarTerm));
        if (tableTerm == null) continue;

        squashedTerms.addAll(filter(subTerms, t -> t.isUsing(boundedVar) && !(t.kind() == UKind.VAR)));
        if ((predTerm != null) && !any(subTerms, t -> t.isUsing(boundedVar) && (t.kind() == UKind.VAR)))
          squashedVars.add(boundedVar);
      }
    }

    // apply squash to bound vars
    if (!squashedTerms.isEmpty() && !squashedVars.isEmpty()) {
      isModified = true;
      summation.body().subTerms().removeAll(squashedTerms);
      for (UVar removedVar : squashedVars) {
        summation.removeBoundedVar(removedVar);
      }
      final UTerm newSquashBody = USum.mk(squashedVars, UMul.mk(new ArrayList<>(squashedTerms)));
      final USquash newSquash = USquash.mk(newSquashBody);
      summation.body().subTerms().add(newSquash);
    }
    if (summation.boundedVars().isEmpty()) return summation.body();
    return summation;
  }

  /**
   * Apply unique constraint to add squash on a summation.
   * \sum{t}([a(t) = e] * R(t) * f(t)) -> || \sum{t}([a(t) = e] * R(t) * f(t)) ||, if a(t) is unique.
   */
  public UTerm applyUniqueAddSquashOuterSMT(UTerm expr, List<Constraint> uniques) {
    if (expr.kind().isUnary()) return expr;
    expr = transformSubTerms(expr, t -> applyUniqueAddSquashOuterSMT(t, uniques));
    if (expr.kind() != UKind.SUMMATION) return expr;
    final USum summation = (USum) expr;
    // todo: generalize it for case \sum{t}([a(t) = e] * || R(t) * f(t) ||)
    if (summation.body().kind() == ADD) return expr;

    final List<UTerm> subTerms = summation.body().subTerms();
    final Set<UTerm> squashedTerms = new HashSet<>(summation.body().subTerms().size());
    final Set<UVar> squashedVars = new HashSet<>(summation.boundedVars().size());

    for (Constraint unique : uniques) {
      if (unique.columns().size() > 1) {
        continue;
      }
      final Column column = unique.columns().get(0);
      final UName tableName = UName.mk(column.tableName());
      for (UVar bv : filter(summation.boundedVars(), v -> !squashedVars.contains(v))) {
        final List<UTerm> relatedTerms = filter(subTerms, t -> t.isUsing(bv));
        final String projString = getIndexStringByInfo(column.tableName(), column.name(), translator.getTupleVarSchema(bv));
        if (projString == null) continue;
        if (canApplyUniqueAddSquashOuter(tableName, bv, UName.mk(projString), UMul.mk(relatedTerms))) {
          squashedVars.add(bv);
          squashedTerms.addAll(relatedTerms);
        }
      }
    }

    // apply squash to bound vars
    if (!squashedTerms.isEmpty() && !squashedVars.isEmpty()) {
      isModified = true;
      summation.body().subTerms().removeAll(squashedTerms);
      for (UVar removedVar : squashedVars) {
        summation.removeBoundedVar(removedVar);
      }
      final UTerm newSquashBody = USum.mk(squashedVars, UMul.mk(new ArrayList<>(squashedTerms)));
      final USquash newSquash = USquash.mk(newSquashBody);
      summation.body().subTerms().add(newSquash);
    }
    if (summation.boundedVars().isEmpty()) return summation.body();
    return summation;
  }

  /**
   * Given a table "r", a bound var "x" and an expression f(x),
   * where $k is a unique-constrained column in table r,
   * if f is 0/1, r(x)=0 -> f(x)=0
   * and there exists C such that $k(x)<>C -> f(x)=0,
   * then <code>applyUniqueAddSquashOuter</code> can be applied to x.
   */
  private boolean canApplyUniqueAddSquashOuter(UName r, UVar x, UName k, UTerm f) {
    // collect var type info
    final Set<String> fvs = f.getFVs();
    final Map<UVar, List<Value>> varSchema = new HashMap<>();
    for (String varName : fvs) {
      final UVar var = UVar.mkBase(UName.mk(varName));
      varSchema.put(var, translator.getTupleVarSchema(var));
    }
    // Assume that f contains x (bound var) and y (the other vars)
    // i.e. fvs = x union y
    final NameSequence liaVarName = NameSequence.mkIndexed("u", 0);
    final Map<UVar, String> varMap = new HashMap<>();
    final Map<USum, String> sumVarMap = new HashMap<>();
    // check validity of "f = 0 \/ f = 1"
    final LiaStar fLia = LiaTranslator.translate(f, varSchema, liaVarName, varMap, sumVarMap);
    final LiaStar fIsZero = LiaStar.mkEq(false, fLia, LiaStar.mkConst(false, 0));
    final LiaStar fIsOne = LiaStar.mkEq(false, fLia, LiaStar.mkConst(false, 1));
    final LiaStar premise1 = LiaStar.mkOr(false, fIsZero, fIsOne);
    if (!Z3Support.isValidLia(premise1)) {
      return false;
    }
    // check validity of "forall x,y. r(x) = 0 -> f(x,y) = 0"
    final UTerm rX = UTable.mk(r, x);
    final LiaStar rXLia = LiaTranslator.translate(rX, varSchema, liaVarName, varMap, sumVarMap);
    final LiaStar rXIsZero = LiaStar.mkEq(false, rXLia, LiaStar.mkConst(false, 0));
    final LiaStar premise2 = LiaStar.mkImplies(false, rXIsZero, fIsZero);
    if (!Z3Support.isValidLia(premise2)) {
      return false;
    }
    // check validity of "exists C. forall x. $k(x) <> C -> forall y. f(x,y) = 0"
    // i.e. satisfiability of "forall x,y. $k(x) <> C -> f(x,y) = 0"
    // TODO: modify
    final String cName = liaVarName.next();
    final int kIndex = Integer.parseInt(k.toString().substring(1));
    final String kXType = varSchema.get(x).get(kIndex).type();
    final LiaStar cLia = LiaStar.mkVar(false, cName, kXType);
    final UTerm kX = UVarTerm.mk(UVar.mkProj(k, x));
    final LiaStar kXLia = LiaTranslator.translate(kX, varSchema, liaVarName, varMap, sumVarMap);
    final LiaStar kXIsNotC = LiaStar.mkNeq(false, kXLia, cLia);
    final LiaStar premise3 = LiaStar.mkImplies(false, kXIsNotC, fIsZero);
    return Z3Support.isSatisfiable(premise3, Set.of(cName));
  }

  /**
   * Apply unique constraint to replace a bounded var into a free var tuple.
   * \sum{t}([a(t) = e] * R(t) * f(t)), where a is unique and e is a constant(int constant or string)
   * ->  ([a(X) = e] * || R(X) || * f(X)).
   */
  public UTerm applyUniqueReplaceBoundedVarFreeVar(UTerm expr, List<Constraint> uniques, Map<Integer, VarSchema> constToTuple) {
    expr = transformSubTerms(expr, t -> applyUniqueReplaceBoundedVarFreeVar(t, uniques, constToTuple));
    if (expr.kind() != UKind.SUMMATION || ((USum) expr).body().kind() != MULTIPLY) return expr;
    final USum summation = (USum) expr;
    UMul multiply = (UMul) ((USum) expr).body();
    final NaturalCongruence<UTerm> congruence = NaturalCongruence.mk();
    getEqCongruenceRecursive(multiply, pred -> true, congruence, expr, false);

    for (Constraint unique : uniques) {
      if (unique.columns().size() > 1) {
        continue;
      }
      final Column column = unique.columns().get(0);
      final UName tableName = UName.mk(column.tableName());
      for (final UVar boundedVar : summation.boundedVars()) {
        final UTerm tableTerm =
                linearFind(multiply.subTerms(), t -> t.equals(UTable.mk(tableName, boundedVar)));
        final String projString = getIndexStringByInfo(column.tableName(), column.name(), translator.getTupleVarSchema(boundedVar));
        if (projString == null || tableTerm == null) continue;
        final UVarTerm projVarTerm = UVarTerm.mk(UVar.mkProj(UName.mk(projString), boundedVar));
        final List<UTerm> constTerms = filter(congruence.eqClassOf(projVarTerm), t ->
                t.kind() == CONST
                || t.kind() == STRING
                || ((t instanceof UVarTerm varTerm)
                && varTerm.var().is(UVar.VarKind.PROJ)
                && varTerm.var().isUsing(translator.getVisibleVar())));
        if (constTerms.isEmpty()) continue;

        VarSchema replaceVar = null;
        for (final UTerm constTerm : constTerms) {
          if (constToTuple.containsKey(constTerm.hashCode())) {
            replaceVar = constToTuple.get(constTerm.hashCode());
            break;
          }
        }
        if (replaceVar != null) {
          if (!CalciteSupport.isExactlyEqualTwoValueList(translator.getTupleVarSchema(boundedVar), replaceVar.schema()))
            continue;
          translator.putTupleVarSchema(replaceVar.var(), replaceVar.schema());
        }
        if (replaceVar == null) {
          replaceVar = new VarSchema(mkFreshFreeVar(), translator.getTupleVarSchema(boundedVar));
          translator.putTupleVarSchema(replaceVar.var(), replaceVar.schema());
        }
        for (final UTerm constTerm : constTerms) {
          constToTuple.put(constTerm.hashCode(), replaceVar);
        }

        // modify here
        isModified = true;
        final Set<UVar> newBoundedVars = summation.boundedVars();
        newBoundedVars.remove(boundedVar);
        multiply = UMul.mk(USquash.mk(tableTerm), filter(multiply.subTerms(), t -> !t.equals(tableTerm)));
        if (newBoundedVars.isEmpty()) {
          return multiply.replaceVar(boundedVar, replaceVar.var(), false);
        }
        return USum.mk(newBoundedVars, multiply.replaceVar(boundedVar, replaceVar.var(), false));
      }
    }

    return expr;
  }

  /**
   * \sum{..., x, ...} {|| \sum{..., y, z, ...} (T(y) * [$0(y) = $0(x)] * [$1(y) = $1(x)] * ...)||}
   * -> \sum{..., y, ...} {|| \sum{..., z, ...} (T(y) * [$0(y) = $0(y)] * [$1(y) = $1(y)] * ...)||}
   * assume that every column of x is the primary keys of T.
   */
  private UTerm applyPrimaryReplaceBoundedVar(UTerm expr, List<Constraint> primaries) {
    expr = transformSubTerms(expr, t -> applyPrimaryReplaceBoundedVar(t, primaries));
    if (expr.kind() != UKind.SUMMATION
            || ((USum) expr).body().kind() != MULTIPLY
            || ((USum) expr).body().subTerms().size() != 1
            || ((USum) expr).body().subTerms().get(0).kind() != SQUASH) return expr;

    final USum outerSummation = (USum) expr;
    final USquash squash = (USquash) outerSummation.body().subTerms().get(0);

    if (squash.body().kind() != SUMMATION || ((USum) squash.body()).body().kind() != MULTIPLY) return expr;

    final USum innerSummation = (USum) squash.body();
    final UMul multiply = (UMul) innerSummation.body();
    final NaturalCongruence<UVar> congruence = getEqVarCongruenceInTermsOfMul(multiply);

    for (final UVar outerBoundedVar : outerSummation.boundedVars()) {
      final List<UVar> outerBoundedProjVars = map(getValueListBySize(translator.getTupleVarSchema(outerBoundedVar).size()),
              v -> translator.mkProjVar(v, outerBoundedVar));
      for (final UVar innerBoundedVar : innerSummation.boundedVars()) {
        final List<UTerm> tableTerm = filter(multiply.subTerms(), t -> t.kind() == TABLE && t.isUsing(innerBoundedVar));
        // only consider one table term.
        if (tableTerm.size() != 1) continue;
        final List<UVar> innerBoundedProjVars = getProjVarsOfTableSingleColumn(innerBoundedVar,
                ((UTable) tableTerm.get(0)).tableName().toString(),
                primaries);
        // hack: only consider one schema here.
        if (innerBoundedProjVars.size() != 1 || outerBoundedProjVars.size() != 1) continue;
        final UVar outerBoundedProjVar = outerBoundedProjVars.get(0);
        final UVar innerBoundedProjVar = innerBoundedProjVars.get(0);
        if (congruence.eqClassOf(innerBoundedProjVar).contains(outerBoundedProjVar)) {
          // modify here.
          isModified = true;
          final UTerm newMultiply = multiply.replaceAtomicTerm(UVarTerm.mk(outerBoundedProjVar), UVarTerm.mk(innerBoundedProjVar));
          final Set<UVar> newOuterBoundedVars = outerSummation.boundedVars();
          final Set<UVar> newInnerBoundedVars = innerSummation.boundedVars();
          newOuterBoundedVars.remove(outerBoundedVar);
          newOuterBoundedVars.add(innerBoundedVar);
          newInnerBoundedVars.remove(innerBoundedVar);
          final UTerm newInnerSummation = newInnerBoundedVars.isEmpty() ? newMultiply : USum.mk(newInnerBoundedVars, newMultiply);
          return USum.mk(newOuterBoundedVars, UMul.mk(USquash.mk(newInnerSummation)));
        }
      }
    }

    return expr;
  }

  /**
   * If t has primary key $k and x,y share the same schema,
   * then sum{x,y,...}(t(x)*t(y)*[$k(x)=$k(y)]*f(y))
   * = sum{x,...}(t(x)*f(x))
   */
  private UTerm applyPrimaryImplyTupleEq(UTerm expr, List<Constraint> primaries) {
    return applyPrimaryImplyTupleEq0(expr, primaries, new HashMap<>());
  }

  // recursion
  // tableVars: which tuples are in each table
  private UTerm applyPrimaryImplyTupleEq0(UTerm expr, List<Constraint> primaries, Map<UVar, String> varTable) {
    // collect such table terms:
    // when one of them is zero, the value of expr is determined
    final Map<UVar, String> newVarTable = new HashMap<>(varTable);
    if (expr instanceof UMul mul) {
      collectTableTermsByVars(mul, newVarTable);
    }
    // recursion
    expr = transformSubTerms(expr, t -> applyPrimaryImplyTupleEq0(t, primaries, newVarTable));

    // when current level (expr) is not a summation, skip
    if (expr.kind() != UKind.SUMMATION
            || ((USum) expr).body().kind() != MULTIPLY) return expr;

    // collect table terms of outer bound vars in the current level of sum
    final Set<UVar> bvs = ((USum) expr).boundedVars();
    collectTableTermsByVars(((USum) expr).body(), newVarTable, t -> !bvs.contains(t.var()));

    expr = applyPrimaryImplyTupleEqToSum(expr, primaries);
    expr = applyPrimaryImplyTupleEqToSumWithCtx(expr, primaries, newVarTable);

    return expr;
  }

  private void collectTableTermsByVars(UTerm expr, Map<UVar, String> varTable) {
    collectTableTermsByVars(expr, varTable, t -> true);
  }

  private void collectTableTermsByVars(UTerm expr, Map<UVar, String> varTable, Function<UTable, Boolean> filter) {
    for (UTerm term : filter(expr.subTerms(), t -> t.kind() == TABLE && filter.apply((UTable) t))) {
      final UTable table = (UTable) term;
      final String tableName = table.tableName().toString();
      // hack: ignore multiple table terms of the same var
      varTable.putIfAbsent(table.var(), tableName);
    }
  }

  /** Handle cases where bound vars are in the same summation. */
  private UTerm applyPrimaryImplyTupleEqToSum(UTerm expr, List<Constraint> primaries) {
    if (!(expr instanceof USum sum)) return expr;
    final UMul multiply = (UMul) sum.body();
    final NaturalCongruence<UVar> congruence = getEqVarCongruenceInTermsOfMul(multiply);
    final Set<UVar> newBVs = new HashSet<>(sum.boundedVars());
    final List<UVar> bvs = new ArrayList<>(sum.boundedVars());

    boolean isUpdated = false;
    for (int i = 0, bound = bvs.size(); i < bound; i++) {
      for (int j = i + 1; j < bound; j++) {
        final UVar x = bvs.get(i);
        final UVar y = bvs.get(j);
        final List<UTerm> tableTermsX = filter(multiply.subTerms(), t -> t.kind() == TABLE && t.isUsing(x));
        final List<UTerm> tableTermsY = filter(multiply.subTerms(), t -> t.kind() == TABLE && t.isUsing(y));
        // hack: ignore multiple table terms
        if (tableTermsX.isEmpty() || tableTermsY.isEmpty()) continue;
        final UTable tableTermX = (UTable) tableTermsX.get(0);
        final UTable tableTermY = (UTable) tableTermsY.get(0);
        isUpdated = isUpdated ||
                applyPrimaryImplyTupleEqToBVs(x, y, tableTermX, tableTermY, primaries, congruence, newBVs, multiply);
      }
    }

    if (!isUpdated) return expr;
    if (newBVs.isEmpty()) return multiply;
    return USum.mk(newBVs, multiply);
  }

  /** Handle cases where bound vars are in different summations. */
  private UTerm applyPrimaryImplyTupleEqToSumWithCtx(UTerm expr, List<Constraint> primaries, Map<UVar, String> varTable) {
    if (!(expr instanceof USum sum)) return expr;
    final UMul multiply = (UMul) sum.body();
    final NaturalCongruence<UVar> congruence = getEqVarCongruenceInTermsOfMul(multiply);
    final Set<UVar> newBVs = new HashSet<>(sum.boundedVars());

    boolean isUpdated = false;
    for (UVar x : varTable.keySet()) {
      for (UVar y : sum.boundedVars()) {
        final List<UTerm> tableTermsY = filter(multiply.subTerms(), t -> t.kind() == TABLE && t.isUsing(y));
        // hack: ignore multiple table terms
        if (tableTermsY.isEmpty()) continue;
        final UTable tableTermX = UTable.mk(UName.mk(varTable.get(x)), x.copy());
        final UTable tableTermY = (UTable) tableTermsY.get(0);
        isUpdated = isUpdated ||
                applyPrimaryImplyTupleEqToBVs(x, y, tableTermX, tableTermY, primaries, congruence, newBVs, multiply);
      }
    }

    if (!isUpdated) return expr;
    if (newBVs.isEmpty()) return multiply;
    return USum.mk(newBVs, multiply);
  }

  /** Try to replace y with x in "multiply" and remove y from "boundVars". */
  private boolean applyPrimaryImplyTupleEqToBVs(UVar x, UVar y, UTable tableTermX, UTable tableTermY, List<Constraint> primaries, NaturalCongruence<UVar> congruence,
                                                Set<UVar> boundVars, UMul multiply) {
    final List<Value> schema = translator.getTupleVarSchema(x);
    if (!isExactlyEqualTwoValueList(schema, translator.getTupleVarSchema(y))) return false;
    // both table terms should have the same table name
    final UName table = tableTermX.tableName();
    if (!table.equals(tableTermY.tableName())) return false;
    // hack: one single-column PK only.
    final List<UVar> xPKs = getProjVarsOfTableSingleColumn(x, table.toString(), primaries);
    if (xPKs.size() != 1) return false;
    final UVar xPK = xPKs.get(0);
    final UVar yPK = getProjVarsOfTableSingleColumn(y, table.toString(), primaries).get(0);
    if (congruence.eqClassOf(xPK).contains(yPK)) {
      // replacement is safe, since congruence excluding y is consistent with the U-exp after replacement
      // remove y from bound vars and replace y with x
      boundVars.remove(y);
      multiply.replaceVarInplace(y, x, false);
      return true;
    }
    return false;
  }
}
