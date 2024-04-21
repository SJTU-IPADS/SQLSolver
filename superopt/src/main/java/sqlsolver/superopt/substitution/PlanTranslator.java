package sqlsolver.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.SetOpKind;
import sqlsolver.sql.plan.*;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;
import sqlsolver.superopt.constraint.Constraint;
import sqlsolver.superopt.constraint.Constraints;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.fragment.*;

import java.util.*;

import static java.util.Collections.singletonList;
import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.sql.SqlSupport.*;
import static sqlsolver.sql.ast.ExprFields.ColRef_ColName;
import static sqlsolver.common.datasource.DbSupport.MySQL;
import static sqlsolver.sql.ast.SqlNodeFields.ColName_Col;
import static sqlsolver.sql.ast.constants.BinaryOpKind.EQUAL;
import static sqlsolver.superopt.constraint.Constraint.Kind.*;

class PlanTranslator {
  private final Substitution rule;
  private final NameSequence aliasSeq;

  private final Constraints constraints;
  private final Symbols srcSyms;

  private final Map<Symbol, SourceDesc> sourceDescs;
  private final Map<Symbol, AttrsDesc> attrsDescs;
  private final Map<Symbol, PredDesc> predDescs;
  private final Map<Symbol, SchemaDesc> schemaDesc;

  private final NameSequence tableSeq;
  private final NameSequence sourceSeq;
  private final NameSequence colSeq;
  private final NameSequence predSeq;
  private final NameSequence synNameSeq;

  private Schema schema;

  PlanTranslator(Substitution rule) {
    this.rule = rule;
    this.constraints = rule.constraints();
    this.srcSyms = rule._0().symbols();
    this.sourceDescs = new HashMap<>();
    this.attrsDescs = new HashMap<>();
    this.predDescs = new HashMap<>();
    this.schemaDesc = new HashMap<>();
    this.aliasSeq = NameSequence.mkIndexed("q", 0);
    this.tableSeq = NameSequence.mkIndexed("r", 0);
    this.sourceSeq = NameSequence.mkIndexed("t", 0);
    this.colSeq = NameSequence.mkIndexed("c", 0);
    this.predSeq = NameSequence.mkIndexed("p", 0);
    this.synNameSeq = NameSequence.mkIndexed("%", 0);
  }

  Pair<PlanContext, PlanContext> translate() {
    assignAll();
    schema = mkSchema();
    final PlanContext sourcePlan = new PlanConstructor(rule._0(), false).translate();
    final PlanContext targetPlan = new PlanConstructor(rule._1(), true).translate();
    if (sourcePlan == null || targetPlan == null) return Pair.of(null, null);

    PlanSupport.resolvePlan(sourcePlan);
    PlanSupport.resolvePlan(targetPlan);
    return Pair.of(sourcePlan, targetPlan);
  }

  private <T> T getDescOf(Map<Symbol, T> descs, Symbol sym) {
    return sym.ctx() == srcSyms ? descs.get(sym) : descs.get(constraints.instantiationOf(sym));
  }

  private SourceDesc sourceDescOf(Symbol tableSym) {
    return getDescOf(sourceDescs, tableSym);
  }

  private AttrsDesc attrsDescOf(Symbol attrsSym) {
    final AttrsDesc desc = getDescOf(attrsDescs, attrsSym);
    if (desc != null) return desc;
    if (attrsSym.ctx() != srcSyms) attrsSym = constraints.instantiationOf(attrsSym);
    final Symbol schemaSym = constraints.sourceOf(attrsSym);
    final Op op = srcSyms.ownerOf(schemaSym);
    assert op.kind() == OpKind.PROJ;
    return attrsDescOf(((Proj) op).attrs());
  }

  private PredDesc predDescOf(Symbol predSym) {
    return getDescOf(predDescs, predSym);
  }

  private SchemaDesc schemaDescOf(Symbol schemaSym) {
    return getDescOf(schemaDesc, schemaSym);
  }

  public Schema schema() {
    if (schema == null) schema = mkSchema();
    return schema;
  }

