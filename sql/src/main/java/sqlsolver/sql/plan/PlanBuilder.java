package sqlsolver.sql.plan;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.SetOpKind;
import sqlsolver.sql.ast.constants.SetOpOption;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.Table;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlKind;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.common.utils.Commons.coalesce;
import static sqlsolver.common.utils.Commons.dumpException;
import static sqlsolver.common.utils.SetSupport.map;
import static sqlsolver.sql.SqlSupport.selectItemNameOf;
import static sqlsolver.sql.support.locator.LocatorSupport.gatherColRefs;

class PlanBuilder {
  private static final int FAIL = Integer.MIN_VALUE;

  private final SqlNode ast;
  private final Schema schema;
  private final PlanContext plan;
  private final ValuesRegistry valuesReg;
  private final SqlContext tmpCtx;
  private final NameSequence synNameSeq;

  private String error;

  PlanBuilder(SqlNode ast, Schema schema) {
    this.ast = requireNonNull(ast);
    this.schema = requireNonNull(coalesce(ast.context().schema(), schema));
    this.plan = PlanContext.mk(schema, 0);
    this.valuesReg = plan.valuesReg();
    this.tmpCtx = SqlContext.mk(8);
    this.synNameSeq = NameSequence.mkIndexed(PlanSupport.SYN_NAME_PREFIX, 0);
  }

  boolean build() {
    try {
      final int root = build0(ast);
      if (root == FAIL) return false;
      plan.setRoot(root);
      return true;
    } catch (RuntimeException ex) {
      error = dumpException(ex);
      return false;
    }
  }

  PlanContext plan() {
    return plan;
  }

  String lastError() {
    return error;
  }

  private int build0(SqlNode ast) {
    final SqlKind kind = ast.kind();

    switch (kind) {
      case TableSource:
        return buildTableSource(ast);
      case SetOp:
        return buildSetOp(ast);
      case Query: {
        int nodeId = build0(ast.$(SqlNodeFields.Query_Body));
        nodeId = buildSort(ast.$(SqlNodeFields.Query_OrderBy), nodeId);
        nodeId = buildLimit(ast.$(SqlNodeFields.Query_Limit), ast.$(SqlNodeFields.Query_Offset), nodeId);
        return nodeId;
      }
      case QuerySpec: {
        int nodeId = buildTableSource(ast.$(SqlNodeFields.QuerySpec_From));
        nodeId = buildFilters(ast.$(SqlNodeFields.QuerySpec_Where), nodeId);
        nodeId = buildProjection(ast, nodeId);
        return nodeId;
      }
      default:
        return onError(PlanSupport.FAILURE_INVALID_QUERY + ast);
    }
  }

  private int buildSetOp(SqlNode setOp) {
    final int lhs = build0(setOp.$(SqlNodeFields.SetOp_Left));
    final int rhs = build0(setOp.$(SqlNodeFields.SetOp_Right));

    if (lhs == FAIL || rhs == FAIL) return FAIL;

    final SetOpKind opKind = setOp.$(SqlNodeFields.SetOp_Kind);
    final boolean deduplicated = setOp.$(SqlNodeFields.SetOp_Option) == SetOpOption.DISTINCT;
    final SetOpNode setOpNode = SetOpNode.mk(deduplicated, opKind);

    final int nodeId = plan.bindNode(setOpNode);
    plan.setChild(nodeId, 0, lhs);
    plan.setChild(nodeId, 1, rhs);

    return nodeId;
  }

