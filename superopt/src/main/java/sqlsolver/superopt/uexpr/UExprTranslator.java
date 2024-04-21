package sqlsolver.superopt.uexpr;

import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.Lazy;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.common.utils.NaturalCongruence;
import sqlsolver.sql.plan.Expression;
import sqlsolver.superopt.constraint.Constraint;
import sqlsolver.superopt.constraint.Constraints;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.substitution.SubstitutionTranslatorResult;

import java.util.*;
import java.util.function.Function;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.plan.PlanSupport.isSimpleExprDetermineNotNull;
import static sqlsolver.sql.plan.PlanSupport.isSimpleIntArithmeticExpr;
import static sqlsolver.superopt.constraint.Constraint.Kind.*;
import static sqlsolver.superopt.fragment.AggFuncKind.*;
import static sqlsolver.superopt.fragment.OpKind.*;
import static sqlsolver.superopt.uexpr.UExprSupport.*;
import static sqlsolver.superopt.uexpr.UKind.*;

/**
 * Translate a <b>valid</b> candidate rule to U-expr.
 *
 * <p>A rule (S,T,C) is valid only if:
 *
 * <ul>
 *   <li>Any Attrs in S has a single, viable source.
 *   <li>Any Table in T has a single, exclusive instantiation.
 *   <li>Any Attrs/Pred in T has a single instantiation
 *   <li>Any Attrs in T has a viable implied source.
 * </ul>
 */
class UExprTranslator {
  private final Substitution rule;
  private final NameSequence varSeq;
  private final Map<Symbol, UName> initiatedNames;
  private final Lazy<Set<Pair<SchemaDesc, SchemaDesc>>> knownEqSchemas;
  private final Map<SchemaDesc, UVar> pivotVars;
  private final UExprTranslationResult result;
  private final boolean enableDependentSubquery,
      enableSchemaFeasibilityCheck,
      enableIntegrityConstraintRewrite,
      enableConcretePlanFeature;
  private final SubstitutionTranslatorResult extraInfo;
  private int nextSchema;

  UExprTranslator(Substitution rule, int tweak, SubstitutionTranslatorResult extraInfo) {
    this.rule = rule;
    this.varSeq = NameSequence.mkIndexed("x", 0);
    this.initiatedNames = new HashMap<>(16);
    this.knownEqSchemas = Lazy.mk(HashSet::new);
    this.pivotVars = new HashMap<>();
    this.result = new UExprTranslationResult(rule);
    this.enableDependentSubquery = (tweak & UEXPR_FLAG_SUPPORT_DEPENDENT_SUBQUERY) != 0;
    this.enableSchemaFeasibilityCheck = (tweak & UEXPR_FLAG_CHECK_SCHEMA_FEASIBLE) != 0;
    this.enableIntegrityConstraintRewrite = (tweak & UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE) != 0;
    this.enableConcretePlanFeature = (tweak & UEXPR_FLAG_VERIFY_CONCRETE_PLAN) != 0;
    if (this.enableConcretePlanFeature) {
      assert extraInfo != null;
      this.extraInfo = extraInfo;
    } else {
      this.extraInfo = null;
    }
    this.nextSchema = 1;
  }

  UExprTranslationResult translate() {
    if (new TemplateTranslator(rule._0(), false).translate()
        && new TemplateTranslator(rule._1(), true).translate()) {
      if (enableConcretePlanFeature) result.alignOutVar();
      return result;
    } else {
      return null;
    }
  }

  private boolean isUniqueKey(Symbol rel, Symbol attrs) {
    return any(
        rule.constraints().ofKind(Unique),
        uk -> uk.symbols()[0] == rel && uk.symbols()[1] == attrs);
  }

  private boolean isNotNull(Symbol rel, Symbol attrs) {
    return any(
        rule.constraints().ofKind(NotNull),
        uk -> uk.symbols()[0] == rel && uk.symbols()[1] == attrs);
  }

  class TemplateTranslator {
    private final Fragment template;
    private final boolean isTargetSide;
    private final List<UVar> freeVars; // Free vars in current scope.
    private final List<UVar> visibleVars; // visible vars in current scope.
    // free and viable variables will diverge at InSub operator
    private UVar auxVar; // Auxiliary variable from outer query.

    private TemplateTranslator(Fragment template, boolean isTargetSide) {
      this.template = template;
      this.isTargetSide = isTargetSide;
      this.freeVars = new ArrayList<>(3);
      this.visibleVars = new ArrayList<>(3);
      this.auxVar = null;
    }

    private boolean translate() {
      final UTerm raw = tr(template.root());
      if (raw == null) return false;

      UTerm expr = normalizeExpr(raw);
      if (enableIntegrityConstraintRewrite) expr = rewriteExprByIC(expr);

      final UVar outVar = tail(visibleVars);
      assert freeVars.size() == 1;
      assert visibleVars.size() == 1;
      assert auxVar == null;

      if (!isTargetSide) {
        result.srcExpr = expr;
        result.srcOutVar = outVar;
      } else {
        result.tgtExpr = expr;
        result.tgtOutVar = outVar;
      }
      return true;
    }

    private UTerm rewriteExprByIC(UTerm expr) {
      boolean rewrittenByIC;
      // Iteratively rewrite by IC and normalize expr until no modification occurs
      do{
        expr = normalizeExprEnhance(expr);

        final ICRewriter icRewriter = new ICRewriter();
        expr = icRewriter.rewriteExpr(expr);
        rewrittenByIC = icRewriter.isModified();
      } while (rewrittenByIC);

      return expr;
    }

    private UName mkName(Symbol sym) {
      /* Create a new or retrieve an existing name for a symbol. */
      final UName existing = initiatedNames.get(sym);
      if (existing != null) return existing;

      final UName name;
      if (isTargetSide) {
        name = initiatedNames.get(rule.constraints().instantiationOf(sym));
      } else {
        name = UName.mk(rule.naming().nameOf(sym));
        for (Symbol eqSym : rule.constraints().eqSymbols().eqClassOf(sym))
          initiatedNames.put(eqSym, name);
      }

      assert name != null : rule.naming().nameOf(sym);
      return name;
    }

    /** Functions of creating vars */
    private UVar mkFreshVar(SchemaDesc schema) {
      /* Create a variable with distinct name and given schema. */
      final UVar var = UVar.mkBase(UName.mk(varSeq.next()));
      result.setVarSchema(var, schema);
      return var;
    }

    private UVar mkVisibleVar() {
      /*  <!> This feature is for dependent subquery <!>
       * Visible variable is the concat of the free variable in current scope
       * and auxiliary variables from outer scope.*/
      final UVar var = tail(visibleVars);
      assert var != null;
      if (!enableDependentSubquery) return var;
      if (auxVar == null) return var;

      final UVar visibleVar = UVar.mkConcat(auxVar, var);
      result.setVarSchema(visibleVar, result.schemaOf(auxVar), result.schemaOf(var));
      return visibleVar;
    }

    /** Functions of creating descriptors */
    private TableDesc mkTableDesc(Symbol tableSym) {
      // Each Table at the source side corresponds to a distinct desc.
      // Each Table at the target side shares the desc of its instantiation.
      final TableDesc desc;
      if (isTargetSide) {
        desc = result.symToTable.get(rule.constraints().instantiationOf(tableSym));

      } else {
        final UName name = mkName(tableSym);
        final SchemaDesc schema = mkSchema(tableSym);
        final UVar var = mkFreshVar(schema);
        final UTable tableTerm = UTable.mk(name, var);
        desc = new TableDesc(tableTerm, schema);
      }

      assert desc != null;
      result.symToTable.put(tableSym, desc);
      return desc;
    }

    private AttrsDesc mkAttrDesc(Symbol attrSym) {
      // The congruent Attrs (i.e., identically named) share a desc instance.
      final UName name = mkName(attrSym);
      final AttrsDesc existing = result.symToAttrs.get(attrSym);
      if (existing != null) return existing;

      final AttrsDesc desc;
      if (isTargetSide) {
        desc = result.symToAttrs.get(rule.constraints().instantiationOf(attrSym));
      } else {
        desc = new AttrsDesc(name);
        for (Symbol eqSym : rule.constraints().eqClassOf(attrSym))
          result.symToAttrs.put(eqSym, desc);
      }
      return desc;
    }

