package sqlsolver.sql.plan;

import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.SetOpOption;
import sqlsolver.sql.ast.*;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sqlsolver.common.utils.Commons.isNullOrEmpty;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.common.utils.ListSupport.zipMap;

class ToAstTranslator {
  private final PlanContext plan;
  private final SqlContext sql;
  private boolean allowIncomplete;
  private String lastError;

  ToAstTranslator(PlanContext plan) {
    this.plan = plan;
    this.sql = SqlContext.mk(32);
    sql.setSchema(plan.schema());
    sql.setDbType(plan.schema().dbType());
  }

  SqlNode translate(int nodeId, boolean allowIncomplete) {
    this.allowIncomplete = allowIncomplete;

    final QueryBuilder builder = onNode(nodeId);
    if (builder.isInvalid()) return null;
    else return builder.asQuery();
  }

  String lastError() {
    return lastError;
  }

  private QueryBuilder onNode(int node) {
    switch (plan.kindOf(node)) {
      case Input:
        return onInput(node);
      case Join:
        return onJoin(node);
      case Filter:
        return onFilter(node);
      case InSub:
        return onInSub(node);
      case Exists:
        return onExists(node);
      case Proj:
        return onProj(node);
      case Agg:
        return onAgg(node);
      case Sort:
        return onSort(node);
      case Limit:
        return onLimit(node);
      case SetOp:
        return onSetOp(node);
      default:
        throw new IllegalArgumentException("unknown op kind: " + plan.kindOf(node));
    }
  }

  private QueryBuilder onInput(int nodeId) {
    final InputNode input = (InputNode) plan.nodeAt(nodeId);
    final SqlNode tableSource = SqlSupport.mkSimpleSource(sql, input.table().name(), input.qualification());
    return mkBuilder().setSource(tableSource).setQualification(input.qualification());
  }

  private QueryBuilder onJoin(int nodeId) {
    final SqlNode tableSource0 = onNode(plan.childOf(nodeId, 0)).asTableSource();
    final SqlNode tableSource1 = onNode(plan.childOf(nodeId, 1)).asTableSource();
    if (tableSource0 == null || tableSource1 == null) return QueryBuilder.INVALID;

    final JoinNode join = (JoinNode) plan.nodeAt(nodeId);
    final JoinKind joinKind = PlanSupport.joinKindOf(plan, nodeId);
    final SqlNode joinNode =
        SqlSupport.mkJoinSource(sql, tableSource0, tableSource1, mkExpr(join.joinCond()), joinKind);

    return mkBuilder().setSource(joinNode);
  }

  private QueryBuilder onFilter(int nodeId) {
    final QueryBuilder query = onNode(plan.childOf(nodeId, 0));
    if (query.isInvalid()) return QueryBuilder.INVALID;

    final SimpleFilterNode filter = (SimpleFilterNode) plan.nodeAt(nodeId);
    return query.pushFilter(mkExpr(filter.predicate()));
  }

  private QueryBuilder onInSub(int nodeId) {
    final QueryBuilder query = onNode(plan.childOf(nodeId, 0));
    if (query.isInvalid()) return QueryBuilder.INVALID;

    final SqlNode subqueryNode = onNode(plan.childOf(nodeId, 1)).asQueryExpr();
    if (subqueryNode == null) return QueryBuilder.INVALID;

    final InSubNode inSub = (InSubNode) plan.nodeAt(nodeId);
    final SqlNode filterNode = SqlSupport.mkBinary(sql, BinaryOpKind.IN_SUBQUERY, mkExpr(inSub.expr()), subqueryNode);
    return query.pushFilter(filterNode);
  }

  private QueryBuilder onExists(int nodeId) {
    final QueryBuilder query = onNode(plan.childOf(nodeId, 0));
    if (query.isInvalid()) return QueryBuilder.INVALID;

    final SqlNode subqueryNode = onNode(plan.childOf(nodeId, 1)).asQueryExpr();
    if (subqueryNode == null) return QueryBuilder.INVALID;

    final SqlNode existsNode = SqlNode.mk(sql, ExprKind.Exists);
    existsNode.$(ExprFields.Exists_Subquery, subqueryNode);
    return query.pushFilter(existsNode);
  }

  private QueryBuilder onProj(int nodeId) {
    final QueryBuilder query = onNode(plan.childOf(nodeId, 0));
    final ProjNode proj = (ProjNode) plan.nodeAt(nodeId);
    final List<SqlNode> selectItems = zipMap(proj.attrExprs(), proj.attrNames(), this::mkSelection);
    return query
        .setSelectItems(selectItems, false)
        .setQualification(mkQualification(nodeId))
        .setDeduplicated(PlanSupport.isDedup(plan, nodeId));
  }

