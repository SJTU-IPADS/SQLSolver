package sqlsolver.superopt.substitution;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import sqlsolver.common.utils.Lazy;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.ast.constants.SetOpKind;
import sqlsolver.sql.plan.*;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Table;
import sqlsolver.superopt.constraint.Constraint;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.fragment.*;

import java.util.*;
import java.util.stream.Collectors;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.ast.SqlNodeFields.Name2_1;
import static sqlsolver.sql.ast.constants.ConstraintKind.FOREIGN;
import static sqlsolver.sql.plan.PlanSupport.*;
import static sqlsolver.sql.schema.SchemaSupport.findIC;
import static sqlsolver.sql.schema.SchemaSupport.findRelatedIC;
import static sqlsolver.superopt.fragment.Symbol.Kind.*;

public class SubstitutionTranslator {
  private final PlanContext src, tgt;
  private final List<Constraint> constraints;
  private Fragment srcFragment, tgtFragment;
  private final Lazy<Map<Symbol, Object>> srcAssignments; // Symbol -> Ast Objects
  private final Lazy<Map<Symbol, Object>> tgtAssignments;
  private final ListMultimap<Symbol, Symbol> viableSources;
  private final SubstitutionTranslatorResult result;
  // private final Lazy<Map<Integer, Op>> srcOps; // plan nodeId -> Op
  // private final Lazy<Map<Integer, Op>> tgtOps;

  public SubstitutionTranslator(PlanContext src, PlanContext tgt) {
    this.src = src;
    this.tgt = tgt;
    this.constraints = new ArrayList<>();
    this.srcAssignments = Lazy.mk(HashMap::new);
    this.tgtAssignments = Lazy.mk(HashMap::new);
    this.result = new SubstitutionTranslatorResult(src, tgt);
    this.viableSources = MultimapBuilder.hashKeys().linkedListValues().build();
    // this.srcOps = Lazy.mk(HashMap::new);
    // this.tgtOps = Lazy.mk(HashMap::new);
  }

  private boolean fail(String reason) {
    result.error = reason;
    return false;
  }

  public String error() {
    return result.error;
  }

  public SubstitutionTranslatorResult translate() {
    srcFragment = new FragmentConstructor(src, false).translate();
    tgtFragment = new FragmentConstructor(tgt, true).translate();
    if (srcFragment == null || tgtFragment == null) return result;

    if (mkConstraints()) result.setRule(Substitution.mk(srcFragment, tgtFragment, constraints));

    return result;
  }

  private boolean mkConstraints() {
    if (!mkSymbolEq()) return fail("Fail to generate eq-type constraints");
    if (!mkAttrsSub()) return fail("Fail to generate AttrsSub constraints");
    if (!mkIntegrityConstraints()) return fail("Fail to generate integrity constraints");
    if (!mkInstantiation()) return fail("Fail to generate instantiation constraints");

    return true;
  }

  /**
   * SymbolEq-type constraints *
   */
  private boolean mkSymbolEq() {
    // Only targets at source side symbols
    mkTableEq();
    mkAttrsEq();
    mkPredEq();
    mkSchemaEq();
    mkFuncEq();

    return true;
  }

  private void mkTableEq() {
    final List<Symbol> tableSyms = srcFragment.symbols().symbolsOf(TABLE);
    final int bound = tableSyms.size();
    for (int i = 0; i < bound; ++i) {
      for (int j = i + 1; j < bound; ++j) {
        if (tableCanEq(tableSyms.get(i), tableSyms.get(j)))
          constraints.add(Constraint.mk(Constraint.Kind.TableEq, tableSyms.get(i), tableSyms.get(j)));
      }
    }
  }

  private void mkAttrsEq() {
    final List<Symbol> attrsSyms = srcFragment.symbols().symbolsOf(ATTRS);
    final int bound = attrsSyms.size();
    for (int i = 0; i < bound; ++i) {
      for (int j = i + 1; j < bound; ++j) {
        if (attrsCanEq(attrsSyms.get(i), attrsSyms.get(j)))
          constraints.add(Constraint.mk(Constraint.Kind.AttrsEq, attrsSyms.get(i), attrsSyms.get(j)));
      }
    }
  }

  private void mkPredEq() {
    final List<Symbol> predSyms = srcFragment.symbols().symbolsOf(PRED);
    final int bound = predSyms.size();
    for (int i = 0; i < bound; ++i) {
      for (int j = i + 1; j < bound; ++j) {
        if (predCanEq(predSyms.get(i), predSyms.get(j)))
          constraints.add(Constraint.mk(Constraint.Kind.PredicateEq, predSyms.get(i), predSyms.get(j)));
      }
    }
  }

  private void mkSchemaEq() {
    final List<Symbol> schemaSyms = srcFragment.symbols().symbolsOf(SCHEMA);
    final int bound = schemaSyms.size();
    for (int i = 0; i < bound; ++i) {
      for (int j = i + 1; j < bound; ++j) {
        if (schemaCanEq(schemaSyms.get(i), schemaSyms.get(j)))
          constraints.add(Constraint.mk(Constraint.Kind.SchemaEq, schemaSyms.get(i), schemaSyms.get(j)));
      }
    }
  }

  private void mkFuncEq() {
    final List<Symbol> funcSyms = srcFragment.symbols().symbolsOf(FUNC);
    final int bound = funcSyms.size();
    for (int i = 0; i < bound; ++i) {
      for (int j = i + 1; j < bound; ++j) {
        if (funcCanEq(funcSyms.get(i), funcSyms.get(j)))
          constraints.add(Constraint.mk(Constraint.Kind.FuncEq, funcSyms.get(i), funcSyms.get(j)));
      }
    }
  }

  /**
   * AttrsSub constraints *
   */
  private boolean mkAttrsSub() {
    analyzeSource(srcFragment.root(), viableSources);
    analyzeSource(tgtFragment.root(), viableSources);
    for (Symbol attrs : srcFragment.symbols().symbolsOf(ATTRS)) {
      boolean hasSource = false;
      for (Symbol source : viableSources.get(attrs)) {
        final List<Value> subAttrs = ofAttrs(attrs, false);
        final List<Value> srcAttrs = ofOutAttrs(source, false);
        if (subAttrs == null || srcAttrs == null) return false;

        if (srcAttrs.containsAll(subAttrs)) {
          constraints.add(Constraint.mk(Constraint.Kind.AttrsSub, attrs, source));
          hasSource = true;
          break;
        }
      }
      if (hasSource) continue;
      // Compromise for AttrsSub constraint:
      // If an attrs symbol a's assignment has some values from one of a's source
      // e.g. [R.col1, S.col2] from R join S, then just try to make AttrsSub(a, R)!
      for (Symbol source : viableSources.get(attrs)) {
        final List<Value> subAttrs = ofAttrs(attrs, false);
        final List<Value> srcAttrs = ofOutAttrs(source, false);
        if (subAttrs == null || srcAttrs == null) return false;

        if (any(subAttrs, srcAttrs::contains)) {
          constraints.add(Constraint.mk(Constraint.Kind.AttrsSub, attrs, source));
          hasSource = true;
          break;
        }
      }
      if (!hasSource) return false;
    }

    return true;
  }

