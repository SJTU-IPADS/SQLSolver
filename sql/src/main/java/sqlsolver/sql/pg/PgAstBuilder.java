package sqlsolver.sql.pg;

import org.antlr.v4.runtime.tree.TerminalNode;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.*;
import sqlsolver.sql.parser.AstBuilderMixin;
import sqlsolver.sql.pg.internal.PGParser;
import sqlsolver.sql.pg.internal.PGParserBaseVisitor;
import sqlsolver.sql.ast.constants.*;
import sqlsolver.sql.ast.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static sqlsolver.common.utils.Commons.assertFalse;
import static sqlsolver.common.utils.Commons.coalesce;
import static sqlsolver.sql.SqlSupport.copyAst;
import static sqlsolver.sql.ast.constants.BinaryOpKind.*;
import static sqlsolver.sql.ast.constants.ConstraintKind.*;
import static sqlsolver.sql.ast.constants.SetOpKind.*;
import static sqlsolver.sql.ast.constants.UnaryOpKind.NOT;
import static sqlsolver.sql.pg.PgAstHelper.*;

public class PgAstBuilder extends PGParserBaseVisitor<SqlNode> implements AstBuilderMixin {
  private final SqlContext ast;

  PgAstBuilder() {
    ast = SqlContext.mk(32);
  }

  @Override
  public SqlContext ast() {
    return ast;
  }

  @Override
  public SqlNode visitCreate_table_statement(PGParser.Create_table_statementContext ctx) {
    final SqlNode node = mkNode(SqlKind.CreateTable);

    node.$(SqlNodeFields.CreateTable_Name, mkTableName(stringifyIdentifier(ctx.name)));

    final var defCtx = ctx.define_table();
    final List<SqlNode> colDefs = new ArrayList<>();
    final List<SqlNode> constrDefs = new ArrayList<>();

    for (var colOrConstrDef : defCtx.define_columns().table_column_def()) {
      if (colOrConstrDef.table_column_definition() != null) {
        final SqlNode colDef = colOrConstrDef.table_column_definition().accept(this);
        if (colDef != null) colDefs.add(colDef);

      } else if (colOrConstrDef.tabl_constraint != null) {
        final SqlNode constrDef = colOrConstrDef.tabl_constraint.accept(this);
        if (constrDef != null) constrDefs.add(constrDef);
      }
    }

    node.$(SqlNodeFields.CreateTable_Cols, mkNodes(colDefs));
    node.$(SqlNodeFields.CreateTable_Cons, mkNodes(constrDefs));

    return node;
  }

  @Override
  public SqlNode visitTable_column_definition(PGParser.Table_column_definitionContext ctx) {
    final SqlNode node = mkNode(SqlKind.ColDef);
    node.$(SqlNodeFields.ColDef_Name, mkColName(null, null, stringifyIdentifier(ctx.identifier())));

    final var dataTypeCtx = ctx.data_type();
    node.$(SqlNodeFields.ColDef_RawType, dataTypeCtx.getText());
    node.$(SqlNodeFields.ColDef_DataType, parseDataType(dataTypeCtx));

    for (var constr : ctx.constraint_common()) addConstraint(node, constr.constr_body());

    return node;
  }

  @Override
  public SqlNode visitConstraint_common(PGParser.Constraint_commonContext ctx) {
    final SqlNode node = mkNode(SqlKind.IndexDef);
    if (ctx.identifier() != null) node.$(SqlNodeFields.IndexDef_Name, stringifyIdentifier(ctx.identifier()));
    // only EXCLUDE and FOREIGN key can be defined at table level
    final var bodyCtx = ctx.constr_body();
    if (bodyCtx.REFERENCES() != null) {
      node.$(SqlNodeFields.IndexDef_Cons, ConstraintKind.FOREIGN);
      final var namesLists = bodyCtx.names_in_parens();

      // should be 2, otherwise it's actually invalid
      if (namesLists.size() == 2)
        node.$(SqlNodeFields.IndexDef_Keys, mkKeyParts(namesLists.get(0).names_references()));

      final SqlNode ref = mkNode(SqlKind.Reference);
      ref.$(SqlNodeFields.Reference_Table, mkTableName(stringifyIdentifier(bodyCtx.schema_qualified_name())));
      if (bodyCtx.ref != null) ref.$(SqlNodeFields.Reference_Cols, mkColNames(bodyCtx.ref.names_references()));

      node.$(SqlNodeFields.IndexDef_Refs, ref);

    } else if (bodyCtx.UNIQUE() != null || bodyCtx.PRIMARY() != null) {
      node.$(SqlNodeFields.IndexDef_Cons, bodyCtx.UNIQUE() != null ? UNIQUE : PRIMARY);
      node.$(SqlNodeFields.IndexDef_Keys, mkKeyParts(bodyCtx.names_in_parens(0).names_references()));

    } else return null;

    return node;
  }

  @Override
  public SqlNode visitAlter_sequence_statement(PGParser.Alter_sequence_statementContext ctx) {
    final SqlNode node = mkNode(SqlKind.AlterSeq);
    node.$(SqlNodeFields.AlterSeq_Name, mkName3(stringifyIdentifier(ctx.name)));

    for (var bodyCtx : ctx.sequence_body()) {
      if (bodyCtx.OWNED() != null) {
        node.$(SqlNodeFields.AlterSeq_Op, "owned_by");
        node.$(SqlNodeFields.AlterSeq_Payload, mkColName(stringifyIdentifier(bodyCtx.col_name)));
      }
      // currently we only handle ALTER .. OWNED BY.
      // because it makes a column auto-increment, which concerns data gen
    }

    return node;
  }

  @Override
  public SqlNode visitAlter_table_statement(PGParser.Alter_table_statementContext ctx) {
    final SqlNode node = mkNode(SqlKind.AlterTable);
    node.$(SqlNodeFields.AlterTable_Name, mkTableName(stringifyIdentifier(ctx.name)));

    final List<SqlNode> actions = new ArrayList<>();

    final var actionCtxs = ctx.table_action();
    if (actionCtxs != null)
      for (var actionCtx : actionCtxs)
        if (actionCtx.tabl_constraint != null) {
          final SqlNode action = mkNode(SqlKind.AlterTableAction);
          action.$(SqlNodeFields.AlterTableAction_Name, "add_constraint");
          final SqlNode constraint = actionCtx.tabl_constraint.accept(this);
          if (constraint == null) actionCtx.tabl_constraint.accept(this);
          action.$(SqlNodeFields.AlterTableAction_Payload, constraint);
          actions.add(action);
        }

    // we only care about ADD CONSTRAINT fow now

    node.$(SqlNodeFields.AlterTable_Actions, mkNodes(actions));
    return node;
  }