    private PredDesc mkPredDesc(Symbol predSym) {
      // The congruent Pred (i.e., identically named) share a desc instance.
      final UName name = mkName(predSym);
      final PredDesc existing = result.symToPred.get(predSym);
      if (existing != null) return existing;

      final PredDesc desc;
      if (isTargetSide) {
        desc = result.symToPred.get(rule.constraints().instantiationOf(predSym));
      } else {
        desc = new PredDesc(name);
        for (Symbol eqSym : rule.constraints().eqClassOf(predSym))
          result.symToPred.put(eqSym, desc);
      }
      return desc;
    }

    /** Functions of creating Proj-type vars */
    private UVar mkProj(Symbol attrSym, AttrsDesc desc, UVar base) {
      // project `attrSym` (whose desc is `desc`) on the `base` tuple
      Symbol source = null;
      if (isTargetSide) {
        source = rule.constraints().sourceOf(attrSym);
        if (source == null) attrSym = rule.constraints().instantiationOf(attrSym);
      }
      assert attrSym != null;

      if (source == null) source = rule.constraints().sourceOf(attrSym);
      assert source != null : rule.naming().nameOf(attrSym);

      // apply AttrsSub: pick the component from concat (if there is)
      // Suppose we have concat(x,y), where x from T and y from R, and AttrsSub(a,R).
      // then a(concat(x,y)) becomes a(y).
      final SchemaDesc varSchema = result.schemaOf(base);
      final SchemaDesc restrictionSchema = mkSchema(source);
      // Special case for Agg source schema, which has 2 components
      // Judge whether attrSym equals groupByAttrs or aggOutAttrs, and perform projection on concat var
      if (source.ctx().ownerOf(source).kind() == AGG) {
        assert restrictionSchema.components.length == 2;
        final Agg aggSourceOp = (Agg) source.ctx().ownerOf(source);
        int target = -1;
        if (rule.constraints().isEq(attrSym, aggSourceOp.groupByAttrs()))
          target = restrictionSchema.components[0];
        else if (rule.constraints().isEq(attrSym, aggSourceOp.aggregateOutputAttrs()))
          target = restrictionSchema.components[1];
        final UVar comp;
        if (base.is(UVar.VarKind.CONCAT) && target > 0) {
          final int index = varSchema.indexOf(target);
          comp = index >= 0 ? base.args()[index] : base;
        } else comp = base;

        final UVar ret = UVar.mkProj(desc.name(), comp.is(UVar.VarKind.PROJ) ? comp.args()[0] : comp);
        result.setVarSchema(ret, mkSchema(attrSym));
        return ret;
      }
      assert restrictionSchema.components.length == 1;
      int restriction = restrictionSchema.components[0];
      int index;

      // indirection source cases.
      //  e.g. Proj<a0 s0>(Proj<a1 s1>(t0)) vs. Proj<a2 s2>(t0)
      //       AttrsSub(a1,t0) /\ AttrsSub(a0,a1) /\ AttrsEq(a2,a0)
      //  In this case, a2 see a tuple of schema t0, while a1 see a tuple of schema s1.
      //  We have to further trace the source of s1.
      while ((index = varSchema.indexOf(restriction)) < 0) {
        // Currently cope with AGG's schema source (may need detailed impl. in the future)
        source = rule.constraints().sourceOf(source);
        if (source == null) break;
        assert source != null : "wrong constraint! " + rule.naming().nameOf(attrSym);
        restriction = mkSchema(source).components[0];
      }

      final UVar comp;
      // Allow concat(x1, x2) from Agg
      if (source == null) comp = base;
      else comp = base.is(UVar.VarKind.CONCAT) ? base.args()[index] : base;
      final UVar ret = UVar.mkProj(desc.name(), comp.is(UVar.VarKind.PROJ) ? comp.args()[0] : comp);
      // AttrsSub(a,b) makes a(b(x)) become a(x).

      result.setVarSchema(ret, mkSchema(attrSym));
      return ret;
    }

    /** Functions of creating schema descriptor */
    private SchemaDesc mkSchema(Symbol /* Table or Schema or Attrs */ sym) {
      return mkSchema(sym, 1);
    }

    private SchemaDesc mkSchema(Symbol /* Table or Schema or Attrs */ sym, int componentNum) {
      /* An integer that distinguishes the schema of a relation/tuple.
       * For tables at the source side, each T_i is assigned with 2^i.
       * Tables at the target side are assigned the same as the instantiation source.
       * Tuple concat(x1,x2) is assigned with schemaOf(x1) | schemaOf(x2) */
      assert sym.kind() != Symbol.Kind.PRED;

      final SchemaDesc existing = result.symToSchema.get(sym);
      if (existing != null) return existing;

      final SchemaDesc ret;
      if (isTargetSide) ret = result.schemaOf(rule.constraints().instantiationOf(sym));
      else {
        final int[] components = new int[componentNum];
        for (int i = 0; i < componentNum; ++i) components[i] = nextSchema++;
        ret = new SchemaDesc(components);
      }

      result.setSymSchema(sym, ret);
      if (sym.kind() == Symbol.Kind.ATTRS) {
        for (Symbol eqSym : rule.constraints().eqClassOf(sym)) result.setSymSchema(eqSym, ret);
      }

      return ret;
    }

    /** Functions of U-expr translation */
    private UTerm tr(Op op) {
      return switch (op.kind()) {
        case INPUT -> trInput((Input) op);
        case SIMPLE_FILTER -> trSimpleFilter((SimpleFilter) op);
        case IN_SUB_FILTER -> trInSubFilter((InSubFilter) op);
        case EXISTS_FILTER -> trExistsFilter((ExistsFilter) op);
        case PROJ -> trProj((Proj) op);
        case INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN, CROSS_JOIN -> trJoin((Join) op);
        case UNION, INTERSECT, EXCEPT -> trSetOp((SetOp) op);
        case AGG -> trAgg((Agg) op);
        default -> throw new IllegalArgumentException("unknown op");
      };
    }

    private UTerm trInput(Input input) {
      /* Input(T) --> T(x) */
      final TableDesc desc = mkTableDesc(input.table());
      final UVar var = desc.term().var();
      push(freeVars, var);
      push(visibleVars, var);
      result.setVarSchema(var, desc.schema());
      pivotVars.put(desc.schema(), var);
      return UMul.mk(desc.term().copy());
    }

    private UTerm trSimpleFilter(SimpleFilter filter) {
      /* Filter(p,a) --> E * [p(a(x))] */
      final UTerm predecessor = tr(filter.predecessors()[0]);
      if (predecessor == null) return null;

      final AttrsDesc attrDesc = mkAttrDesc(filter.attrs());
      final UVar visibleVar = mkVisibleVar();
      final UVar projVar = mkProj(filter.attrs(), attrDesc, visibleVar);
      UTerm pred;
      if (enableConcretePlanFeature) { // For concrete plan
        assert extraInfo != null;
        final Expression expr = extraInfo.getConcretePred(filter.predicate());
        if (isSimpleIntArithmeticExpr(expr)) {
          // Concrete arithmetic predicate, e.g. a = 1
          // We do not need to determine whether parameter of it can be determined not null
          // Since `[a(t) = 1]` infers a(t)'s value is 1 and is not null
          pred = UExprSupport.mkBinaryArithmeticPred(expr, projVar);
        }
        else {
          // Concrete predicate that can only be expressed by [p(a(t))]
          final PredDesc predDesc = mkPredDesc(filter.predicate());
          pred = UPred.mkFunc(predDesc.name(), projVar);
          // Since given `[p(a(t))]`, we do not known whether a(t) is null or not,
          // we check the concrete expr to known whether parameter of it can be determined not null
          // e.g. WHERE col = 'str', then col must be not null
          if (isSimpleExprDetermineNotNull(expr))
            pred = UMul.mk(pred, mkNotNullPred(projVar));
        }
      }
      else {
        final PredDesc predDesc = mkPredDesc(filter.predicate());
        pred = UPred.mkFunc(predDesc.name(), projVar);
      }
      if (pred == null) return null;

      return UMul.mk(predecessor, pred);
    }

