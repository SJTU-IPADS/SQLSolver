package sqlsolver.sql.plan;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.COW;
import sqlsolver.sql.schema.Column;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.utils.IterableSupport.zip;
import static sqlsolver.common.utils.ListSupport.join;

class ValuesRegistryImpl implements ValuesRegistry {
  private int nextId;
  private final PlanContext ctx;
  private final COW<TIntObjectMap<Values>> nodeValues;
  private final COW<Map<Value, Column>> valueColumns;
  private final COW<Map<Value, Expression>> valueExprs;
  private final COW<Map<Expression, Values>> exprRefs;

  protected ValuesRegistryImpl(PlanContext ctx) {
    this.nextId = 0;
    this.ctx = ctx;
    this.nodeValues = new COW<>(new TIntObjectHashMap<>(ctx.maxNodeId()), null);
    this.valueColumns = new COW<>(new IdentityHashMap<>(), null);
    this.valueExprs = new COW<>(new IdentityHashMap<>(), null);
    this.exprRefs = new COW<>(new IdentityHashMap<>(), null);
  }

  protected ValuesRegistryImpl(ValuesRegistryImpl toCopy, PlanContext newPlan) {
    this.ctx = newPlan;
    this.nextId = toCopy.nextId;
    this.nodeValues = new COW<>(toCopy.nodeValues.forRead(), TIntObjectHashMap::new);
    this.valueColumns = new COW<>(toCopy.valueColumns.forRead(), IdentityHashMap::new);
    this.valueExprs = new COW<>(toCopy.valueExprs.forRead(), IdentityHashMap::new);
    this.exprRefs = new COW<>(toCopy.exprRefs.forRead(), IdentityHashMap::new);
  }

  @Override
  public Values valuesOf(int nodeId) {
    if (nodeId == NO_SUCH_NODE) return Values.mk(emptyList());

    Values values = nodeValues.forRead().get(nodeId);
    if (values != null) return values;

    List<Expression> exprs = null;
    switch (ctx.kindOf(nodeId)) {
      case Filter:
      case InSub:
      case Exists:
      case SetOp:
      case Limit:
      case Sort:
        return valuesOf(ctx.childOf(nodeId, 0));

      case Join:
        return Values.mk(join(valuesOf(ctx.childOf(nodeId, 0)), valuesOf(ctx.childOf(nodeId, 1))));

      case Proj:
      case Agg:
        final Pair<Values, List<Expression>> pair =
            mkValuesOfExporter((Exporter) ctx.nodeAt(nodeId));
        values = pair.getLeft();
        exprs = pair.getRight();
        break;

      case Input:
        values = mkValuesOfInput((InputNode) ctx.nodeAt(nodeId));
        break;

      default:
        throw new PlanException("unknown plan node kind: " + ctx.kindOf(nodeId));
    }

    bindValues(nodeId, values);
    if (exprs != null) zip(values, exprs, this::bindExpr);

    return values;
  }

  @Override
  public int initiatorOf(Value value) {
    final InitiatorFinder finder = new InitiatorFinder(value);
    nodeValues.forRead().forEachEntry(finder);
    return finder.initiator;
  }

  @Override
  public Column columnOf(Value value) {
    return valueColumns.forRead().get(value);
  }

  @Override
  public Expression exprOf(Value value) {
    return valueExprs.forRead().get(value);
  }

  @Override
  public void bindValueRefs(Expression expr, List<Value> valueRefs) {
    final Values vs;
    if (valueRefs instanceof Values) vs = (Values) valueRefs;
    else vs = Values.mk(valueRefs);
    exprRefs.forWrite().put(expr, vs);
  }

  @Override
  public Values valueRefsOf(Expression expr) {
    return exprRefs.forRead().get(expr);
  }

  @Override
  public void bindValues(int nodeId, List<Value> rawValues) {
    final Values values;
    if (rawValues instanceof Values) values = (Values) rawValues;
    else values = Values.mk(rawValues);
    nodeValues.forWrite().put(nodeId, values);
  }

  @Override
  public void bindExpr(Value value, Expression expr) {
    valueExprs.forWrite().put(value, expr);
  }

  void relocateNode(int from, int to) {
    final Values values = nodeValues.forRead().get(from);
    if (values != null) {
      nodeValues.forWrite().put(to, values);
      nodeValues.forWrite().remove(from);
    }
  }

  void deleteNode(int id) {
    if (nodeValues.forRead().containsKey(id)) {
      final Values values = nodeValues.forWrite().remove(id);
      if (values != null && !values.isEmpty() && initiatorOf(values.get(0)) == NO_SUCH_NODE)
        for (Value value : values) deleteValue(value);
    }
  }

  private void deleteValue(Value value) {
    if (valueColumns.forRead().containsKey(value)) valueColumns.forWrite().remove(value);
    if (valueExprs.forRead().containsKey(value)) {
      final Expression expr = valueExprs.forWrite().remove(value);
      if (!valueExprs.forRead().containsValue(expr)) exprRefs.forWrite().remove(expr);
    }
  }

  private Values mkValuesOfInput(InputNode input) {
    final Collection<Column> columns = input.table().columns();
    final String qualification = input.qualification();

    final Values values = Values.mk();
    for (Column column : columns) {
      int id = ++nextId;
      final Value value = new ValueImpl(id, qualification, column.name());
      values.add(value);
      valueColumns.forWrite().put(value, column);
    }

    return values;
  }

  private Pair<Values, List<Expression>> mkValuesOfExporter(Exporter exporter) {
    final String qualification = exporter.qualification();
    final List<String> attrNames = exporter.attrNames();

    final Values values = Values.mk();
    for (String attrName : attrNames) {
      int id = ++nextId;
      values.add(new ValueImpl(id, qualification, attrName));
    }

    return Pair.of(values, exporter.attrExprs());
  }

  private static class InitiatorFinder implements TIntObjectProcedure<Values> {
    private int initiator = NO_SUCH_NODE;
    private final Value target;

    private InitiatorFinder(Value target) {
      this.target = target;
    }

    @Override
    public boolean execute(int a, Values b) {
      if (b.contains(target)) {
        initiator = a;
        return false;
      }
      return true;
    }
  }
}
