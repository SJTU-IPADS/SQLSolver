package sqlsolver.superopt.optimizer;

import com.google.common.collect.Iterables;
import sqlsolver.common.utils.Lazy;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.plan.*;
import sqlsolver.sql.schema.Column;
import sqlsolver.superopt.constraint.Constraint;
import sqlsolver.superopt.constraint.Constraints;
import sqlsolver.superopt.fragment.Symbol;

import java.util.*;

import static sqlsolver.common.utils.IterableSupport.*;
import static sqlsolver.sql.ast.constants.ConstraintKind.FOREIGN;
import static sqlsolver.sql.ast.constants.ConstraintKind.NOT_NULL;
import static sqlsolver.sql.plan.PlanSupport.*;
import static sqlsolver.sql.schema.SchemaSupport.findIC;
import static sqlsolver.sql.schema.SchemaSupport.findRelatedIC;

class Model {
  private final Model base;
  private final Constraints constraints;
  private final Lazy<Map<Symbol, Object>> assignments;

  private PlanContext plan;

  private Model(Model other) {
    this.base = other;
    this.constraints = other.constraints;
    this.assignments = Lazy.mk(HashMap::new);
    this.plan = other.plan;
  }

  Model(Constraints constraints) {
    this.base = null;
    this.constraints = constraints;
    this.assignments = Lazy.mk(HashMap::new);
  }

  Model setPlan(PlanContext plan) {
    this.plan = plan;
    return this;
  }

  Model base() {
    return base;
  }

  PlanContext plan() {
    return plan;
  }

  Constraints constraints() {
    return constraints;
  }

  Model derive() {
    return new Model(this);
  }

  void reset() {
    if (assignments.isInitialized()) assignments.get().clear();
  }

  boolean isAssigned(Symbol sym) {
    return of(sym) != null;
  }

  Integer ofTable(Symbol tableSym) {
    return of(tableSym);
  }

  List<Value> ofSchema(Symbol schemaSym) {
    return of(schemaSym);
  }

  List<Value> ofAttrs(Symbol attrsSym) {
    return of(attrsSym);
  }

  Expression ofPred(Symbol predSym) {
    return of(predSym);
  }

  List<Expression> ofFunctions(Symbol funcSym) {
    return of(funcSym);
  }

  boolean assign(Symbol sym, Object assignment) {
    assert sym.kind() != Symbol.Kind.TABLE || assignment instanceof Integer;
    assert sym.kind() != Symbol.Kind.ATTRS || assignment instanceof List<?>;
    assert sym.kind() != Symbol.Kind.PRED || assignment instanceof Expression;
    assert sym.kind() != Symbol.Kind.SCHEMA || assignment instanceof List<?>;
    assert sym.kind() != Symbol.Kind.FUNC || assignment instanceof List<?>;

    assignments.get().put(sym, assignment);

    for (Symbol eqSym : constraints.eqClassOf(sym)) {
      if (eqSym == sym) continue;
      final Object otherAssignment = of(eqSym);
      if (otherAssignment != null && !checkCompatible(sym.kind(), assignment, otherAssignment)) {
        return false;
      }
    }

    return true;
  }

  boolean checkConstraints() {
    return all(constraints, this::checkConstraint);
  }

  private <T> T of(Symbol sym) {
    final Object o = assignments.get().get(sym);
    if (o != null) return (T) o;
    else if (base != null) return base.of(sym);
    else return null;
  }