    private UTerm trInSubFilter(InSubFilter filter) {
      final UTerm lhs = tr(filter.predecessors()[0]);
      if (lhs == null) return null;

      final UVar lhsVisibleVar = tail(visibleVars);
      assert lhsVisibleVar != null;

      auxVar = lhsVisibleVar;
      final UTerm rhs = tr(filter.predecessors()[1]);
      auxVar = null;

      if (rhs == null) return null;

      final UVar rhsVisibleVar = pop(visibleVars); // RHS vars are no longer visible.
      final UVar rhsFreeVar = pop(freeVars);
      assert rhsVisibleVar != null && rhsFreeVar != null;

      final AttrsDesc attrsDesc = mkAttrDesc(filter.attrs());
      final UVar lhsProjVar = mkProj(filter.attrs(), attrsDesc, lhsVisibleVar);
      final UTerm eqVar = UPred.mkBinary(UPred.PredKind.EQ, lhsProjVar, rhsVisibleVar);
      final UTerm notNull = mkNotNullPred(rhsVisibleVar);
      putKnownEqSchema(result.schemaOf(lhsProjVar), result.schemaOf(rhsVisibleVar));

      UTerm decoratedRhs = UMul.mk(eqVar, notNull, rhs);
      // Summation must be added if absent.
      final boolean needSum = rhsVisibleVar.kind() != UVar.VarKind.PROJ;
      if (needSum) decoratedRhs = USum.mk(UVar.getBaseVars(rhsFreeVar), decoratedRhs);
      // Normally, the RHS has to be squashed. If RHS is known to be deduplicated,
      // then Squash can be omitted. This trick allows proving more cases.
      final boolean needSquash = !isEffectiveDeduplicated(filter.predecessors()[1]);
      if (needSquash) decoratedRhs = USquash.mk(decoratedRhs);
      // If not adding Squash and Summation, then RHS free vars are "exposed" to the outer scope.
      if (!needSum && !needSquash) {
        final UVar lhsFreeVar = pop(freeVars);
        assert lhsFreeVar != null;
        push(freeVars, UVar.mkConcat(lhsFreeVar, rhsFreeVar));
      }

      return UMul.mk(lhs, decoratedRhs);
    }

    private UTerm trExistsFilter(ExistsFilter filter) {
      final UTerm lhs = tr(filter.predecessors()[0]);
      if (lhs == null) return null;

      final UVar lhsFreeVars = pop(freeVars);
      assert lhsFreeVars != null;

      auxVar = tail(visibleVars);
      final UTerm rhs = tr(filter.predecessors()[1]);
      auxVar = null;

      if (rhs == null) return null;

      final UVar rhsVisibleVars = pop(visibleVars);
      final UVar rhsFreeVars = pop(freeVars);
      assert rhsVisibleVars != null && rhsFreeVars != null;
      push(freeVars, UVar.mkConcat(lhsFreeVars, rhsFreeVars));

      return UMul.mk(lhs, USquash.mk(rhs));
    }

    private UTerm trJoin(Join join) {
      final UTerm lhs = tr(join.predecessors()[0]);
      final UTerm rhs = tr(join.predecessors()[1]);
      if (lhs == null || rhs == null) return null;

      final UVar rhsVisibleVar = pop(visibleVars);
      final UVar lhsVisibleVar = pop(visibleVars);
      final UVar rhsFreeVar = pop(freeVars);
      final UVar lhsFreeVar = pop(freeVars);
      assert rhsVisibleVar != null && rhsFreeVar != null;
      assert lhsVisibleVar != null && lhsFreeVar != null;

      final SchemaDesc lhsSchema = result.schemaOf(lhsVisibleVar);
      final SchemaDesc rhsSchema = result.schemaOf(rhsVisibleVar);
      final UVar joinedVar = UVar.mkConcat(lhsVisibleVar, rhsVisibleVar);
      push(visibleVars, joinedVar);
      push(freeVars, UVar.mkConcat(lhsFreeVar, rhsFreeVar));
      result.setVarSchema(joinedVar, lhsSchema, rhsSchema);

      if (join.kind() == CROSS_JOIN) return UMul.mk(lhs, rhs);

      final Symbol lhsKey = join.lhsAttrs(), rhsKey = join.rhsAttrs();
      final AttrsDesc lhsAttrsDesc = mkAttrDesc(lhsKey);
      final AttrsDesc rhsAttrsDesc = mkAttrDesc(rhsKey);
      final UVar lhsProjVar = mkProj(lhsKey, lhsAttrsDesc, lhsVisibleVar);
      final UVar rhsProjVar = mkProj(rhsKey, rhsAttrsDesc, rhsVisibleVar);
      putKnownEqSchema(result.schemaOf(lhsProjVar), result.schemaOf(rhsProjVar));

      final UTerm eqCond = UPred.mkBinary(UPred.PredKind.EQ, lhsProjVar, rhsProjVar);
      final UTerm notNullCond = mkNotNullPred(rhsProjVar);
      if (join.kind() == INNER_JOIN) return UMul.mk(lhs, rhs, eqCond, notNullCond);

      // Left Join
      if (join.kind() == LEFT_JOIN) {
        final UMul symm = UMul.mk(rhs, eqCond, notNullCond);
        UTerm newSum = USum.mk(UVar.getBaseVars(rhsFreeVar), symm.copy());
        newSum = replaceAllBoundedVars(newSum);
        final UMul asymm = UMul.mk(mkIsNullPred(rhsVisibleVar), UNeg.mk(newSum));
        return UMul.mk(lhs, UAdd.mk(symm, asymm));
      }
      // Right Join
      if (join.kind() == RIGHT_JOIN) {
        final UMul symm = UMul.mk(lhs, eqCond, notNullCond);
        UTerm newSum = USum.mk(UVar.getBaseVars(lhsFreeVar), symm.copy());
        newSum = replaceAllBoundedVars(newSum);
        final UMul asymm = UMul.mk(mkIsNullPred(lhsVisibleVar), UNeg.mk(newSum));
        return UMul.mk(rhs, UAdd.mk(symm, asymm));
      }
      // Full Join
      final UTerm innerJoinBody = UMul.mk(lhs, rhs, eqCond, notNullCond);
      UTerm newSumLJoin = USum.mk(UVar.getBaseVars(rhsFreeVar), UMul.mk(rhs.copy(), eqCond.copy(), notNullCond.copy()));
      newSumLJoin = replaceAllBoundedVars(newSumLJoin);
      final UTerm asymmLeft = UMul.mk(lhs.copy(), mkIsNullPred(rhsVisibleVar), UNeg.mk(newSumLJoin));

      UTerm newSumRJoin = USum.mk(UVar.getBaseVars(lhsFreeVar), UMul.mk(lhs.copy(), eqCond.copy(), notNullCond.copy()));
      newSumRJoin = replaceAllBoundedVars(newSumRJoin);
      final UTerm asymmRight = UMul.mk(rhs.copy(), mkIsNullPred(lhsVisibleVar), UNeg.mk(newSumRJoin));

      return UAdd.mk(innerJoinBody, asymmLeft, asymmRight);
    }