  private int buildProjection(SqlNode querySpec, int child) {
    if (child == FAIL) return FAIL;

    final SqlNodes items = querySpec.$(SqlNodeFields.QuerySpec_SelectItems);
    final SqlNodes groupBys = coalesce(querySpec.$(SqlNodeFields.QuerySpec_GroupBy), SqlNodes.mkEmpty());
    final SqlNode having = querySpec.$(SqlNodeFields.QuerySpec_Having);
    final boolean deduplicated = querySpec.isFlag(SqlNodeFields.QuerySpec_Distinct);

    final List<String> attrNames = new ArrayList<>(items.size());
    final List<Expression> attrExprs = new ArrayList<>(items.size());

    if (!containsAgg(items) && groupBys.isEmpty()) {
      mkAttrs(child, items, attrNames, attrExprs);
      final ProjNode proj = ProjNode.mk(deduplicated, attrNames, attrExprs);
      final int projNodeId = plan.bindNode(proj);
      if (child != NO_SUCH_NODE) plan.setChild(projNodeId, 0, child);

      // Build sub-query plans in projection attributes
      for (Expression attrExpr : attrExprs) {
        final PlanSubQueryBuilder subQueryBuilder = new PlanSubQueryBuilder(projNodeId);
        attrExpr.template().accept(subQueryBuilder);
      }

      return projNodeId;

    } else {
      /*
       We translate aggregation as Agg(Proj(..)).
       The inner Proj projects all the attributes used in aggregations.
       e.g., SELECT SUM(salary) FROM T GROUP BY dept HAVING MAX(age) > 40
          => Proj[salary]

       (Actually such statement is invalid in standard SQL,
        in which all the columns appear in GROUP BY must also appear in selection.
        This is a vendor-extension.)
      */

//      if (any(items, it1 -> it1.$(SelectItem_Expr).$(Aggregate_WindowSpec) != null))
//        return onError(FAILURE_UNSUPPORTED_FEATURE + "window function");

      // 1. build Agg node
      mkAttrs(child, items, attrNames, attrExprs);
      final List<Expression> groupByExprs = new ArrayList<>(groupBys.size());
      for (SqlNode groupBy : groupBys) groupByExprs.add(Expression.mk(groupBy));
      final Expression havingExpr = having == null ? null : Expression.mk(having);

      final AggNode agg = AggNode.mk(deduplicated, attrNames, attrExprs, groupByExprs, havingExpr);

      // 2. Extract column refs used in selectItems, groups and having
      final List<SqlNode> colRefs = new ArrayList<>(attrExprs.size() + groupBys.size() + 1);
      for (Expression attrExpr : attrExprs) colRefs.addAll(attrExpr.colRefs());
      final int numRefInProj = colRefs.size();
      colRefs.addAll(gatherColRefs(groupBys));
      if (having != null) colRefs.addAll(LocatorSupport.gatherColRefs(having));

      // 3. build Proj node
      final Set<String> aggItemNames = map(items, it -> it.$(SqlNodeFields.SelectItem_Alias));
      final ProjNode proj = mkForwardProj(colRefs, containsDeduplicatedAgg(items), aggItemNames, numRefInProj);

      // 4. assemble
      final int projNodeId = plan.bindNode(proj);
      final int aggNodeId = plan.bindNode(agg);
      if (child != NO_SUCH_NODE) plan.setChild(projNodeId, 0, child);
      plan.setChild(aggNodeId, 0, projNodeId);

      // FIXME: Build sub-query plans in projection attributes below an Aggregation
//      for (Expression attrExpr : attrExprs) {
//        final PlanSubQueryBuilder subQueryBuilder = new PlanSubQueryBuilder(projNodeId);
//        attrExpr.template().accept(subQueryBuilder);
//      }

      return aggNodeId;
    }
  }

  private int buildFilters(SqlNode expr, int child) {
    if (child == FAIL) return FAIL;
    if (expr == null) return child;

    final TIntList filters = buildFilters0(expr, new TIntArrayList(4));
    if (filters == null) return FAIL;
    if (filters.isEmpty()) return child;

    for (int i = 1, bound = filters.size(); i < bound; ++i)
      plan.setChild(filters.get(i), 0, filters.get(i - 1));
    if (child != NO_SUCH_NODE)
      plan.setChild(filters.get(0), 0, child);

    return filters.get(filters.size() - 1);
  }

  private TIntList buildFilters0(SqlNode expr, TIntList filters) {
    final BinaryOpKind opKind = expr.$(ExprFields.Binary_Op);
    if (opKind == BinaryOpKind.AND) {
      buildFilters0(expr.$(ExprFields.Binary_Left), filters);
      buildFilters0(expr.$(ExprFields.Binary_Right), filters);

    } else if (opKind == BinaryOpKind.IN_SUBQUERY) {
      final InSubNode filter = InSubNode.mk(Expression.mk(expr.$(ExprFields.Binary_Left)));
      final int subqueryId = build0(expr.$(ExprFields.Binary_Right).$(ExprFields.QueryExpr_Query));
      if (subqueryId == FAIL) return null;
      final int nodeId = plan.bindNode(filter);
      plan.setChild(nodeId, 1, subqueryId);
      filters.add(nodeId);

    } else if (ExprKind.Exists.isInstance(expr)) {
      final ExistsNode filter = ExistsNode.mk();
      final int subqueryId = build0(expr.$(ExprFields.Exists_Subquery).$(ExprFields.QueryExpr_Query));
      if (subqueryId == FAIL) return null;
      final int nodeId = plan.bindNode(filter);
      plan.setChild(nodeId, 1, subqueryId);
      filters.add(nodeId);

//    } else if (!PlanSupport.isBoolConstant(expr)){
      } else {
      // Preclude ones like "1=1".
      final SqlNode normalized = PlanSupport.normalizePredicate(expr, tmpCtx);
      final SimpleFilterNode filter = SimpleFilterNode.mk(Expression.mk(normalized));
      final int nodeId = plan.bindNode(filter);
      filters.add(nodeId);

      // Build sub-query plans in filter predicate
      final PlanSubQueryBuilder subQueryBuilder = new PlanSubQueryBuilder(nodeId);
      filter.predicate().template().accept(subQueryBuilder);
      if (!subQueryBuilder.success) return null;
    }

    return filters;
  }