  private static List<Symbol> analyzeSource(Op op, ListMultimap<Symbol, Symbol> viableSources) {
    final OpKind kind = op.kind();
    final Op[] predecessor = op.predecessors();
    final List<Symbol> lhs = kind.numPredecessors() > 0 ? analyzeSource(predecessor[0], viableSources) : null;
    final List<Symbol> rhs = kind.numPredecessors() > 1 ? analyzeSource(predecessor[1], viableSources) : null;

    switch (kind) {
      case INPUT:
        return Collections.singletonList(((Input) op).table());
      case INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN:
        final Join join = (Join) op;
        viableSources.putAll(join.lhsAttrs(), lhs);
        viableSources.putAll(join.rhsAttrs(), rhs);
        return ListSupport.join(lhs, rhs);
      case CROSS_JOIN:
        return ListSupport.join(lhs, rhs);
      case SIMPLE_FILTER, IN_SUB_FILTER:
        viableSources.putAll(((AttrsFilter) op).attrs(), lhs);
      case EXISTS_FILTER:
        return lhs;
      case PROJ:
        final Proj proj = (Proj) op;
        viableSources.putAll(proj.attrs(), lhs);
        return Collections.singletonList(proj.schema());
      case UNION, INTERSECT, EXCEPT:
        return ListSupport.join(lhs, rhs);
      case AGG:
        final Agg agg = (Agg) op;
        viableSources.putAll(agg.groupByAttrs(), lhs);
        viableSources.putAll(agg.aggregateAttrs(), lhs);
        viableSources.putAll(agg.aggregateOutputAttrs(), Collections.singletonList(agg.schema()));
        return Collections.singletonList(agg.schema());
      default:
        throw new IllegalArgumentException("unsupported op kind " + kind);
    }
  }

  /**
   * Integrity constraints *
   */
  private boolean mkIntegrityConstraints() {
    mkUnique();
    mkNotNull();
    mkReference();

    return true;
  }

  private void mkUnique() {
    final List<Constraint> attrsSubConstraints =
        filter(constraints,
            c -> c.kind() == Constraint.Kind.AttrsSub && c.symbols()[1].kind() == TABLE);
    for (Constraint constraint : attrsSubConstraints) {
      // Must be an attrs `a` with a table source `r`: AttrsSub(a, r)
      final Symbol attrsSym = constraint.symbols()[0];
      final Symbol tableSym = constraint.symbols()[1];

      final List<Value> attrs = ofAttrs(attrsSym, false);
      final Integer input = ofTable(tableSym, false);
      if (attrs.isEmpty()) continue;

      final List<Column> columns = tryResolveColumns(src, attrs);
      if (columns == null || columns.isEmpty()) continue; // some attrs has no backed column

      if (none(columns, column -> isParticipateIn(src, column, ConstraintKind.UNIQUE))) continue;

      if (PlanSupport.isUniqueCoreAt(src, new HashSet<>(attrs), input)) {
        constraints.add(Constraint.mk(Constraint.Kind.Unique, tableSym, attrsSym));
      }
    }
  }

  private void mkNotNull() {
    final List<Constraint> attrsSubConstraints =
        filter(constraints,
            c -> c.kind() == Constraint.Kind.AttrsSub && c.symbols()[1].kind() == TABLE);
    for (Constraint constraint : attrsSubConstraints) {
      // Must be an attrs `a` with a table source `r`: AttrsSub(a, r)`
      final Symbol attrsSym = constraint.symbols()[0];
      final Symbol tableSym = constraint.symbols()[1];

      final List<Value> attrs = ofAttrs(attrsSym, false);
      final Integer input = ofTable(tableSym, false);
      if (attrs.isEmpty()) continue;

      final List<Column> columns = tryResolveColumns(src, attrs);
      if (columns == null || columns.isEmpty()) continue; // some attrs has no backed column

      if (none(columns, column -> isParticipateIn(src, column, ConstraintKind.NOT_NULL))) continue;

      if (any(attrs, attr -> PlanSupport.isNotNullAt(src, attr, input))) {
        constraints.add(Constraint.mk(Constraint.Kind.NotNull, tableSym, attrsSym));
      }
    }
  }

  private void mkReference() {
    final List<Constraint> attrsSubConstraints =
        filter(constraints,
            c -> c.kind() == Constraint.Kind.AttrsSub && c.symbols()[1].kind() == TABLE);
    // Must be an attrs `a` with a table source `r`: AttrsSub(a, r)
    for (Constraint constraint0 : attrsSubConstraints) {
      for (Constraint constraint1 : attrsSubConstraints) {
        if (constraint0.symbols()[0] == constraint1.symbols()[0]) continue;

        final Symbol referringTableSym = constraint0.symbols()[1];
        final Symbol referringAttrsSym = constraint0.symbols()[0];
        final Symbol referredTableSym = constraint1.symbols()[1];
        final Symbol referredAttrsSym = constraint1.symbols()[0];

        final List<Value> referringAttrs = ofAttrs(referringAttrsSym, false);
        final List<Column> referringColumns = tryResolveColumns(src, referringAttrs);
        if (referringAttrs.isEmpty() || referringColumns == null || referringColumns.isEmpty()) continue;

        final List<Value> referredAttrs = ofAttrs(referredAttrsSym, false);
        final List<Column> referredCols = tryResolveColumns(src, referredAttrs);
        if (referredAttrs.isEmpty() || referredCols == null || referredCols.isEmpty()) continue;

        final var fks = findIC(src.schema(), referringColumns, FOREIGN);
        if (!referringColumns.equals(referredCols)
            && linearFind(fks, it -> it.refColumns().equals(referredCols)) == null) continue;

        final Integer surface = ofTable(referredTableSym, false);

        // Check if the referred attributes are filtered, which invalidates the FK on schema.
        Value rootRef = referredAttrs.get(0);
        if (!isRootRef(src, rootRef)) rootRef = deRef(src, rootRef);
        assert rootRef != null;

        int path = src.valuesReg().initiatorOf(rootRef);
        boolean filtered = false;
        while (path != surface) {
          final int parent = src.parentOf(path);
          if (src.kindOf(parent).isFilter()) {
            filtered = true;
            break;
          }
          path = parent;
        }
        if (filtered) continue;

        constraints.add(Constraint.mk(Constraint.Kind.Reference,
            referringTableSym, referringAttrsSym, referredTableSym, referredAttrsSym));
      }
    }
  }

