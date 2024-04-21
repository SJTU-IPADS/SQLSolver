package sqlsolver.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlKind;
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
import static sqlsolver.common.utils.Commons.joining;
import static sqlsolver.common.utils.ListSupport.*;
import static sqlsolver.sql.SqlSupport.*;
import static sqlsolver.sql.ast.ExprFields.ColRef_ColName;
import static sqlsolver.common.datasource.DbSupport.MySQL;
import static sqlsolver.sql.ast.SqlNodeFields.ColName_Col;
import static sqlsolver.sql.ast.SqlNodeFields.GroupItem_Expr;
import static sqlsolver.sql.ast.constants.BinaryOpKind.EQUAL;
import static sqlsolver.superopt.constraint.Constraint.Kind.*;

class PlanTranslator2 {
  private static final String ALIAS_PREFIX = "q";
  private static final String TABLE_PREFIX = "r";
  private static final String SOURCE_PREFIX = "t";
  private static final String COL_PREFIX = "c";
  private static final String PRED_PREFIX = "p";
  private static final String SYN_NAME_PREFIX = "%";

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
  private final Map<Symbol, String> synNameReg;

  private Schema schema;

  PlanTranslator2(Substitution rule) {
    this.rule = rule;
    this.constraints = rule.constraints();
    this.srcSyms = rule._0().symbols();
    this.sourceDescs = new HashMap<>();
    this.attrsDescs = new HashMap<>();
    this.predDescs = new HashMap<>();
    this.schemaDesc = new HashMap<>();
    this.aliasSeq = NameSequence.mkIndexed(ALIAS_PREFIX, 0);
    this.tableSeq = NameSequence.mkIndexed(TABLE_PREFIX, 0);
    this.sourceSeq = NameSequence.mkIndexed(SOURCE_PREFIX, 0);
    this.colSeq = NameSequence.mkIndexed(COL_PREFIX, 0);
    this.predSeq = NameSequence.mkIndexed(PRED_PREFIX, 0);
    this.synNameSeq = NameSequence.mkIndexed(SYN_NAME_PREFIX, 0);

    this.synNameReg = new HashMap<>();
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

  private String getSynName(Symbol sym) {
    if (sym.ctx() != srcSyms) sym = constraints.instantiationOf(sym);
    final Op op = srcSyms.ownerOf(sym);
    assert op.kind() == OpKind.AGG && sym == ((Agg) op).aggregateAttrs();

    final String synName = synNameReg.get(sym);
    if (synName == null) {
      final String newSynName = synNameSeq.next();
      synNameReg.put(sym, newSynName);
      return newSynName;
    }
    return synName;
  }

  private <T> T getDescOf(Map<Symbol, T> descs, Symbol sym) {
    return sym.ctx() == srcSyms ? descs.get(sym) : descs.get(constraints.instantiationOf(sym));
  }

  private SourceDesc sourceDescOf(Symbol tableSym) {
    return getDescOf(sourceDescs, tableSym);
  }

  private AttrsDesc attrsDescOf(Symbol attrsSym) {
    return getDescOf(attrsDescs, attrsSym);
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
    for (Symbol table : srcSyms.symbolsOf(Symbol.Kind.TABLE)) assignSourceDesc(table);
    for (Symbol attrs : srcSyms.symbolsOf(Symbol.Kind.ATTRS)) {
      assignAttrsDesc(attrs);
    }
    for (Symbol pred : srcSyms.symbolsOf(Symbol.Kind.PRED)) assignPredDesc(pred);
    for (Symbol schema : srcSyms.symbolsOf(Symbol.Kind.SCHEMA)) assignSchemaDesc(schema);
  }

  private TableDesc assignTableDesc(Symbol table) {
    final Constraints constraints = this.constraints;
    for (var pair : sourceDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), table)) return pair.getValue().tableDesc;
    }
    return new TableDesc(tableSeq.next());
  }

  private void assignSourceDesc(Symbol table) {
    final TableDesc tableDesc = assignTableDesc(table);
    sourceDescs.put(table, new SourceDesc(tableDesc, sourceSeq.next()));
  }

  private void assignAttrsDesc(Symbol attrs) {
    final AttrsDesc desc = getDescOf(attrsDescs, attrs);
    if (desc != null) return;

    // Check AttrsEq constraints:
    // If AttrsEq(a0,a1), a1 has been assigned an AttrsDesc, then a0 owns the same AttrsDesc.
    // It could only happen iff. \exists t1,t2. AttrsSub(a1,t1) /\ AttrsSub(a2,t2) /\
    // TableEq(t1,t2).
    for (var pair : attrsDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), attrs) && pair.getValue() != null) {
        attrsDescs.put(attrs, pair.getValue());
        return;
      }
    }

    final Op thisOp = srcSyms.ownerOf(attrs);
    final Symbol source = constraints.sourceOf(attrs);
    final Op srcOp = srcSyms.ownerOf(source);
    // Backtrace: the column added to attrs may also be added to its sources.
    final List<Symbol> sourceChain = concreteSourceChain(attrs);
    // Check whether there is a single column attrs and find the bottom one.
    Symbol bottomSingleColAttrs = null;
    for (int i = sourceChain.size() - 1; i >= 0; i--) {
      Symbol sourceSym = sourceChain.get(i);
      if (sourceSym.kind() != Symbol.Kind.TABLE && isSingleColKeyAttrs(sourceSym)) {
        bottomSingleColAttrs = sourceSym;
        break;
      }
    }

    // Decide the column name assign to 'attrs'
    final String colName;
    if (bottomSingleColAttrs == null) {
      colName = colSeq.next();
    } else {
      assignAttrsDesc(bottomSingleColAttrs);
      final AttrsDesc bottomSingleColAttrsDesc = attrsDescOf(bottomSingleColAttrs);
      assert bottomSingleColAttrsDesc.isSingleColAttrs();
      colName = bottomSingleColAttrsDesc.colNames.get(0);
    }

    // Assign column to `attrs`
    // If `attrs` is from an Agg's schema, let attrs include the synName of aggregated attrs.
    final AttrsDesc attrsDesc = new AttrsDesc(colName);
    if (!isSingleColKeyAttrs(attrs)
        && !(thisOp.kind() == OpKind.AGG && attrs == ((Agg) thisOp).aggregateAttrs())
        && source.kind() == Symbol.Kind.SCHEMA
        && srcOp.kind() == OpKind.AGG) {
      final Symbol aggAttrs = ((Agg) srcOp).aggregateAttrs();
      attrsDesc.addColName(getSynName(aggAttrs));
    }
    attrsDescs.put(attrs, attrsDesc);

    // Assign new column to each source if no single column attrs
    if (bottomSingleColAttrs == null) {
      // Add `colName` to source symbol's `colNames`
      for (Symbol sourceSym : sourceChain) {
        if (sourceSym.kind() == Symbol.Kind.ATTRS) {
          assignAttrsDesc(sourceSym);
          final AttrsDesc srcAttrsDesc = attrsDescOf(sourceSym);
          srcAttrsDesc.addColName(colName);
        } else {
          final TableDesc tableDesc = sourceDescOf(sourceSym).tableDesc;
          tableDesc.addColName(colName);
        }
      }
    }
  }

  // private void assignAttrsDesc(Symbol attrs) {
  //   final AttrsDesc desc = getDescOf(attrsDescs, attrs);
  //   if (desc != null) return;
  //
  //   // Check AttrsEq constraints:
  //   // If AttrsEq(a0,a1), a1 has been assigned an AttrsDesc, then a0 owns the same AttrsDesc.
  //   // It could only happen iff. \exists t1,t2. AttrsSub(a1,t1) /\ AttrsSub(a2,t2) /\
  // TableEq(t1,t2).
  //   for (var pair : attrsDescs.entrySet()) {
  //     if (constraints.isEq(pair.getKey(), attrs) && pair.getValue() != null) {
  //       attrsDescs.put(attrs, pair.getValue());
  //       return;
  //     }
  //   }
  //
  //   Symbol source = constraints.sourceOf(attrs);
  //   Op thisOp = srcSyms.ownerOf(attrs);
  //   Op srcOp = srcSyms.ownerOf(source);
  //   // Assign column to an attrs
  //   // If attrs is from an Agg's schema, let attrs include the synName of aggregated attrs.
  //   final String colName = colSeq.next();
  //   final AttrsDesc attrsDesc = new AttrsDesc(colName);
  //   if (!(thisOp.kind() == AGG && attrs == ((Agg) thisOp).aggregateAttrs())
  //       && source.kind() == SCHEMA && srcOp.kind() == AGG) {
  //     final Symbol aggAttrs = ((Agg) srcOp).aggregateAttrs();
  //     attrsDesc.addColName(getSynName(aggAttrs));
  //   }
  //   attrsDescs.put(attrs, attrsDesc);
  //
  //
  //   // Backtrace: the column added to attrs should also be added to sources.
  //   // (while its source is a SCHEMA).
  //   while (source.kind() != TABLE) {
  //     srcOp = srcSyms.ownerOf(source);
  //     assert source.kind() == SCHEMA;
  //     assert srcOp.kind() == PROJ || srcOp.kind() == AGG;
  //     final Symbol srcAttrs;
  //     if (srcOp.kind() == PROJ) srcAttrs = ((Proj) srcOp).attrs();
  //     else srcAttrs = ((Agg) srcOp).groupByAttrs(); // Only add new column to Agg's groupBy attrs
  //
  //     assignAttrsDesc(srcAttrs);
  //     final AttrsDesc srcAttrsDesc = attrsDescOf(srcAttrs);
  //     srcAttrsDesc.addColName(colName);
  //
  //     source = constraints.sourceOf(srcAttrs);
  //   }
  //
  //   // When source is a TABLE, add `colName` to its `colNames`
  //   final TableDesc tableDesc = sourceDescOf(source).tableDesc;
  //   tableDesc.addColName(colName);
  // }

  private boolean isSingleColKeyAttrs(Symbol attrs) {
    if (attrs.ctx() != srcSyms) attrs = constraints.instantiationOf(attrs);

    // A symbol on JOIN, InSubFilter should be single-column key attrs
    Op owner = srcSyms.ownerOf(attrs);
    if (owner.kind().isJoin() || owner.kind().isSubquery()) return true;

    for (Symbol eqSym : constraints.eqClassOf(attrs)) {
      Op thatOwner = srcSyms.ownerOf(eqSym);
      if (thatOwner.kind().isJoin() || thatOwner.kind().isSubquery()) return true;
    }
    return false;
  }

  private List<Symbol> concreteSourceChain(Symbol attrs) {
    // Only return attrs or table
    Symbol source = constraints.sourceOf(attrs);
    List<Symbol> sourceChain = new ArrayList<>();

    while (source.kind() != Symbol.Kind.TABLE) {
      final Op srcOp = srcSyms.ownerOf(source);
      assert srcOp.kind() == OpKind.PROJ || srcOp.kind() == OpKind.AGG;
      final Symbol srcAttrs;
      if (srcOp.kind() == OpKind.PROJ) srcAttrs = ((Proj) srcOp).attrs();
      else srcAttrs = ((Agg) srcOp).groupByAttrs(); // Only add new column to Agg's groupBy attrs

      sourceChain.add(srcAttrs);
      source = constraints.sourceOf(srcAttrs);
    }
    sourceChain.add(source);
    return sourceChain;
  }

  private void assignPredDesc(Symbol pred) {
    final Constraints constraints = this.constraints;
    for (var pair : predDescs.entrySet()) {
      if (constraints.isEq(pair.getKey(), pred)) {
        predDescs.put(pred, pair.getValue());
        return;
      }
    }
    predDescs.put(pred, new PredDesc(predSeq.next()));
  }

  private void assignSchemaDesc(Symbol schemaSym) {
    schemaDesc.put(schemaSym, new SchemaDesc(aliasSeq.next()));
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

    int constraintId = 0;
    final Set<String> initiatedConstraints = new HashSet<>();
    for (Constraint notNull : constraints.ofKind(NotNull)) {
      final AttrsDesc attrs = attrsDescs.get(notNull.symbols()[1]);
      final String colNames = joining(",", attrs.colNames);
      if (!initiatedConstraints.add(colNames)) continue;

      final TableDesc table = sourceDescs.get(notNull.symbols()[0]).tableDesc;
      for (String colName : attrs.colNames) {
        builder
            .append("alter table ")
            .append(table.name)
            .append(" modify column ")
            .append(colName)
            .append(" int not null;\n");
      }
    }

    constraintId = 0;
    initiatedConstraints.clear();
    for (Constraint uniqueKey : constraints.ofKind(Unique)) {
      final AttrsDesc attrs = attrsDescs.get(uniqueKey.symbols()[1]);
      final String colNames = joining(",", attrs.colNames);
      if (!initiatedConstraints.add(colNames)) continue;

      final TableDesc table = sourceDescs.get(uniqueKey.symbols()[0]).tableDesc;
      builder
          .append("alter table ")
          .append(table.name)
          .append(" add constraint ")
          .append("unique_")
          .append(constraintId++)
          .append(" unique (")
          .append(colNames)
          .append(");");
    }

    constraintId = 0;
    initiatedConstraints.clear();
    for (Constraint foreignKey : constraints.ofKind(Reference)) {
      if (!sourceDescs.containsKey(foreignKey.symbols()[0])) continue;

      final AttrsDesc attrs = attrsDescs.get(foreignKey.symbols()[1]);
      final AttrsDesc refAttrs = attrsDescs.get(foreignKey.symbols()[3]);
      final String colNames = joining(",", attrs.colNames);
      final String refColNames = joining(",", refAttrs.colNames);
      if (!initiatedConstraints.add(colNames + "-" + refColNames)) continue;

      final TableDesc refTable = sourceDescs.get(foreignKey.symbols()[2]).tableDesc;
      final TableDesc table = sourceDescs.get(foreignKey.symbols()[0]).tableDesc;

      builder
          .append("alter table ")
          .append(table.name)
          .append(" add constraint ")
          .append("fk_")
          .append(constraintId++)
          .append(" foreign key (")
          .append(colNames)
          .append(") references ")
          .append(refTable.name)
          .append('(')
          .append(refColNames)
          .append(");\n");
    }

    return SchemaSupport.parseSchema(MySQL, builder.toString());
  }

  private static class TableDesc {
    private final String name;
    private final List<String> colNames;
    private boolean initialized;

    private TableDesc(String name) {
      this.name = name;
      this.colNames = new ArrayList<>();
      this.initialized = false;
    }

    private void addColName(String colName) {
      this.colNames.add(colName);
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
    private final List<String> colNames;

    private AttrsDesc(String colName) {
      this.colNames = new ArrayList<>(singletonList(colName));
    }

    private AttrsDesc() {
      this.colNames = new ArrayList<>();
    }

    private void addColName(String colName) {
      this.colNames.add(colName);
    }

    private boolean isSingleColAttrs() {
      return colNames.size() == 1;
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
        case INNER_JOIN:
        case LEFT_JOIN:
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

      final SqlNode lhsKey = trAttrsSingle(join.lhsAttrs(), join.predecessors()[0]);
      final SqlNode rhsKey = trAttrsSingle(join.rhsAttrs(), join.predecessors()[1]);
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

      final List<SqlNode> key = trAttrs(filter.attrs(), filter.predecessors()[0]);
      if (key == null) return NO_SUCH_NODE;

      final String predName = predDescOf(filter.predicate()).predName;
      final SqlNode pred = mkFuncCall(sql, predName, key);
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

      final SqlNode key = trAttrsSingle(filter.attrs(), filter.predecessors()[0]);
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

      final List<SqlNode> colRefs = trAttrs(proj.attrs(), proj.predecessors()[0]);
      if (colRefs == null) return NO_SUCH_NODE;

      final List<String> colNameList = map(colRefs, c -> c.$(ColRef_ColName).$(ColName_Col));
      final List<Expression> exprList = map(colRefs, Expression::mk);
      final ProjNode node = ProjNode.mk(proj.deduplicated(), colNameList, exprList);
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

      final List<String> groupColNames = attrsDescOf(agg.groupByAttrs()).colNames;
      final List<String> aggColNames = attrsDescOf(agg.aggregateAttrs()).colNames;

      // Insert a Proj node: translate aggregation as Agg(Proj(..))
      final List<SqlNode> groupRefAsts = trAttrs(agg.groupByAttrs(), agg.predecessors()[0]);
      final List<SqlNode> aggRefAsts = trAttrs(agg.aggregateAttrs(), agg.predecessors()[0]);
      if (groupRefAsts == null || aggRefAsts == null) return NO_SUCH_NODE;

      final var projAttrNames = join(groupColNames, aggColNames, groupColNames, aggColNames);
      final var projAttrExprs =
          map(join(groupRefAsts, aggRefAsts, groupRefAsts, aggRefAsts), Expression::mk);
      final ProjNode projNode = ProjNode.mk(false, projAttrNames, projAttrExprs);
      projNode.setQualification(associateSchemaQual(schemaDescOf(agg.schema()).schemaName));

      // Then build Agg node based on Proj. TODO qualification

      // final String aggFuncName = "count";
      final String aggFuncName = (agg.aggFuncKind() == AggFuncKind.UNKNOWN) ? "count" : agg.aggFuncKind().text();
      final SqlNode aggAst = mkAggregate(sql, aggRefAsts, aggFuncName);

      final String havingPredName = predDescOf(agg.havingPred()).predName;
      final SqlNode havingRefAst = copyAst(aggAst, sql);
      final SqlNode predAst = mkFuncCall(sql, havingPredName, singletonList(havingRefAst));
      final Expression havingExpr = Expression.mk(predAst);

      final var attrNames = concat(groupColNames, singletonList(getSynName(agg.aggregateAttrs())));
      final var attrExprs = map(concat(groupRefAsts, singletonList(aggAst)), Expression::mk);
      final var groupExprs = map(groupRefAsts, this::mkGroupItemExpr);
      final AggNode aggNode = AggNode.mk(agg.deduplicated(), attrNames, attrExprs, groupExprs, havingExpr);
      aggNode.setQualification(schemaDescOf(agg.schema()).schemaName);

      // Bind node id: aggNodeId -> projNodeId -> lhsChild
      final int projNodeId = plan.bindNode(projNode);
      final int aggNodeId = plan.bindNode(aggNode);
      instantiatedOps.put(agg, aggNode);
      plan.setChild(projNodeId, 0, lhsChild);
      plan.setChild(aggNodeId, 0, projNodeId);
      return aggNodeId;
    }

    private List<SqlNode> trAttrs(Symbol attrs, Op predecessor) {
      List<SqlNode> colRefs = new ArrayList<>(attrsDescOf(attrs).colNames.size());
      for (String colName : attrsDescOf(attrs).colNames) {
        final String qualification =
            findSourceIn(deepSourceOf(attrs, isAggregatedCol(colName)), predecessor);
        if (qualification == null) return null;
        colRefs.add(mkColRef(sql, qualification, colName));
      }
      return colRefs;
    }

    private SqlNode trAttrsSingle(Symbol attrs, Op predecessor) {
      final List<String> names = attrsDescOf(attrs).colNames;
      //      assert names.size() == 1;
      final String qualification =
          findSourceIn(deepSourceOf(attrs, isAggregatedCol(names.get(0))), predecessor);
      if (qualification == null) return null;
      return mkColRef(sql, qualification, names.get(0));
    }

    private Symbol deepSourceOf(Symbol attrs, boolean aggregatedCol) {
      if (isTargetSide) attrs = constraints.instantiationOf(attrs);
      Symbol source = constraints.sourceOf(attrs);
      while (source.kind() != Symbol.Kind.TABLE) {
        assert source.kind() == Symbol.Kind.SCHEMA;
        source = castSchema2Attrs(source, aggregatedCol);
      }
      return source;
    }

    private Symbol castSchema2Attrs(Symbol sym, boolean aggregatedCol) {
      final Op op = srcSyms.ownerOf(sym);
      assert op.kind() == OpKind.PROJ || op.kind() == OpKind.AGG;

      if (op.kind() == OpKind.PROJ) return constraints.sourceOf(((Proj) op).attrs());
      if (aggregatedCol) return constraints.sourceOf(((Agg) op).aggregateAttrs());
      else return constraints.sourceOf(((Agg) op).groupByAttrs());
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

    private Expression mkGroupItemExpr(SqlNode colRef) {
      final SqlNode groupItem = SqlNode.mk(sql, SqlKind.GroupItem);
      groupItem.$(GroupItem_Expr, copyAst(colRef, sql));
      return Expression.mk(groupItem);
    }

    private static JoinKind joinKindOf(Join join) {
      final OpKind kind = join.kind();
      if (kind == OpKind.LEFT_JOIN) return JoinKind.LEFT_JOIN;
      else if (kind == OpKind.INNER_JOIN) return JoinKind.INNER_JOIN;
      else throw new IllegalArgumentException("unsupported join kind: " + kind);
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

    private static boolean isAggregatedCol(String colName) {
      return colName.startsWith(SYN_NAME_PREFIX);
    }

    private static String associateSchemaQual(String schemaQual) {
      return schemaQual + "_";
    }
  }
}