  private class PlanSubQueryBuilder implements SqlVisitor {
    private final int parentNodeId;
    private int childIndex;
    private boolean success;

    protected PlanSubQueryBuilder(int parentNodeId) {
      this.parentNodeId = parentNodeId;
      this.childIndex = 1;
      this.success = true;
    }

    @Override
    public boolean enterQueryExpr(SqlNode queryExpr) {
      final SqlNode subQuery = queryExpr.$(ExprFields.QueryExpr_Query);
      final int subRoot = build0(subQuery);
      if (!success) return false;

      if (subRoot == FAIL) {
        success = false;
        onError(PlanSupport.FAILURE_INVALID_PLAN);
      } else {
        // For different parents, handle different cases
        if(plan.kindOf(parentNodeId) == PlanKind.Filter)
          plan.setChild(parentNodeId, childIndex++, subRoot);
        if(plan.kindOf(parentNodeId) == PlanKind.Proj)
          plan.setChild(parentNodeId, plan.nodeAt(parentNodeId).numChildren(plan), subRoot);
        plan.setSubQueryPlanRootId(subQuery.nodeId(), subRoot);
        plan.setSubQueryPlanRootIdBySqlNode(subQuery, subRoot);
      }
      return false;
    }
  }


  private int buildSort(SqlNodes orders, int child) {
    if (child == FAIL) return FAIL;
    if (orders == null || orders.isEmpty()) return child;

    final List<Expression> sortSpec = ListSupport.map(orders, Expression::mk);
    final SortNode sortNode = SortNode.mk(sortSpec);

    final int nodeId = plan.bindNode(sortNode);
    plan.setChild(nodeId, 0, child);

    return nodeId;
  }

  private int buildLimit(SqlNode limit, SqlNode offset, int child) {
    if (child == FAIL) return FAIL;
    if (limit == null && offset == null) return child;

    final LimitNode limitNode =
        LimitNode.mk(
            limit == null ? null : Expression.mk(limit),
            offset == null ? null : Expression.mk(offset));

    final int nodeId = plan.bindNode(limitNode);
    plan.setChild(nodeId, 0, child);

    return nodeId;
  }

  private int buildTableSource(SqlNode tableSource) {
    if (tableSource == null) return NO_SUCH_NODE;
    assert SqlKind.TableSource.isInstance(tableSource);
    return switch (tableSource.$(SqlNodeFields.TableSource_Kind)) {
      case SimpleSource -> buildSimpleTableSource(tableSource);
      case JoinedSource -> buildJoinedTableSource(tableSource);
      case DerivedSource -> buildDerivedTableSource(tableSource);
    };
  }

  private int buildSimpleTableSource(SqlNode tableSource) {
    final String tableName = tableSource.$(TableSourceFields.Simple_Table).$(SqlNodeFields.TableName_Table);
    final Table table = schema.table(tableName);

    if (table == null) return onError(PlanSupport.FAILURE_UNKNOWN_TABLE + tableSource);

    final String alias = coalesce(tableSource.$(TableSourceFields.Simple_Alias), tableName);
    return plan.bindNode(InputNode.mk(table, alias));
  }

  private int buildJoinedTableSource(SqlNode tableSource) {
    final int lhs = build0(tableSource.$(TableSourceFields.Joined_Left));
    final int rhs = build0(tableSource.$(TableSourceFields.Joined_Right));
    if (lhs == FAIL || rhs == FAIL) return FAIL;

    final SqlNode condition = tableSource.$(TableSourceFields.Joined_On);
    JoinKind joinKind = tableSource.$(TableSourceFields.Joined_Kind);
    if (condition != null && joinKind == JoinKind.CROSS_JOIN)
      joinKind = JoinKind.INNER_JOIN;

    final JoinNode joinNode = JoinNode.mk(joinKind, condition == null ? null : Expression.mk(condition));

    final int nodeId = plan.bindNode(joinNode);
    plan.setChild(nodeId, 0, lhs);
    plan.setChild(nodeId, 1, rhs);

    return nodeId;
  }