  private boolean isParticipateIn(PlanContext plan, Column column, ConstraintKind integrityConstraint) {
    return !Iterables.isEmpty(findRelatedIC(plan.schema(), column, integrityConstraint));
  }

  /**
   * Instantiation-type constraints *
   */
  private boolean mkInstantiation() {
    if (!mkTableInstantiation()) return false;
    if (!mkSchemaInstantiation()) return false;
    if (!mkAttrsInstantiation()) return false; // Put it after SchemaInstantiation for checking validity
    if (!mkPredInstantiation()) return false;
    if (!mkFuncInstantiation()) return false;
    //
    // mkTableInstantiation();
    // mkSchemaInstantiation();
    // mkAttrsInstantiation();
    // mkPredInstantiation();
    // mkFuncInstantiation();

    return true;
  }

  private boolean mkTableInstantiation() {
    final List<Symbol> tgtTableSyms = tgtFragment.symbols().symbolsOf(TABLE);
    final List<Symbol> srcTableSyms = srcFragment.symbols().symbolsOf(TABLE);

    // A src table should only be instantiated by exactly one tgt table.
    final Set<Symbol> instantiatedSrcTables = new HashSet<>();
    for (Symbol tgtSym : tgtTableSyms) {
      boolean hasInstantiation = false;
      for (Symbol srcSym : srcTableSyms) {
        if (instantiatedSrcTables.contains(srcSym)) continue;
        if (tableCanEq(tgtSym, srcSym)) {
          constraints.add(Constraint.mk(Constraint.Kind.TableEq, tgtSym, srcSym));
          instantiatedSrcTables.add(srcSym);
          hasInstantiation = true;
          break;
        }
      }
      if (!hasInstantiation) return false;
    }
    return true;
  }

  private boolean mkSchemaInstantiation() {
    final List<Symbol> tgtSchemaSyms = tgtFragment.symbols().symbolsOf(SCHEMA);
    final List<Symbol> srcSchemaSyms = srcFragment.symbols().symbolsOf(SCHEMA);

    final Set<Symbol> instantiatedSrcSchemas = new HashSet<>();
    final List<Symbol> tgtFailures = new ArrayList<>();
    for (Symbol tgtSym : tgtSchemaSyms) {
      // Search for preferred schema instantiation
      final Symbol preferredInstantiation = preferredSchemaInstantiation(tgtSym);
      if (preferredInstantiation != null && !instantiatedSrcSchemas.contains(preferredInstantiation)) {
        constraints.add(Constraint.mk(Constraint.Kind.SchemaEq, tgtSym, preferredInstantiation));
        instantiatedSrcSchemas.add(preferredInstantiation);
        continue;
      }
      // Common case
      boolean hasInstantiation = false;
      for (Symbol srcSym : srcSchemaSyms) {
        if (instantiatedSrcSchemas.contains(srcSym)) continue;
        if (schemaCanEq(tgtSym, srcSym)) {
          constraints.add(Constraint.mk(Constraint.Kind.SchemaEq, tgtSym, srcSym));
          instantiatedSrcSchemas.add(srcSym);
          hasInstantiation = true;
          break;
        }
      }
      if (hasInstantiation) continue;
      // Compromise special case for Union: Union(Proj_l, Proj_r).
      // If no instantiation found, instantiate Proj_r's schema's with the same instantiation of Proj_l
      final Op owner = tgtSym.ctx().ownerOf(tgtSym), parent = owner.successor();
      if (parent != null && parent.kind() == OpKind.UNION) {
        final Op another = owner == parent.predecessors()[0] ? parent.predecessors()[1] : parent.predecessors()[0];
        if (owner.kind() == another.kind()) {
          final Symbol anotherSchema =
              another.kind() == OpKind.PROJ ? ((Proj) another).schema() : ((Agg) another).schema();
          final Symbol currAnotherInstantiation = currentInstantiationOf(anotherSchema);
          if (currAnotherInstantiation != null) {
            constraints.add(Constraint.mk(Constraint.Kind.SchemaEq, tgtSym, currAnotherInstantiation));
            continue;
          }
        }
      }
      tgtFailures.add(tgtSym);
    }
    // Compromise for tgt schema failing to get src instantiation.
    // If values of tgt schema is contained by src schema, or the opposite, just make it eq!
    for (Symbol tgtSym : tgtFailures) {
      boolean hasInstantiation = false;
      for (Symbol srcSym : srcSchemaSyms) {
        if (instantiatedSrcSchemas.contains(srcSym)) continue;
        if (schemaCanEqCompromise(tgtSym, srcSym)) {
          constraints.add(Constraint.mk(Constraint.Kind.SchemaEq, tgtSym, srcSym));
          instantiatedSrcSchemas.add(srcSym);
          hasInstantiation = true;
          break;
        }
      }
      // This tgt schema has no choice to get instantiation anymore
      if (!hasInstantiation) return false;
    }
    return true;
  }