    private UTerm trSetOp(SetOp setOp) {
      UTerm lhs = tr(setOp.predecessors()[0]);
      UTerm rhs = tr(setOp.predecessors()[1]);
      if (lhs == null || rhs == null) return null;

      final UVar rhsVisibleVar = pop(visibleVars);
      final UVar lhsVisibleVar = pop(visibleVars);
      final UVar rhsFreeVar = pop(freeVars);
      final UVar lhsFreeVar = pop(freeVars);
      assert rhsVisibleVar != null && rhsFreeVar != null;
      assert lhsVisibleVar != null && lhsFreeVar != null;

      if (!varKindAligned(lhsVisibleVar, rhsVisibleVar)) return null;
      assert lhsVisibleVar.kind() == rhsVisibleVar.kind();
      // `getPrimaryVar` only to ensure using consistent visible var of each side
      final UVar primaryVar = getPrimaryVar(lhsVisibleVar, rhsVisibleVar);
      final UVar secondaryVar = (primaryVar == lhsVisibleVar) ? rhsVisibleVar : lhsVisibleVar;
      if (primaryVar.is(UVar.VarKind.BASE)) {
        if (primaryVar == lhsVisibleVar) rhs.replaceVarInplace(secondaryVar, primaryVar, false);
        else lhs.replaceVarInplace(secondaryVar, primaryVar, false);
        push(visibleVars, primaryVar);
        push(freeVars, primaryVar);
      } else { // PROJ or CONCAT var
        // visible vars of lhs and rhs are `a1(t1)` and `a2(t2)`,
        // add eq pred `[t = a1(t1)]` and `[t = a2(t2)]` to lhs and rhs respectively
        final UVar outVar;
        if (!isTargetSide) {
          outVar = mkFreshVar(result.schemaOf(primaryVar));
          pivotVars.put(result.schemaOf(primaryVar), outVar);
        } else outVar = pivotVars.get(result.schemaOf(primaryVar));

        final UTerm lhsEqPred = UPred.mkBinary(UPred.PredKind.EQ, outVar, lhsVisibleVar);
        final UTerm rhsEqPred = UPred.mkBinary(UPred.PredKind.EQ, outVar, rhsVisibleVar);
        lhs = USum.mk(UVar.getBaseVars(lhsVisibleVar), UMul.mk(lhsEqPred, lhs));
        rhs = USum.mk(UVar.getBaseVars(rhsVisibleVar), UMul.mk(rhsEqPred, rhs));
        push(visibleVars, outVar);
        push(freeVars, outVar);
        putKnownEqSchema(result.schemaOf(lhsVisibleVar), result.schemaOf(rhsVisibleVar));
      }
      switch (setOp.kind()) {
        case UNION: // `lhs + rhs`, `||lhs + rhs||` if deduplicated
          UTerm union = UAdd.mk(lhs, rhs);
          if (setOp.deduplicated()) union = USquash.mk(union);
          return union;
        case INTERSECT: // `lhs * ||rhs|| + ||lhs|| * rhs`, `||lhs * rhs||` if deduplicated
          if (setOp.deduplicated()) return USquash.mk(UMul.mk(lhs, rhs));
          else return UAdd.mk(UMul.mk(lhs, USquash.mk(rhs)), UMul.mk(USquash.mk(lhs.copy()), rhs.copy()));
        case EXCEPT: // `lhs * not(rhs)`, `||lhs * not(rhs)||` if deduplicated
          UTerm except = UMul.mk(lhs, UNeg.mk(rhs));
          if (setOp.deduplicated()) except = USquash.mk(except);
          return except;
        default: throw new IllegalArgumentException("unknown set operator" + setOp);
      }
    }

    private boolean varKindAligned(UVar var0, UVar var1) {
      if (var0.kind() != var1.kind() || var0.args().length != var1.args().length) return false;

      if (var0.is(UVar.VarKind.BASE)) return true;
      else if (var0.is(UVar.VarKind.PROJ) || var0.is(UVar.VarKind.CONCAT)) {
        for (int i = 0, bound = var0.args().length; i < bound ; ++i) {
          if (!varKindAligned(var0.args()[i], var1.args()[i])) return false;
        }
        return true;
      }
      return false;
    }

    private UVar getPrimaryVar(UVar var0, UVar var1) {
      assert varKindAligned(var0, var1);
      if (var0.toString().compareTo(var1.toString()) <= 0) return var0;
      else return var1;
    }

    private UTerm trAgg(Agg agg) {
      // e.g. `SELECT a0, count(a1) as a2 FROM ...`, and func symbol is `f0`
      // Agg(groupBy: a0, aggregate: aggFunc f0<count>(a1), aggOut: a2, schema s, having pred p)
      // `t1` = out var of predecessor expr, `t` = out var of this Agg op
      // Q(concat(t_grp, t_agg)) ∶= ||\sum{t1} (E(t1) * [t_grp = a0(t1)]|| * AggUExpr(t_agg, a1, count, E) * [p(a2(t))]
      // E(t1) = predecessor(t1) * [a0(t)  = a0(t1)]
      final UTerm predecessor = tr(agg.predecessors()[0]);
      if (predecessor == null) return null;
      if (!checkSchemaFeasible(agg)) return null;

      final UVar viableVar = pop(visibleVars);
      final UVar freeVar = pop(freeVars);
      assert viableVar != null && freeVar != null;

      final SchemaDesc outSchema = mkSchema(agg.schema(), 2);
      // Build up Proj vars used in the following process
      final AttrsDesc groupByAttrDesc = mkAttrDesc(agg.groupByAttrs()); // AttrsDesc: a0
      final UVar groupByProjVar = mkProj(agg.groupByAttrs(), groupByAttrDesc, viableVar); // a0(t1)
      final UVar groupByOutVar; // t_grp
      final SchemaDesc subSchemaDesc0 = new SchemaDesc(outSchema.components[0]);
      if (!isTargetSide) {
        groupByOutVar = mkFreshVar(subSchemaDesc0);
        pivotVars.put(subSchemaDesc0, groupByOutVar);
      } else groupByOutVar = pivotVars.get(subSchemaDesc0);

      final AttrsDesc aggAttrDesc = mkAttrDesc(agg.aggregateAttrs()); // AttrsDesc: a1
      final UVar aggProjVar = mkProj(agg.aggregateAttrs(), aggAttrDesc, viableVar); // a1(t1)

      final AttrsDesc aggOutAttrDesc = mkAttrDesc(agg.aggregateOutputAttrs()); // AttrsDesc: a2
      final UVar aggOutVar; // t_agg
      final SchemaDesc subSchemaDesc1 = new SchemaDesc(outSchema.components[1]);
      if (!isTargetSide) {
        aggOutVar = mkFreshVar(subSchemaDesc1);
        pivotVars.put(subSchemaDesc1, aggOutVar);
      } else aggOutVar = pivotVars.get(subSchemaDesc1);

      // Create the output var
      final UVar outVar = UVar.mkConcat(groupByOutVar, aggOutVar);
      push(freeVars, outVar);
      push(visibleVars, outVar.copy());
      result.setVarSchema(outVar, outSchema);

      // Step 1. Build up common expression: ||\sum{t1} (predecessor(t1) * [a0(t) = a0(t1)]||
      final UTerm eqCond = UPred.mkBinary(UPred.PredKind.EQ, groupByOutVar, groupByProjVar);
      final UTerm predecessorWithPred = UMul.mk(predecessor, eqCond);
      final UTerm commonExpr = USquash.mk(USum.mk(UVar.getBaseVars(freeVar), predecessorWithPred));

      // Step 2. Build up: AggUExpr(t, a1, count, E), which depends on different agg functions
      // E(t1) = predecessor(t1) * [a0(t)  = a0(t1)], it is exactly `predecessorWithPred`
      final UTerm predecessorWithPred2 = predecessorWithPred.copy();
      final UTerm projOutVarTerm = UVarTerm.mk(aggOutVar); // Wrapped term of `a2(t)`
      UTerm aggExpr = null;
      switch (agg.aggFuncKind()) {
        case SUM, AVERAGE, COUNT -> {
          if (agg.aggFuncKind() == COUNT && agg.deduplicated()) {
            final UVar newOuterVar = mkFreshVar(result.varToSchema.get(aggProjVar));
            final UTerm innerSumEq = UPred.mkBinary(UPred.PredKind.EQ, newOuterVar, aggProjVar);
            final UTerm innerSumNotNull = mkNotNullPred(aggProjVar);
            final UTerm innerSumBody = UMul.mk(predecessorWithPred2, innerSumEq, innerSumNotNull);
            UTerm innerSum = USum.mk(UVar.getBaseVars(freeVar), innerSumBody);
            innerSum = replaceAllBoundedVars(innerSum);
            final UTerm outerSum = USum.mk(new HashSet<>(Set.of(newOuterVar)), USquash.mk(innerSum));
            aggExpr = UPred.mkBinary(UPred.PredKind.EQ, projOutVarTerm, outerSum);
          } else {
            final UTerm notNullCond = mkNotNullPred(aggProjVar);
            final UTerm countBody = UMul.mk(predecessorWithPred2, notNullCond);
            UTerm countSum = USum.mk(UVar.getBaseVars(freeVar), countBody);
            countSum = replaceAllBoundedVars(countSum);
            final UTerm sumBody = UMul.mk(predecessorWithPred2.copy(), notNullCond.copy(), UVarTerm.mk(aggProjVar));
            UTerm sumSum = USum.mk(UVar.getBaseVars(freeVar), sumBody);
            sumSum = replaceAllBoundedVars(sumSum);
            switch (agg.aggFuncKind()) {
              case SUM ->
                  aggExpr = UPred.mkBinary(UPred.PredKind.EQ, projOutVarTerm, sumSum);
              case AVERAGE ->
                  aggExpr = UPred.mkBinary(UPred.PredKind.EQ, UMul.mk(projOutVarTerm, countSum), sumSum);
              case COUNT ->
                  aggExpr = UPred.mkBinary(UPred.PredKind.EQ, projOutVarTerm, countSum);
            }
          }
        }
        case MAX, MIN -> {
          // Build ||\sum(..)||, make bounded vars to be fresh vars
          final UTerm squashSumPred = UPred.mkBinary(UPred.PredKind.EQ, aggProjVar, aggOutVar);
          UTerm squashSum = USum.mk(UVar.getBaseVars(freeVar), UMul.mk(predecessorWithPred2, squashSumPred));
          squashSum = replaceAllBoundedVars(squashSum);
          final UTerm squash = USquash.mk(squashSum);
          // Build not(\sum(..)), , make bounded vars to be fresh vars
          final UTerm predecessorWithPred2_ = predecessorWithPred.copy();
          final UPred.PredKind predKind = agg.aggFuncKind() == MAX ? UPred.PredKind.GT : UPred.PredKind.LT;
          final UTerm notSumPred = UPred.mkBinary(predKind, aggProjVar, aggOutVar);
          UTerm notSum = USum.mk(UVar.getBaseVars(freeVar), UMul.mk(predecessorWithPred2_, notSumPred));
          notSum = replaceAllBoundedVars(notSum);
          final UTerm not = UNeg.mk(notSum);
          aggExpr = UMul.mk(not, squash);
        }
      }
      if (aggExpr == null) return null;

      // Step 3. Build up having pred: [p(a2(t))]
      UTerm havingPred;
      if (enableConcretePlanFeature) {
        assert extraInfo != null;
        final Expression expr = extraInfo.getConcretePred(agg.havingPred());
        if (expr.equals(Expression.EXPRESSION_TRUE)) // Some concrete query doesn't have `HAVING` clause
          return UMul.mk(commonExpr, aggExpr);

        if (isSimpleIntArithmeticExpr(expr))
          havingPred = UExprSupport.mkBinaryArithmeticPred(expr, aggOutVar);
        else {
          // Concrete predicate that can only be expressed by [p(a(t))]
          final PredDesc havingPredDesc = mkPredDesc(agg.havingPred());
          havingPred = UPred.mkFunc(havingPredDesc.name(), aggOutVar);
        }
        // Agg result in `HAVING` pred should not be null, no need to add not([IsNull]) after the predicate
      }
      else {
        final PredDesc havingPredDesc = mkPredDesc(agg.havingPred());
        havingPred = UPred.mkFunc(havingPredDesc.name(), aggOutVar);
      }
      if (havingPred == null) return null;

      return UMul.mk(commonExpr, aggExpr, havingPred);
    }