  private boolean isPredEq(Object v0, Object v1) {
    String predName0 = "" + v0.toString();
    String predName1 = "" + v1.toString();
    boolean eq = predName0.equals(predName1);
    if(eq)
      return true;
    for(int i = 0; i <= 40; ++ i) {
      String params = "`#`.`#`";
      for(int j = 0; j < i; ++ j) {
        params = params + ", `#`.`#`";
      }
      String max = "MAX(" + params + ")";
      String min = "MIN(" + params + ")";
      String sum = "SUM(" + params + ")";
      String avg = "AVERAGE(" + params + ")";
      String count = "COUNT(" + params + ")";
      predName0 = predName0.replace(max, params);
      predName1 = predName1.replace(max, params);
      predName0 = predName0.replace(min, params);
      predName1 = predName1.replace(min, params);
      predName0 = predName0.replace(sum, params);
      predName1 = predName1.replace(sum, params);
      predName0 = predName0.replace(count, params);
      predName1 = predName1.replace(count, params);
      predName0 = predName0.replace(avg, params);
      predName1 = predName1.replace(avg, params);
    }

    if (predName0.equals(predName1) )
      return true;

    for(int i = 0; i <=10; ++ i) {
      String predName = "P" + i + "(";
      if ((predName0.indexOf(predName) == 0) && (predName1.indexOf(predName) == 0)) {
        return true;
      }
    }

    return false;
  }

  private boolean checkCompatible(Symbol.Kind kind, Object v0, Object v1) {
    if (kind == Symbol.Kind.SCHEMA) return true;
    if (kind == Symbol.Kind.PRED)
      return v1 instanceof Expression && isPredEq(v0, v1);
    if (kind == Symbol.Kind.ATTRS) return v1 instanceof List && isAttrsEq((List<Value>) v0, (List<Value>) v1);
    if (kind == Symbol.Kind.FUNC)
      return v1 instanceof List<?> && isFuncsEq((List<Expression>) v0, (List<Expression>) v1);
    if (kind == Symbol.Kind.TABLE) {
      if (!(v1 instanceof Integer)) return false;
      if ((OptimizerSupport.optimizerTweaks & OptimizerSupport.TWEAK_ENABLE_QUERY_AS_EQ_INPUT) == 0) {
        return isEqualTree(plan, (Integer) v0, plan, (Integer) v1);
      } else {
        return isLiteralEq(plan, (Integer) v0, plan, (Integer) v1);
      }
    }

    throw new IllegalArgumentException("unexpected assignment: " + v0);
  }

  private boolean isFuncsEq(List<Expression> v0, List<Expression> v1) {
    if (v0.size() != v1.size()) return false;
    for (var pair : zip(v0, v1)) {
      if (!pair.getLeft().toString().equals(pair.getRight().toString())) return false;
    }
    return true;
  }

  private boolean checkConstraint(Constraint constraint) {
    switch (constraint.kind()) {
      case AttrsSub:
        return checkAttrsSub(constraint);
      case Unique:
        return checkUnique(constraint);
        //      case NotNull:
        //        return checkNotNull(constraint);
      case Reference:
        return checkReference(constraint);
      default:
        return true;
    }
  }

  private boolean checkAttrsSub(Constraint attrsSub) {
    final Symbol attrsSym = attrsSub.symbols()[0];
    final Symbol srcSym = attrsSub.symbols()[1];

    final List<Value> subAttrs = ofAttrs(attrsSym);
    if (subAttrs == null) return true;

    final List<Value> srcAttrs = ofOutAttrs(srcSym);
    if (srcAttrs == null) return true;

    return srcAttrs.containsAll(subAttrs);
  }

  private boolean checkUnique(Constraint unique) {
    final Symbol attrsSym = unique.symbols()[1];
    final Symbol tableSym = unique.symbols()[0];

    final List<Value> attrs = ofAttrs(attrsSym);
    if (attrs == null) return true; // not assigned yet, pass

    final List<Column> columns = tryResolveColumns(plan, attrs);
    if (columns == null) return false; // some attrs has no backed column

    //    if (none(columns, column -> isParticipateIn(column, UNIQUE))) return false;

    final Integer input = ofTable(tableSym);
    if (input == null) return true; // not assigned yet, pass

    return PlanSupport.isUniqueCoreAt(plan, new HashSet<>(attrs), input);
  }