  @Override
  public SqlNode visitCreate_index_statement(PGParser.Create_index_statementContext ctx) {
    final SqlNode node = mkNode(SqlKind.IndexDef);
    node.$(SqlNodeFields.IndexDef_Table, mkTableName(stringifyIdentifier(ctx.table_name)));
    node.$(SqlNodeFields.IndexDef_Name, stringifyIdentifier(ctx.name));
    if (ctx.UNIQUE() != null) node.$(SqlNodeFields.IndexDef_Cons, UNIQUE);

    final var restCtx = ctx.index_rest();
    if (restCtx.method != null) {
      final var methodText = restCtx.method.getText().toLowerCase();
      node.$(SqlNodeFields.IndexDef_Kind, parseIndexKind(methodText));
    }

    final List<SqlNode> keyParts =
        ListSupport.map(
            (Iterable<PGParser.Sort_specifierContext>)
                restCtx.index_sort().sort_specifier_list().sort_specifier(),
            (Function<? super PGParser.Sort_specifierContext, ? extends SqlNode>) this::toKeyPart);

    node.$(SqlNodeFields.IndexDef_Keys, mkNodes(keyParts));

    return node;
  }

  @Override
  public SqlNode visitSelect_stmt(PGParser.Select_stmtContext ctx) {
    if (ctx.with_clause() != null) return null;
    final SqlNode body = ctx.select_ops().accept(this);
    if (body == null) return null;

    final SqlNode node = mkNode(SqlKind.Query);
    node.$(SqlNodeFields.Query_Body, body);
    ctx.after_ops().forEach(it -> addAfterOp(node, it));
    return node;
  }

  @Override
  public SqlNode visitSelect_stmt_no_parens(PGParser.Select_stmt_no_parensContext ctx) {
    if (ctx.with_clause() != null) return null;

    final SqlNode node = mkNode(SqlKind.Query);
    node.$(SqlNodeFields.Query_Body, ctx.select_ops_no_parens().accept(this));
    ctx.after_ops().forEach(it -> addAfterOp(node, it));
    return node;
  }

  @Override
  public SqlNode visitSelect_ops(PGParser.Select_opsContext ctx) {
    if (ctx.select_stmt() != null) return ctx.select_stmt().accept(this);
    else if (ctx.select_primary() != null) return ctx.select_primary().accept(this);
    else {
      final SqlNode node = mkNode(SqlKind.SetOp);
      node.$(SqlNodeFields.SetOp_Left, wrapAsQuery(ctx.select_ops(0).accept(this)));
      node.$(SqlNodeFields.SetOp_Right, wrapAsQuery(ctx.select_ops(1).accept(this)));

      final SetOpKind op;
      if (ctx.UNION() != null) op = UNION;
      else if (ctx.INTERSECT() != null) op = INTERSECT;
      else if (ctx.EXCEPT() != null) op = EXCEPT;
      else return assertFalse();
      node.$(SqlNodeFields.SetOp_Kind, op);

      if (ctx.set_qualifier() != null)
        node.$(SqlNodeFields.SetOp_Option, SetOpOption.valueOf(ctx.set_qualifier().getText().toUpperCase()));
      return node;
    }
  }

  @Override
  public SqlNode visitSelect_ops_no_parens(PGParser.Select_ops_no_parensContext ctx) {
    if (ctx.select_ops() != null) {
      final SqlNode node = mkNode(SqlKind.SetOp);
      node.$(SqlNodeFields.SetOp_Left, wrapAsQuery(ctx.select_ops().accept(this)));
      node.$(
          SqlNodeFields.SetOp_Right,
          wrapAsQuery(
              ctx.select_stmt() != null
                  ? ctx.select_stmt().accept(this)
                  : ctx.select_primary().accept(this)));

      final SetOpKind op;
      if (ctx.UNION() != null) op = UNION;
      else if (ctx.INTERSECT() != null) op = INTERSECT;
      else if (ctx.EXCEPT() != null) op = EXCEPT;
      else return assertFalse();
      node.$(SqlNodeFields.SetOp_Kind, op);

      final SetOpOption option =
          ctx.set_qualifier() == null
              ? SetOpOption.DISTINCT
              : SetOpOption.valueOf(ctx.set_qualifier().getText().toUpperCase());
      node.$(SqlNodeFields.SetOp_Option, option);

      return node;

    } else if (ctx.select_primary() != null) {
      return ctx.select_primary().accept(this);
    } else {
      return assertFalse();
    }
  }

