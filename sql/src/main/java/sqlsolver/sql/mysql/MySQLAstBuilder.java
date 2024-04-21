package sqlsolver.sql.mysql;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.common.utils.Commons;
import sqlsolver.common.utils.IterableSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.*;
import sqlsolver.sql.parser.AstBuilderMixin;
import sqlsolver.sql.mysql.internal.MySQLParser;
import sqlsolver.sql.mysql.internal.MySQLParserBaseVisitor;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sqlsolver.common.utils.Commons.assertFalse;
import static sqlsolver.common.utils.Commons.unquoted;
import static sqlsolver.common.utils.FuncSupport.func;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.sql.ast.constants.BinaryOpKind.*;
import static sqlsolver.sql.ast.constants.ConstraintKind.*;
import static sqlsolver.sql.ast.constants.IndexKind.FULLTEXT;
import static sqlsolver.sql.ast.constants.IndexKind.SPATIAL;
import static sqlsolver.sql.ast.constants.UnaryOpKind.BINARY;
import static sqlsolver.sql.ast.constants.UnaryOpKind.NOT;

class MySQLAstBuilder extends MySQLParserBaseVisitor<SqlNode> implements AstBuilderMixin {
  private final SqlContext ast;

  MySQLAstBuilder() {
    this.ast = SqlContext.mk(32);
  }

  @Override
  public SqlContext ast() {
    return ast;
  }

  @Override
  protected SqlNode aggregateResult(SqlNode aggregate, SqlNode nextResult) {
    return Commons.coalesce(aggregate, nextResult);
  }

  @Override
  public SqlNode visitAlterTable(MySQLParser.AlterTableContext ctx) {
    final SqlNode alter = mkNode(SqlKind.AlterTable);
    final SqlNode tableName = visitTableRef(ctx.tableRef());

    final List<SqlNode> actions =
        map(
            (Iterable<MySQLParser.AlterListItemContext>)
                ctx.alterTableActions().alterCommandList().alterList().alterListItem(),
            (Function<? super MySQLParser.AlterListItemContext, ? extends SqlNode>)
                this::visitAlterListItem);

    alter.$(SqlNodeFields.AlterTable_Name, tableName);
    alter.$(SqlNodeFields.AlterTable_Actions, mkNodes(actions));

    return alter;
  }

  @Override
  public SqlNode visitAlterListItem(MySQLParser.AlterListItemContext ctx) {
    if (ctx.tableConstraintDef() != null) {
      final SqlNode action = mkNode(SqlKind.AlterTableAction);

      action.$(SqlNodeFields.AlterTableAction_Name, "add_constraint");
      action.$(SqlNodeFields.AlterTableAction_Payload, visitTableConstraintDef(ctx.tableConstraintDef()));
      return action;

    } else if (ctx.MODIFY_SYMBOL() != null) {
      final SqlNode action = mkNode(SqlKind.AlterTableAction);
      final SqlNode colDef = mkNode(SqlKind.ColDef);
      final SqlNode colName = mkNode(SqlKind.ColName);

      colName.$(SqlNodeFields.ColName_Col, MySQLAstHelper.stringifyIdentifier(ctx.columnInternalRef().identifier()));
      colDef.$(SqlNodeFields.ColDef_Name, colName);
      parseFieldDefinition(ctx.fieldDefinition(), colDef);

      action.$(SqlNodeFields.AlterTableAction_Name, "modify_column");
      action.$(SqlNodeFields.AlterTableAction_Payload, colDef);
      return action;
    }
    return null;
  }

  @Override
  public SqlNode visitTableName(MySQLParser.TableNameContext tableName) {
    return parseTableName(tableName.qualifiedIdentifier(), tableName.dotIdentifier());
  }

  @Override
  public SqlNode visitTableRef(MySQLParser.TableRefContext tableName) {
    return parseTableName(tableName.qualifiedIdentifier(), tableName.dotIdentifier());
  }

  @Override
  public SqlNode visitColumnName(MySQLParser.ColumnNameContext ctx) {
    final SqlNode node = mkNode(SqlKind.ColName);
    final String schema, table, column;

    if (ctx.identifier() != null) {
      schema = null;
      table = null;
      column = MySQLAstHelper.stringifyIdentifier(ctx.identifier());

    } else if (ctx.fieldIdentifier() != null) {
      final String[] triple = MySQLAstHelper.stringifyIdentifier(ctx.fieldIdentifier());
      schema = triple[0];
      table = triple[1];
      column = triple[2];

    } else {
      return assertFalse();
    }

    node.$(SqlNodeFields.ColName_Schema, schema);
    node.$(SqlNodeFields.ColName_Table, table);
    node.$(SqlNodeFields.ColName_Col, column);

    return node;
  }

  @Override
  public SqlNode visitCreateTable(MySQLParser.CreateTableContext ctx) {
    if (ctx.tableElementList() == null) return null;

    final SqlNode node = mkNode(SqlKind.CreateTable);
    node.$(SqlNodeFields.CreateTable_Name, visitTableName(ctx.tableName()));

    final List<SqlNode> columnDefs = new ArrayList<>();
    final List<SqlNode> constraintDefs = new ArrayList<>();

    for (var element : ctx.tableElementList().tableElement()) {
      final var colDefs = element.columnDefinition();
      final var consDefs = element.tableConstraintDef();

      if (colDefs != null) columnDefs.add(visitColumnDefinition(colDefs));
      else if (consDefs != null) constraintDefs.add(visitTableConstraintDef(consDefs));
      else return assertFalse();
    }

    node.$(SqlNodeFields.CreateTable_Cols, mkNodes(columnDefs));
    node.$(SqlNodeFields.CreateTable_Cons, mkNodes(constraintDefs));

    final var tableOptions = ctx.createTableOptions();
    if (tableOptions != null)
      for (var option : tableOptions.createTableOption())
        if (option.ENGINE_SYMBOL() != null) {
          node.$(SqlNodeFields.CreateTable_Engine, MySQLAstHelper.stringifyText(option.engineRef().textOrIdentifier()));
          break;
        }

    return node;
  }

  @Override
  public SqlNode visitColumnDefinition(MySQLParser.ColumnDefinitionContext ctx) {
    final SqlNode node = mkNode(SqlKind.ColDef);
    node.$(SqlNodeFields.ColDef_Name, visitColumnName(ctx.columnName()));

    parseFieldDefinition(ctx.fieldDefinition(), node);

    final var checkOrRef = ctx.checkOrReferences();
    if (checkOrRef != null)
      if (checkOrRef.references() != null)
        node.$(SqlNodeFields.ColDef_Ref, visitReferences(checkOrRef.references()));
      else if (checkOrRef.checkConstraint() != null) node.flag(SqlNodeFields.ColDef_Cons, CHECK);

    return node;
  }

  @Override
  public SqlNode visitReferences(MySQLParser.ReferencesContext ctx) {
    final SqlNode node = mkNode(SqlKind.Reference);

    node.$(SqlNodeFields.Reference_Table, visitTableRef(ctx.tableRef()));

    final var idList = ctx.identifierListWithParentheses();
    if (idList != null) {
      final var ids = idList.identifierList().identifier();
      final List<SqlNode> columns = new ArrayList<>(ids.size());

      for (var id : ids) {
        final SqlNode columnRef = mkNode(SqlKind.ColName);
        columnRef.$(SqlNodeFields.ColName_Col, MySQLAstHelper.stringifyIdentifier(id));
        columns.add(columnRef);
      }

      node.$(SqlNodeFields.Reference_Cols, mkNodes(columns));
    }

    return node;
  }