  private QueryBuilder onAgg(int nodeId) {
    final QueryBuilder query = onNode(plan.childOf(nodeId, 0));
    if (query.isInvalid()) return QueryBuilder.INVALID;

    final AggNode agg = (AggNode) plan.nodeAt(nodeId);
    final List<SqlNode> selectItems = zipMap(agg.attrExprs(), agg.attrNames(), this::mkSelection);
    final List<SqlNode> groupBys = map(agg.groupByExprs(), this::mkExpr);
    final SqlNode having = mkExpr(agg.havingExpr());

    return query
        .setSelectItems(selectItems, true)
        .setGroupBy(groupBys, having)
        .setQualification(mkQualification(nodeId));
    // Agg's deduplication should be specified by the placeholder Proj.
  }

  private QueryBuilder onSort(int nodeId) {
    final QueryBuilder query = onNode(plan.childOf(nodeId, 0));
    if (query.isInvalid()) return QueryBuilder.INVALID;

    final SortNode sort = (SortNode) plan.nodeAt(nodeId);
    final List<SqlNode> exprs = map(sort.sortSpec(), this::mkExpr);
    return query.setOrderBy(exprs);
  }

  private QueryBuilder onLimit(int nodeId) {
    final QueryBuilder query = onNode(plan.childOf(nodeId, 0));
    if (query.isInvalid()) return QueryBuilder.INVALID;

    final LimitNode limit = (LimitNode) plan.nodeAt(nodeId);
    return query.setLimit(mkExpr(limit.limit()), mkExpr(limit.offset()));
  }

  private QueryBuilder onSetOp(int nodeId) {
    final SqlNode q0 = onNode(plan.childOf(nodeId, 0)).asQuery();
    final SqlNode q1 = onNode(plan.childOf(nodeId, 1)).asQuery();
    if (q0 == null || q1 == null) return QueryBuilder.INVALID;

    final SetOpNode setOp = (SetOpNode) plan.nodeAt(nodeId);
    final SqlNode unionNode = SqlSupport.mkSetOp(sql, q0, q1, setOp.opKind());
    unionNode.$(SqlNodeFields.SetOp_Option, setOp.deduplicated() ? SetOpOption.DISTINCT : SetOpOption.ALL);
    return mkBuilder().setSource(SqlSupport.mkQuery(sql, unionNode)).setQualification(mkQualification(nodeId));
  }

  private QueryBuilder mkBuilder() {
    return new QueryBuilder(this);
  }

  private SqlNode mkSelection(Expression expr, String name) {
    final String alias = name.startsWith(PlanSupport.SYN_NAME_PREFIX) ? null : name;
    return SqlSupport.mkSelectItem(sql, mkExpr(expr), alias);
  }

  private SqlNode mkExpr(Expression expr) {
    if (expr == null) return null;
    final Values values = plan.valuesReg().valueRefsOf(expr);
    return expr.interpolate(sql, values);
  }

  private SqlNode onError(String err) {
    this.lastError = err;
    return null;
  }

  private String mkQualification(int nodeId) {
    final PlanNode node = plan.nodeAt(nodeId);
    if (node instanceof Exporter) {
      final Exporter exporter = (Exporter) node;
      if (exporter.qualification() != null) return exporter.qualification();
    }

    final Values values = plan.valuesReg().valuesOf(nodeId);
    String guess = null;
    for (Value value : values) {
      final String qualification = value.qualification();
      if (qualification != null)
        if (guess == null) guess = qualification;
        else if (!guess.equals(qualification)) return null;
    }

    return guess;
  }

  private static class QueryBuilder {
    private static final QueryBuilder INVALID = new QueryBuilder(null);

    private final ToAstTranslator translator;

    private SqlNode tableSource = null;
    private List<SqlNode> filters = null;
    private List<SqlNode> selectItems = null;
    private List<SqlNode> groupBys = null;
    private List<SqlNode> orderBys = null;
    private SqlNode having = null;
    private SqlNode limit = null;
    private SqlNode offset = null;
    private String qualification = null;
    private boolean deduplicated = false;
    private boolean isAgg = false;

    private QueryBuilder(ToAstTranslator translator) {
      this.translator = translator;
    }

    boolean isInvalid() {
      return this == INVALID;
    }

    SqlNode asTableSource() {
      assert !isInvalid();

      if (isPureSource() && SqlKind.TableSource.isInstance(tableSource)) return tableSource;
      if (qualification == null) return translator.onError(PlanSupport.FAILURE_MISSING_QUALIFICATION);
      return SqlSupport.mkDerivedSource(translator.sql, asQuery(), qualification);
    }