    private UTerm replaceAllBoundedVars(UTerm expr) {
      expr = transformSubTerms(expr, this::replaceAllBoundedVars);
      if (expr.kind() != SUMMATION) return expr;

      final Set<UVar> oldVars = new HashSet<>(((USum) expr).boundedVars());
      for (UVar oldVar : oldVars) {
        final UVar newVar = UVar.mkBase(UName.mk(varSeq.next()));
        expr = expr.replaceVar(oldVar, newVar, true);
        result.setVarSchema(newVar, result.schemaOf(oldVar));
      }
      return expr;
    }

    private UTerm trProj(Proj proj) {
      final UTerm predecessor = tr(proj.predecessors()[0]);
      if (predecessor == null) return null;
      if (!checkSchemaFeasible(proj)) return null;

      final UVar viableVar = pop(visibleVars);
      final UVar freeVar = pop(freeVars);
      assert viableVar != null && freeVar != null;

      final AttrsDesc attrDesc = mkAttrDesc(proj.attrs());
      final UVar projVar = mkProj(proj.attrs(), attrDesc, viableVar);
      final SchemaDesc outSchema = mkSchema(proj.schema());
      // final boolean isClosed = needNewFreeVar(proj);
      final boolean isClosed = true;
      // In some cases, we "inline" the new variable introduced by the Proj.
      // e.g., Proj(IJoin<k0 k1>(t0, Proj<a>(t1))) is translated to
      //   Sum{x,y}(.. * t0(x) * [k0(x) = k1(a(y))] * t1(y)), instead of
      //   Sum{x,z}(.. * t0(x) * [k0(x) = k1(z)] * Sum{y}([z = a(y)] * t1(y))).
      // This will save a lot of bother in the subsequent process.
      final UVar outVar;
      if (!isClosed) outVar = projVar;
      else if (!isTargetSide) outVar = mkFreshVar(outSchema);
      else outVar = pivotVars.get(outSchema);

      push(freeVars, outVar);
      push(visibleVars, outVar);

      result.setVarSchema(outVar, outSchema);
      pivotVars.put(outSchema, outVar);

      if (isClosed) {
        // If new var is required, we need to add a predicate [outVar = a(inVar)].
        UPred eq = UPred.mkBinary(UPred.PredKind.EQ, outVar, projVar);

        // Cope with special values in concrete plan: SELECT 1 FROM ...
        if (enableConcretePlanFeature) {
          assert extraInfo != null;
          final Object value = extraInfo.getSpecialProjAttrs(proj.attrs());
          if (value != null) {
            final UTerm specialTerm = translateSpecialValues(proj.attrs(), value);
            if (specialTerm != null)
              eq = UPred.mkBinary(UPred.PredKind.EQ, UVarTerm.mk(outVar), specialTerm);
          }
        }

        final USum s = USum.mk(UVar.getBaseVars(freeVar), UMul.mk(eq, predecessor));
        if (proj.deduplicated()
            && !isImplicitDeduplicated(proj)
            && !isEffectiveDeduplicated(proj)) {
          return USquash.mk(s);
        } else {
          return s;
        }

      } else {
        return USum.mk(UVar.getBaseVars(freeVar), predecessor);
      }
    }

    private boolean needNewFreeVar(Proj proj) {
      return isOutermostProj(proj) // 1. directly affects the output
          // 2. The projection need explicit deduplication
          || (proj.deduplicated()
              && !isImplicitDeduplicated(proj)
              && !isEffectiveDeduplicated(proj));
    }