  private boolean mkAttrsInstantiation() {
    final List<Symbol> tgtAttrsSyms = tgtFragment.symbols().symbolsOf(ATTRS);
    final List<Symbol> srcAttrsSyms = srcFragment.symbols().symbolsOf(ATTRS);

    final List<Symbol> tgtFailures = new ArrayList<>();
    for (Symbol tgtSym : tgtAttrsSyms) {
      boolean hasInstantiation = false;
      for (Symbol srcSym : srcAttrsSyms) {
        if (!validateAttrsInstantiation(srcSym, tgtSym)) continue;
        if (attrsCanEq(tgtSym, srcSym)) {
          constraints.add(Constraint.mk(Constraint.Kind.AttrsEq, tgtSym, srcSym));
          hasInstantiation = true;
          break;
        }
      }
      if (hasInstantiation || result.getSpecialProjAttrs(tgtSym) != null) continue;
      // Special case for Union: Union(Proj_l, Proj_r).
      // If no attrs instantiation found, instantiate Proj_r's attrs with the same instantiation of Proj_l's attrs
      final Op owner = tgtSym.ctx().ownerOf(tgtSym), parent = owner.successor();
      if (parent != null && parent.kind() == OpKind.UNION) {
        final Op another = owner == parent.predecessors()[0] ? parent.predecessors()[1] : parent.predecessors()[0];
        if (owner.kind() == another.kind() && (owner.kind() == OpKind.PROJ || owner.kind() == OpKind.AGG)) {
          final Symbol anotherAttrs;
          if (another.kind() == OpKind.PROJ) anotherAttrs = ((Proj) another).attrs();
          else {
            if (tgtSym == ((Agg) owner).groupByAttrs()) anotherAttrs = ((Agg) another).groupByAttrs();
            else if (tgtSym == ((Agg) owner).aggregateAttrs()) anotherAttrs = ((Agg) another).aggregateAttrs();
            else anotherAttrs = ((Agg) another).aggregateOutputAttrs();
          }
          if (attrsCanEq(tgtSym, anotherAttrs) && currentInstantiationOf(anotherAttrs) != null) {
            constraints.add(Constraint.mk(Constraint.Kind.AttrsEq, tgtSym, currentInstantiationOf(anotherAttrs)));
            continue;
          }
        }
      }
      tgtFailures.add(tgtSym);
    }
    // Compromise for tgt attrs failing to get src instantiation.
    // If values of tgt attrs is contained by src attrs, or the opposite, just make it eq!
    for (Symbol tgtSym : tgtFailures) {
      boolean hasInstantiation = false;
      for (Symbol srcSym : srcAttrsSyms) {
        if (!validateAttrsInstantiation(srcSym, tgtSym)) continue;
        if (attrsCompromiseEqContain(tgtSym, srcSym)) {
          constraints.add(Constraint.mk(Constraint.Kind.AttrsEq, tgtSym, srcSym));
          hasInstantiation = true;
          break;
        }
      }
      // This tgt attrs has no choice to get instantiation anymore
      if (!hasInstantiation) return false;
    }
    return true;
  }

  private boolean mkPredInstantiation() {
    final List<Symbol> tgtPredSyms = tgtFragment.symbols().symbolsOf(PRED);
    final List<Symbol> srcPredSyms = srcFragment.symbols().symbolsOf(PRED);

    for (Symbol tgtSym : tgtPredSyms) {
      boolean hasInstantiation = false;
      for (Symbol srcSym : srcPredSyms) {
        if (predCanEq(tgtSym, srcSym)) {
          constraints.add(Constraint.mk(Constraint.Kind.PredicateEq, tgtSym, srcSym));
          hasInstantiation = true;
          break;
        }
      }
      // Maybe a different arithmetic predicate
      if (!hasInstantiation && !isSimpleIntArithmeticExpr(ofPred(tgtSym, true))) return false;
    }
    return true;
  }

  private boolean mkFuncInstantiation() {
    final List<Symbol> tgtFuncSyms = tgtFragment.symbols().symbolsOf(FUNC);
    final List<Symbol> srcFuncSyms = srcFragment.symbols().symbolsOf(FUNC);

    for (Symbol tgtSym : tgtFuncSyms) {
      boolean hasInstantiation = false;
      for (Symbol srcSym : srcFuncSyms) {
        if (funcCanEq(tgtSym, srcSym)) {
          constraints.add(Constraint.mk(Constraint.Kind.FuncEq, tgtSym, srcSym));
          hasInstantiation = true;
          break;
        }
      }
      if (!hasInstantiation) return false;
    }
    return true;
  }

  /**
   * Judge whether symbols can be EQ (EQs in source side or instantiations) *
   */
  private boolean tableCanEq(Symbol sym0, Symbol sym1) {
    assert sym0.kind() == TABLE && sym1.kind() == TABLE;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final PlanContext plan0 = targetSide0 ? tgt : src;
    final PlanContext plan1 = targetSide1 ? tgt : src;

    final Integer nodeId0 = ofTable(sym0, targetSide0);
    final Integer nodeId1 = ofTable(sym1, targetSide1);
    final Table table0 = ((InputNode) plan0.nodeAt(nodeId0)).table();
    final Table table1 = ((InputNode) plan1.nodeAt(nodeId1)).table();
    return Objects.equals(table0, table1);
  }

  private boolean attrsCanEq(Symbol sym0, Symbol sym1) {
    assert sym0.kind() == ATTRS && sym1.kind() == ATTRS;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final PlanContext plan0 = targetSide0 ? tgt : src;
    final PlanContext plan1 = targetSide1 ? tgt : src;

    final List<Value> v0 = ofAttrs(sym0, targetSide0);
    final List<Value> v1 = ofAttrs(sym1, targetSide1);
    return isAttrsEq(plan0, v0, plan1, v1);
  }

  private boolean attrsCompromiseEqContain(Symbol sym0, Symbol sym1) {
    // Compromise condition: values of one are contained by values of the other
    assert sym0.kind() == ATTRS && sym1.kind() == ATTRS;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final PlanContext plan0 = targetSide0 ? tgt : src;
    final PlanContext plan1 = targetSide1 ? tgt : src;

    final List<Value> v0 = ofAttrs(sym0, targetSide0);
    final List<Value> v1 = ofAttrs(sym1, targetSide1);
    return isAttrsContained(plan0, v0, plan1, v1) || isAttrsContained(plan1, v1, plan0, v0);
  }

  private boolean attrsCompromiseEqIntersect(Symbol sym0, Symbol sym1) {
    // Compromise condition: values of one are contained by values of the other
    assert sym0.kind() == ATTRS && sym1.kind() == ATTRS;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final PlanContext plan0 = targetSide0 ? tgt : src;
    final PlanContext plan1 = targetSide1 ? tgt : src;

    final List<Value> v0 = ofAttrs(sym0, targetSide0);
    final List<Value> v1 = ofAttrs(sym1, targetSide1);
    return isAttrsIntersect(plan0, v0, plan1, v1);
  }

  private boolean schemaCanEq(Symbol sym0, Symbol sym1) {
    assert sym0.kind() == SCHEMA && sym1.kind() == SCHEMA;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final PlanContext plan0 = targetSide0 ? tgt : src;
    final PlanContext plan1 = targetSide1 ? tgt : src;

    final List<Value> v0 = ofSchema(sym0, targetSide0);
    final List<Value> v1 = ofSchema(sym1, targetSide1);
    return isAttrsEq(plan0, v0, plan1, v1);
  }