    SqlNode asQuery() {
      assert !isInvalid();

      final SqlContext sql = translator.sql;

      final SqlNode q;
      if (SqlKind.Query.isInstance(tableSource)) {
        assert filters == null && selectItems == null && groupBys == null && having == null;
        assert !deduplicated && !isAgg;
        q = tableSource;
      } else {
        if (selectItems == null && !translator.allowIncomplete)
          return translator.onError(PlanSupport.FAILURE_MISSING_PROJECTION);

        final SqlNode spec = SqlNode.mk(sql, SqlKind.QuerySpec);

        if (tableSource != null) spec.$(SqlNodeFields.QuerySpec_From, tableSource);
        if (!isNullOrEmpty(filters)) spec.$(SqlNodeFields.QuerySpec_Where, SqlSupport.mkConjunction(sql, filters));
        if (!isNullOrEmpty(groupBys)) spec.$(SqlNodeFields.QuerySpec_GroupBy, SqlNodes.mk(sql, groupBys));
        if (having != null) spec.$(SqlNodeFields.QuerySpec_Having, having);

        final SqlNodes selections;
        if (selectItems != null) selections = SqlNodes.mk(sql, selectItems);
        else selections = SqlNodes.mk(sql, singletonList(SqlSupport.mkWildcard(sql, null)));
        spec.$(SqlNodeFields.QuerySpec_SelectItems, selections);

        if (!isAgg && deduplicated) spec.$(SqlNodeFields.QuerySpec_Distinct, true);
        if (isAgg && selectItems != null) {
          for (SqlNode selectItem : selectItems) {
            final SqlNode expr = selectItem.$(SqlNodeFields.SelectItem_Expr);
            if (ExprKind.Aggregate.isInstance(expr)) expr.flag(ExprFields.Aggregate_Distinct, deduplicated);
          }
        }

        q = SqlSupport.mkQuery(sql, spec);
      }

      if (orderBys != null) q.$(SqlNodeFields.Query_OrderBy, SqlNodes.mk(sql, orderBys));
      if (offset != null) q.$(SqlNodeFields.Query_Offset, offset);
      if (limit != null) q.$(SqlNodeFields.Query_Limit, limit);

      return q;
    }

    SqlNode asQueryExpr() {
      final SqlNode q = asQuery();
      return q == null ? null : SqlSupport.mkQueryExpr(translator.sql, q);
    }

    QueryBuilder setSource(SqlNode tableSource) {
      if (isInvalid()) return INVALID;

      assert this.tableSource == null;
      assert SqlKind.TableSource.isInstance(tableSource) || SqlKind.Query.isInstance(tableSource);

      this.tableSource = tableSource;
      return this;
    }

    QueryBuilder pushFilter(SqlNode filter) {
      if (isInvalid()) return INVALID;
      if (isFullQuery() && !collapseAsTableSource()) return INVALID;

      if (filters == null) filters = new LinkedList<>();
      filters.add(filter);
      return this;
    }

    QueryBuilder setSelectItems(List<SqlNode> selectItems, boolean isAgg) {
      if (isInvalid()) return INVALID;
      if (isAgg && (this.isAgg || this.selectItems == null)) return INVALID;
      if (!isAgg && isFullQuery() && !collapseAsTableSource()) return INVALID;

      assert !this.isAgg;
      assert isAgg || this.selectItems == null;

      this.isAgg = isAgg;
      this.selectItems = selectItems;
      return this;
    }

    QueryBuilder setQualification(String qualification) {
      if (isInvalid()) return INVALID;
      if (qualification == null) return this;
      this.qualification = qualification;
      return this;
    }

    QueryBuilder setDeduplicated(boolean deduplicated) {
      if (isInvalid()) return INVALID;

      assert !this.isAgg;
      assert this.selectItems != null;

      this.deduplicated = deduplicated;
      return this;
    }

    QueryBuilder setGroupBy(List<SqlNode> groupBys, SqlNode having) {
      if (isInvalid()) return INVALID;
      if (this.groupBys != null) return INVALID;

      assert this.selectItems != null && this.isAgg;
      this.groupBys = groupBys;
      this.having = having;
      return this;
    }

    QueryBuilder setOrderBy(List<SqlNode> orderBys) {
      if (isInvalid()) return INVALID;

      assert this.selectItems != null || SqlKind.Query.isInstance(tableSource);

      if (this.orderBys != null && !collapseAsQuery()) return INVALID;
      assert this.orderBys == null;

      this.orderBys = orderBys;
      return this;
    }

    QueryBuilder setLimit(SqlNode limit, SqlNode offset) {
      if (isInvalid()) return INVALID;

      assert this.selectItems != null || SqlKind.Query.isInstance(tableSource);

      if ((this.limit != null || this.offset != null) && !collapseAsQuery()) return INVALID;
      assert this.limit == null && this.offset == null;

      this.limit = limit;
      this.offset = offset;
      return this;
    }

    private void init() {
      tableSource = null;
      filters = null;
      selectItems = null;
      groupBys = null;
      orderBys = null;
      having = null;
      limit = null;
      offset = null;
      qualification = null;
      deduplicated = false;
      isAgg = false;
    }

    private boolean collapseAsTableSource() {
      final SqlNode tableSource = asTableSource();
      if (tableSource == null) return false;
      init();
      this.tableSource = tableSource;
      return true;
    }

    private boolean collapseAsQuery() {
      final SqlNode query = asQuery();
      if (query == null) return false;
      init();
      this.tableSource = query;
      return true;
    }

    private boolean isPureSource() {
      return filters == null
          && selectItems == null
          && groupBys == null
          && orderBys == null
          && having == null
          && limit == null
          && offset == null;
    }

    private boolean isFullQuery() {
      return SqlKind.Query.isInstance(tableSource) || selectItems != null;
    }
  }
}