  private int buildDerivedTableSource(SqlNode tableSource) {
    final String alias = tableSource.$(TableSourceFields.Derived_Alias);

    if (alias == null) return onError(PlanSupport.FAILURE_MISSING_QUALIFICATION + tableSource);

    final int subquery = build0(tableSource.$(TableSourceFields.Derived_Subquery));
    if (subquery == FAIL) return FAIL;

    int qualifiedNodeId = subquery;
    while (!(plan.nodeAt(qualifiedNodeId) instanceof Qualified))
      qualifiedNodeId = plan.childOf(qualifiedNodeId, 0);
    ((Qualified) plan.nodeAt(qualifiedNodeId)).setQualification(alias);

    return subquery;
  }

  private int onError(String error) {
    this.error = error;
    return FAIL;
  }

  private static boolean containsDeduplicatedAgg(SqlNodes selectItem) {
    for (SqlNode item : selectItem)
      if (item.$(SqlNodeFields.SelectItem_Expr).isFlag(ExprFields.Aggregate_Distinct)) {
        return true;
      }
    return false;
  }

  private static boolean containsAgg(SqlNodes selectItems) {
    for (SqlNode item : selectItems)
      if (ExprKind.Aggregate.isInstance(item.$(SqlNodeFields.SelectItem_Expr))) {
        return true;
      }
    return false;
  }

  private void mkAttrs(
      int inputNodeId, SqlNodes selectItems, List<String> attrNames, List<Expression> attrExprs) {
    for (SqlNode item : selectItems) {
      final SqlNode exprAst = item.$(SqlNodeFields.SelectItem_Expr);
      if (ExprKind.Wildcard.isInstance(exprAst)) {
        expandWildcard(inputNodeId, exprAst, attrNames, attrExprs);
      } else {
        attrNames.add(mkAttrName(item));
        attrExprs.add(Expression.mk(exprAst));
      }
    }
  }

  private void expandWildcard(
      int inputNodeId, SqlNode wildcard, List<String> attrNames, List<Expression> attrExprs) {
    final String qualification;
    if (wildcard.$(ExprFields.Wildcard_Table) == null) qualification = null;
    else qualification= wildcard.$(ExprFields.Wildcard_Table).$(SqlNodeFields.TableName_Table);

    final Values values = valuesReg.valuesOf(inputNodeId);
    // A corner case is wildcard can refer to an anonymous attribute,
    // e.g., "Select * From (Select x.a + x.b From x) sub"
    // We synthesized a name for it (see mkAttrName). This leads to SQL:
    // "Select sub._anony0 From (Select x.a + x.b As _anony0 From x) sub".
    // This is okay when playing with plan, but actually the final output schema is changed.
    // We work around this issue in ToSqlTranslator.
    for (Value value : values) {
      if (qualification == null || (qualification.equals(value.qualification()))) {
        attrNames.add(value.name());
        attrExprs.add(PlanSupport.mkColRefExpr(value));
      }
    }
  }

  private String mkAttrName(SqlNode selectItem) {
    final String alias = selectItemNameOf(selectItem);
    // For some derived select-item, like 'salary / age'.
    return alias != null ? alias : synNameSeq.next();
  }

  private ProjNode mkForwardProj(List<SqlNode> colRefs, boolean deduplicated,
                                 Set<String> aggItemNames, int numRefsInProj) {
    final List<String> attrNames = new ArrayList<>(colRefs.size());
    final List<Expression> attrExprs = new ArrayList<>(colRefs.size());
    final NameSequence seq = NameSequence.mkIndexed("agg", 0);

    for (int i = 0, bound = colRefs.size(); i < bound; i++) {
      final SqlNode colRef = colRefs.get(i);
      if (i >= numRefsInProj) {
        final SqlNode colName = colRef.$(ExprFields.ColRef_ColName);
        final String qualification = colName.$(SqlNodeFields.ColName_Table);
        final String refName = colName.$(SqlNodeFields.ColName_Col);
        if (qualification == null && aggItemNames.contains(refName)) continue;
      }

      attrNames.add(seq.next());
      attrExprs.add(Expression.mk(colRef));
    }

    return ProjNode.mk(deduplicated, attrNames, attrExprs);
  }
}