  private boolean schemaCanEqCompromise(Symbol sym0, Symbol sym1) {
    assert sym0.kind() == SCHEMA && sym1.kind() == SCHEMA;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final PlanContext plan0 = targetSide0 ? tgt : src;
    final PlanContext plan1 = targetSide1 ? tgt : src;

    final List<Value> v0 = ofSchema(sym0, targetSide0);
    final List<Value> v1 = ofSchema(sym1, targetSide1);
    return isAttrsContained(plan0, v0, plan1, v1) || isAttrsContained(plan1, v1, plan0, v0);
  }

  private boolean predCanEq(Symbol sym0, Symbol sym1) {
    assert sym0.kind() == PRED && sym1.kind() == PRED;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final Expression pred0 = ofPred(sym0, targetSide0);
    final Expression pred1 = ofPred(sym1, targetSide1);
    return Objects.equals(pred0.toString(), pred1.toString());
  }

  private boolean funcCanEq(Symbol sym0, Symbol sym1) {
    assert sym0.kind() == FUNC && sym1.kind() == FUNC;
    final boolean targetSide0 = sym0.ctx() == tgtFragment.symbols();
    final boolean targetSide1 = sym1.ctx() == tgtFragment.symbols();
    final List<Expression> v0 = ofFunc(sym0, targetSide0);
    final List<Expression> v1 = ofFunc(sym1, targetSide1);

    return isFuncEq(v0, v1);
  }

  private static boolean isAttrsEq(PlanContext plan0, List<Value> attrs0, PlanContext plan1, List<Value> attrs1) {
    if (attrs0.size() != attrs1.size()) return false;
    // `plan0` may be same or different with `plan1`
    for (int i = 0, bound = attrs0.size(); i < bound; i++) {
      if (!valueEq(plan0, attrs0.get(i), plan1, attrs1.get(i))) return false;
    }
    return true;
  }

  private static boolean isAttrsContained(PlanContext plan0, List<Value> attrs0, PlanContext plan1, List<Value> attrs1) {
    // check: attrs0 \in attrs1
    if (attrs0.size() > attrs1.size()) return false;
    return all(attrs0, attr0 -> any(attrs1, attr1 -> valueEq(plan0, attr0, plan1, attr1)));
  }

  private static boolean isAttrsIntersect(PlanContext plan0, List<Value> attrs0, PlanContext plan1, List<Value> attrs1) {
    // check: attrs0 \intersect attrs1 != emptySet
    return any(attrs0, attr0 -> any(attrs1, attr1 -> valueEq(plan0, attr0, plan1, attr1)));
  }

  private static boolean valueEq(PlanContext plan0, Value value0, PlanContext plan1, Value value1) {
    final Value rootRef0 = traceRef(plan0, value0);
    final Value rootRef1 = traceRef(plan1, value1);
    final Column column0 = tryResolveColumn(plan0, rootRef0);
    final Column column1 = tryResolveColumn(plan1, rootRef1);
    if (column0 == null ^ column1 == null) return false;
    if (column0 != null) return column0.equals(column1);

    final ValuesRegistry valuesReg0 = plan0.valuesReg();
    final ValuesRegistry valuesReg1 = plan1.valuesReg();
    final Expression expr0 = valuesReg0.exprOf(rootRef0);
    final Expression expr1 = valuesReg1.exprOf(rootRef1);
    if (expr0 == null ^ expr1 == null) return false;
    assert expr0 != null;

    return Objects.equals(expr0.template().toString(), expr1.template().toString())
        && isAttrsEq(plan0, valuesReg0.valueRefsOf(expr0), plan1, valuesReg1.valueRefsOf(expr1));
  }

  private static boolean isFuncEq(List<Expression> v0, List<Expression> v1) {
    if (v0.size() != v1.size()) return false;
    for (var pair : zip(v0, v1)) {
      if (!pair.getLeft().toString().equals(pair.getRight().toString())) return false;
    }
    return true;
  }

  /**
   * Functions to guide valid instantiation generation *
   */
  private Symbol preferredSchemaInstantiation(Symbol to) {
    // Inspired by calcite rule #82, it is used to guide the generation of valid instantiations
    assert to.kind() == SCHEMA;
    final Op toOwner = to.ctx().ownerOf(to);
    for (Symbol from : srcFragment.symbols().symbolsOf(SCHEMA)) {
      final Op fromOwner = from.ctx().ownerOf(from);
      // For root node of 2 side, make schemas of them eq
      if (toOwner == tgtFragment.root() && fromOwner == srcFragment.root() &&
          tgt.planRoot().kind() == src.planRoot().kind())
        return from;
    }
    for (Symbol from : srcFragment.symbols().symbolsOf(SCHEMA)) {
      final Op fromOwner = from.ctx().ownerOf(from);
      // Precondition 1. Should be eq schemas; 2. Should both from Proj or both from Agg
      if (!schemaCanEq(from, to)) continue;
      if (toOwner.kind() != fromOwner.kind()) continue;
      if (toOwner.kind() == OpKind.PROJ) {
        // Precondition 3. corresponding `attrs` on Proj should be eq
        final Symbol toOwnerAttrs = ((Proj) toOwner).attrs(), fromOwnerAttrs = ((Proj) fromOwner).attrs();
        if (!attrsCanEq(fromOwnerAttrs, toOwnerAttrs)) continue;

        final Symbol toSource = directTableSource(to), fromSource = directTableSource(from);
        if (toSource != null && fromSource != null && currentInstantiationOf(toSource) == fromSource)
          return from;
      }
    }
    return null;
  }

  private Symbol directTableSource(Symbol sym) {
    // Search for patterns containing only Proj ops:
    // Proj(sym, ) -> Proj(, ..) -> Input(r), then directTableSource(sym) = r
    assert sym.kind() == ATTRS || sym.kind() == SCHEMA;
    final Op owner = sym.ctx().ownerOf(sym);
    if (sym.kind() == SCHEMA) {
      if (owner.kind() != OpKind.PROJ) return null;
      sym = ((Proj) owner).attrs();
    }

    while (true) {
      final List<Symbol> sources = viableSources.get(sym);
      if (sources.size() > 1) return null;

      final Symbol source = sources.get(0);
      if (source.kind() == TABLE) return source;

      assert source.kind() == SCHEMA;
      final Op sourceOwner = source.ctx().ownerOf(source);
      if (sourceOwner.kind() != OpKind.PROJ) return null;

      sym = ((Proj) sourceOwner).attrs();
    }
  }