  private boolean checkNotNull(Constraint notNull) {
    final Symbol attrsSym = notNull.symbols()[1];
    final Symbol tableSym = notNull.symbols()[0];

    final List<Value> attrs = ofAttrs(attrsSym);
    if (attrs == null) return true; // not assigned yet, pass

    final List<Column> columns = tryResolveColumns(plan, attrs);
    if (columns == null) return false; // some attrs has no backed column

    if (none(columns, column -> isParticipateIn(column, NOT_NULL))) return false;

    final Integer input = ofTable(tableSym);
    if (input == null) return true; // not assigned yet, pass

    final PlanContext plan = this.plan;
    return any(attrs, attr -> PlanSupport.isNotNullAt(plan, attr, input));
  }

  private boolean checkReference(Constraint reference) {
    assert reference.kind() == Constraint.Kind.Reference;

    final Symbol referringAttrsSym = reference.symbols()[1];
    final Symbol referredTableSym = reference.symbols()[2];
    final Symbol referredAttrsSym = reference.symbols()[3];

    final List<Value> referringAttrs = ofAttrs(referringAttrsSym);
    if (referringAttrs == null) return true;
    final List<Column> referringColumns = tryResolveColumns(plan, referringAttrs);
    if (referringColumns == null) return false;
    //    if (fks.isEmpty()) return false;

    final List<Value> referredAttrs = ofAttrs(referredAttrsSym);
    if (referredAttrs == null) return true;
    final List<Column> referredCols = tryResolveColumns(plan, referredAttrs);
    if (referredCols == null) return false;

    final var fks = findIC(plan.schema(), referringColumns, FOREIGN);
    if (!referringColumns.equals(referredCols)
        && linearFind(fks, it -> it.refColumns().equals(referredCols)) == null) return false;

    final Integer surface = ofTable(referredTableSym);
    if (surface == null) return true;

    // Check if the referred attributes are filtered, which invalidates the FK on schema.
    Value rootRef = referredAttrs.get(0);
    if (!isRootRef(plan, rootRef)) rootRef = deRef(plan, rootRef);
    assert rootRef != null;

    int path = plan.valuesReg().initiatorOf(rootRef);
    while (path != surface) {
      final int parent = plan.parentOf(path);
      if (plan.kindOf(parent).isFilter()) return false;
      if (plan.kindOf(parent) == PlanKind.Agg && ((AggNode) plan.nodeAt(parent)).havingExpr() != null)
        return false;
      path = parent;
    }

    return true;
  }

  private List<Value> ofOutAttrs(Symbol sym) {
    if (sym.kind() == Symbol.Kind.TABLE) {
      final Integer nodeId = ofTable(sym);
      return nodeId == null ? null : plan.valuesReg().valuesOf(nodeId);
    } else {
      assert sym.kind() == Symbol.Kind.SCHEMA;
      return ofAttrs(sym);
    }
  }

  private boolean isParticipateIn(Column column, ConstraintKind integrityConstraint) {
    return !Iterables.isEmpty(findRelatedIC(plan.schema(), column, integrityConstraint));
  }

  private boolean isAttrsEq(List<Value> attrs0, List<Value> attrs1) {
    if (attrs0.size() != attrs1.size()) return false;

    for (int i = 0, bound = attrs0.size(); i < bound; i++) {
      final Value attr0 = attrs0.get(i), attr1 = attrs1.get(i);

      final Value rootRef0 = traceRef(plan, attr0);
      final Value rootRef1 = traceRef(plan, attr1);
      final Column column0 = tryResolveColumn(plan, rootRef0);
      final Column column1 = tryResolveColumn(plan, rootRef1);
      if (column0 == null ^ column1 == null) return false;
      if (column0 != null) return column0.equals(column1);

      final ValuesRegistry valuesReg = plan.valuesReg();
      final Expression expr0 = valuesReg.exprOf(rootRef0);
      final Expression expr1 = valuesReg.exprOf(rootRef1);
      if (expr0 == null ^ expr1 == null) return false;
      assert expr0 != null;

      if (!Objects.equals(expr0.template().toString(), expr1.template().toString())
          || !isAttrsEq(valuesReg.valueRefsOf(expr0), valuesReg.valueRefsOf(expr1))) {
        return false;
      }
    }

    return true;
  }
}