  @Override
  public SqlNode visitTableConstraintDef(MySQLParser.TableConstraintDefContext ctx) {
    final SqlNode node = mkNode(SqlKind.IndexDef);
    final Token type = ctx.type;
    if (type == null) return null;

    final var indexNameAndType = ctx.indexNameAndType();
    final var indexName = ctx.indexName();
    final var indexOptions = ctx.indexOption();

    final ConstraintKind c;
    final IndexKind t;
    final String name;
    switch (type.getText().toLowerCase()) {
      case "key":
      case "index":
        c = null;
        t = Commons.coalesce(MySQLAstHelper.parseIndexKind(indexNameAndType), MySQLAstHelper.parseIndexKind(indexOptions));
        name = MySQLAstHelper.stringifyIndexName(indexNameAndType);
        break;

      case "fulltext":
        c = null;
        t = FULLTEXT;
        name = MySQLAstHelper.stringifyIndexName(indexName);
        break;

      case "spatial":
        c = null;
        t = SPATIAL;
        name = MySQLAstHelper.stringifyIndexName(indexName);
        break;

      case "primary":
        c = PRIMARY;
        t = Commons.coalesce(MySQLAstHelper.parseIndexKind(indexNameAndType), MySQLAstHelper.parseIndexKind(indexOptions));
        name = MySQLAstHelper.stringifyIndexName(indexNameAndType);
        break;

      case "unique":
        c = UNIQUE;
        t = Commons.coalesce(MySQLAstHelper.parseIndexKind(indexNameAndType), MySQLAstHelper.parseIndexKind(indexOptions));
        name = MySQLAstHelper.stringifyIndexName(indexNameAndType);
        break;

      case "foreign":
        c = FOREIGN;
        t = null;
        name = MySQLAstHelper.stringifyIndexName(indexName);
        break;

      default:
        return assertFalse();
    }

    node.$(SqlNodeFields.IndexDef_Cons, c);
    node.$(SqlNodeFields.IndexDef_Kind, t);
    node.$(SqlNodeFields.IndexDef_Name, name);

    final var keyListVariants = ctx.keyListVariants();
    final var keyList = keyListVariants != null ? keyListVariants.keyList() : ctx.keyList();
    final List<SqlNode> keys;

    if (keyList != null) {
      keys =
          map(
              (Iterable<MySQLParser.KeyPartContext>) keyList.keyPart(),
              (Function<? super MySQLParser.KeyPartContext, ? extends SqlNode>) this::visitKeyPart);
    } else if (keyListVariants != null) {
      keys =
          map(
              (Iterable<MySQLParser.KeyPartOrExpressionContext>)
                  keyListVariants.keyListWithExpression().keyPartOrExpression(),
              (Function<? super MySQLParser.KeyPartOrExpressionContext, ? extends SqlNode>)
                  this::visitKeyPartOrExpression);
    } else {
      return assertFalse();
    }

    node.$(SqlNodeFields.IndexDef_Keys, mkNodes(keys));

    final var references = ctx.references();
    if (references != null) node.$(SqlNodeFields.IndexDef_Refs, visitReferences(references));

    return node;
  }

  @Override
  public SqlNode visitKeyPart(MySQLParser.KeyPartContext ctx) {
    final SqlNode node = mkNode(SqlKind.KeyPart);
    node.$(SqlNodeFields.KeyPart_Col, MySQLAstHelper.stringifyIdentifier(ctx.identifier()));
    node.$(SqlNodeFields.KeyPart_Direction, MySQLAstHelper.parseDirection(ctx.direction()));
    if (ctx.fieldLength() != null) node.$(SqlNodeFields.KeyPart_Len, MySQLAstHelper.fieldLength2Int(ctx.fieldLength()));
    return node;
  }

  @Override
  public SqlNode visitKeyPartOrExpression(MySQLParser.KeyPartOrExpressionContext ctx) {
    if (ctx.keyPart() != null) return visitKeyPart(ctx.keyPart());
    else if (ctx.exprWithParentheses() != null) {
      final SqlNode node = mkNode(SqlKind.KeyPart);

      node.$(SqlNodeFields.KeyPart_Expr, toExpr(ctx.exprWithParentheses().expr()));
      if (ctx.direction() != null) node.$(SqlNodeFields.KeyPart_Direction, MySQLAstHelper.parseDirection(ctx.direction()));

      return node;
    } else return null;
  }

  @Override
  public SqlNode visitQueryExpression(MySQLParser.QueryExpressionContext ctx) {
    // currently WITH is not supported
    final SqlNode query = mkNode(SqlKind.Query);
    if (ctx.withClause() != null) return null;

    if (ctx.queryExpressionBody() != null) {
      query.$(SqlNodeFields.Query_Body, ctx.queryExpressionBody().accept(this));
    } else if (ctx.queryExpressionParens() != null) {
      query.$(SqlNodeFields.Query_Body, ctx.queryExpressionParens().accept(this));
    }

    final var orderByClause = ctx.orderClause();
    if (orderByClause != null) query.$(SqlNodeFields.Query_OrderBy, mkNodes(toOrderItems(orderByClause)));

    final var limitClause = ctx.limitClause();
    if (limitClause != null) {
      final var limitOptions = limitClause.limitOptions().limitOption();
      if (limitOptions.size() == 1) {
        query.$(SqlNodeFields.Query_Limit, limitOptions.get(0).accept(this));

      } else if (limitOptions.size() == 2) {
        if (limitClause.limitOptions().OFFSET_SYMBOL() != null) {
          query.$(SqlNodeFields.Query_Offset, limitOptions.get(1).accept(this));
          query.$(SqlNodeFields.Query_Limit, limitOptions.get(0).accept(this));
        } else {
          query.$(SqlNodeFields.Query_Offset, limitOptions.get(0).accept(this));
          query.$(SqlNodeFields.Query_Limit, limitOptions.get(1).accept(this));
        }
      }
    }

    return query;
  }

  @Override
  public SqlNode visitLimitOption(MySQLParser.LimitOptionContext ctx) {
    if (ctx.PARAM_MARKER() != null) return mkExpr(ExprKind.Param);
    if (ctx.ULONGLONG_NUMBER() != null)
      return mkLiteral(LiteralKind.LONG, Long.parseLong(ctx.ULONGLONG_NUMBER().getText()));
    if (ctx.LONG_NUMBER() != null)
      return mkLiteral(LiteralKind.LONG, Long.parseLong(ctx.LONG_NUMBER().getText()));
    if (ctx.INT_NUMBER() != null)
      return mkLiteral(LiteralKind.INTEGER, Integer.parseInt(ctx.INT_NUMBER().getText()));
    if (ctx.identifier() != null) {
      final SqlNode variable = mkExpr(ExprKind.Variable);
      variable.$(ExprFields.Variable_Name, MySQLAstHelper.stringifyIdentifier(ctx.identifier()));
    }
    return null;
  }

  @Override
  public SqlNode visitQueryExpressionBody(MySQLParser.QueryExpressionBodyContext ctx) {
    if (ctx.setOp() == null) return ctx.querySpecification().accept(this);

    final SqlNode left, right;
    if (ctx.queryExpressionBody() != null) {
      left = ctx.queryExpressionBody().accept(this);
      if (ctx.querySpecification() != null) right = ctx.querySpecification().accept(this);
      else right = ctx.queryExpressionParens(0).accept(this);

    } else if (ctx.queryExpressionParens(0) != null) {
      left = ctx.queryExpressionParens(0).accept(this);
      if (ctx.querySpecification() != null) right = ctx.querySpecification().accept(this);
      else right = ctx.queryExpressionParens(1).accept(this);
    } else return assertFalse();

    final SetOpOption option =
        ctx.unionOption() == null
            ? SetOpOption.DISTINCT
            : SetOpOption.valueOf(ctx.unionOption().getText().toUpperCase());

    final SqlNode node = mkNode(SqlKind.SetOp);
    node.$(SqlNodeFields.SetOp_Kind, SetOpKind.valueOf(ctx.setOp().getText().toUpperCase()));
    node.$(SqlNodeFields.SetOp_Left, wrapAsQuery(left));
    node.$(SqlNodeFields.SetOp_Right, wrapAsQuery(right));
    node.$(SqlNodeFields.SetOp_Option, option);

    return node;
  }