  private void assignAll() {
    for (Symbol table : srcSyms.symbolsOf(Symbol.Kind.TABLE)) sourceDescs.put(table, assignSourceDesc(table));
    for (Symbol attrs : srcSyms.symbolsOf(Symbol.Kind.ATTRS)) attrsDescs.put(attrs, assignAttrsDesc(attrs));
    for (Symbol pred : srcSyms.symbolsOf(Symbol.Kind.PRED)) predDescs.put(pred, assignPredDesc(pred));
    for (Symbol schema : srcSyms.symbolsOf(Symbol.Kind.SCHEMA))
      schemaDesc.put(schema, assignSchemaDesc(schema));
  }

  private TableDesc assignTableDesc(Symbol table) {
    final Constraints constraints = this.constraints;
    for (var pair : sourceDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), table)) return pair.getValue().tableDesc;
    }
    return new TableDesc(tableSeq.next());
  }

  private SourceDesc assignSourceDesc(Symbol table) {
    final TableDesc tableDesc = assignTableDesc(table);
    return new SourceDesc(tableDesc, sourceSeq.next());
  }

  private AttrsDesc assignAttrsDesc(Symbol attrs) {
    final Constraints constraints = this.constraints;
    final Symbol source = constraints.sourceOf(attrs);
    if (source.kind() == Symbol.Kind.SCHEMA) return null;

    for (var pair : attrsDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), attrs) && pair.getValue() != null) return pair.getValue();
    }

    final TableDesc tableDesc = sourceDescOf(source).tableDesc;
    final String colName = colSeq.next();
    tableDesc.colNames.add(colName);
    return new AttrsDesc(colName);
  }

  private PredDesc assignPredDesc(Symbol pred) {
    final Constraints constraints = this.constraints;
    for (var pair : predDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), pred)) return pair.getValue();
    }

    return new PredDesc(predSeq.next());
  }

  private SchemaDesc assignSchemaDesc(Symbol pred) {
    return new SchemaDesc(aliasSeq.next());
  }

  private Schema mkSchema() {
    final StringBuilder builder = new StringBuilder();

    for (SourceDesc source : sourceDescs.values()) {
      final TableDesc table = source.tableDesc;
      if (table.initialized) continue;
      table.initialized = true;

      builder.append("create table ").append(table.name).append("(\n");
      for (String attrName : table.colNames)
        builder.append("  ").append(attrName).append(" int,\n");
      builder.append("  ").append(colSeq.next()).append(" int);");
    }

    final Set<String> initiatedConstraints = new HashSet<>();
    for (Constraint notNull : constraints.ofKind(NotNull)) {
      final AttrsDesc attr = attrsDescs.get(notNull.symbols()[1]);
      if (!initiatedConstraints.add(attr.colName)) continue;

      final TableDesc table = sourceDescs.get(notNull.symbols()[0]).tableDesc;
      builder
          .append("alter table ")
          .append(table.name)
          .append(" modify column ")
          .append(attr.colName)
          .append(" int not null;\n");
    }

    initiatedConstraints.clear();
    for (Constraint uniqueKey : constraints.ofKind(Unique)) {
      final AttrsDesc attrs = attrsDescs.get(uniqueKey.symbols()[1]);
      if (!initiatedConstraints.add(attrs.colName)) continue;

      final TableDesc table = sourceDescs.get(uniqueKey.symbols()[0]).tableDesc;
      builder
          .append("alter table ")
          .append(table.name)
          .append(" add unique (")
          .append(attrs.colName)
          .append(");");
    }

    initiatedConstraints.clear();
    for (Constraint foreignKey : constraints.ofKind(Reference)) {
      if (!sourceDescs.containsKey(foreignKey.symbols()[0])) continue;

      final AttrsDesc attrs = attrsDescs.get(foreignKey.symbols()[1]);
      final AttrsDesc refAttrs = attrsDescs.get(foreignKey.symbols()[3]);
      if (!initiatedConstraints.add(attrs.colName + refAttrs.colName)) continue;

      final TableDesc refTable = sourceDescs.get(foreignKey.symbols()[2]).tableDesc;
      final TableDesc table = sourceDescs.get(foreignKey.symbols()[0]).tableDesc;

      builder
          .append("alter table ")
          .append(table.name)
          .append(" add foreign key (")
          .append(attrs.colName)
          .append(") references ")
          .append(refTable.name)
          .append('(')
          .append(refAttrs.colName)
          .append(");\n");
    }

    return SchemaSupport.parseSchema(MySQL, builder.toString());
  }

  private static class TableDesc {
    private final String name;
    private final Set<String> colNames;
    private boolean initialized;

    private TableDesc(String name) {
      this.name = name;
      this.colNames = new HashSet<>();
      this.initialized = false;
    }
  }

  private static class SourceDesc {
    private final TableDesc tableDesc;
    private final String qualification;

    private SourceDesc(TableDesc tableDesc, String qualification) {
      this.tableDesc = tableDesc;
      this.qualification = qualification;
    }
  }

  private static class AttrsDesc {
    private final String colName;

    private AttrsDesc(String colName) {
      this.colName = colName;
    }
  }

  private static class PredDesc {
    private final String predName;

    private PredDesc(String predName) {
      this.predName = predName;
    }
  }

  private static class SchemaDesc {
    private final String schemaName;

    private SchemaDesc(String schemaName) {
      this.schemaName = schemaName;
    }
  }

  private class PlanConstructor {
    private final Fragment template;
    private final SqlContext sql;
    private final PlanContext plan;
    private final boolean isTargetSide;
    private final Map<Op, PlanNode> instantiatedOps;

    private PlanConstructor(Fragment template, boolean isTargetSide) {
      this.template = template;
      this.sql = SqlContext.mk(16);
      this.plan = PlanContext.mk(schema, 0);
      this.isTargetSide = isTargetSide;
      this.instantiatedOps = new HashMap<>(8);
    }

    private PlanContext translate() {
      final int rootId = trTree(template.root());
      if (rootId == NO_SUCH_NODE) return null;
      return plan.setRoot(rootId);
    }

    private int trTree(Op op) {
      switch (op.kind()) {
        case INPUT:
          return trInput((Input) op);
        case INNER_JOIN, LEFT_JOIN:
          return trJoin((Join) op);
        case SIMPLE_FILTER:
          return trSimpleFilter((SimpleFilter) op);
        case IN_SUB_FILTER:
          return trInSubFilter((InSubFilter) op);
        case PROJ:
          return trProj((Proj) op);
        case UNION, INTERSECT, EXCEPT:
          return trSetOp((SetOp) op);
        case AGG:
          return trAgg((Agg) op);
        default:
          throw new IllegalArgumentException("unknown operator type: " + op.kind());
      }
    }

    private int trInput(Input input) {
      final SourceDesc desc = sourceDescOf(input.table());
      final InputNode node = InputNode.mk(schema.table(desc.tableDesc.name), desc.qualification);
      instantiatedOps.put(input, node);
      return plan.bindNode(node);
    }

    private int trJoin(Join join) {
      final int lhsChild = trTree(join.predecessors()[0]);
      final int rhsChild = trTree(join.predecessors()[1]);
      if (lhsChild == NO_SUCH_NODE || rhsChild == NO_SUCH_NODE) return NO_SUCH_NODE;

      final SqlNode lhsKey = trAttrs(join.lhsAttrs(), join.predecessors()[0]);
      final SqlNode rhsKey = trAttrs(join.rhsAttrs(), join.predecessors()[1]);
      if (lhsKey == null || rhsKey == null) return NO_SUCH_NODE;

      final SqlNode joinCond = mkBinary(sql, EQUAL, lhsKey, rhsKey);
      final Expression joinCondExpr = Expression.mk(joinCond);
      final JoinNode node = JoinNode.mk(joinKindOf(join), joinCondExpr);

      instantiatedOps.put(join, node);
      final int nodeId = plan.bindNode(node);
      plan.setChild(nodeId, 0, lhsChild);
      plan.setChild(nodeId, 1, rhsChild);
      return nodeId;
    }

    private int trSimpleFilter(SimpleFilter filter) {
      final int lhsChild = trTree(filter.predecessors()[0]);
      if (lhsChild == NO_SUCH_NODE) return NO_SUCH_NODE;

      final SqlNode key = trAttrs(filter.attrs(), filter.predecessors()[0]);
      if (key == null) return NO_SUCH_NODE;

      final String predName = predDescOf(filter.predicate()).predName;
      final SqlNode pred = mkFuncCall(sql, predName, singletonList(key));
      final SimpleFilterNode node = SimpleFilterNode.mk(Expression.mk(pred));

      instantiatedOps.put(filter, node);
      final int nodeId = plan.bindNode(node);
      plan.setChild(nodeId, 0, lhsChild);
      return nodeId;
    }

    private int trInSubFilter(InSubFilter filter) {
      final int lhsChild = trTree(filter.predecessors()[0]);
      final int rhsChild = trTree(filter.predecessors()[1]);
      if (lhsChild == NO_SUCH_NODE || rhsChild == NO_SUCH_NODE) return NO_SUCH_NODE;

      final SqlNode key = trAttrs(filter.attrs(), filter.predecessors()[0]);
      if (key == null) return NO_SUCH_NODE;

      final InSubNode node = InSubNode.mk(Expression.mk(key));

      instantiatedOps.put(filter, node);
      final int nodeId = plan.bindNode(node);
      plan.setChild(nodeId, 0, lhsChild);
      plan.setChild(nodeId, 1, rhsChild);
      return nodeId;
    }

    private int trProj(Proj proj) {
      final int lhsChild = trTree(proj.predecessors()[0]);
      if (lhsChild == NO_SUCH_NODE) return NO_SUCH_NODE;

      final SqlNode colRef = trAttrs(proj.attrs(), proj.predecessors()[0]);
      if (colRef == null) return NO_SUCH_NODE;

      final String colName = colRef.$(ColRef_ColName).$(ColName_Col);
      final List<String> nameList = singletonList(colName);
      final List<Expression> exprList = singletonList(Expression.mk(colRef));
      final ProjNode node = ProjNode.mk(proj.deduplicated(), nameList, exprList);
      node.setQualification(schemaDescOf(proj.schema()).schemaName);

      instantiatedOps.put(proj, node);
      final int nodeId = plan.bindNode(node);
      plan.setChild(nodeId, 0, lhsChild);
      return nodeId;
    }

    private int trSetOp(SetOp setOp) {
      final int lhsChild = trTree(setOp.predecessors()[0]);
      final int rhsChild = trTree(setOp.predecessors()[1]);
      if (lhsChild == NO_SUCH_NODE || rhsChild == NO_SUCH_NODE) return NO_SUCH_NODE;

      final SetOpNode node = SetOpNode.mk(setOp.deduplicated(), setOpKindOf(setOp));

      instantiatedOps.put(setOp, node);
      final int nodeId = plan.bindNode(node);
      plan.setChild(nodeId, 0, lhsChild);
      plan.setChild(nodeId, 1, rhsChild);
      return nodeId;
    }

    private int trAgg(Agg agg) {
      final int lhsChild = trTree(agg.predecessors()[0]);
      if (lhsChild == NO_SUCH_NODE) return NO_SUCH_NODE;

      final SqlNode groupRefAst = trAttrs(agg.groupByAttrs(), agg.predecessors()[0]);
      final SqlNode aggRefAst = trAttrs(agg.aggregateAttrs(), agg.predecessors()[0]);
      if (aggRefAst == null || groupRefAst == null) return NO_SUCH_NODE;

      final String groupColName = attrsDescOf(agg.groupByAttrs()).colName;
      final String aggColName = attrsDescOf(agg.aggregateAttrs()).colName;
      final String aggFuncName = (agg.aggFuncKind() == AggFuncKind.UNKNOWN) ? "count" : agg.aggFuncKind().text(); // TODO
      final SqlNode aggAst = mkAggregate(sql, singletonList(aggRefAst), aggFuncName);

      final String havingPredName = predDescOf(agg.havingPred()).predName;
      final SqlNode havingRefAst = copyAst(aggAst, sql);
      final SqlNode predAst = mkFuncCall(sql, havingPredName, singletonList(havingRefAst));
      final Expression havingExpr = Expression.mk(predAst);

      final var attrNames = List.of(groupColName, synNameSeq.next());
      final var attrExprs = List.of(Expression.mk(groupRefAst), Expression.mk(aggAst));
      final var groupExprs = singletonList(Expression.mk(groupRefAst));
      final AggNode aggNode = AggNode.mk(agg.deduplicated(), attrNames, attrExprs, groupExprs, havingExpr);
      aggNode.setQualification(schemaDescOf(agg.schema()).schemaName);

      // Insert a proj node: translate aggregation as Agg(Proj(..))
      final var projAttrNames = List.of(groupColName, aggColName, groupColName, aggColName);
      final var projAttrExprs =
          map(List.of(groupRefAst, aggRefAst, groupRefAst, aggRefAst), Expression::mk);
      final ProjNode projNode = ProjNode.mk(false, projAttrNames, projAttrExprs);
      projNode.setQualification(aliasSeq.next());

      final int projNodeId = plan.bindNode(projNode);
      final int aggNodeId = plan.bindNode(aggNode);
      instantiatedOps.put(agg, aggNode);
      plan.setChild(projNodeId, 0, lhsChild);
      plan.setChild(aggNodeId, 0, projNodeId);
      return aggNodeId;
    }

    private SqlNode trAttrs(Symbol attrs, Op predecessor) {
      final String name = attrsDescOf(attrs).colName;
      final String qualification = findSourceIn(deepSourceOf(attrs), predecessor);
      if (qualification == null) return null;
      return mkColRef(sql, qualification, name);
    }

    private Symbol deepSourceOf(Symbol attrs) {
      final Constraints constraints = rule.constraints();

      Symbol source = null;
      if (isTargetSide) {
        source = constraints.sourceOf(attrs);
        if (source == null) attrs = constraints.instantiationOf(attrs);
      }
      if (source == null) source = constraints.sourceOf(attrs);

      while (source.kind() != Symbol.Kind.TABLE) {
        final Op op = srcSyms.ownerOf(source);
        assert op.kind() == OpKind.PROJ;
        source = constraints.sourceOf(((Proj) op).attrs());
      }
      return source;
    }

    private String findSourceIn(Symbol source, Op root) {
      if (root.kind() == OpKind.INPUT) {
        Symbol table = ((Input) root).table();
        if (isTargetSide) table = rule.constraints().instantiationOf(table);
        if (source == table) return ((InputNode) instantiatedOps.get(root)).qualification();

      } else if (root.kind() == OpKind.PROJ || root.kind() == OpKind.AGG) {
        if (findSourceIn(source, root.predecessors()[0]) != null)
          return ((Exporter) instantiatedOps.get(root)).qualification();

      } else {
        for (Op predecessor : root.predecessors()) {
          final String found = findSourceIn(source, predecessor);
          if (found != null) return found;
        }
      }

      return null;
    }

    private static JoinKind joinKindOf(Join join) {
      final OpKind kind = join.kind();
      return switch (kind) {
        case LEFT_JOIN -> JoinKind.LEFT_JOIN;
        case INNER_JOIN -> JoinKind.INNER_JOIN;
        default -> throw new IllegalArgumentException("unsupported join kind: " + kind);
      };
    }

    private static SetOpKind setOpKindOf(SetOp setOp) {
      final OpKind kind = setOp.kind();
      return switch (kind) {
        case UNION -> SetOpKind.UNION;
        case INTERSECT -> SetOpKind.INTERSECT;
        case EXCEPT -> SetOpKind.EXCEPT;
        default -> throw new IllegalArgumentException("unsupported SetOp kind: " + kind);
      };
    }
  }
}