    private boolean isOutermostProj(Proj proj) {
      Op op = proj;
      Op succ = op.successor();
      while (succ != null) {
        final OpKind succKind = succ.kind();
        if (succKind == PROJ) return false;
        if (succKind.isSubquery() && succ.predecessors()[1] == op) return false;
        op = succ;
        succ = succ.successor();
      }
      return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isImplicitDeduplicated(Proj proj) {
      Op op = proj;
      Op succ = op.successor();
      while (succ != null) {
        final OpKind succKind = succ.kind();
        if (succKind == PROJ && ((Proj) succ).deduplicated()) return true;
        if (succKind.isSubquery() && succ.predecessors()[1] == op) return true;
        op = succ;
        succ = succ.successor();
      }
      return false;
    }

    // Check if |Tr(op)| == Tr(op)
    private boolean isEffectiveDeduplicated(Op op) {
      // Principle: if the output of the op contains unique key, then no need to be squashed.
      if (op.kind().isFilter()) return isEffectiveDeduplicated(op.predecessors()[0]);

      if (op.kind().isJoin())
        return isEffectiveDeduplicated(op.predecessors()[0])
            && isEffectiveDeduplicated(op.predecessors()[1]);

      if (op.kind() == PROJ) {
        return isUniqueCoreAt(((Proj) op).attrs(), op.predecessors()[0]);
      }

      if (op.kind() == INPUT) {
        // Check if any attrs of this table is unique.
        final Symbol sym = ((Input) op).table();
        final Constraints C = rule.constraints();
        final Symbol table = isTargetSide ? C.instantiationOf(sym) : sym;
        return any(C.ofKind(Unique), it -> C.isEq(table, it.symbols()[0]));
      }

      return false;
    }

    private boolean isUniqueCoreAt(Symbol attrs, Op surface) {
      final OpKind kind = surface.kind();
      if (kind.isFilter()) return isUniqueCoreAt(attrs, surface.predecessors()[0]);

      if (kind == INPUT) {
        final Constraints C = rule.constraints();
        Symbol table = ((Input) surface).table();
        if (isTargetSide) {
          attrs = C.instantiationOf(attrs);
          table = C.instantiationOf(table);
        }
        return C.sourceOf(attrs) == table && isUniqueKey(table, attrs);
      }

      if (kind.isJoin()) {
        final Join join = (Join) surface;
        if (isUniqueCoreAt(attrs, surface.predecessors()[0])) {
          return isUniqueCoreAt(join.rhsAttrs(), surface.predecessors()[1]);
        } else if (isUniqueCoreAt(attrs, surface.predecessors()[1])) {
          return isUniqueCoreAt(join.lhsAttrs(), surface.predecessors()[0]);
        } else {
          return false;
        }
      }

      if (kind == PROJ) {
        final Proj proj = (Proj) surface;
        return rule.constraints().isEq(attrs, proj.attrs())
            && isUniqueCoreAt(proj.attrs(), proj.predecessors()[0]);
      }

      if (kind == AGG) return false;

      if (kind.isSetOp()) return ((Union) surface).deduplicated();

      assert false;
      return false;
    }

    private void putKnownEqSchema(SchemaDesc s0, SchemaDesc s1) {
      if (!isTargetSide && enableSchemaFeasibilityCheck) {
        knownEqSchemas.get().add(Pair.of(s0, s1));
        knownEqSchemas.get().add(Pair.of(s1, s0));
      }
    }

    private boolean checkSchemaFeasible(Proj tgtProj) {
      if (!isTargetSide || !enableSchemaFeasibilityCheck) return true;
      // Given a Proj<a' s'> in the target side. Suppose a' is instantiated from a0,
      // s' is instantiated from s. s0 is owned by Proj<a1 s> in the source side.
      // Then the instantiation <s -> s'> is feasible only if a1 and a0 is "possible-eq",
      // which requires AttrsEq(a0,a1) or a0 and a1 is a pair of join keys.
      final Symbol schema0 = rule.constraints().instantiationOf(tgtProj.schema());
      final Symbol attrs0 = rule.constraints().instantiationOf(tgtProj.attrs());
      final Op srcProj = rule.constraints().sourceSymbols().ownerOf(schema0);
      assert srcProj.kind() == PROJ;
      final Symbol attrs1 = ((Proj) srcProj).attrs();
      final SchemaDesc s0 = mkSchema(attrs0), s1 = mkSchema(attrs1);
      return s0 == s1 || knownEqSchemas.get().contains(Pair.of(s0, s1));
    }

    private boolean checkSchemaFeasible(Agg tgtAgg) {
      if (!isTargetSide || !enableSchemaFeasibilityCheck) return true;

      final Symbol schema0 = rule.constraints().instantiationOf(tgtAgg.schema());
      final Op srcAgg = rule.constraints().sourceSymbols().ownerOf(schema0);
      assert srcAgg.kind() == AGG;
      final Symbol groupByAttrs0 = rule.constraints().instantiationOf(tgtAgg.groupByAttrs());
      final Symbol groupByAttrs1 = ((Agg) srcAgg).groupByAttrs();
      final SchemaDesc groupByS0 = mkSchema(groupByAttrs0), groupByS1 = mkSchema(groupByAttrs1);
      // final Symbol aggAttrs0 = rule.constraints().instantiationOf(tgtAgg.aggregateAttrs());
      // final Symbol aggAttrs1 = ((Agg) srcAgg).aggregateAttrs();
      // final SchemaDesc aggS0 = mkSchema(aggAttrs0), aggS1 = mkSchema(aggAttrs1);
      return (groupByS0 == groupByS1 || knownEqSchemas.get().contains(Pair.of(groupByS0, groupByS1)));
          // && (aggS0 == aggS1 || knownEqSchemas.get().contains(Pair.of(aggS0, aggS1)));
    }

    private UTerm translateSpecialValues(Symbol sym, Object val) {
      // To be added
      assert sym.kind() != Symbol.Kind.ATTRS || val instanceof Integer;

      assert enableConcretePlanFeature && extraInfo != null;
      if (sym.kind() == Symbol.Kind.ATTRS) {
        return UConst.mk((Integer) val);
      }

      return null;
    }
  }

  class ICRewriter {
    private boolean isModified;

    public ICRewriter() {
      this.isModified = false;
    }

    public boolean isModified() {
      return isModified;
    }

    private UTerm rewriteExpr(UTerm expr) {
      expr = performICRewriteRule(expr, this::applyReference);
      expr = performICRewriteRule(expr, this::applyNotNull);
      expr = performICRewriteRule(expr, this::applyUniqueOnSelfJoin);
      expr = performICRewriteRule(expr, this::applyUniqueAddSquash);
      return expr;
    }

    private UTerm performICRewriteRule(UTerm expr, Function<UTerm, UTerm> transformation) {
      expr = transformation.apply(expr);
      // Routine normalizations for IC rewrite
      expr = normalizeExprEnhance(expr);
      return expr;
    }

    /** NotNull(R, a): [IsNull(a(t))] -> 0, if there exists term `R(t)` in multiplication factors */
    private UTerm applyNotNull(UTerm expr) {
      // TODO: Support multi-schema issue in SET operators
      expr = transformSubTerms(expr, this::applyNotNull);
      if (expr.kind() != MULTIPLY) return expr;
      // In `[a0(t0) = a1(t1)] * not([IsNull(a1(t1))])`, `[IsNull(a1(t1))]` can also be rewritten by NotNull(r0, a0)
      // Create an equivalence class for vars in this UMul
      final NaturalCongruence<UVar> varEqClass = UExprSupport.getEqVarCongruenceInTermsOfMul(expr);
      final UTerm mulContext = expr.copy();
      expr = checkNotNullPreds(mulContext, expr, varEqClass);
      return expr;
    }

    private UTerm checkNotNullPreds(UTerm mulContext, UTerm expr, NaturalCongruence<UVar> varEqClass) {
      expr = transformSubTerms(expr, e -> checkNotNullPreds(mulContext, e, varEqClass));
      assert mulContext.kind() == MULTIPLY;

      if (!varIsNullPred(expr)) return expr;
      // If expr is some `[IsNull(a(t))]`
      final UVar predVar = UExprSupport.getIsNullPredVar((UPred) expr); // predVar = `a(t)` (or `t`)
      for (UVar eqVar : varEqClass.eqClassOf(predVar)) {
        if (!eqVar.isUnaryVar()) continue;
        final UVar predBaseVar = UVar.getSingleBaseVar(eqVar); // predBaseVar = `t`
        // Check whether exists some `R(t)` term for expr `[IsNull(a(t))]`
        if (!any(mulContext.subTermsOfKind(TABLE), t -> t.isUsing(predBaseVar))) continue;
        // check if exists any NotNull() constraint that matches the pred `[IsNull(..)]`
        if (any(varEqClass.eqClassOf(eqVar), this::uVarNotNullByConstraint)) {
          expr = UConst.zero();
          isModified = true;
          break;
        }
      }
      return expr;
    }

    /** Reference(R0, a0, R1, a1):
     * \sum_{t1}(R0(t0) * R1(t1) * [a0(t0) = a1(t1)] * not([IsNull(a0(t0))])) -> R0(t0) * not([IsNull(a0(t0))])) */
    private UTerm applyReference(UTerm expr) {
      return applyReference0(expr, false);
    }