  @Override
  public SqlNode visitQuerySpecification(MySQLParser.QuerySpecificationContext ctx) {
    final SqlNode node = mkNode(SqlKind.QuerySpec);

    if (ctx.selectOption() != null
        && IterableSupport.linearFind(
                ctx.selectOption(), it -> "DISTINCT".equalsIgnoreCase(it.getText()))
            != null) node.flag(SqlNodeFields.QuerySpec_Distinct);

    final var selectItemList = ctx.selectItemList();
    final List<SqlNode> items = new ArrayList<>(selectItemList.selectItem().size() + 1);
    if (selectItemList.MULT_OPERATOR() != null) items.add(mkSelectItem(mkExpr(ExprKind.Wildcard), null));
    items.addAll(map(selectItemList.selectItem(), this::visitSelectItem));

    node.$(SqlNodeFields.QuerySpec_SelectItems, mkNodes(items));

    final var fromClause = ctx.fromClause();
    if (fromClause != null) {
      final SqlNode from = visitFromClause(fromClause);
      if (from != null) node.$(SqlNodeFields.QuerySpec_From, from);
    }

    final var whereClause = ctx.whereClause();
    if (whereClause != null) node.$(SqlNodeFields.QuerySpec_Where, toExpr(whereClause.expr()));

    final var groupByClause = ctx.groupByClause();
    if (groupByClause != null) {
      final OLAPOption olapOption = MySQLAstHelper.parseOLAPOption(groupByClause.olapOption());
      if (olapOption != null) node.$(SqlNodeFields.QuerySpec_OlapOption, olapOption);
      node.$(SqlNodeFields.QuerySpec_GroupBy, mkNodes(toGroupItems(groupByClause.orderList())));
    }

    final var havingClause = ctx.havingClause();
    if (havingClause != null) node.$(SqlNodeFields.QuerySpec_Having, toExpr(havingClause.expr()));

    final var windowClause = ctx.windowClause();
    if (windowClause != null)
      node.$(
          SqlNodeFields.QuerySpec_Windows,
          mkNodes(map(windowClause.windowDefinition(), this::visitWindowDefinition)));

    return node;
  }

  @Override
  public SqlNode visitFromClause(MySQLParser.FromClauseContext ctx) {
    if (ctx.DUAL_SYMBOL() != null) return null;

    return ctx.tableReferenceList().tableReference().stream()
        .map(this::visitTableReference)
        .reduce((l, r) -> mkJoined(l, r, JoinKind.CROSS_JOIN))
        .orElse(null);
  }

  @Override
  public SqlNode visitSingleTable(MySQLParser.SingleTableContext ctx) {
    final SqlNode node = mkTableSource(TableSourceKind.SimpleSource);
    node.$(TableSourceFields.Simple_Table, visitTableRef(ctx.tableRef()));

    if (ctx.usePartition() != null) {
      final var identifiers =
          ctx.usePartition().identifierListWithParentheses().identifierList().identifier();
      node.$(TableSourceFields.Simple_Partition, map(identifiers, MySQLAstHelper::stringifyIdentifier));
    }

    if (ctx.tableAlias() != null)
      node.$(TableSourceFields.Simple_Alias, MySQLAstHelper.stringifyIdentifier(ctx.tableAlias().identifier()));

    if (ctx.indexHintList() != null)
      node.$(TableSourceFields.Simple_Hints, mkNodes(map(ctx.indexHintList().indexHint(), this::visitIndexHint)));

    return node;
  }

  @Override
  public SqlNode visitDerivedTable(MySQLParser.DerivedTableContext ctx) {
    final SqlNode node = mkTableSource(TableSourceKind.DerivedSource);

    if (ctx.LATERAL_SYMBOL() != null) node.flag(TableSourceFields.Derived_Lateral);

    node.$(TableSourceFields.Derived_Subquery, visitSubquery(ctx.subquery()));

    if (ctx.tableAlias() != null)
      node.$(TableSourceFields.Derived_Alias, MySQLAstHelper.stringifyIdentifier(ctx.tableAlias().identifier()));

    if (ctx.columnInternalRefList() != null) {
      final List<String> internalRefs =
          ctx.columnInternalRefList().columnInternalRef().stream()
              .map(MySQLParser.ColumnInternalRefContext::identifier)
              .map(MySQLAstHelper::stringifyIdentifier)
              .collect(Collectors.toList());
      node.$(TableSourceFields.Derived_InternalRefs, internalRefs);
    }

    return node;
  }

  @Override
  public SqlNode visitTableRefList(MySQLParser.TableRefListContext ctx) {
    return ctx.tableRef().stream()
        .map(this::visitTableRef)
        .reduce((left, right) -> mkJoined(left, right, JoinKind.CROSS_JOIN))
        .orElse(null);
  }

  @Override
  public SqlNode visitTableReference(MySQLParser.TableReferenceContext ctx) {
    final SqlNode left = ctx.tableFactor().accept(this);
    final var joinedTables = ctx.joinedTable();
    if (joinedTables == null || joinedTables.isEmpty()) return left;
    return joinedTables.stream()
        .map(this::visitJoinedTable)
        .reduce(
            left,
            (l, r) -> {
              r.$(TableSourceFields.Joined_Left, l);
              return r;
            });
  }

  @Override
  public SqlNode visitJoinedTable(MySQLParser.JoinedTableContext ctx) {
    final SqlNode node = mkTableSource(TableSourceKind.JoinedSource);
    final JoinKind joinType =
        Commons.coalesce(
            MySQLAstHelper.parseJoinKind(ctx.innerJoinType()),
            MySQLAstHelper.parseJoinKind(ctx.outerJoinType()),
            MySQLAstHelper.parseJoinKind(ctx.naturalJoinType()));

    assert joinType != null;

    node.$(TableSourceFields.Joined_Kind, joinType);

    if (ctx.expr() != null) node.$(TableSourceFields.Joined_On, toExpr(ctx.expr()));
    if (ctx.identifierListWithParentheses() != null) {
      final var identifiers = ctx.identifierListWithParentheses().identifierList().identifier();
      node.$(TableSourceFields.Joined_Using, map(identifiers, MySQLAstHelper::stringifyIdentifier));
    }

    final SqlNode right;
    if (ctx.tableReference() != null) right = ctx.tableReference().accept(this);
    else if (ctx.tableFactor() != null) right = ctx.tableFactor().accept(this);
    else return assertFalse();

    node.$(TableSourceFields.Joined_Right, right);

    return node;
  }

  @Override
  public SqlNode visitIndexHint(MySQLParser.IndexHintContext ctx) {
    final SqlNode node = mkNode(SqlKind.IndexHint);
    node.$(SqlNodeFields.IndexHint_Kind, MySQLAstHelper.parseIndexHintType(ctx));

    final IndexHintTarget target = MySQLAstHelper.parseIndexHintTarget(ctx.indexHintClause());
    if (target != null) node.$(SqlNodeFields.IndexHint_Target, target);

    final var indexList = ctx.indexList();
    if (indexList != null) {
      node.$(
          SqlNodeFields.IndexHint_Names,
          ListSupport.map(indexList.indexListElement(), MySQLAstHelper::parseIndexListElement));
    }

    return node;
  }

  @Override
  public SqlNode visitSelectItem(MySQLParser.SelectItemContext ctx) {
    final SqlNode item = mkNode(SqlKind.SelectItem);
    if (ctx.tableWild() != null) item.$(SqlNodeFields.SelectItem_Expr, visitTableWild(ctx.tableWild()));

    if (ctx.expr() != null) item.$(SqlNodeFields.SelectItem_Expr, toExpr(ctx.expr()));

    if (ctx.selectAlias() != null) {
      final var selectAlias = ctx.selectAlias();
      final String alias =
          selectAlias.identifier() != null
              ? MySQLAstHelper.stringifyIdentifier(selectAlias.identifier())
              : MySQLAstHelper.stringifyText(selectAlias.textStringLiteral());
      item.$(SqlNodeFields.SelectItem_Alias, alias);
    }

    return item;
  }