  @Override
  public SqlNode visitSelect_primary(PGParser.Select_primaryContext ctx) {
    if (ctx.values_stmt() != null) return null;

    final SqlNode node = mkNode(SqlKind.QuerySpec);

    if (ctx.set_qualifier() != null && ctx.set_qualifier().DISTINCT() != null) {
      node.flag(SqlNodeFields.QuerySpec_Distinct);
      if (ctx.distinct != null)
        node.$(
            SqlNodeFields.QuerySpec_DistinctOn,
            mkNodes(
                ListSupport.map(
                    (Iterable<PGParser.VexContext>) ctx.distinct,
                    (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));
    }

    if (ctx.select_list() != null)
      node.$(
          SqlNodeFields.QuerySpec_SelectItems,
          mkNodes(
              ListSupport.map(
                  (Iterable<PGParser.Select_sublistContext>) ctx.select_list().select_sublist(),
                  (Function<? super PGParser.Select_sublistContext, ? extends SqlNode>)
                      this::visitSelect_sublist)));

    final var fromItems = ctx.from_item();
    if (fromItems != null) {
      final SqlNode tableSourceNode =
          fromItems.stream()
              .map(this::visitFrom_item)
              .reduce((left, right) -> mkJoined(left, right, JoinKind.CROSS_JOIN))
              .orElse(null);
      node.$(SqlNodeFields.QuerySpec_From, tableSourceNode);
    }

    if (ctx.where != null) node.$(SqlNodeFields.QuerySpec_Where, ctx.where.accept(this));
    if (ctx.having != null) node.$(SqlNodeFields.QuerySpec_Having, ctx.having.accept(this));

    if (ctx.groupby_clause() != null)
      node.$(
          SqlNodeFields.QuerySpec_GroupBy,
          mkNodes(
              ListSupport.map(
                  (Iterable<PGParser.Grouping_elementContext>)
                      ctx.groupby_clause().grouping_element_list().grouping_element(),
                  (Function<? super PGParser.Grouping_elementContext, ? extends SqlNode>)
                      this::visitGrouping_element)));

    // TODO: WINDOW

    return node;
  }

  @Override
  public SqlNode visitFrom_item(PGParser.From_itemContext ctx) {
    if (ctx.LEFT_PAREN() != null) return ctx.from_item(0).accept(this);
    if (ctx.from_primary() != null) return ctx.from_primary().accept(this);

    final SqlNode left = ctx.from_item(0).accept(this);
    final SqlNode right = ctx.from_item(1).accept(this);
    final SqlNode node = mkJoined(left, right, parseJoinKind(ctx));
    if (ctx.vex() != null) node.$(TableSourceFields.Joined_On, ctx.vex().accept(this));
    // TODO: USING

    return node;
  }

  @Override
  public SqlNode visitFrom_primary(PGParser.From_primaryContext ctx) {
    if (ctx.schema_qualified_name() != null) {
      final SqlNode node = mkTableSource(TableSourceKind.SimpleSource);
      node.$(TableSourceFields.Simple_Table, mkTableName(stringifyIdentifier(ctx.schema_qualified_name())));
      if (ctx.alias_clause() != null) node.$(TableSourceFields.Simple_Alias, parseAlias(ctx.alias_clause()));
      return node;

    } else if (ctx.table_subquery() != null) {
      final SqlNode node = mkTableSource(TableSourceKind.DerivedSource);
      final SqlNode subquery = ctx.table_subquery().accept(this);
      if (subquery == null) throw new UnsupportedOperationException("unsupported table source");

      node.$(TableSourceFields.Derived_Subquery, subquery);
      if (ctx.alias_clause() != null) node.$(TableSourceFields.Derived_Alias, parseAlias(ctx.alias_clause()));
      return node;
    }
    // TODO: other table sources
    throw new UnsupportedOperationException("unsupported table source");
  }

  @Override
  public SqlNode visitTable_subquery(PGParser.Table_subqueryContext ctx) {
    return ctx.select_stmt().accept(this);
  }

  @Override
  public SqlNode visitSelect_sublist(PGParser.Select_sublistContext ctx) {
    final SqlNode node = mkNode(SqlKind.SelectItem);
    node.$(SqlNodeFields.SelectItem_Expr, ctx.vex().accept(this));

    if (ctx.col_label() != null) node.$(SqlNodeFields.SelectItem_Alias, stringifyIdentifier(ctx.col_label()));
    else if (ctx.id_token() != null) node.$(SqlNodeFields.SelectItem_Alias, stringifyIdentifier(ctx.id_token()));

    return node;
  }

  @Override
  public SqlNode visitGrouping_element(PGParser.Grouping_elementContext ctx) {
    if (ctx.vex() != null) return mkGroupItem(ctx.vex().accept(this));
    else return null; // TODO
  }

  @Override
  public SqlNode visitVex(PGParser.VexContext ctx) {
    if (ctx.CAST_EXPRESSION() != null) {
      final SqlNode node = mkExpr(ExprKind.Cast);
      node.$(ExprFields.Cast_Expr, ctx.vex(0).accept(this));
      node.$(ExprFields.Cast_Type, parseDataType(ctx.data_type()));
      return node;

    } else if (ctx.collate_identifier() != null) {
      final SqlNode node = mkExpr(ExprKind.Collate);
      node.$(ExprFields.Collate_Expr, ctx.vex(0).accept(this));
      node.$(ExprFields.Collate_Collation, mkName3(stringifyIdentifier(ctx.collate_identifier().collation)));
      return node;

    } else if (ctx.value_expression_primary() != null) {
      return ctx.value_expression_primary().accept(this);

    } else if (ctx.unary_operator != null) {
      return mkUnary(ctx.vex(0).accept(this), UnaryOpKind.ofOp(ctx.unary_operator.getText()));

    } else if (ctx.binary_operator != null) {
      final SqlNode left = ctx.vex(0).accept(this);
      final SqlNode right = ctx.vex(1).accept(this);
      final SqlNode node =
          mkBinary(
              left,
              right,
              ctx.EXP() != null ? EXP : BinaryOpKind.ofOp(ctx.binary_operator.getText()));
      if (ExprKind.ComparisonMod.isInstance(right)) {
        node.$(ExprFields.Binary_Right, copyAst(right.$(ExprFields.ComparisonMod_Expr), ast));
        node.$(ExprFields.Binary_SubqueryOption, right.$(ExprFields.ComparisonMod_Option));
      }
      return ctx.NOT() != null ? mkUnary(node, NOT) : node;

    } else if (ctx.AT() != null) {
      return mkBinary(ctx.vex(0).accept(this), ctx.vex(1).accept(this), AT_TIME_ZONE);

    } else if (ctx.SIMILAR() != null) {
      return mkBinary(ctx.vex(0).accept(this), ctx.vex(1).accept(this), SIMILAR_TO);

    } else if (ctx.ISNULL() != null) {
      return mkBinary(ctx.vex(0).accept(this), mkLiteral(LiteralKind.NULL, null), IS);

    } else if (ctx.NOTNULL() != null) {
      return mkUnary(mkBinary(ctx.vex(0).accept(this), mkLiteral(LiteralKind.NULL, null), IS), NOT);

    } else if (ctx.IN() != null) {
      final SqlNode left = ctx.vex(0).accept(this);
      final SqlNode node;
      if (ctx.select_stmt_no_parens() != null)
        node =
            mkBinary(left, wrapAsQueryExpr(ctx.select_stmt_no_parens().accept(this)), IN_SUBQUERY);
      else {
        final var vexs = ctx.vex();
        final SqlNode tuple = mkExpr(ExprKind.Tuple);
        tuple.$(
            ExprFields.Tuple_Exprs,
            mkNodes(
                ListSupport.map(
                    (Iterable<PGParser.VexContext>) vexs.subList(1, vexs.size()),
                    (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));
        node = mkBinary(left, tuple, IN_LIST);
      }
      return ctx.NOT() == null ? node : mkUnary(node, NOT);

    } else if (ctx.BETWEEN() != null) {
      final SqlNode node = mkExpr(ExprKind.Ternary);
      node.$(ExprFields.Ternary_Op, TernaryOp.BETWEEN_AND);
      node.$(ExprFields.Ternary_Left, ctx.vex(0).accept(this));
      node.$(ExprFields.Ternary_Middle, ctx.vex_b().accept(this));
      node.$(ExprFields.Ternary_Right, ctx.vex(1).accept(this));
      return ctx.NOT() == null ? node : mkUnary(node, NOT);

    } else if (ctx.IS() != null) {
      final SqlNode left = ctx.vex(0).accept(this);

      final SqlNode right;
      if (ctx.truth_value() != null) right = mkLiteral(LiteralKind.BOOL, parseTruthValue(ctx.truth_value()));
      else if (ctx.NULL() != null) right = mkLiteral(LiteralKind.NULL, null);
      else if (ctx.DOCUMENT() != null) right = mkSymbol("DOCUMENT");
      else if (ctx.UNKNOWN() != null) right = mkLiteral(LiteralKind.UNKNOWN, null);
      else if (ctx.vex(1) != null) right = ctx.vex(1).accept(this);
      // TODO: IS OF
      else return assertFalse();

      final BinaryOpKind op;
      if (ctx.DISTINCT() == null) op = IS;
      else op = IS_DISTINCT_FROM;

      final SqlNode node = mkBinary(left, right, op);
      return ctx.NOT() != null ? mkUnary(node, NOT) : node;

    } else if (ctx.op() != null) {
      final var opCtx = ctx.op();
      if (opCtx.OPERATOR() != null) return null; // TODO: custom operator
      final String opString = ctx.op().getText();
      if (ctx.left != null || ctx.right != null) {
        final UnaryOpKind op = UnaryOpKind.ofOp(opString);
        if (op == null) return null;
        return mkUnary(coalesce(ctx.left, ctx.right).accept(this), op);

      } else {
        final BinaryOpKind op = BinaryOpKind.ofOp(opString);
        if (op == null) return null;
        final SqlNode left = ctx.vex(0).accept(this);
        final SqlNode right = ctx.vex(1).accept(this);

        if (!ExprKind.Array.isInstance(left) && !ExprKind.Array.isInstance(right) && op == CONCAT) {
          final SqlNode funcCallNode = mkExpr(ExprKind.FuncCall);
          funcCallNode.$(ExprFields.FuncCall_Name, mkName2(null, "concat"));
          funcCallNode.$(ExprFields.FuncCall_Args, mkNodes(Arrays.asList(left, right)));
          return funcCallNode;
        }

        return op != ARRAY_CONTAINED_BY
            ? mkBinary(left, right, op)
            : mkBinary(right, left, ARRAY_CONTAINS);
      }

    } else if (ctx.vex().size() == 1) {
      final SqlNode node = ctx.vex(0).accept(this);
      return ctx.indirection_list() == null
          ? node
          : mkIndirection(node, parseIndirectionList(ctx.indirection_list()));

    } else if (ctx.vex().size() >= 1) {
      final SqlNode tuple = mkExpr(ExprKind.Tuple);
      tuple.$(
          ExprFields.Tuple_Exprs,
          mkNodes(
              ListSupport.map(
                  (Iterable<PGParser.VexContext>) ctx.vex(),
                  (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));
      return tuple;

    } else return assertFalse();
  }

  @Override
  public SqlNode visitVex_b(PGParser.Vex_bContext ctx) {
    if (ctx.CAST_EXPRESSION() != null) {
      final SqlNode node = mkExpr(ExprKind.Cast);
      node.$(ExprFields.Cast_Expr, ctx.vex_b(0).accept(this));
      node.$(ExprFields.Cast_Type, parseDataType(ctx.data_type()));
      return node;

    } else if (ctx.value_expression_primary() != null) {
      return ctx.value_expression_primary().accept(this);

    } else if (ctx.unary_operator != null) {
      return mkUnary(ctx.vex_b(0).accept(this), UnaryOpKind.ofOp(ctx.unary_operator.getText()));

    } else if (ctx.binary_operator != null) {
      final SqlNode left = ctx.vex_b(0).accept(this);
      final SqlNode right = ctx.vex_b(1).accept(this);
      final SqlNode node =
          mkBinary(
              left,
              right,
              ctx.EXP() != null ? EXP : BinaryOpKind.ofOp(ctx.binary_operator.getText()));

      if (ExprKind.ComparisonMod.isInstance(right)) {
        node.$(ExprFields.Binary_Right, right.$(ExprFields.ComparisonMod_Expr));
        node.$(ExprFields.Binary_SubqueryOption, right.$(ExprFields.ComparisonMod_Option));
      }
      return ctx.NOT() != null ? mkUnary(node, NOT) : node;

    } else if (ctx.IS() != null) {
      final SqlNode left = ctx.vex_b(0).accept(this);

      final SqlNode right;
      if (ctx.DOCUMENT() != null) right = mkSymbol("DOCUMENT");
      else if (ctx.UNKNOWN() != null) right = mkLiteral(LiteralKind.UNKNOWN, null);
      else if (ctx.vex_b(1) != null) right = ctx.vex_b(1).accept(this);
      // TODO: omit IS OF for now
      else return assertFalse();

      final BinaryOpKind op;
      if (ctx.DISTINCT() == null) op = IS;
      else op = IS_DISTINCT_FROM;

      final SqlNode node = mkBinary(left, right, op);
      return ctx.NOT() != null ? mkUnary(node, NOT) : node;

    } else if (ctx.op() != null) {
      final var opCtx = ctx.op();
      if (opCtx.OPERATOR() != null) return null; // TODO: custom operator
      final String opString = ctx.op().getText();
      if (ctx.left != null || ctx.right != null) {
        final UnaryOpKind op = UnaryOpKind.ofOp(opString);
        if (op == null) return null;
        return mkUnary(coalesce(ctx.left, ctx.right).accept(this), op);

      } else {
        final BinaryOpKind op = BinaryOpKind.ofOp(opString);
        if (op == null) return null;
        final SqlNode left = ctx.vex_b(0).accept(this);
        final SqlNode right = ctx.vex_b(1).accept(this);

        if (!ExprKind.Array.isInstance(left) && !ExprKind.Array.isInstance(right) && op == CONCAT) {
          final SqlNode funcCallNode = mkExpr(ExprKind.FuncCall);
          funcCallNode.$(ExprFields.FuncCall_Name, mkName2(null, "concat"));
          funcCallNode.$(ExprFields.FuncCall_Args, mkNodes(Arrays.asList(left, right)));
          return funcCallNode;
        }

        return op != ARRAY_CONTAINED_BY
            ? mkBinary(left, right, op)
            : mkBinary(right, left, ARRAY_CONTAINS);
      }

    } else if (ctx.vex().size() == 1) {
      final SqlNode node = ctx.vex(0).accept(this);
      return ctx.indirection_list() == null
          ? node
          : mkIndirection(node, parseIndirectionList(ctx.indirection_list()));

    } else if (ctx.vex().size() >= 1) {
      final SqlNode tuple = mkExpr(ExprKind.Tuple);
      tuple.$(
          ExprFields.Tuple_Exprs,
          mkNodes(
              ListSupport.map(
                  (Iterable<PGParser.VexContext>) ctx.vex(),
                  (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));
      return tuple;

    } else return assertFalse();
  }

  @Override
  public SqlNode visitValue_expression_primary(PGParser.Value_expression_primaryContext ctx) {
    if (ctx.unsigned_value_specification() != null)
      return parseUnsignedLiteral(ctx.unsigned_value_specification());
    else if (ctx.NULL() != null) return mkLiteral(LiteralKind.NULL, null);
    else if (ctx.MULTIPLY() != null) return mkWildcard(null);
    else if (ctx.EXISTS() != null) {
      final SqlNode node = mkExpr(ExprKind.Exists);
      node.$(ExprFields.Exists_Subquery, wrapAsQueryExpr(ctx.table_subquery().accept(this)));
      return node;

    } else if (ctx.select_stmt_no_parens() != null) {
      final SqlNode queryExpr = mkExpr(ExprKind.QueryExpr);
      queryExpr.$(ExprFields.QueryExpr_Query, ctx.select_stmt_no_parens().accept(this));
      return ctx.indirection_list() == null
          ? queryExpr
          : mkIndirection(queryExpr, parseIndirectionList(ctx.indirection_list()));

    } else if (ctx.case_expression() != null) {
      return ctx.case_expression().accept(this);

    } else if (ctx.comparison_mod() != null) {
      return ctx.comparison_mod().accept(this);

    } else if (ctx.function_call() != null) {
      return ctx.function_call().accept(this);

    } else if (ctx.indirection_var() != null) {
      return ctx.indirection_var().accept(this);

    } else if (ctx.type_coercion() != null) {
      return ctx.type_coercion().accept(this);

    } else if (ctx.datetime_overlaps() != null) {
      return ctx.datetime_overlaps().accept(this);

    } else if (ctx.array_expression() != null) {
      return ctx.array_expression().accept(this);

    } else return assertFalse();
  }

  @Override
  public SqlNode visitIndirection_var(PGParser.Indirection_varContext ctx) {
    if (ctx.dollar_number() != null) {
      final SqlNode param = parseParam(ctx.dollar_number());
      if (ctx.indirection_list() != null) {
        final SqlNodes indirections = parseIndirectionList(ctx.indirection_list());
        final SqlNode indirection = mkExpr(ExprKind.Indirection);
        indirection.$(ExprFields.Indirection_Expr, param);
        indirection.$(ExprFields.Indirection_Comps, indirections);
        return indirection;
      } else return param;

    } else if (ctx.identifier() != null) {
      final String identifier = stringifyIdentifier(ctx.identifier());
      if (ctx.indirection_list() == null) return mkColRef(null, null, identifier);
      else {
        final SqlNodes indirections = parseIndirectionList(ctx.indirection_list());
        return buildIndirection(identifier, indirections);
      }

    } else return assertFalse();
  }

  @Override
  public SqlNode visitIndirection(PGParser.IndirectionContext ctx) {
    final SqlNode node = mkExpr(ExprKind.IndirectionComp);
    if (ctx.col_label() != null) {
      node.$(ExprFields.IndirectionComp_Start, mkSymbol(ctx.col_label().getText()));

    } else if (ctx.COLON() != null) {
      final var start = ctx.start;
      final var end = ctx.end;
      if (start != null) node.$(ExprFields.IndirectionComp_Start, start.accept(this));
      if (end != null) node.$(ExprFields.IndirectionComp_End, end.accept(this));
      node.flag(ExprFields.IndirectionComp_Subscript);

    } else {
      node.$(ExprFields.IndirectionComp_Start, ctx.vex(0).accept(this));
      node.flag(ExprFields.IndirectionComp_Subscript);
    }

    return node;
  }

  @Override
  public SqlNode visitCase_expression(PGParser.Case_expressionContext ctx) {
    final SqlNode _case = mkExpr(ExprKind.Case);
    if (ctx.condition != null) _case.$(ExprFields.Case_Cond, ctx.condition.accept(this));
    if (ctx.otherwise != null) _case.$(ExprFields.Case_Else, ctx.otherwise.accept(this));
    final var whens = ctx.when;
    final var thens = ctx.then;
    final List<SqlNode> whenNodes = new ArrayList<>(whens.size());
    for (int i = 0; i < whens.size(); i++) {
      final var when = whens.get(i);
      final var then = thens.get(i);
      final SqlNode whenNode = mkExpr(ExprKind.When);
      whenNode.$(ExprFields.When_Cond, when.accept(this));
      whenNode.$(ExprFields.When_Expr, then.accept(this));
      whenNodes.add(whenNode);
    }

    _case.$(ExprFields.Case_Whens, mkNodes(whenNodes));
    return _case;
  }

  @Override
  public SqlNode visitComparison_mod(PGParser.Comparison_modContext ctx) {
    final SqlNode node = mkExpr(ExprKind.ComparisonMod);

    final SubqueryOption option;
    if (ctx.ALL() != null) option = SubqueryOption.ALL;
    else if (ctx.ANY() != null) option = SubqueryOption.ANY;
    else if (ctx.SOME() != null) option = SubqueryOption.SOME;
    else option = null;

    node.$(ExprFields.ComparisonMod_Option, option);
    if (ctx.vex() != null) node.$(ExprFields.ComparisonMod_Expr, ctx.vex().accept(this));
    else if (ctx.select_stmt_no_parens() != null)
      node.$(ExprFields.ComparisonMod_Expr, wrapAsQueryExpr(ctx.select_stmt_no_parens().accept(this)));

    return node;
  }

  @Override
  public SqlNode visitArray_expression(PGParser.Array_expressionContext ctx) {
    if (ctx.array_elements() != null) return ctx.array_elements().accept(this);
    else if (ctx.table_subquery() != null) {
      final SqlNode node = mkExpr(ExprKind.Array);
      node.$(ExprFields.Array_Elements, mkNodes(singletonList(ctx.table_subquery().accept(this))));
      return node;
    }
    return assertFalse();
  }

  @Override
  public SqlNode visitArray_elements(PGParser.Array_elementsContext ctx) {
    final SqlNode node = mkExpr(ExprKind.Array);
    final List<SqlNode> elements =
        ListSupport.map(
            (Iterable<PGParser.Array_elementContext>) ctx.array_element(),
            (Function<? super PGParser.Array_elementContext, ? extends SqlNode>)
                this::visitArray_element);
    node.$(ExprFields.Array_Elements, mkNodes(elements));
    return node;
  }

  @Override
  public SqlNode visitArray_element(PGParser.Array_elementContext ctx) {
    if (ctx.vex() != null) return ctx.vex().accept(this);
    else if (ctx.array_elements() != null) return ctx.array_elements().accept(this);
    else return assertFalse();
  }

  @Override
  public SqlNode visitType_coercion(PGParser.Type_coercionContext ctx) {
    final SqlNode node = mkExpr(ExprKind.TypeCoercion);
    node.$(ExprFields.TypeCoercion_Type, parseDataType(ctx.data_type()));
    node.$(ExprFields.TypeCoercion_String, stringifyText(ctx.character_string()));
    return node;
  }

  @Override
  public SqlNode visitDatetime_overlaps(PGParser.Datetime_overlapsContext ctx) {
    final SqlNode node = mkExpr(ExprKind.DateTimeOverlap);
    node.$(ExprFields.DateTimeOverlap_LeftStart, ctx.vex(0).accept(this));
    node.$(ExprFields.DateTimeOverlap_LeftEnd, ctx.vex(1).accept(this));
    node.$(ExprFields.DateTimeOverlap_RightStart, ctx.vex(2).accept(this));
    node.$(ExprFields.DateTimeOverlap_RightEnd, ctx.vex(3).accept(this));
    return node;
  }

  @Override
  public SqlNode visitWindow_definition(PGParser.Window_definitionContext ctx) {
    final SqlNode node = mkNode(SqlKind.WindowSpec);

    if (ctx.identifier() != null) node.$(SqlNodeFields.WindowSpec_Name, stringifyIdentifier(ctx.identifier()));
    if (ctx.partition_by_columns() != null)
      node.$(
          SqlNodeFields.WindowSpec_Part,
          mkNodes(
              ListSupport.map(
                  (Iterable<PGParser.VexContext>) ctx.partition_by_columns().vex(),
                  (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));
    if (ctx.orderby_clause() != null) node.$(SqlNodeFields.WindowSpec_Order, toOrderItems(ctx.orderby_clause()));
    if (ctx.frame_clause() != null) node.$(SqlNodeFields.WindowSpec_Frame, ctx.frame_clause().accept(this));

    return node;
  }

  @Override
  public SqlNode visitFrame_clause(PGParser.Frame_clauseContext ctx) {
    final SqlNode node = mkNode(SqlKind.WindowFrame);

    if (ctx.RANGE() != null) node.$(SqlNodeFields.WindowFrame_Unit, WindowUnit.RANGE);
    else if (ctx.ROWS() != null) node.$(SqlNodeFields.WindowFrame_Unit, WindowUnit.ROWS);
    else if (ctx.GROUPS() != null) node.$(SqlNodeFields.WindowFrame_Unit, WindowUnit.GROUPS);

    node.$(SqlNodeFields.WindowFrame_Start, ctx.frame_bound(0).accept(this));
    if (ctx.BETWEEN() != null) node.$(SqlNodeFields.WindowFrame_End, ctx.frame_bound(1).accept(this));

    if (ctx.EXCLUDE() != null) {
      if (ctx.CURRENT() != null) node.$(SqlNodeFields.WindowFrame_Exclusion, WindowExclusion.CURRENT_ROW);
      else if (ctx.GROUP() != null) node.$(SqlNodeFields.WindowFrame_Exclusion, WindowExclusion.GROUP);
      else if (ctx.TIES() != null) node.$(SqlNodeFields.WindowFrame_Exclusion, WindowExclusion.TIES);
      else if (ctx.OTHERS() != null) node.$(SqlNodeFields.WindowFrame_Exclusion, WindowExclusion.NO_OTHERS);
    }

    return node;
  }

  @Override
  public SqlNode visitFrame_bound(PGParser.Frame_boundContext ctx) {
    final SqlNode node = mkNode(SqlKind.FrameBound);

    if (ctx.CURRENT() != null) node.$(SqlNodeFields.FrameBound_Expr, mkSymbol("current row"));
    else if (ctx.vex() != null) node.$(SqlNodeFields.FrameBound_Expr, ctx.vex().accept(this));

    if (ctx.PRECEDING() != null) node.$(SqlNodeFields.FrameBound_Direction, FrameBoundDirection.PRECEDING);
    else if (ctx.FOLLOWING() != null) node.$(SqlNodeFields.FrameBound_Direction, FrameBoundDirection.FOLLOWING);

    return node;
  }

  @Override
  public SqlNode visitFunction_call(PGParser.Function_callContext ctx) {
    if (ctx.schema_qualified_name_nontype() != null) {
      final String[] funcName = stringifyIdentifier(ctx.schema_qualified_name_nontype());
      return ctx.WITHIN() != null || ctx.OVER() != null || isAggregator(funcName)
          ? parseAggregate(ctx, funcName)
          : parseFuncCall(ctx, funcName);
    } else return parseFuncCall(ctx, null);
  }

  private void addConstraint(SqlNode node, PGParser.Constr_bodyContext ctx) {
    if (ctx.CHECK() != null) node.flag(SqlNodeFields.ColDef_Cons, CHECK);
    else if (ctx.NOT() != null) node.flag(SqlNodeFields.ColDef_Cons, ConstraintKind.NOT_NULL);
    else if (ctx.UNIQUE() != null) node.flag(SqlNodeFields.ColDef_Cons, UNIQUE);
    else if (ctx.PRIMARY() != null) node.flag(SqlNodeFields.ColDef_Cons, PRIMARY);
    else if (ctx.DEFAULT() != null) node.flag(SqlNodeFields.ColDef_Default);
    else if (ctx.GENERATED() != null) node.flag(SqlNodeFields.ColDef_Generated);
    else if (ctx.identity_body() != null) node.flag(SqlNodeFields.ColDef_AutoInc);
    else if (ctx.REFERENCES() != null) {
      final SqlNode ref = mkNode(SqlKind.Reference);
      ref.$(SqlNodeFields.Reference_Table, mkTableName(stringifyIdentifier(ctx.schema_qualified_name())));
      if (ctx.ref != null) ref.$(SqlNodeFields.Reference_Cols, mkColNames(ctx.ref.names_references()));

      node.$(SqlNodeFields.ColDef_Ref, ref);
    }
  }

  private void addAfterOp(SqlNode node, PGParser.After_opsContext ctx) {
    if (ctx.LIMIT() != null && ctx.ALL() == null) node.$(SqlNodeFields.Query_Limit, ctx.vex().accept(this));
    else if (ctx.FETCH() != null)
      node.$(SqlNodeFields.Query_Limit, ctx.vex() != null ? ctx.vex().accept(this) : mkLiteral(LiteralKind.INTEGER, 1));
    else if (ctx.OFFSET() != null) node.$(SqlNodeFields.Query_Offset, ctx.vex().accept(this));
    else if (ctx.orderby_clause() != null)
      node.$(SqlNodeFields.Query_OrderBy, toOrderItems(ctx.orderby_clause()));

    // TODO: FOR UPDATE and other options
  }

  private SqlNode toKeyPart(PGParser.Sort_specifierContext ctx) {
    final SqlNode node = mkNode(SqlKind.KeyPart);
    final SqlNode expr = ctx.vex().accept(this);

    if (ExprKind.ColRef.isInstance(expr)) node.$(SqlNodeFields.KeyPart_Col, expr.$(ExprFields.ColRef_ColName).$(SqlNodeFields.ColName_Col));
    else node.$(SqlNodeFields.KeyPart_Expr, expr);

    if (ctx.order != null) {
      final String direction = ctx.order.getText().toLowerCase();
      if ("asc".equals(direction)) node.$(SqlNodeFields.KeyPart_Direction, KeyDirection.ASC);
      else if ("desc".equals(direction)) node.$(SqlNodeFields.KeyPart_Direction, KeyDirection.DESC);
      // TODO: USING
    }

    // TODO: opclass
    // TODO: NULL ordering
    return node;
  }

  private SqlNode toOrderItem(PGParser.Sort_specifierContext ctx) {
    final SqlNode node = mkNode(SqlKind.OrderItem);
    final SqlNode expr = ctx.vex().accept(this);

    node.$(SqlNodeFields.OrderItem_Expr, expr);

    if (ctx.order != null) {
      final String direction = ctx.order.getText().toLowerCase();
      if ("asc".equals(direction)) node.$(SqlNodeFields.OrderItem_Direction, KeyDirection.ASC);
      else if ("desc".equals(direction)) node.$(SqlNodeFields.OrderItem_Direction, KeyDirection.DESC);
      // TODO: USING
    }

    // TODO: opclass
    // TODO: NULL ordering
    return node;
  }

  private SqlNodes toOrderItems(PGParser.Orderby_clauseContext ctx) {
    return mkNodes(
        ListSupport.map(
            (Iterable<PGParser.Sort_specifierContext>) ctx.sort_specifier_list().sort_specifier(),
            (Function<? super PGParser.Sort_specifierContext, ? extends SqlNode>)
                this::toOrderItem));
  }

  private SqlNode parseFuncCall(PGParser.Function_callContext ctx, String[] name) {
    final SqlNode node = mkExpr(ExprKind.FuncCall);
    if (name != null) {
      node.$(ExprFields.FuncCall_Name, mkName2(name));
      node.$(
          ExprFields.FuncCall_Args,
          mkNodes(
              ListSupport.map(
                  (Iterable<PGParser.Vex_or_named_notationContext>) ctx.vex_or_named_notation(),
                  (Function<? super PGParser.Vex_or_named_notationContext, ? extends SqlNode>)
                      this::visitVex_or_named_notation)));
      return node;

    } else if (ctx.function_construct() != null) {
      final var funcConstructCtx = ctx.function_construct();
      if (funcConstructCtx.funcName != null) {
        node.$(ExprFields.FuncCall_Name, mkName2(null, funcConstructCtx.funcName.getText()));
        node.$(
            ExprFields.FuncCall_Args,
            mkNodes(
                ListSupport.map(
                    (Iterable<PGParser.VexContext>) funcConstructCtx.vex(),
                    (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));
        return node;

      } else if (funcConstructCtx.ROW() != null) {
        final SqlNode tupleNode = mkExpr(ExprKind.Tuple);
        tupleNode.$(
            ExprFields.Tuple_Exprs,
            mkNodes(
                ListSupport.map(
                    (Iterable<PGParser.VexContext>) funcConstructCtx.vex(),
                    (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));
        tupleNode.flag(ExprFields.Tuple_AsRow);
        return tupleNode;
      } else return assertFalse();

    } else if (ctx.extract_function() != null) {
      final var extractFuncCtx = ctx.extract_function();
      node.$(ExprFields.FuncCall_Name, mkName2(null, "extract"));
      final SqlNode firstArg =
          mkSymbol(
              coalesce(
                  stringifyIdentifier(extractFuncCtx.identifier()),
                  stringifyText(extractFuncCtx.character_string())));
      final SqlNode secondArg = extractFuncCtx.vex().accept(this);
      node.$(ExprFields.FuncCall_Args, mkNodes(Arrays.asList(firstArg, secondArg)));
      return node;

    } else if (ctx.system_function() != null) {
      final var systemFuncCtx = ctx.system_function();
      if (systemFuncCtx.cast_specification() != null) {
        node.$(ExprFields.FuncCall_Name, mkName2(null, systemFuncCtx.getText()));
        node.$(ExprFields.FuncCall_Args, mkNodes(Collections.emptyList()));
        return node;

      } else {
        final var castCtx = systemFuncCtx.cast_specification();
        final SqlNode castNode = mkExpr(ExprKind.Cast);
        castNode.$(ExprFields.Cast_Expr, castCtx.vex().accept(this));
        castNode.$(ExprFields.Cast_Type, parseDataType(castCtx.data_type()));
        return castNode;
      }

    } else if (ctx.date_time_function() != null) {
      final var dateTimeFuncCtx = ctx.date_time_function();
      node.$(ExprFields.FuncCall_Name, mkName2(null, dateTimeFuncCtx.funcName.getText()));
      if (dateTimeFuncCtx.type_length() != null)
        node.$(
            ExprFields.FuncCall_Args,
            mkNodes(
                singletonList(mkLiteral(LiteralKind.INTEGER, typeLength2Int(dateTimeFuncCtx.type_length())))));
      else node.$(ExprFields.FuncCall_Args, mkNodes(Collections.emptyList()));

      return node;

    } else if (ctx.string_value_function() != null) {
      final var strValueFuncCtx = ctx.string_value_function();
      final String funcName = strValueFuncCtx.funcName.getText();
      node.$(ExprFields.FuncCall_Name, mkName2(null, funcName));

      if ("trim".equalsIgnoreCase(funcName)) {
        final TerminalNode firstArg =
            coalesce(strValueFuncCtx.LEADING(), strValueFuncCtx.TRAILING(), strValueFuncCtx.BOTH());
        final SqlNode arg0 = firstArg == null ? null : mkSymbol(firstArg.getText());
        final SqlNode arg1 =
            strValueFuncCtx.chars == null ? null : strValueFuncCtx.chars.accept(this);
        final SqlNode arg2 = strValueFuncCtx.str.accept(this);
        node.$(ExprFields.FuncCall_Args, mkNodes(Arrays.asList(arg0, arg1, arg2)));

      } else if ("position".equalsIgnoreCase(funcName)) {
        node.$(
            ExprFields.FuncCall_Args,
            mkNodes(
                Arrays.asList(
                    strValueFuncCtx.vex_b().accept(this), strValueFuncCtx.vex(0).accept(this))));

      } else
        node.$(
            ExprFields.FuncCall_Args,
            mkNodes(
                ListSupport.map(
                    (Iterable<PGParser.VexContext>) strValueFuncCtx.vex(),
                    (Function<? super PGParser.VexContext, ? extends SqlNode>) this::visitVex)));

      return node;

    } else if (ctx.xml_function() != null) {
      return null; // TODO

    } else return assertFalse();
  }

  private SqlNode parseAggregate(PGParser.Function_callContext ctx, String[] name) {
    final SqlNode node = mkExpr(ExprKind.Aggregate);
    node.$(ExprFields.Aggregate_Name, name[1]);

    if (ctx.set_qualifier() != null && ctx.set_qualifier().DISTINCT() != null)
      node.flag(ExprFields.Aggregate_Distinct);

    final var argsCtx = ctx.vex_or_named_notation();
    if (argsCtx != null) {
      final List<SqlNode> argExprs = new ArrayList<>(argsCtx.size());

      for (final var argCtx : argsCtx) {
        final SqlNode argNode = argCtx.vex().accept(this);

        argExprs.add(argNode);
        if (argCtx.VARIADIC() != null) argNode.flag(SqlNodeFields.Expr_FuncArgVariadic);
        if (argCtx.argname != null) argNode.$(SqlNodeFields.Expr_ArgName, stringifyIdentifier(argCtx.argname));
      }

      node.$(ExprFields.Aggregate_Args, mkNodes(argExprs));

    } else node.$(ExprFields.Aggregate_Args, mkNodes(Collections.emptyList()));

    if (ctx.order0 != null) node.$(ExprFields.Aggregate_Order, toOrderItems(ctx.order0));

    if (ctx.order1 != null) node.$(ExprFields.Aggregate_WithinGroupOrder, toOrderItems(ctx.order1));

    if (ctx.filter_clause() != null)
      node.$(ExprFields.Aggregate_Filter, ctx.filter_clause().vex().accept(this));

    if (ctx.identifier() != null)
      node.$(ExprFields.Aggregate_WindowName, stringifyIdentifier(ctx.identifier()));
    else if (ctx.window_definition() != null)
      node.$(ExprFields.Aggregate_WindowSpec, ctx.window_definition().accept(this));

    return node;
  }

  private SqlNode mkName2(String[] triple) {
    return mkName2(triple[0], triple[1]);
  }

  private SqlNode mkName3(String[] triple) {
    return mkName3(triple[0], triple[1], triple[2]);
  }

  private SqlNode mkTableName(String[] triple) {
    return mkTableName(triple[1], triple[2]);
  }

  private SqlNode mkColName(String[] triple) {
    return mkColName(triple[0], triple[1], triple[2]);
  }

  private SqlNodes mkColNames(PGParser.Names_referencesContext ctx) {
    final List<SqlNode> nodes =
        ctx.schema_qualified_name().stream()
            .map(PgAstHelper::stringifyIdentifier)
            .map(this::mkColName)
            .collect(Collectors.toList());
    return mkNodes(nodes);
  }

  private SqlNodes mkKeyParts(PGParser.Names_referencesContext ctx) {
    final var idContexts = ctx.schema_qualified_name();
    final List<SqlNode> keyParts = new ArrayList<>(idContexts.size());

    for (var idContext : idContexts) {
      final SqlNode keyPart = mkNode(SqlKind.KeyPart);
      keyPart.$(SqlNodeFields.KeyPart_Col, stringifyIdentifier(idContext)[2]);

      keyParts.add(keyPart);
    }

    return mkNodes(keyParts);
  }

  private SqlNode parseUnsignedLiteral(PGParser.Unsigned_value_specificationContext ctx) {
    final Object value = parseUnsignedValue(ctx);
    if (value instanceof Boolean) return mkLiteral(LiteralKind.BOOL, value);
    else if (value instanceof Double) return mkLiteral(LiteralKind.FRACTIONAL, value);
    else if (value instanceof Long)
      return (Long) value <= Integer.MAX_VALUE ? mkLiteral(LiteralKind.INTEGER, value) : mkLiteral(LiteralKind.LONG, value);
    else if (value instanceof String) return mkLiteral(LiteralKind.TEXT, value);
    else return assertFalse();
  }

  private SqlNode parseParam(PGParser.Dollar_numberContext ctx) {
    return mkParam(Integer.parseInt(ctx.getText().substring(1)));
  }

  private SqlNode buildIndirection(String id, List<SqlNode> indirections) {
    assert indirections.size() > 0;

    final SqlNode _0 = indirections.get(0);

    if (indirections.size() == 1) return buildIndirection1(id, _0);

    final SqlNode _1 = indirections.get(1);
    final SqlNode header = buildIndirection2(id, _0, _1);

    if (indirections.size() == 2) return header;

    assert ExprKind.ColRef.isInstance(header) || ExprKind.Indirection.isInstance(header);

    return ExprKind.ColRef.isInstance(header)
        ? mkIndirection(header, mkNodes(indirections.subList(2, indirections.size())))
        : mkIndirection(
            header.$(ExprFields.Indirection_Expr),
            mkNodes(
                header.$(ExprFields.Indirection_Comps).size() == 2
                    ? indirections
                    : indirections.subList(1, indirections.size())));
  }

  private SqlNode buildIndirection1(String id, SqlNode indirection) {
    if (!indirection.isFlag(ExprFields.IndirectionComp_Subscript)) {
      final SqlNode indirectionExpr = indirection.$(ExprFields.IndirectionComp_Start);
      if (ExprKind.Symbol.isInstance(indirectionExpr))
        return mkColRef(null, id, indirectionExpr.$(ExprFields.Symbol_Text));
      else if (ExprKind.Wildcard.isInstance(indirectionExpr)) return mkWildcard(mkTableName(null, id));
    }

    return mkIndirection(mkColRef(null, null, id), mkNodes(singletonList(indirection)));
  }

  private SqlNode buildIndirection2(String id, SqlNode _0, SqlNode _1) {
    if (_0.isFlag(ExprFields.IndirectionComp_Subscript))
      return mkIndirection(mkColRef(null, null, id), mkNodes(Arrays.asList(_0, _1)));

    final SqlNode expr0 = _0.$(ExprFields.IndirectionComp_Start);
    if (!ExprKind.Symbol.isInstance(expr0))
      return mkIndirection(mkColRef(null, null, id), mkNodes(Arrays.asList(_0, _1)));

    if (_1.isFlag(ExprFields.IndirectionComp_Subscript))
      return mkIndirection(buildIndirection1(id, expr0), mkNodes(singletonList(_1)));

    final SqlNode expr1 = _1.$(ExprFields.IndirectionComp_Start);

    if (ExprKind.Symbol.isInstance(expr1)) return mkColRef(id, expr0.$(ExprFields.Symbol_Text), expr1.$(ExprFields.Symbol_Text));
    else if (ExprKind.Wildcard.isInstance(expr1)) return mkWildcard(mkTableName(null, expr0.$(ExprFields.Symbol_Text)));
    else return mkIndirection(mkColRef(null, id, expr0.$(ExprFields.Symbol_Text)), mkNodes(singletonList(_1)));
  }

  private SqlNodes parseIndirectionList(PGParser.Indirection_listContext ctx) {
    final List<SqlNode> indirections =
        ListSupport.map(
            (Iterable<PGParser.IndirectionContext>) ctx.indirection(),
            (Function<? super PGParser.IndirectionContext, ? extends SqlNode>)
                this::visitIndirection);
    if (ctx.MULTIPLY() != null) {
      final SqlNode node = mkExpr(ExprKind.IndirectionComp);
      node.$(ExprFields.IndirectionComp_Start, mkWildcard(null));
      indirections.add(node);
    }
    return mkNodes(indirections);
  }
}