  private boolean validateAttrsInstantiation(Symbol from, Symbol to) {
    final List<Symbol> sourceChain = new ArrayList<>(4);
    collectSourceChain(from, sourceChain);

    for (Symbol sourceOfTo : viableSources.get(to)) {
      final Symbol sourceInstantiation = currentInstantiationOf(sourceOfTo);
      assert sourceInstantiation != null;
      if (sourceChain.contains(sourceInstantiation)) return true;
    }

    return false;
  }

  private void collectSourceChain(Symbol sym, List<Symbol> sourceChain) {
    assert sym.kind() == Symbol.Kind.ATTRS || sym.kind() == Symbol.Kind.SCHEMA || sym.kind() == Symbol.Kind.TABLE;
    Symbol.Kind kind = sym.kind();

    if (kind == Symbol.Kind.ATTRS) {
      final Symbol source = currentSourceOf(sym);
      assert source != null;
      sourceChain.add(source);
      collectSourceChain(source, sourceChain);
      return;
    }

    if (kind == Symbol.Kind.SCHEMA) {
      final Op owner = sym.ctx().ownerOf(sym);
      assert owner.kind() == OpKind.PROJ || owner.kind() == OpKind.AGG;
      if (owner.kind() == OpKind.PROJ) {
        final Proj proj = ((Proj) owner);
        collectSourceChain(proj.attrs(), sourceChain);
      } else {
        final Agg agg = ((Agg) owner);
        collectSourceChain(agg.groupByAttrs(), sourceChain);
        collectSourceChain(agg.aggregateAttrs(), sourceChain);
      }
    }
  }

  private Symbol currentSourceOf(Symbol sym) {
    assert sym.kind() == Symbol.Kind.ATTRS;
    final List<Constraint> sourceConstraints = filter(constraints, c -> c.kind() == Constraint.Kind.AttrsSub);
    for (Constraint c : sourceConstraints) {
      if (c.symbols()[0] == sym) return c.symbols()[1];
    }
    return null;
  }

  private Symbol currentInstantiationOf(Symbol sym) {
    assert sym.ctx() == tgtFragment.symbols();
    for (Constraint c : constraints) {
      if (c.symbols()[0] == sym) return c.symbols()[1];
    }
    return null;
  }

  /**
   * Get assignments of symbols *
   */
  Integer ofTable(Symbol tableSym, boolean isTargetSide) {
    return of(tableSym, isTargetSide);
  }

  List<Value> ofSchema(Symbol schemaSym, boolean isTargetSide) {
    return of(schemaSym, isTargetSide);
  }

  List<Value> ofAttrs(Symbol attrsSym, boolean isTargetSide) {
    return of(attrsSym, isTargetSide);
  }

  Expression ofPred(Symbol predSym, boolean isTargetSide) {
    return of(predSym, isTargetSide);
  }

  List<Expression> ofFunc(Symbol funcSym, boolean isTargetSide) {
    return of(funcSym, isTargetSide);
  }

  private List<Value> ofOutAttrs(Symbol sym, boolean isTargetSide) {
    if (sym.kind() == TABLE) {
      final Integer nodeId = ofTable(sym, isTargetSide);
      return isTargetSide ? tgt.valuesReg().valuesOf(nodeId) : src.valuesReg().valuesOf(nodeId);
    } else {
      assert sym.kind() == SCHEMA;
      return ofSchema(sym, isTargetSide);
    }
  }

  private <T> T of(Symbol sym, boolean isTargetSide) {
    final Object o = isTargetSide ? tgtAssignments.get().get(sym) : srcAssignments.get().get(sym);
    if (o != null) return (T) o;
    else return null;
  }

  private class FragmentConstructor {

    private final PlanContext plan;
    private Fragment fragment;
    private final boolean isTargetSide;


    private FragmentConstructor(PlanContext plan, boolean isTargetSide) {
      this.plan = plan;
      this.isTargetSide = isTargetSide;
    }

    private Fragment translate() {
      final Op root = buildTree(plan.root());
      if (root == null) return null;

      fragment = Fragment.mk(root, true);
      if (tr(plan.root(), fragment.root())) return fragment;
      return null;
    }

    private Op buildTree(int nodeId) {
      Op child0, child1, op;
      if (plan.kindOf(nodeId) == PlanKind.Agg) {
        child0 = buildTree(plan.childOf(plan.childOf(nodeId, 0), 0));
        child1 = null;
      } else {
        child0 = plan.kindOf(nodeId).numChildren() > 0 ? buildTree(plan.childOf(nodeId, 0)) : null;
        child1 = plan.kindOf(nodeId).numChildren() > 1 ? buildTree(plan.childOf(nodeId, 1)) : null;
      }
      if (plan.kindOf(nodeId).numChildren() > 0 && child0 == null) return null;
      if (plan.kindOf(nodeId).numChildren() > 1 && child1 == null) return null;

      final PlanNode node = plan.nodeAt(nodeId);
      switch (plan.kindOf(nodeId)) {
        case Input -> op = Op.mk(OpKind.INPUT);
        case Join -> {
          final OpKind joinKind = joinKindOf((JoinNode) node);
          if (joinKind == null) {
            fail("Unsupported JOIN type: " + ((JoinNode) node).joinKind());
            return null;
          }
          op = Op.mk(joinKind);
        }
        case Filter -> op = Op.mk(OpKind.SIMPLE_FILTER);
        case InSub -> op = Op.mk(OpKind.IN_SUB_FILTER);
        case Exists -> op = Op.mk(OpKind.EXISTS_FILTER);
        case Proj -> {
          op = Op.mk(OpKind.PROJ);
          if (((ProjNode) node).deduplicated()) ((Proj) op).setDeduplicated(true);
        }
        case SetOp -> {
          final OpKind setOpKind = setOpKindOf((SetOpNode) node);
          if (setOpKind == null) {
            fail("Unsupported SET-OP type: " + ((SetOpNode) node).opKind());
            return null;
          }
          op = Op.mk(setOpKind);
          if (((SetOpNode) node).deduplicated()) ((SetOp) op).setDeduplicated(true);
        }
        case Agg -> {
          op = Op.mk(OpKind.AGG);
          if (((AggNode) node).deduplicated()) ((Agg) op).setDeduplicated(true);
        }
        default -> {
          fail("Unsupported operator type: " + plan.kindOf(nodeId));
          return null;
        }
      }
      // registerOp(nodeId, op);

      if (op.kind().numPredecessors() > 0) op.setPredecessor(0, child0);
      if (op.kind().numPredecessors() > 1) op.setPredecessor(1, child1);
      return op;
    }