  @Override
  public SqlNode visitTableWild(MySQLParser.TableWildContext ctx) {
    final SqlNode table = mkNode(SqlKind.TableName);

    final List<MySQLParser.IdentifierContext> ids = ctx.identifier();
    final String id0 = MySQLAstHelper.stringifyIdentifier(ids.get(0));
    final String id1 = ids.size() >= 2 ? MySQLAstHelper.stringifyIdentifier(ids.get(1)) : null;

    final String schemaName = id1 != null ? id0 : null;
    final String tableName = id1 != null ? id1 : id0;

    table.$(SqlNodeFields.TableName_Schema, schemaName);
    table.$(SqlNodeFields.TableName_Table, tableName);

    return mkWildcard(table);
  }

  @Override
  public SqlNode visitExprIs(MySQLParser.ExprIsContext ctx) {
    final SqlNode left = ctx.boolPri().accept(this);
    if (ctx.IS_SYMBOL() == null) return left;

    final SqlNode right;
    if (ctx.UNKNOWN_SYMBOL() != null) right = mkLiteral(LiteralKind.UNKNOWN, null);
    else if (ctx.TRUE_SYMBOL() != null) right = mkLiteral(LiteralKind.BOOL, true);
    else if (ctx.FALSE_SYMBOL() != null) right = mkLiteral(LiteralKind.BOOL, false);
    else return assertFalse();

    final SqlNode node = mkBinary(left, right, IS);

    if (ctx.notRule() == null) return node;
    else return mkUnary(node, NOT);
  }

  @Override
  public SqlNode visitExprNot(MySQLParser.ExprNotContext ctx) {
    return mkUnary(ctx.expr().accept(this), NOT);
  }

  @Override
  public SqlNode visitExprAnd(MySQLParser.ExprAndContext ctx) {
    return mkBinary(ctx.expr(0).accept(this), ctx.expr(1).accept(this), AND);
  }

  @Override
  public SqlNode visitExprOr(MySQLParser.ExprOrContext ctx) {
    return mkBinary(ctx.expr(0).accept(this), ctx.expr(1).accept(this), OR);
  }

  @Override
  public SqlNode visitExprXor(MySQLParser.ExprXorContext ctx) {
    return mkBinary(ctx.expr(0).accept(this), ctx.expr(1).accept(this), XOR_SYMBOL);
  }

  @Override
  public SqlNode visitPrimaryExprIsNull(MySQLParser.PrimaryExprIsNullContext ctx) {
    final SqlNode node =
        mkBinary(ctx.boolPri().accept(this), mkLiteral(LiteralKind.NULL, null), IS);

    if (ctx.notRule() == null) return node;
    else {
      return mkUnary(node, NOT);
    }
  }

  @Override
  public SqlNode visitPrimaryExprCompare(MySQLParser.PrimaryExprCompareContext ctx) {
    return mkBinary(
        ctx.boolPri().accept(this),
        visitPredicate(ctx.predicate()),
        BinaryOpKind.ofOp(ctx.compOp().getText()));
  }

  @Override
  public SqlNode visitPrimaryExprAllAny(MySQLParser.PrimaryExprAllAnyContext ctx) {
    final SqlNode node =
        mkBinary(
            ctx.boolPri().accept(this),
            wrapAsQueryExpr(ctx.subquery().accept(this)),
            BinaryOpKind.ofOp(ctx.compOp().getText()));

    if (ctx.ALL_SYMBOL() != null) node.$(ExprFields.Binary_SubqueryOption, SubqueryOption.ALL);
    else if (ctx.ANY_SYMBOL() != null) node.$(ExprFields.Binary_SubqueryOption, SubqueryOption.ANY);

    return node;
  }

  @Override
  public SqlNode visitPredicate(MySQLParser.PredicateContext ctx) {
    final SqlNode bitExpr = visitBitExpr(ctx.bitExpr(0));
    if (ctx.predicateOperations() != null) {
      SqlNode node = null;
      final var predicateOp = ctx.predicateOperations();

      if (predicateOp instanceof MySQLParser.PredicateExprInContext) {
        final var inExpr = (MySQLParser.PredicateExprInContext) predicateOp;
        final var subquery = inExpr.subquery();

        final BinaryOpKind op;
        final SqlNode right;
        if (subquery != null) {
          op = BinaryOpKind.IN_SUBQUERY;
          right = wrapAsQueryExpr(subquery.accept(this));

        } else {
          final SqlNode tuple = mkExpr(ExprKind.Tuple);
          tuple.$(ExprFields.Tuple_Exprs, mkNodes(toExprs(inExpr.exprList())));
          op = BinaryOpKind.IN_LIST;
          right = tuple;
        }

        node = mkBinary(bitExpr, right, op);
      }

      if (predicateOp instanceof MySQLParser.PredicateExprBetweenContext) {
        node = mkExpr(ExprKind.Ternary);
        final var betweenExpr = (MySQLParser.PredicateExprBetweenContext) predicateOp;

        node.$(ExprFields.Ternary_Op, TernaryOp.BETWEEN_AND);
        node.$(ExprFields.Ternary_Left, bitExpr);
        node.$(ExprFields.Ternary_Middle, betweenExpr.bitExpr().accept(this));
        node.$(ExprFields.Ternary_Right, betweenExpr.predicate().accept(this));
      }

      if (predicateOp instanceof MySQLParser.PredicateExprLikeContext)
        node =
            mkBinary(
                bitExpr,
                ((MySQLParser.PredicateExprLikeContext) predicateOp).simpleExpr(0).accept(this),
                BinaryOpKind.LIKE);

      if (predicateOp instanceof MySQLParser.PredicateExprRegexContext)
        node =
            mkBinary(
                bitExpr,
                ((MySQLParser.PredicateExprRegexContext) predicateOp).bitExpr().accept(this),
                BinaryOpKind.REGEXP);

      if (node != null) {
        if (ctx.notRule() != null) node = mkUnary(node, NOT);
        return node;
      } else return assertFalse();
    }

    if (ctx.MEMBER_SYMBOL() != null)
      return mkBinary(
          bitExpr,
          ctx.simpleExprWithParentheses().simpleExpr().accept(this),
          BinaryOpKind.MEMBER_OF);

    if (ctx.SOUNDS_SYMBOL() != null)
      return mkBinary(bitExpr, ctx.bitExpr(1).accept(this), BinaryOpKind.SOUNDS_LIKE);

    return bitExpr;
  }

  @Override
  public SqlNode visitBitExpr(MySQLParser.BitExprContext ctx) {
    if (ctx.simpleExpr() != null) return ctx.simpleExpr().accept(this);

    final SqlNode right;
    if (ctx.interval() != null) {
      right = mkInterval(toExpr(ctx.expr()), MySQLAstHelper.parseIntervalUnit(ctx.interval()));

    } else right = ctx.bitExpr(1).accept(this);

    return mkBinary(
        ctx.bitExpr(0).accept(this),
        right,
        ctx.BITWISE_XOR_OPERATOR() != null
            ? BinaryOpKind.BITWISE_XOR
            : BinaryOpKind.ofOp(ctx.op.getText()));
  }

  @Override
  public SqlNode visitWindowDefinition(MySQLParser.WindowDefinitionContext ctx) {
    final SqlNode node = ctx.windowSpec().accept(this);
    if (ctx.windowName() != null)
      node.$(SqlNodeFields.WindowSpec_Alias, MySQLAstHelper.stringifyIdentifier(ctx.windowName().identifier()));
    return node;
  }