    private UTerm applyReference0(UTerm expr, boolean inSetSem) {
      final boolean setSem = inSetSem || expr.kind() == SQUASH || expr.kind() == NEGATION;
      expr = transformSubTerms(expr, e -> applyReference0(e, setSem));
      if (expr.kind() != SUMMATION) return expr;

      final USum summation = (USum) expr;
      final UTerm body = summation.body();
      final NaturalCongruence<UVar> varEqClass = UExprSupport.getEqVarCongruenceInTermsOfMul(body);
      for (Constraint ref : rule.constraints().ofKind(Reference)) {
        final Symbol rSym0 = ref.symbols()[0], aSym0 = ref.symbols()[1];
        final Symbol rSym1 = ref.symbols()[2], aSym1 = ref.symbols()[3];
        // Precondition of this axiom: Reference(R0, a0, R1, a1) + Unique(R1, a1)
        // Check existence of Unique(R1, a1), which can be ignored if inside ||..|| or not(..)
        if (!setSem && !isUniqueKey(rSym1, aSym1)) continue;
        // Get all sub-terms `R(t1)`, where `t1` is a bounded var
        final List<UTable> uTables = new ArrayList<>();
        for (UTerm subTerm : body.subTermsOfKind(TABLE)) {
          final UTable uTable = (UTable) subTerm;
          final UVar tblBaseVar = uTable.var();
          if (!summation.isUsingBoundedVar(tblBaseVar)) continue;
          if (uTableMatchRelSym((UTable) subTerm, rSym1)) uTables.add((UTable) subTerm);
        }
        if (uTables.isEmpty()) continue;
        // For each `R(t1)`, check existence of `[a0(t0) = a1(t1)]` and `not([IsNull(a0(t0)])`
        for (UTable uTable : uTables) {
          final UVar tgtVar = uTable.var(); // tgtVar = `t1`
          UPred eqPred = null;
          for (UTerm subTerm : body.subTermsOfKind(PRED)) {
            final UPred pred = (UPred) subTerm;
            if (!pred.isPredKind(UPred.PredKind.EQ) || !isPredOfVarArg(pred)) continue;
            final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
            assert pArgs.size() == 2;
            if (!((uVarMatchAttrsSym(pArgs.get(0), aSym0, false)
                && uVarMatchAttrsSym(pArgs.get(1), aSym1, false))
                || uVarMatchAttrsSym(pArgs.get(1), aSym0, false)
                && uVarMatchAttrsSym(pArgs.get(0), aSym1, false)))
              continue;
            // projVar0 = `a0(t0)`, projVar1 = `a1(t1)`, confirm projVar1 is using tgtVar `t1`
            final UVar projVar0 = uVarMatchAttrsSym(pArgs.get(1), aSym1, false) ? pArgs.get(0) : pArgs.get(1);
            final UVar projVar1 = uVarMatchAttrsSym(pArgs.get(1), aSym1, false) ? pArgs.get(1) : pArgs.get(0);
            if (!projVar1.isUsing(tgtVar)) continue;
            // Check whether `not([IsNull(a0(t0)])` holds:
            // 1. has `not([IsNull(a0(t0)])` term, 2. has NotNull(R0, a0) constraint
            if (!any(varEqClass.eqClassOf(projVar0), this::uVarNotNullByConstraint)
                && !any(varEqClass.eqClassOf(projVar0), v -> uVarNotNullByTerm(v, body))) continue;
            // All satisfied, let eqPred = `[a0(t0) = a1(t1)]`
            eqPred = pred;
            break;
          }
          if (eqPred == null) continue;
          // Try replacing all `a1(t1)` with `a0(t0)` and check whether other sub-terms uses `t1`
          final List<UVar> eqPredArgs = UExprSupport.getPredVarArgs(eqPred);
          assert eqPredArgs.size() == 2;
          final UVar eqArg1 = eqPredArgs.get(1).isUsing(tgtVar) ? eqPredArgs.get(1) : eqPredArgs.get(0); // `a1(t1)`
          final UVar eqArg0 = eqPredArgs.get(1).isUsing(tgtVar) ? eqPredArgs.get(0) : eqPredArgs.get(1); // `a0(t0)`
          boolean extraUseTgtVar = false;
          for (UTerm subTerm : body.subTerms()) {
            if (subTerm == uTable || subTerm == eqPred) continue;
            if (subTerm.replaceVar(eqArg1, eqArg0, false).isUsing(tgtVar)) {
              extraUseTgtVar = true;
              break;
            }
          }
          if (extraUseTgtVar) continue;
          // If no extra using of `t1`, then perform rewrite: \sum_{t1}(R1(t1) * [a0(t0) = a1(t1)]) -> 1
          isModified = true;
          body.subTerms().removeAll(List.of(uTable, eqPred));
          body.subTerms().add(UConst.one());
          body.replaceVarInplace(eqArg1, eqArg0, false);
          summation.removeBoundedVar(tgtVar);
          // If bounded vars are all removed, remove the whole summation and return body of sum
          if (summation.boundedVars().isEmpty()) return body;
        }
      }
      return expr;
    }

    /** Unique(R, a) add squash */
    private UTerm applyUniqueAddSquash(UTerm expr) {
      // \sum_{t}([a(t) = e] * R(t) * f(t)) -> || \sum_{t}([a(t) = e] * R(t) * f(t)) ||
      expr = squashSummation(expr);
      // \sum_{t}(R(t) * g(t)) -> \sum_{t}||R(t) * g(t)||, g(t) does not contain [a(t) = e]
      expr = squashMulTerms(expr);
      return expr;
    }

    private UTerm squashMulTerms(UTerm expr) {
      if (expr.kind().isUnary()) return expr;
      expr = transformSubTerms(expr, this::squashMulTerms);
      if (expr.kind() != MULTIPLY) return expr;

      // If only matching `R(ti)` without `[a(ti) = e]`, put each sub-term using `ti` into squash
      final UMul mul = (UMul) expr;
      final List<UTerm> squashedTerm = new ArrayList<>();
      for (Constraint unique : rule.constraints().ofKind(Unique)) {
        final Symbol rSym = unique.symbols()[0], aSym = unique.symbols()[1];
        for (UTerm subTerm : mul.subTermsOfKind(TABLE)) {
          if (uTableMatchRelSym((UTable) subTerm, rSym)) {
            final UVar tgtVar = ((UTable) subTerm).var();
            mul.subTerms().stream().filter(t -> t.isUsing(tgtVar)).forEach(squashedTerm::add);
            mul.subTerms().removeIf(t -> t.isUsing(tgtVar));
          }
        }
      }
      if (!squashedTerm.isEmpty()) {
        isModified = true;
        final USquash newSquash = USquash.mk(UMul.mk(squashedTerm));
        mul.subTerms().add(newSquash);
      }
      return expr;
    }

    private UTerm squashSummation(UTerm expr) {
      if (expr.kind().isUnary()) return expr;
      expr = transformSubTerms(expr, this::squashSummation);
      if (expr.kind() != SUMMATION) return expr;

      final USum summation = (USum) expr;
      final UTerm body = summation.body();
      final List<UTerm> squashedTerms = new ArrayList<>(body.subTerms().size());
      final Set<UVar> squashedVars = new HashSet<>(summation.boundedVars().size());
      for (Constraint unique : rule.constraints().ofKind(Unique)) {
        final Symbol rSym = unique.symbols()[0], aSym = unique.symbols()[1];
        final List<UTable> uTables = new ArrayList<>();
        // Get all `R(ti)` from sub-terms
        for (UTerm subTerm : body.subTermsOfKind(TABLE)) {
          final UTable uTable = (UTable) subTerm;
          final UVar tblBaseVar = uTable.var();
          if (!summation.isUsingBoundedVar(tblBaseVar)) continue;
          if (uTableMatchRelSym((UTable) subTerm, rSym)) uTables.add((UTable) subTerm);
        }
        if (uTables.isEmpty()) continue;
        // For each `R(ti)`, check existence of `[a(ti) = e]`
        for (UTable uTable : uTables) {
          final UVar targetVar = uTable.var();
          UPred uPred = null;
          for (UTerm subTerm : body.subTermsOfKind(PRED)) {
            final UPred pred = (UPred) subTerm;
            if (!pred.isPredKind(UPred.PredKind.EQ) || !isPredOfVarArg(pred)) continue;
            final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
            assert pArgs.size() == 2;
            if (!uVarMatchAttrsSym(pArgs.get(0), aSym, true)
                && !uVarMatchAttrsSym(pArgs.get(1), aSym, true)) continue;
            final UVar matchVar = uVarMatchAttrsSym(pArgs.get(0), aSym, true) ? pArgs.get(0) : pArgs.get(1); // `a(ti)`
            final UVar constVar = uVarMatchAttrsSym(pArgs.get(0), aSym, true) ? pArgs.get(1) : pArgs.get(0); // `e`
            if (!matchVar.isUnaryVar()) continue;
            if (matchVar.isUsing(targetVar) && !constVar.isUsing(UVar.getSingleBaseVar(matchVar))) {
              uPred = pred;
              break;
            }
          }
          if (uPred == null) continue;
          // Move var `ti` and all terms using `ti` into squash
          // TODO: refactor adjacent squashing
          squashedTerms.addAll(List.of(uTable, uPred));
          body.subTerms().removeAll(List.of(uTable, uPred));
          final List<UVar> adjacentSquashVar = new ArrayList<>();
          push(adjacentSquashVar, targetVar);
          // First check constVar of `uPred`
          final List<UVar> predArgs = UExprSupport.getPredVarArgs(uPred);
          final UVar constVar = predArgs.get(0).isUsing(targetVar) ? predArgs.get(1) : predArgs.get(0);
          if ((constVar.isUnaryVar())
              && summation.isUsingBoundedVar(UVar.getSingleBaseVar(constVar))
              && any(rule.constraints().ofKind(Unique),
              u -> uVarMatchAttrsSym(constVar, u.symbols()[1], true))) {
            push(adjacentSquashVar, UVar.getSingleBaseVar(constVar));
          }
          while (!adjacentSquashVar.isEmpty()) {
            final UVar squashVar = pop(adjacentSquashVar);
            final ListIterator<UTerm> iter = body.subTerms().listIterator();
            while (iter.hasNext()) {
              final UTerm subTerm = iter.next();
              if (!subTerm.isUsing(squashVar)) continue;
              squashedTerms.add(subTerm);
              iter.remove();
              if (subTerm.kind() == PRED) {
                final UPred pred = (UPred) subTerm;
                if (!pred.isPredKind(UPred.PredKind.EQ) || !isPredOfVarArg(pred)) continue;
                final List<UVar> pArgs = UExprSupport.getPredVarArgs(pred);
                assert pArgs.size() == 2;
                final UVar predVar0 = pArgs.get(0), predVar1 = pArgs.get(1);
                final UVar relatedVar = predVar0.isUsing(squashVar) ? predVar0 : predVar1; // var using targetVar
                final UVar adjacentVar = (relatedVar == predVar0) ? predVar1 : predVar0; // adjacent var
                if ((adjacentVar.isUnaryVar())
                    && summation.isUsingBoundedVar(UVar.getSingleBaseVar(adjacentVar))
                    && any(rule.constraints().ofKind(Unique),
                    u -> uVarMatchAttrsSym(adjacentVar, u.symbols()[1], true))) {
                  push(adjacentSquashVar, UVar.getSingleBaseVar(adjacentVar));
                }
              }
            }
            squashedVars.add(squashVar);
            summation.removeBoundedVar(squashVar);
          }
        }
      }
      // Build the new squash term
      if (!squashedTerms.isEmpty()) {
        isModified = true;
        final USquash newSquash = USquash.mk(USum.mk(squashedVars, UMul.mk(squashedTerms)));
        body.subTerms().add(newSquash);
      }
      if (summation.boundedVars().isEmpty()) expr = body;

      return expr;
    }