    // private void registerOp(int nodeId, Op op) {
    //   if (isTargetSide) tgtOps.get().put(nodeId, op);
    //   else srcOps.get().put(nodeId, op);
    // }
    //
    // private Op getRegisteredOp(int nodeId) {
    //   if (isTargetSide) return tgtOps.get().get(nodeId);
    //   else return srcOps.get().get(nodeId);
    // }

    private boolean tr(int nodeId, Op op) {
      return switch (plan.kindOf(nodeId)) {
        case Input -> trInput(nodeId, op);
        case Join -> trJoin(nodeId, op);
        case Filter -> trFilter(nodeId, op);
        case InSub -> trInSub(nodeId, op);
        case Exists -> trExists(nodeId, op);
        case Proj -> trProj(nodeId, op);
        case SetOp -> trSetOp(nodeId, op);
        case Agg -> trAgg(nodeId, op);
        default -> fail("Unsupported operator type: " + plan.kindOf(nodeId));
      };
    }

    private boolean trInput(int nodeId, Op op) {
      assert op.kind() == OpKind.INPUT;

      final Input inputOp = (Input) op;
      assign(inputOp.table(), nodeId);
      return true;
    }

    private boolean trJoin(int nodeId, Op op) {
      assert op.kind().isJoin();
      if (!tr(plan.childOf(nodeId, 0), op.predecessors()[0]) ||
          !tr(plan.childOf(nodeId, 1), op.predecessors()[1]))
        return false;

      if (op.kind() == OpKind.CROSS_JOIN) return true;

      final InfoCache infoCache = plan.infoCache();
      if (!infoCache.isEquiJoin(nodeId)) return fail("Join is not an equi-join");

      final var keys = infoCache.getJoinKeyOf(nodeId);
      final Join joinOp = (Join) op;
      assign(joinOp.lhsAttrs(), keys.getLeft());
      assign(joinOp.rhsAttrs(), keys.getRight());

      return true;
    }

    private boolean trFilter(int nodeId, Op op) {
      assert op.kind() == OpKind.SIMPLE_FILTER;
      if (!tr(plan.childOf(nodeId, 0), op.predecessors()[0])) return false;

      final Expression predicate;
      if (plan.kindOf(nodeId).isSubqueryFilter()) predicate = plan.infoCache().getSubqueryExprOf(nodeId);
      else predicate = ((SimpleFilterNode) plan.nodeAt(nodeId)).predicate();
      final Values attrs = plan.valuesReg().valueRefsOf(predicate);
      // There may be multiple same ColRefs in a predicate expression,
      // e.g. t.a > 1 or t.a < 3, Values are [`t.a`, `t.a`], but it is represented only as `p(t.a)`
      final Values distinctAttrs = Values.mk(attrs.stream().distinct().collect(Collectors.toList()));

      final SimpleFilter filter = (SimpleFilter) op;
      result.setConcretePred(filter.predicate(), predicate);
      assign(filter.predicate(), predicate);
      assign(filter.attrs(), distinctAttrs);
      return true;
    }

    private boolean trInSub(int nodeId, Op op) {
      assert op.kind() == OpKind.IN_SUB_FILTER;
      if (!tr(plan.childOf(nodeId, 0), op.predecessors()[0]) ||
          !tr(plan.childOf(nodeId, 1), op.predecessors()[1]))
        return false;

      final InSubNode inSubNode = (InSubNode) plan.nodeAt(nodeId);
      if (!inSubNode.isPlain()) return fail("InSub is not plain");
      final Expression expr = inSubNode.expr();
      final Values attrs = plan.valuesReg().valueRefsOf(expr);

      final InSubFilter inSub = (InSubFilter) op;
      assign(inSub.attrs(), attrs);

      return true;
    }

    private boolean trExists(int nodeId, Op op) {
      assert op.kind() == OpKind.EXISTS_FILTER;
      // TODO refine it using auxvar ?
      return tr(plan.childOf(nodeId, 0), op.predecessors()[0])
          && tr(plan.childOf(nodeId, 1), op.predecessors()[1]);
    }

    private boolean trProj(int nodeId, Op op) {
      assert op.kind() == OpKind.PROJ;
      if (!tr(plan.childOf(nodeId, 0), op.predecessors()[0])) return false;

      final ProjNode projNode = (ProjNode) plan.nodeAt(nodeId);
      final ValuesRegistry valuesReg = plan.valuesReg();
      final List<Value> outValues = valuesReg.valuesOf(nodeId);
      final List<Value> inValues = flatMap(projNode.attrExprs(), valuesReg::valueRefsOf);

      final Proj proj = (Proj) op;
      // Cope with special values in concrete plan: SELECT 1 FROM ...
      if (plan.root() == nodeId && inValues.isEmpty()) {
        final Object val = getSpecialValues(proj.attrs(), projNode.attrExprs());
        if (val != null)
          result.setSpecialProjAttrs(proj.attrs(), val);
      }

      assign(proj.attrs(), inValues);
      assign(proj.schema(), outValues);
      return true;
    }

    private Object getSpecialValues(Symbol sym, Object obj) {
      assert sym.kind() != ATTRS ||
          (obj instanceof List<?> && ((List<?>) obj).iterator().next() instanceof Expression);

      if (sym.kind() == ATTRS) {
        final List<Expression> exprList = (List<Expression>) obj;
        if (exprList.size() == 1 && ExprKind.Literal.isInstance(exprList.get(0).template())) {
          final LiteralKind kind = exprList.get(0).template().$(ExprFields.Literal_Kind);
          final Object val = exprList.get(0).template().$(ExprFields.Literal_Value);
          if (kind == LiteralKind.INTEGER) return (Integer) val;
        }
      }

      return null;
    }

    private boolean trSetOp(int nodeId, Op op) {
      assert op.kind().isSetOp();
      return tr(plan.childOf(nodeId, 0), op.predecessors()[0])
          && tr(plan.childOf(nodeId, 1), op.predecessors()[1]);
    }