  @Override
  public SqlNode visitWindowSpecDetails(MySQLParser.WindowSpecDetailsContext ctx) {
    final SqlNode node = mkNode(SqlKind.WindowSpec);
    if (ctx.windowName() != null)
      node.$(SqlNodeFields.WindowSpec_Name, MySQLAstHelper.stringifyIdentifier(ctx.windowName().identifier()));
    // partition by
    // in .g4 partition by clause is defined as an orderList
    // but it should be just a expression list
    if (ctx.orderList() != null) {
      final List<SqlNode> orderItems =
          ctx.orderList().orderExpression().stream()
              .map(MySQLParser.OrderExpressionContext::expr)
              .map(it -> it.accept(this))
              .collect(Collectors.toList());
      node.$(SqlNodeFields.WindowSpec_Part, mkNodes(orderItems));
    }
    // order by
    if (ctx.orderClause() != null)
      node.$(SqlNodeFields.WindowSpec_Order, mkNodes(toOrderItems(ctx.orderClause())));
    if (ctx.windowFrameClause() != null)
      node.$(SqlNodeFields.WindowSpec_Frame, visitWindowFrameClause(ctx.windowFrameClause()));

    return node;
  }

  @Override
  public SqlNode visitWindowFrameClause(MySQLParser.WindowFrameClauseContext ctx) {
    final SqlNode node = mkNode(SqlKind.WindowFrame);

    node.$(SqlNodeFields.WindowFrame_Unit, WindowUnit.valueOf(ctx.windowFrameUnits().getText().toUpperCase()));

    final var frameExclusion = ctx.windowFrameExclusion();
    if (frameExclusion != null) {
      final WindowExclusion exclusion;
      if (frameExclusion.CURRENT_SYMBOL() != null) exclusion = WindowExclusion.CURRENT_ROW;
      else if (frameExclusion.GROUP_SYMBOL() != null) exclusion = WindowExclusion.GROUP;
      else if (frameExclusion.TIES_SYMBOL() != null) exclusion = WindowExclusion.TIES;
      else if (frameExclusion.OTHERS_SYMBOL() != null) exclusion = WindowExclusion.NO_OTHERS;
      else return assertFalse();
      node.$(SqlNodeFields.WindowFrame_Exclusion, exclusion);
    }

    final var frameExtent = ctx.windowFrameExtent();
    if (frameExtent.windowFrameStart() != null) {
      node.$(SqlNodeFields.WindowFrame_Start, visitWindowFrameStart(frameExtent.windowFrameStart()));

    } else if (frameExtent.windowFrameBetween() != null) {
      final var frameBetween = frameExtent.windowFrameBetween();
      node.$(SqlNodeFields.WindowFrame_Start, visitWindowFrameBound(frameBetween.windowFrameBound(0)));
      node.$(SqlNodeFields.WindowFrame_End, visitWindowFrameBound(frameBetween.windowFrameBound(1)));

    } else return assertFalse();

    return node;
  }

  @Override
  public SqlNode visitWindowFrameStart(MySQLParser.WindowFrameStartContext ctx) {
    final SqlNode node = mkNode(SqlKind.FrameBound);
    if (ctx.PRECEDING_SYMBOL() != null) {
      node.$(SqlNodeFields.FrameBound_Direction, FrameBoundDirection.PRECEDING);

      if (ctx.UNBOUNDED_SYMBOL() != null) node.$(SqlNodeFields.FrameBound_Expr, mkSymbol("unbounded"));
      else if (ctx.PARAM_MARKER() != null) node.$(SqlNodeFields.FrameBound_Expr, mkParam());
      else if (ctx.ulonglong_number() != null)
        node.$(SqlNodeFields.FrameBound_Expr, visitUlonglong_number(ctx.ulonglong_number()));
      else if (ctx.INTERVAL_SYMBOL() != null) {
        final SqlNode interval = mkInterval(toExpr(ctx.expr()), MySQLAstHelper.parseIntervalUnit(ctx.interval()));
        node.$(SqlNodeFields.FrameBound_Expr, interval);
      }

    } else node.$(SqlNodeFields.FrameBound_Expr, mkSymbol("current row"));

    return node;
  }

  @Override
  public SqlNode visitWindowFrameBound(MySQLParser.WindowFrameBoundContext ctx) {
    if (ctx.windowFrameStart() != null) return visitWindowFrameStart(ctx.windowFrameStart());

    final SqlNode node = mkNode(SqlKind.FrameBound);
    node.$(SqlNodeFields.FrameBound_Direction, FrameBoundDirection.FOLLOWING);

    if (ctx.UNBOUNDED_SYMBOL() != null) node.$(SqlNodeFields.FrameBound_Expr, mkSymbol("unbounded"));
    else if (ctx.PARAM_MARKER() != null) node.$(SqlNodeFields.FrameBound_Expr, mkParam());
    else if (ctx.ulonglong_number() != null)
      node.$(SqlNodeFields.FrameBound_Expr, visitUlonglong_number(ctx.ulonglong_number()));
    else if (ctx.INTERVAL_SYMBOL() != null) {
      final SqlNode interval = mkInterval(toExpr(ctx.expr()), MySQLAstHelper.parseIntervalUnit(ctx.interval()));
      node.$(SqlNodeFields.FrameBound_Expr, interval);
    }

    return node;
  }

  @Override
  public SqlNode visitUlonglong_number(MySQLParser.Ulonglong_numberContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Literal);

    final Token token =
        Commons.coalesce(
                ctx.INT_NUMBER(),
                ctx.LONG_NUMBER(),
                ctx.ULONGLONG_NUMBER(),
                ctx.DECIMAL_NUMBER(),
                ctx.FLOAT_NUMBER())
            .getSymbol();

    final Pair<LiteralKind, Number> pair = MySQLAstHelper.parseNumericLiteral(token);
    assert pair != null;

    node.$(ExprFields.Literal_Kind, pair.getLeft());
    node.$(ExprFields.Literal_Value, pair.getRight());