    /** Unique(R, a) eliminate self-join: R(t1) * R(t2) * [a(t1) = a(t2)] -> R(t1) * [t1 = t2] */
    private UTerm applyUniqueOnSelfJoin(UTerm expr) {
      expr = transformSubTerms(expr, this::applyUniqueOnSelfJoin);
      if (expr.kind() != MULTIPLY) return expr;

      final NaturalCongruence<UVar> varEqClass = UExprSupport.getEqVarCongruenceInTermsOfMul(expr);
      for (Constraint unique : rule.constraints().ofKind(Unique)) {
        final Symbol rSym = unique.symbols()[0], aSym = unique.symbols()[1];
        final List<UTable> uTables = new ArrayList<>();
        // Get all `R(ti)` and `[a(ti) = a(tj)]` predicates from sub-terms
        for (UTerm subTerm : expr.subTermsOfKind(TABLE)) {
          if (uTableMatchRelSym((UTable) subTerm, rSym)) uTables.add((UTable) subTerm);
        }
        // For each `[a(ti) = a(tj)]`, check existence of `R(ti)` and `R(tj)` and:
        // R(ti) * R(tj) * [a(ti) = a(tj)] -> R(ti) * [ti = tj]
        final List<UVar> eqVars = varEqClass.keys().stream().toList();
        for (int i = 0, bound = eqVars.size();i < bound; ++i) {
          final UVar var0 = eqVars.get(i);
          for (int j = i + 1, bound0 = eqVars.size();j < bound0; ++j) {
            final UVar var1 = eqVars.get(j);
            if (var0.equals(var1) || !varEqClass.isCongruent(var0, var1)) continue;
            if (!uVarMatchAttrsSym(var0, aSym, true) ||
                !uVarMatchAttrsSym(var1, aSym, true)) continue;
            final Set<UVar> baseVars0 = UVar.getBaseVars(var0);
            final Set<UVar> baseVars1 = UVar.getBaseVars(var1);
            assert baseVars0.size() == 1 && baseVars1.size() == 1;
            final UVar baseVar0 = baseVars0.iterator().next();
            final UVar baseVar1 = baseVars1.iterator().next();
            final UTerm uTable0 = linearFind(uTables, t -> t.var().equals(baseVar0));
            final UTerm uTable1 = linearFind(uTables, t -> t.var().equals(baseVar1));
            if (uTable0 == null || uTable1 == null) continue;

            isModified = true;
            expr.subTerms().remove(uTable1);
            expr.subTerms().add(UPred.mkBinary(UPred.PredKind.EQ, baseVar0, baseVar1));
          }
        }
      }
      return expr;
    }

    /** Helper functions */
    private boolean uTableMatchRelSym(UTable uTable, Symbol rSym) {
      final Set<Symbol> eqSyms = rule.constraints().eqClassOf(rSym);
      return any(eqSyms, r -> uTableMatchRelSym0(uTable, r));
    }

    private boolean uTableMatchRelSym0(UTable uTable, Symbol rSym) {
      assert rSym.kind() == Symbol.Kind.TABLE;
      return uTable.tableName().equals(initiatedNames.get(rSym));
    }

    private boolean uVarMatchAttrsSym(UVar uVar, Symbol aSym, boolean allowSuper) {
      final Set<Symbol> eqSyms = rule.constraints().eqClassOf(aSym);
      return any(eqSyms, r -> uVarMatchAttrsSym0(uVar, r, allowSuper));
    }

    private boolean uVarMatchAttrsSym0(UVar uVar, Symbol aSym, boolean allowSuper) {
      assert aSym.kind() == Symbol.Kind.ATTRS;
      if (!uVar.isUnaryVar()) return false;

      final List<Symbol> aSymSuperSets = rule.constraints().attrsAndTableSourceChain(aSym);
      if (aSymSuperSets.isEmpty()) return false;
      final List<Symbol> superAttrs = aSymSuperSets.subList(0, aSymSuperSets.size() - 1);
      superAttrs.add(0, aSym);
      // For `R.a`, if `a(t)` matches aSym `a`, then check schema of t is R
      final Symbol srcTable = aSymSuperSets.get(aSymSuperSets.size() - 1);
      final Set<Symbol> eqSrcTables = rule.constraints().eqClassOf(srcTable);
      final UVar baseVar = UVar.getSingleBaseVar(uVar);
      // Same tables have different schema id, so search for every eq tables of `srcTables` (`eqSrcTables`)
      if (all(eqSrcTables, t -> !result.symToSchema.get(t).equals(result.varToSchema.get(baseVar))))
        return false;
      // For NotNull and Unique constraints: `R.a0 \in R.a1 /\ NotNull(R, a0) -> NotNull(R, a1)`
      // So if `R.a0 \in R.a1` and schema of `t` is `R`, then var `a1(t)` (or `t`) also matches `a0`
      if (allowSuper)
        return (uVar.is(UVar.VarKind.BASE)
            || (uVar.is(UVar.VarKind.PROJ) && any(superAttrs, a -> uVar.name().equals(initiatedNames.get(a)))));
      else return uVar.is(UVar.VarKind.PROJ) && uVar.name().equals(initiatedNames.get(aSym));
    }

    // Check whether value of a UVar is not NULL by consulting constraints
    private boolean uVarNotNullByConstraint(UVar var) {
      // if (!var.is(UVar.VarKind.BASE) && !var.is(UVar.VarKind.PROJ)) return false;
      // final Set<UVar> eqVars = varEqClass.eqClassOf(var);
      final List<Constraint> notNullConstraints = rule.constraints().ofKind(NotNull);
      // check if exists any NotNull() constraint that matches the UVar
      return any(notNullConstraints, c -> uVarMatchAttrsSym(var, c.symbols()[1], true));
    }

    // Check whether there exists a term `not([IsNull(a(t))])` for a UVar a(t)
    private boolean uVarNotNullByTerm(UVar var, UTerm mul) {
      for (UTerm subTerm : mul.subTerms()) {
        if (varIsNotNullPred(subTerm)) {
          final UPred isNullPred = (UPred) ((UNeg) subTerm).body(); // Get [IsNull(a(t))]
          final UVar nullVar = UExprSupport.getIsNullPredVar(isNullPred); // Get a(t)
          if (nullVar.equals(var)) return true;
        }
      }
      return false;
    }
  }
}