    private boolean trAgg(int nodeId, Op op) {
      assert op.kind() == OpKind.AGG;
      // There is a Proj node under each Agg
      if (!tr(plan.childOf(plan.childOf(nodeId, 0), 0), op.predecessors()[0])) return false;

      final ValuesRegistry valuesReg = plan.valuesReg();
      final AggNode aggNode = (AggNode) plan.nodeAt(nodeId);

      final List<Expression> aggFuncs = new ArrayList<>(3);
      final List<Value> aggRefs = new ArrayList<>(3);
      final List<Value> aggOutRefs = new ArrayList<>(3);

      collectAggregates(plan, aggNode, aggFuncs, aggRefs, aggOutRefs);

      if (aggFuncs.isEmpty()) return fail("No aggregate functions");

      Expression havingPredExpr = aggNode.havingExpr();
      if (havingPredExpr != null && !valuesReg.valueRefsOf(havingPredExpr).equals(aggRefs))
        return fail("Having predicate object is not aggregated attrs");

      // if (any(aggNode.groupByExprs(), it -> !ExprKind.ColRef.isInstance(it.template().$(GroupItem_Expr))))
      //   return fail("Group-by items are not all ColRefs");

      final List<Value> groupRefs = flatMap(aggNode.groupByExprs(), valuesReg::valueRefsOf);

      if (!isClosedAggregates(aggNode, aggRefs, groupRefs)) return fail("Agg is not closed");
      final List<Value> schema = valuesReg.valuesOf(nodeId);

      final Agg agg = (Agg) op;
      final Expression havingPredExpr0 = havingPredExpr == null ? Expression.EXPRESSION_TRUE : havingPredExpr;
      result.setConcretePred(agg.havingPred(), havingPredExpr0);
      if (aggFuncs.size() == 1)
        agg.setAggFuncKind(getAggFuncKind(aggFuncs.get(0)));
      assign(agg.schema(), schema);
      assign(agg.aggFunc(), aggFuncs);
      assign(agg.aggregateAttrs(), aggRefs);
      assign(agg.aggregateOutputAttrs(), aggOutRefs);
      assign(agg.groupByAttrs(), groupRefs);
      assign(agg.havingPred(), havingPredExpr0);
      return true;
    }

    private void collectAggregates(
        PlanContext ctx,
        AggNode aggNode,
        List<Expression> aggFuncs,
        List<Value> aggRefs,
        List<Value> aggOutRefs) {
      final ValuesRegistry valuesReg = plan.valuesReg();
      final List<Value> schema = valuesReg.valuesOf(plan.nodeIdOf(aggNode));
      final List<Expression> attrExprs = aggNode.attrExprs();
      assert schema.size() == attrExprs.size();
      for (int i = 0, bound = attrExprs.size(); i < bound; ++i) {
        final Expression attrExpr = attrExprs.get(i);
        if (isAggregateExpression(attrExpr)) {
          aggFuncs.add(attrExpr);
          aggRefs.addAll(valuesReg.valueRefsOf(attrExpr));
          aggOutRefs.add(schema.get(i));
        }
      }
    }

    private boolean isClosedAggregates(AggNode aggNode, List<Value> aggRefs, List<Value> groupRefs) {
      final ValuesRegistry valuesReg = plan.valuesReg();
      for (Expression attrExpr : aggNode.attrExprs()) {
        if (!isAggregateExpression(attrExpr)) continue;
        final Values refs = valuesReg.valueRefsOf(attrExpr);
        if (!aggRefs.containsAll(refs) && !groupRefs.containsAll(refs)) return false;
      }
      return true;
    }

    private boolean isAggregateExpression(Expression expr) {
      if (ExprKind.Aggregate.isInstance(expr.template())) return true;
      if (ExprKind.FuncCall.isInstance(expr.template())) {
        // MySQL ast parser cannot recognize `AVG(*)` as an aggregate function
        final String name = expr.template().$(ExprFields.FuncCall_Name).$(Name2_1);
        if ("avg".equalsIgnoreCase(name) || "average".equalsIgnoreCase(name)) return true;
      }
      return false;
    }

    private AggFuncKind getAggFuncKind(Expression expr) {
      if (!isAggregateExpression((expr))) return null;
      if (ExprKind.Aggregate.isInstance(expr.template())) {
        return aggFunctionKindOf(expr.template().$(ExprFields.Aggregate_Name));
      }
      if (ExprKind.FuncCall.isInstance(expr.template())) {
        final String name = expr.template().$(ExprFields.FuncCall_Name).$(Name2_1);
        if ("avg".equalsIgnoreCase(name) || "average".equalsIgnoreCase(name))
          return aggFunctionKindOf(name);
      }
      return null;
    }

    private void assign(Symbol sym, Object assignment) {
      assert sym.kind() != TABLE || assignment instanceof Integer;
      assert sym.kind() != ATTRS || assignment instanceof List<?>;
      assert sym.kind() != PRED || assignment instanceof Expression;
      assert sym.kind() != SCHEMA || assignment instanceof List<?>;
      assert sym.kind() != FUNC || assignment instanceof List<?>;

      if (isTargetSide) tgtAssignments.get().put(sym, assignment);
      else srcAssignments.get().put(sym, assignment);
    }

    private static OpKind joinKindOf(JoinNode join) {
      final JoinKind kind = join.joinKind();
      return switch (kind) {
        case INNER_JOIN -> OpKind.INNER_JOIN;
        case CROSS_JOIN -> OpKind.CROSS_JOIN;
        case LEFT_JOIN -> OpKind.LEFT_JOIN;
        case RIGHT_JOIN -> OpKind.RIGHT_JOIN;
        case FULL_JOIN -> OpKind.FULL_JOIN;
        default -> null;
        // throw new IllegalArgumentException("unsupported join kind: " + kind);
      };
    }

    private static OpKind setOpKindOf(SetOpNode setOp) {
      final SetOpKind kind = setOp.opKind();
      return switch (kind) {
        case UNION -> OpKind.UNION;
        case INTERSECT -> OpKind.INTERSECT;
        case EXCEPT -> OpKind.EXCEPT;
      };
    }

    private static AggFuncKind aggFunctionKindOf(String aggType) {
      return switch (aggType.toLowerCase()) {
        case "sum" -> AggFuncKind.SUM;
        case "average", "avg" -> AggFuncKind.AVERAGE;
        case "count" -> AggFuncKind.COUNT;
        case "max" -> AggFuncKind.MAX;
        case "min" -> AggFuncKind.MIN;
        default -> null;
        // throw new UnsupportedOperationException("Unsupported aggregate function: " + aggType);
      };
    }

  }
}