    return node;
  }

  @Override
  public SqlNode visitSimpleExprColumnRef(MySQLParser.SimpleExprColumnRefContext ctx) {
    final SqlNode columnRef = visitColumnRef(ctx.columnRef());
    if (ctx.jsonOperator() == null) {
      return columnRef;
    } else {
      final SqlNode node = mkExpr(ExprKind.FuncCall);
      final var jsonOperator = ctx.jsonOperator();

      final List<SqlNode> args =
          Arrays.asList(
              columnRef,
              mkLiteral(LiteralKind.TEXT, MySQLAstHelper.stringifyText(jsonOperator.textStringLiteral())));
      node.$(ExprFields.FuncCall_Name, mkName2(null, "json_extract"));
      node.$(ExprFields.FuncCall_Args, mkNodes(args));

      if (jsonOperator.JSON_UNQUOTED_SEPARATOR_SYMBOL() == null) return node;
      else {
        final SqlNode unquoteCall = mkExpr(ExprKind.FuncCall);

        unquoteCall.$(ExprFields.FuncCall_Name, mkName2(null, "json_unquote"));
        unquoteCall.$(ExprFields.FuncCall_Args, mkNodes(singletonList(node)));

        return unquoteCall;
      }
    }
  }

  @Override
  public SqlNode visitColumnRef(MySQLParser.ColumnRefContext ctx) {
    final SqlNode node = mkExpr(ExprKind.ColRef);

    final SqlNode column = mkNode(SqlKind.ColName);
    final String[] triple = MySQLAstHelper.stringifyIdentifier(ctx.fieldIdentifier());
    column.$(SqlNodeFields.ColName_Schema, triple[0]);
    column.$(SqlNodeFields.ColName_Table, triple[1]);
    column.$(SqlNodeFields.ColName_Col, triple[2]);

    node.$(ExprFields.ColRef_ColName, column);

    return node;
  }

  @Override
  public SqlNode visitVariable(MySQLParser.VariableContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Variable);
    if (ctx.userVariable() != null) {
      final var userVariable = ctx.userVariable();
      final String name;
      if (userVariable.textOrIdentifier() != null)
        name = MySQLAstHelper.stringifyText(userVariable.textOrIdentifier());
      else if (userVariable.AT_TEXT_SUFFIX() != null)
        name = userVariable.AT_SIGN_SYMBOL().getText().substring(1);
      else return assertFalse();

      node.$(ExprFields.Variable_Name, name);
      node.$(ExprFields.Variable_Scope, VariableScope.USER);
      if (ctx.expr() != null) node.$(ExprFields.Variable_Assignment, toExpr(ctx.expr()));

    } else if (ctx.systemVariable() != null) {
      final var systemVariable = ctx.systemVariable();
      final var varIdentType = systemVariable.varIdentType();

      final VariableScope scope;
      if (varIdentType == null) scope = VariableScope.SYSTEM_GLOBAL;
      else {
        final String text = varIdentType.getText();
        final String scopeName = "SYSTEM_" + text.substring(0, text.length() - 1).toUpperCase();
        scope = VariableScope.valueOf(scopeName);
      }

      final StringBuilder builder = new StringBuilder();
      builder.append(MySQLAstHelper.stringifyText(systemVariable.textOrIdentifier()));
      if (systemVariable.dotIdentifier() != null)
        builder.append('.').append(MySQLAstHelper.stringifyIdentifier(systemVariable.dotIdentifier()));
      final String name = builder.toString().toLowerCase();

      node.$(ExprFields.Variable_Scope, scope);
      node.$(ExprFields.Variable_Name, name);

    } else return assertFalse();

    return node;
  }

  @Override
  public SqlNode visitRuntimeFunctionCall(MySQLParser.RuntimeFunctionCallContext ctx) {
    final SqlNode node = mkExpr(ExprKind.FuncCall);

    final List<SqlNode> args;

    if (ctx.name != null) {
      node.$(ExprFields.FuncCall_Name, mkName2(null, ctx.name.getText().toLowerCase()));

      if (ctx.parentheses() != null) {
        // no-arg
        args = emptyList();

      } else if (ctx.exprWithParentheses() != null) {
        // single-arg
        args = singletonList(toExpr(ctx.exprWithParentheses().expr()));

      } else if (ctx.exprListWithParentheses() != null) {
        // var-arg
        args = toExprs(ctx.exprListWithParentheses().exprList());

      } else if (ctx.exprList() != null) {
        args = toExprs(ctx.exprList());

      } else if (ctx.INTERVAL_SYMBOL() != null) {
        // ADDDATE/SUBDATE/DATE_ADD/DATE_SUB with INTERVAL expr
        final SqlNode interval = mkInterval(toExpr(ctx.expr(1)), MySQLAstHelper.parseIntervalUnit(ctx.interval()));
        args = Arrays.asList(toExpr(ctx.expr(0)), interval);

      } else if (ctx.interval() != null) {
        // EXTRACT
        args = Arrays.asList(mkSymbol(ctx.interval().getText().toLowerCase()), toExpr(ctx.expr(0)));

      } else if (ctx.dateTimeTtype() != null) {
        // GET_FORMAT
        args =
            Arrays.asList(
                mkSymbol(ctx.dateTimeTtype().getText().toLowerCase()), ctx.expr(0).accept(this));

      } else if (ctx.bitExpr() != null) {
        // POSITION
        args = Arrays.asList(visit(ctx.bitExpr()), toExpr(ctx.expr(0)));

      } else if (ctx.intervalTimeStamp() != null) {
        // TIMESTAMP_ADD/TIMESTAMP_DIFF
        args =
            Arrays.asList(
                mkSymbol(ctx.intervalTimeStamp().getText().toLowerCase()),
                toExpr(ctx.expr(0)),
                toExpr(ctx.expr(1)));

      } else if (ctx.textLiteral() != null) {
        // OLD_PASSWORD
        args = singletonList(mkLiteral(LiteralKind.TEXT, MySQLAstHelper.stringifyText(ctx.textLiteral())));

      } else if (ctx.timeFunctionParameters() != null) {
        final var fractionalPrecision = ctx.timeFunctionParameters().fractionalPrecision();
        if (fractionalPrecision != null)
          args =
              singletonList(
                  mkLiteral(LiteralKind.INTEGER, Integer.parseInt(fractionalPrecision.getText())));
        else args = emptyList();

      } else if (ctx.expr() != null) {
        args = map(ctx.expr(), this::toExpr);

      } else {
        args = emptyList();
      }
    } else if (ctx.trimFunction() != null) {
      node.$(ExprFields.FuncCall_Name, mkName2(null, "trim"));
      args = new ArrayList<>(3);

      final var trimFunc = ctx.trimFunction();
      if (trimFunc.LEADING_SYMBOL() != null) args.add(mkSymbol("leading"));
      else if (trimFunc.TRAILING_SYMBOL() != null) args.add(mkSymbol("trailing"));
      else if (trimFunc.BOTH_SYMBOL() != null) args.add(mkSymbol("both"));
      else args.add(mkVoid());

      final var exprs = trimFunc.expr();
      if (SqlKind.Void.isInstance(args.get(0))) {
        args.add(toExpr(exprs.get(0)));
        if (exprs.size() == 2) args.add(toExpr(exprs.get(1)));
        else args.add(mkVoid());
      } else {
        if (exprs.size() == 2) {
          args.add(toExpr(exprs.get(0)));
          args.add(toExpr(exprs.get(1)));
        } else {
          args.add(mkVoid());
          args.add(toExpr(exprs.get(0)));
        }
      }

    } else if (ctx.substringFunction() != null) {
      node.$(ExprFields.FuncCall_Name, mkName2(null, "substring"));
      final var substringFunc = ctx.substringFunction();
      args = map(substringFunc.expr(), this::toExpr);

    } else if (ctx.geometryFunction() != null) {
      final var geoFunc = ctx.geometryFunction();
      node.$(ExprFields.FuncCall_Name, mkName2(null, geoFunc.name.getText().toLowerCase()));

      if (geoFunc.exprListWithParentheses() != null) {
        // var-arg
        args = toExprs(geoFunc.exprListWithParentheses().exprList());
      } else if (geoFunc.exprList() != null) {
        args = toExprs(geoFunc.exprList());

      } else if (geoFunc.expr() != null) {
        args = map(geoFunc.expr(), this::toExpr);

      } else {
        args = emptyList();
      }

    } else {
      args = emptyList();
    }

    node.$(ExprFields.FuncCall_Args, mkNodes(args));

    return node;
  }

  @Override
  public SqlNode visitSimpleExprCollate(MySQLParser.SimpleExprCollateContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Collate);

    node.$(ExprFields.Collate_Expr, ctx.simpleExpr().accept(this));
    node.$(ExprFields.Collate_Collation, mkSymbol(MySQLAstHelper.stringifyText(ctx.textOrIdentifier())));

    return node;
  }

  @Override
  public SqlNode visitFunctionCall(MySQLParser.FunctionCallContext ctx) {
    final SqlNode node = mkExpr(ExprKind.FuncCall);

    if (ctx.pureIdentifier() != null) {
      node.$(ExprFields.FuncCall_Name, mkName2(null, MySQLAstHelper.stringifyIdentifier(ctx.pureIdentifier()).toLowerCase()));

      if (ctx.udfExprList() != null) node.$(ExprFields.FuncCall_Args, mkNodes(toExprs(ctx.udfExprList())));
      else node.$(ExprFields.FuncCall_Args, mkNodes(emptyList()));

    } else if (ctx.qualifiedIdentifier() != null) {
      node.$(
          ExprFields.FuncCall_Name,
          mkName2(null, MySQLAstHelper.stringifyIdentifier(ctx.qualifiedIdentifier())[1].toLowerCase()));

      if (ctx.exprList() != null) node.$(ExprFields.FuncCall_Args, mkNodes(toExprs((ctx.exprList()))));
      else node.$(ExprFields.FuncCall_Args, mkNodes(emptyList()));

    } else {
      return assertFalse();
    }

    return node;
  }

  @Override
  public SqlNode visitLiteral(MySQLParser.LiteralContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Literal);

    final LiteralKind type;
    final Object value;
    String unit = null;

    if (ctx.textLiteral() != null) {
      type = LiteralKind.TEXT;
      value = MySQLAstHelper.stringifyText(ctx.textLiteral());

    } else if (ctx.numLiteral() != null) {
      final var num = ctx.numLiteral();
      if (num.INT_NUMBER() != null) {
        type = LiteralKind.INTEGER;
        value = Integer.parseInt(num.INT_NUMBER().getText());

      } else if (num.LONG_NUMBER() != null || num.ULONGLONG_NUMBER() != null) {
        type = LiteralKind.LONG;
        value = Long.parseLong(num.getText());
      } else if (num.FLOAT_NUMBER() != null || num.DECIMAL_NUMBER() != null) {
        type = LiteralKind.FRACTIONAL;
        value = Double.parseDouble(num.getText());
      } else {
        return assertFalse();
      }
    } else if (ctx.temporalLiteral() != null) {
      final var temporal = ctx.temporalLiteral();
      type = LiteralKind.TEMPORAL;
      value = unquoted(temporal.SINGLE_QUOTED_TEXT().getText(), '\'');
      unit =
          temporal.DATE_SYMBOL() != null
              ? "date"
              : temporal.TIME_SYMBOL() != null
                  ? "time"
                  : temporal.TIMESTAMP_SYMBOL() != null ? "timestamp" : null;

    } else if (ctx.nullLiteral() != null) {
      type = LiteralKind.NULL;
      value = null;

    } else if (ctx.boolLiteral() != null) {
      type = LiteralKind.BOOL;
      value = "true".equalsIgnoreCase(ctx.boolLiteral().getText());
    } else {
      type = LiteralKind.HEX;
      value = ctx.HEX_NUMBER() == null ? ctx.HEX_NUMBER().getText() : ctx.BIN_NUMBER().getText();
    }

    node.$(ExprFields.Literal_Kind, type);
    node.$(ExprFields.Literal_Value, value);
    node.$(ExprFields.Literal_Unit, unit);

    return node;
  }

  @Override
  public SqlNode visitSimpleExprParamMarker(MySQLParser.SimpleExprParamMarkerContext ctx) {
    return mkParam();
  }

  @Override
  public SqlNode visitSumExpr(MySQLParser.SumExprContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Aggregate);

    node.$(ExprFields.Aggregate_Name, ctx.name.getText().toLowerCase());

    if (ctx.DISTINCT_SYMBOL() != null) node.flag(ExprFields.Aggregate_Distinct);

    final List<SqlNode> args;
    if (ctx.inSumExpr() != null) args = singletonList(visitInSumExpr(ctx.inSumExpr()));
    else if (ctx.exprList() != null) args = toExprs(ctx.exprList());
    else if (ctx.MULT_OPERATOR() != null) args = singletonList(mkWildcard(null));
    else args = emptyList();

    node.$(ExprFields.Aggregate_Args, mkNodes(args));

    if (ctx.windowingClause() != null) {
      final var windowingClause = ctx.windowingClause();
      if (windowingClause.windowName() != null) {
        node.$(
            ExprFields.Aggregate_WindowName, MySQLAstHelper.stringifyIdentifier(windowingClause.windowName().identifier()));

      } else if (windowingClause.windowSpec() != null) {
        node.$(
            ExprFields.Aggregate_WindowSpec,
            visitWindowSpecDetails(windowingClause.windowSpec().windowSpecDetails()));

      } else return assertFalse();
    }
    if (ctx.orderClause() != null)
      node.$(ExprFields.Aggregate_Order, mkNodes(toOrderItems(ctx.orderClause())));
    if (ctx.textString() != null) node.$(ExprFields.Aggregate_Sep, MySQLAstHelper.stringifyText(ctx.textString()));

    return node;
  }

  @Override
  public SqlNode visitGroupingOperation(MySQLParser.GroupingOperationContext ctx) {
    final SqlNode node = mkExpr(ExprKind.GroupingOp);
    node.$(ExprFields.GroupingOp_Exprs, mkNodes(toExprs(ctx.exprList())));
    return node;
  }

  @Override
  public SqlNode visitWindowFunctionCall(MySQLParser.WindowFunctionCallContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Aggregate);
    if (ctx.ROW_NUMBER_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "ROW_NUMBER");
    else if (ctx.RANK_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "RANK");
    else if (ctx.DENSE_RANK_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "DENSE");
    else if (ctx.CUME_DIST_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "CUME");
    else if (ctx.PERCENT_RANK_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "PERNCENT_RANK");
    else if (ctx.NTILE_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "NTILE");
    else if (ctx.LEAD_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "LEAD");
    else if (ctx.LAG_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "LAG");
    else if (ctx.FIRST_VALUE_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "FIRST_VALUE");
    else if (ctx.LAST_VALUE_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "LAST_VALUE");
    else if (ctx.NTH_VALUE_SYMBOL() != null) node.$(ExprFields.Aggregate_Name, "NTH_VALUE");

    final var clause = ctx.windowingClause();
    if (clause.windowName() != null)
      node.$(ExprFields.Aggregate_WindowName, MySQLAstHelper.stringifyIdentifier(clause.windowName().identifier()));
    else if (clause.windowSpec() != null)
      node.$(ExprFields.Aggregate_WindowSpec, clause.windowSpec().accept(this));

    return node;
  }

  @Override
  public SqlNode visitSimpleExprConcat(MySQLParser.SimpleExprConcatContext ctx) {
    final SqlNode node = mkExpr(ExprKind.FuncCall);
    node.$(ExprFields.FuncCall_Name, mkName2(null, "concat"));
    node.$(ExprFields.FuncCall_Args, mkNodes(map(ctx.simpleExpr(), arg -> arg.accept(this))));

    return node;
  }

  @Override
  public SqlNode visitSimpleExprUnary(MySQLParser.SimpleExprUnaryContext ctx) {
    return mkUnary(ctx.simpleExpr().accept(this), UnaryOpKind.ofOp(ctx.op.getText()));
  }

  @Override
  public SqlNode visitSimpleExprNot(MySQLParser.SimpleExprNotContext ctx) {
    return mkUnary(ctx.simpleExpr().accept(this), NOT);
  }

  @Override
  public SqlNode visitSimpleExprList(MySQLParser.SimpleExprListContext ctx) {
    final List<SqlNode> exprs = toExprs(ctx.exprList());
    final boolean asRow = ctx.ROW_SYMBOL() != null;
    if (!asRow && exprs.size() == 1) return exprs.get(0);

    final SqlNode node = mkExpr(ExprKind.Tuple);
    node.$(ExprFields.Tuple_Exprs, mkNodes(exprs));
    if (asRow) node.flag(ExprFields.Tuple_AsRow);

    return node;
  }

  @Override
  public SqlNode visitSimpleExprSubQuery(MySQLParser.SimpleExprSubQueryContext ctx) {
    final SqlNode subquery = wrapAsQueryExpr(visitSubquery(ctx.subquery()));

    if (ctx.EXISTS_SYMBOL() == null) return subquery;

    final SqlNode node = mkExpr(ExprKind.Exists);
    node.$(ExprFields.Exists_Subquery, subquery);
    return node;
  }

  @Override
  public SqlNode visitSimpleExprMatch(MySQLParser.SimpleExprMatchContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Match);
    final List<SqlNode> cols =
        map(
            ctx.identListArg().identList().simpleIdentifier(),
            func(this::visitSimpleIdentifier).andThen(this::mkColRef));
    node.$(ExprFields.Match_Cols, mkNodes(cols));
    node.$(ExprFields.Match_Expr, ctx.bitExpr().accept(this));

    final var fullTextOption = ctx.fulltextOptions();
    if (fullTextOption != null) {
      final MatchOption option;
      if (fullTextOption.BOOLEAN_SYMBOL() != null) {
        option = MatchOption.BOOLEAN_MODE;
      } else if (fullTextOption.EXPANSION_SYMBOL() != null
          && fullTextOption.NATURAL_SYMBOL() != null) {
        option = MatchOption.NATURAL_MODE_WITH_EXPANSION;
      } else if (fullTextOption.EXPANSION_SYMBOL() != null) {
        option = MatchOption.WITH_EXPANSION;
      } else {
        option = null;
      }
      node.$(ExprFields.Match_Option, option);
    }

    return node;
  }

  @Override
  public SqlNode visitSimpleExprBinary(MySQLParser.SimpleExprBinaryContext ctx) {
    return mkUnary(ctx.simpleExpr().accept(this), BINARY);
  }

  @Override
  public SqlNode visitSimpleExprCast(MySQLParser.SimpleExprCastContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Cast);
    node.$(ExprFields.Cast_Expr, toExpr(ctx.expr()));
    node.$(ExprFields.Cast_Type, MySQLAstHelper.parseDataType(ctx.castType()));
    node.$(ExprFields.Cast_IsArray, ctx.arrayCast() != null);
    return node;
  }

  @Override
  public SqlNode visitSimpleExprConvert(MySQLParser.SimpleExprConvertContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Cast);
    node.$(ExprFields.Cast_Expr, toExpr(ctx.expr()));
    node.$(ExprFields.Cast_Type, MySQLAstHelper.parseDataType(ctx.castType()));
    return node;
  }

  @Override
  public SqlNode visitSimpleExprCase(MySQLParser.SimpleExprCaseContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Case);
    if (ctx.expr() != null) node.$(ExprFields.Case_Cond, toExpr(ctx.expr()));
    if (ctx.elseExpression() != null) node.$(ExprFields.Case_Else, toExpr(ctx.elseExpression().expr()));
    if (ctx.whenExpression() != null) {
      final var whenExprs = ctx.whenExpression();
      final var thenExprs = ctx.thenExpression();
      final List<SqlNode> whens = new ArrayList<>(whenExprs.size());

      for (int i = 0; i < whenExprs.size(); i++) {
        final var whenExpr = whenExprs.get(i);
        final var thenExpr = thenExprs.get(i);

        final SqlNode when = mkExpr(ExprKind.When);
        when.$(ExprFields.When_Cond, toExpr(whenExpr.expr()));
        when.$(ExprFields.When_Expr, toExpr(thenExpr.expr()));
        whens.add(when);
      }

      node.$(ExprFields.Case_Whens, mkNodes(whens));
    }

    return node;
  }

  @Override
  public SqlNode visitSimpleExprConvertUsing(MySQLParser.SimpleExprConvertUsingContext ctx) {
    final SqlNode node = mkExpr(ExprKind.ConvertUsing);
    node.$(ExprFields.ConvertUsing_Expr, toExpr(ctx.expr()));
    node.$(ExprFields.ConvertUsing_Charset, visitCharsetName(ctx.charsetName()));
    return node;
  }

  @Override
  public SqlNode visitSimpleExprDefault(MySQLParser.SimpleExprDefaultContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Default);
    node.$(ExprFields.Default_Col, visitSimpleIdentifier(ctx.simpleIdentifier()));
    return node;
  }

  @Override
  public SqlNode visitSimpleExprValues(MySQLParser.SimpleExprValuesContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Values);
    node.$(ExprFields.Values_Expr, visitSimpleIdentifier(ctx.simpleIdentifier()));
    return node;
  }

  @Override
  public SqlNode visitSimpleExprInterval(MySQLParser.SimpleExprIntervalContext ctx) {
    final SqlNode interval = mkInterval(toExpr(ctx.expr(0)), MySQLAstHelper.parseIntervalUnit(ctx.interval()));
    return mkBinary(interval, toExpr(ctx.expr(1)), BinaryOpKind.PLUS);
  }

  @Override
  public SqlNode visitSimpleIdentifier(MySQLParser.SimpleIdentifierContext ctx) {
    final SqlNode node = mkNode(SqlKind.ColName);
    final String[] triple = MySQLAstHelper.stringifyIdentifier(ctx);
    assert triple != null;
    node.$(SqlNodeFields.ColName_Schema, triple[0]);
    node.$(SqlNodeFields.ColName_Table, triple[1]);
    node.$(SqlNodeFields.ColName_Col, triple[2]);
    return node;
  }

  @Override
  public SqlNode visitInSumExpr(MySQLParser.InSumExprContext ctx) {
    return toExpr(ctx.expr());
  }

  @Override
  public SqlNode visitCharsetName(MySQLParser.CharsetNameContext ctx) {
    final String name;
    if (ctx.textOrIdentifier() != null) name = MySQLAstHelper.stringifyText(ctx.textOrIdentifier());
    else if (ctx.BINARY_SYMBOL() != null) name = "binary";
    else if (ctx.DEFAULT_SYMBOL() != null) name = "default";
    else return assertFalse();
    return mkSymbol(name.toLowerCase());
  }

  @Override
  public SqlNode visitOrderExpression(MySQLParser.OrderExpressionContext ctx) {
    final SqlNode node = mkNode(SqlKind.OrderItem);

    final SqlNode expr = ctx.expr().accept(this);
    final KeyDirection direction = MySQLAstHelper.parseDirection(ctx.direction());
    node.$(SqlNodeFields.OrderItem_Expr, expr);
    node.$(SqlNodeFields.OrderItem_Direction, direction);

    return node;
  }

  private void parseFieldDefinition(MySQLParser.FieldDefinitionContext fieldDef, SqlNode node) {
    node.$(SqlNodeFields.ColDef_RawType, fieldDef.dataType().getText().toLowerCase());
    node.$(SqlNodeFields.ColDef_DataType, MySQLAstHelper.parseDataType(fieldDef.dataType()));

    MySQLAstHelper.collectColumnAttrs(fieldDef.columnAttribute(), node);
    MySQLAstHelper.collectGColumnAttrs(fieldDef.gcolAttribute(), node);

    if (fieldDef.AS_SYMBOL() != null) node.flag(SqlNodeFields.ColDef_Generated);
  }

  private List<SqlNode> toOrderItems(MySQLParser.OrderClauseContext ctx) {
    return map(ctx.orderList().orderExpression(), this::visitOrderExpression);
  }

  private List<SqlNode> toGroupItems(MySQLParser.OrderListContext ctx) {
    return ctx.orderExpression().stream()
        .map(MySQLParser.OrderExpressionContext::expr)
        .map(it -> it.accept(this))
        .map(this::mkGroupItem)
        .collect(Collectors.toList());
  }

  private SqlNode toExpr(MySQLParser.ExprContext expr) {
    return expr.accept(this);
  }

  private List<SqlNode> toExprs(MySQLParser.ExprListContext exprList) {
    return map(exprList.expr(), this::toExpr);
  }

  private List<SqlNode> toExprs(MySQLParser.UdfExprListContext udfExprList) {
    return map(udfExprList.udfExpr(), udfExpr -> toExpr(udfExpr.expr()));
  }

  private SqlNode parseTableName(
      MySQLParser.QualifiedIdentifierContext qualifiedId, MySQLParser.DotIdentifierContext dotId) {
    final String schema, table;

    if (qualifiedId != null) {
      final String[] pair = MySQLAstHelper.stringifyIdentifier(qualifiedId);
      schema = pair[0];
      table = pair[1];

    } else if (dotId != null) {
      schema = null;
      table = MySQLAstHelper.stringifyIdentifier(dotId);

    } else {
      return assertFalse();
    }

    final SqlNode node = mkNode(SqlKind.TableName);
    node.$(SqlNodeFields.TableName_Schema, schema);
    node.$(SqlNodeFields.TableName_Table, table);

    return node;
  }
}
