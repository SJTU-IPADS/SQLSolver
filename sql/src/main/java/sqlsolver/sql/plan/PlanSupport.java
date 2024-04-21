package sqlsolver.sql.plan;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.constants.BinaryOpKind;
import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.ast.constants.LiteralKind;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.Table;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlContext;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sqlsolver.common.tree.TreeSupport.indexOfChild;
import static sqlsolver.common.utils.Commons.coalesce;
import static sqlsolver.common.utils.IterableSupport.all;
import static sqlsolver.sql.SqlSupport.*;
import static sqlsolver.sql.plan.DependentRefInspector.inspectDepRefs;

public abstract class PlanSupport {
  public static final String FAILURE_INVALID_QUERY = "invalid query ";
  public static final String FAILURE_INVALID_PLAN = "invalid plan ";
  public static final String FAILURE_UNSUPPORTED_FEATURE = "unsupported feature ";
  public static final String FAILURE_UNKNOWN_TABLE = "unknown table ";
  public static final String FAILURE_MISSING_PROJECTION = "missing projection ";
  public static final String FAILURE_MISSING_QUALIFICATION = "missing qualification ";
  public static final String FAILURE_MISSING_REF = "missing ref ";
  public static final String FAILURE_BAD_SUBQUERY_EXPR = "bad subquery expr ";

  static final String SYN_NAME_PREFIX = "%";
  static final String PLACEHOLDER_NAME = "#";

  private static final ThreadLocal<String> LAST_ERROR = new ThreadLocal<>();

  private PlanSupport() {
  }

  public static boolean isSupported(SqlNode ast) {
    final SqlContext ctx = ast.context();
    for (int i = 1; i <= ctx.maxNodeId(); i++) {
      if (!ctx.isPresent(i)) continue;
      //      if (ctx.fieldOf(i, Aggregate_WindowSpec) != null) return false;
      if (ctx.kindOf(i) == SqlKind.QuerySpec
              && ctx.fieldOf(i, SqlNodeFields.QuerySpec_From) == null) return false;
    }
    return true;
  }

  /**
   * Build a plan tree from AST. If `schema` is null then fallback to ast.context().schema()
   */
  public static PlanContext buildPlan(SqlNode ast, Schema schema) {
    final PlanBuilder builder = new PlanBuilder(ast, schema);
    if (builder.build()) return builder.plan();
    else {
      LAST_ERROR.set(builder.lastError());
      return null;
    }
  }

  /**
   * Set up values and resolve the value refs.
   */
  public static PlanContext resolvePlan(PlanContext plan) {
    final ValueRefBinder binder = new ValueRefBinder(plan);
    if (binder.bind()) return plan;
    else {
      LAST_ERROR.set(binder.lastError());
      return null;
    }
  }

  /**
   * Build a plan tree from AST, set up values and resolve the value refs. If `schema` is null then
   * fallback to ast.context().schema()
   */
  public static PlanContext assemblePlan(SqlNode ast, Schema schema) {
    PlanContext plan;
    if ((plan = buildPlan(ast, schema)) == null) return null;
    if ((plan = resolvePlan(plan)) == null) return null;
    return disambiguateQualification(plan);
  }

  public static SqlNode translateAsAst(PlanContext context, int nodeId, boolean allowIncomplete) {
    final ToAstTranslator translator = new ToAstTranslator(context);
    final SqlNode sql = translator.translate(nodeId, allowIncomplete);
    if (sql != null) return sql;
    else {
      LAST_ERROR.set(translator.lastError());
      return null;
    }
  }

  public static int locateNode(PlanContext ctx, int startPoint, int... pathExpr) {
    int node = startPoint;
    for (int direction : pathExpr) {
      if (direction < 0) node = ctx.parentOf(node);
      else node = ctx.childOf(node, direction);
    }
    return node;
  }

  public static String getLastError() {
    return LAST_ERROR.get();
  }

  public static boolean isEqualTree(PlanContext ctx0, int tree0, PlanContext ctx1, int tree1) {
    // Now we only support compare two Input nodes.
    final PlanNode node0 = ctx0.nodeAt(tree0), node1 = ctx1.nodeAt(tree1);
    if (node0.kind() != PlanKind.Input || node1.kind() != PlanKind.Input) return false;
    final Table t0 = ((InputNode) node0).table(), t1 = ((InputNode) node1).table();
    return t0.equals(t1);
  }

  public static String stringifyNode(PlanContext ctx, int id) {
    return PlanStringifier.stringifyNode(ctx, id, false, false);
  }

  public static String stringifyNode(PlanContext ctx, int id, boolean compact) {
    return PlanStringifier.stringifyNode(ctx, id, false, compact);
  }

  public static String stringifyTree(PlanContext ctx, int id) {
    return PlanStringifier.stringifyTree(ctx, id, false, false);
  }

  public static String stringifyTree(PlanContext ctx, int id, boolean compact) {
    return PlanStringifier.stringifyTree(ctx, id, compact, compact);
  }

  public static String stringifyTree(PlanContext ctx, int id, boolean compact, boolean oneLine) {
    return PlanStringifier.stringifyTree(ctx, id, oneLine, compact);
  }

  public static boolean isRootRef(PlanContext ctx, Value value) {
    return ctx.valuesReg().exprOf(value) == null;
  }

  /**
   * Returns the direct ref of the value. Returns null if the value is a base value (i.e., directly
   * derives from a table source) or is not a ColRef (i.e., a complex expression)
   */
  public static Value deRef(PlanContext ctx, Value value) {
    final ValuesRegistry valueReg = ctx.valuesReg();
    final Expression expr = valueReg.exprOf(value);
    if (expr == null) return null;
    if (!isColRef(expr)) return null;
    final Values refs = valueReg.valueRefsOf(expr);
    assert refs.size() == 1;
    return refs.get(0);
  }

  /**
   * Returns the root of ref chain.
   *
   * @return if (deRef(ctx, value) == null), then `value`, otherwise traceRef(ctx, deRef(ctx,
   * value()))
   */
  public static Value traceRef(PlanContext ctx, Value value) {
    final Value ref = deRef(ctx, value);
    return ref == null ? value : traceRef(ctx, ref);
  }

  public static List<Column> tryResolveColumns(PlanContext ctx, List<Value> values, boolean allowsNullColumn) {
    final List<Column> columns = new ArrayList<>(values.size());
    for (Value value : values) {
      final Column column = tryResolveColumn(ctx, value);
      if (column == null) {
        if (!allowsNullColumn) return null;
        // otherwise, ignore the null column
      } else columns.add(column);
    }
    return columns;
  }

  public static List<Column> tryResolveColumns(PlanContext ctx, List<Value> values) {
    return tryResolveColumns(ctx, values, false);
  }

  public static Column tryResolveColumn(PlanContext ctx, Value value) {
    final ValuesRegistry valueReg = ctx.valuesReg();
    final Expression expr = valueReg.exprOf(value);
    if (expr == null) return valueReg.columnOf(value);
    if (!isColRef(expr)) return null;

    final Values refs = valueReg.valueRefsOf(expr);
    assert refs.size() == 1;
    return tryResolveColumn(ctx, refs.get(0));
  }

  public static JoinKind joinKindOf(PlanContext ctx, int nodeId) {
    if (ctx.kindOf(nodeId) != PlanKind.Join) return null;
    final JoinKind joinKind = ctx.infoCache().getJoinKindOf(nodeId);
    if (joinKind != null) return joinKind;
    return ((JoinNode) ctx.nodeAt(nodeId)).joinKind();
  }

  public static boolean isDedup(PlanContext ctx, int nodeId) {
    final PlanKind nodeKind = ctx.kindOf(nodeId);
    if (nodeKind == PlanKind.SetOp) return ((SetOpNode) ctx.nodeAt(nodeId)).deduplicated();
    if (nodeKind == PlanKind.Proj) {
      final Boolean assigned = ctx.infoCache().getDeduplicatedOf(nodeId);
      if (assigned != null) return assigned;
      else return ((ProjNode) ctx.nodeAt(nodeId)).deduplicated();
    }
    return false;
  }

  public static boolean isUniqueCoreAt(PlanContext ctx, Collection<Value> attrs, int surfaceId) {
    return new UniquenessInference(ctx).isUniqueCoreAt(attrs, surfaceId);
  }

  public static boolean isNotNullAt(PlanContext ctx, Value attrs, int surfaceId) {
    return new NotNullInference(ctx).isNotNullAt(attrs, surfaceId);
  }

  // Must be invoked after `resolvePlan`
  public static PlanContext disambiguateQualification(PlanContext ctx) {
    final List<PlanNode> nodes = gatherNodes(ctx, EnumSet.of(PlanKind.Proj, PlanKind.Agg, PlanKind.Input));
    final Set<String> knownQualifications = new HashSet<>(nodes.size());
    final NameSequence seq = NameSequence.mkIndexed("q", 0);

    for (PlanNode node : nodes) {
      assert node instanceof Qualified;
      final Qualified qualified = (Qualified) node;

      if (!mustBeQualified(ctx, ctx.nodeIdOf(node))) continue;
      final String oldQualification = qualified.qualification();
      if (oldQualification != null && knownQualifications.add(qualified.qualification())) continue;

      final String newQualification;
      if (oldQualification != null) {
        int suffix = 0;
        while (true) {
          final String tmp = oldQualification + (suffix++);
          if (!knownQualifications.contains(tmp)) {
            newQualification = tmp;
            break;
          }
        }
      } else {
        newQualification = seq.nextUnused(knownQualifications);
      }

      knownQualifications.add(newQualification);

      qualified.setQualification(newQualification);
      for (Value value : ctx.valuesOf(node)) value.setQualification(newQualification);
    }

    return ctx;
  }

  static List<PlanNode> gatherNodes(PlanContext ctx, PlanKind kind) {
    final List<PlanNode> inputs = new ArrayList<>(8);

    for (int i = 0, bound = ctx.maxNodeId(); i <= bound; i++)
      if (ctx.isPresent(i) && ctx.kindOf(i) == kind) {
        inputs.add(ctx.nodeAt(i));
      }

    return inputs;
  }

  static List<PlanNode> gatherNodes(PlanContext ctx, EnumSet<PlanKind> kinds) {
    final List<PlanNode> inputs = new ArrayList<>(8);

    for (int i = 0, bound = ctx.maxNodeId(); i <= bound; i++)
      if (ctx.isPresent(i) && kinds.contains(ctx.kindOf(i))) {
        inputs.add(ctx.nodeAt(i));
      }

    return inputs;
  }

  public static List<Expression> getExprsIn(PlanContext plan, int treeRoot) {
    final PlanKind kind = plan.kindOf(treeRoot);
    switch (kind) {
      case Input:
      case Exists:
      case SetOp:
      case Limit:
        return emptyList();
      case Filter:
        return singletonList(((SimpleFilterNode) plan.nodeAt(treeRoot)).predicate());
      case InSub:
        return singletonList(((InSubNode) plan.nodeAt(treeRoot)).expr());
      case Proj:
        return ((ProjNode) plan.nodeAt(treeRoot)).attrExprs();
      case Agg: {
        final AggNode agg = (AggNode) plan.nodeAt(treeRoot);
        final List<Expression> exprs =
                new ArrayList<>(agg.attrExprs().size() + agg.groupByExprs().size() + 1);
        exprs.addAll(agg.attrExprs());
        exprs.addAll(agg.groupByExprs());
        if (agg.havingExpr() != null) exprs.add(agg.havingExpr());
        return exprs;
      }
      case Sort:
        return ((SortNode) plan.nodeAt(treeRoot)).sortSpec();
      case Join:
        return singletonList(((JoinNode) plan.nodeAt(treeRoot)).joinCond());
      default:
        throw new IllegalArgumentException("unsupported node: " + kind);
    }
  }

  public static List<Value> getRefBindingLookup(PlanContext plan, int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    final ValuesRegistry valuesReg = plan.valuesReg();
    if (kind == PlanKind.Sort) {
      // Order By can use the attributes exposed in table-source
      // e.g., Select t.x From t Order By t.y
      // So we have to lookup in deeper descendant.

      final int child = plan.childOf(nodeId, 0);
      final int grandChild = plan.childOf(child, 0);
      final PlanKind childKind = plan.kindOf(child);

      final Values secondaryLookup = valuesReg.valuesOf(child);
      final List<Value> primaryLookup;
      if (childKind == PlanKind.Proj) {
        primaryLookup = valuesReg.valuesOf(grandChild);
      } else if (childKind == PlanKind.Agg) {
        primaryLookup = valuesReg.valuesOf(plan.childOf(grandChild, 0));
      } else if (childKind == PlanKind.SetOp) {
        primaryLookup = emptyList();
      } else {
        assert false;
        return emptyList();
      }

      return ListSupport.join(primaryLookup, secondaryLookup);

    } else if (kind == PlanKind.Agg) {
      //final Values primaryLookup = valuesReg.valuesOf(nodeId);
      final Values secondaryLookup = valuesReg.valuesOf(plan.childOf(plan.childOf(nodeId, 0), 0));
      //return ListSupport.join(primaryLookup, secondaryLookup);
      return secondaryLookup;

    } else if (kind.numChildren() == 1 || kind.isSubqueryFilter()) {
      return valuesReg.valuesOf(plan.childOf(nodeId, 0));

    } else if (kind.numChildren() == 2) {
      return valuesReg.valuesOf(nodeId);
    }
    {
      assert false;
      return emptyList();
    }
  }

  public static List<Value> getRefBindingForeignLookup(PlanContext plan, int nodeId) {
    final ValuesRegistry valuesReg = plan.valuesReg();
    List<Value> foreignLookup = null;
    int parent = plan.parentOf(nodeId), child = nodeId;

    while (plan.isPresent(parent)) {
      if (plan.kindOf(parent).isSubqueryFilter() && indexOfChild(plan, child) == 1) {
        final List<Value> foreignLookup0 = valuesReg.valuesOf(plan.childOf(parent, 0));
        if (foreignLookup == null) foreignLookup = foreignLookup0;
        else foreignLookup = ListSupport.join(foreignLookup, foreignLookup0);
      }
      child = parent;
      parent = plan.parentOf(parent);
    }

    return coalesce(foreignLookup, emptyList());
  }

  public static void setupJoinKeyOf(PlanContext plan, int joinNodeId) {
    final ValuesRegistry valuesReg = plan.valuesReg();
    final JoinNode joinNode = (JoinNode) plan.nodeAt(joinNodeId);
    final Expression joinCond = joinNode.joinCond();

    if (!SqlSupport.isEquiJoinPredicate(joinCond.template())) return;

    final List<Value> valueRefs = valuesReg.valueRefsOf(joinCond);
    if ((valueRefs.size() & 1) == 1) return;

    final Values lhsValues = valuesReg.valuesOf(plan.childOf(joinNodeId, 0));
    final List<Value> lhsRefs = new ArrayList<>(valueRefs.size() >> 1);
    final List<Value> rhsRefs = new ArrayList<>(valueRefs.size() >> 1);
    for (int i = 0, bound = valueRefs.size(); i < bound; i += 2) {
      final Value key0 = valueRefs.get(i), key1 = valueRefs.get(i + 1);
      final boolean lhs0 = lhsValues.contains(key0), lhs1 = lhsValues.contains(key1);
      if (lhs0 && !lhs1) {
        lhsRefs.add(key0);
        rhsRefs.add(key1);
      } else if (!lhs0 && lhs1) {
        lhsRefs.add(key1);
        rhsRefs.add(key0);
      } else {
        return;
      }
    }

    plan.infoCache().putJoinKeyOf(joinNodeId, lhsRefs, rhsRefs);
  }

  public static boolean setupSubqueryExprOf(PlanContext plan, int nodeId) {
    /* Make expression for subquery */
    // e.g., The expr of "InSub<q0.a>(T, Proj<R.c>(Filter<p, T.b>(R)))" is
    //       "#.# IN (Select R.c From T Where p(#.#))"
    final PlanKind kind = plan.kindOf(nodeId);
    if (kind == PlanKind.InSub) return setupInSub(plan, nodeId);
    else if (kind == PlanKind.Exists) return setupExists(plan, nodeId);
    else throw new IllegalArgumentException("not a subquery filter: " + nodeId + " in " + plan);
  }

  private static boolean setupInSub(PlanContext plan, int nodeId) {
    final SqlNode inSubExprAst = mkInSubExpr(plan, nodeId);
    if (inSubExprAst == null) return false;

    final ValuesRegistry valuesReg = plan.valuesReg();
    final InSubNode inSub = (InSubNode) plan.nodeAt(nodeId);
    // collect the dependent refs from the subquery.
    final var deps = inspectDepRefs(plan, plan.childOf(nodeId, 1));
    final List<Value> depValueRefs = deps.getLeft();
    final List<SqlNode> depColRefs = deps.getRight();
    depValueRefs.addAll(0, valuesReg.valueRefsOf(inSub.expr()));
    depColRefs.addAll(0, inSub.expr().colRefs());

    final Expression inSubExpr = Expression.mk(inSubExprAst, depColRefs);
    valuesReg.bindValueRefs(inSubExpr, depValueRefs);
    plan.infoCache().putSubqueryExprOf(nodeId, inSubExpr);

    return true;
  }

  private static boolean setupExists(PlanContext plan, int nodeId) {
    final SqlNode existsExprAst = mkExistsExpr(plan, nodeId);
    if (existsExprAst == null) return false;

    final DependentRefInspector inspector = new DependentRefInspector(plan);
    inspector.inspect(plan.childOf(nodeId, 1));
    final List<Value> depValueRefs = inspector.dependentValueRefs();
    final List<SqlNode> depColRefs = inspector.dependentColRefs();
    final Expression existsExpr = Expression.mk(existsExprAst, depColRefs);
    plan.valuesReg().bindValueRefs(existsExpr, depValueRefs);
    plan.infoCache().putSubqueryExprOf(nodeId, existsExpr);
    plan.infoCache().putDependentNodesIn(nodeId, inspector.dependentNodes());

    return true;
  }

  //// Expression-related
  public static boolean isSimpleExprDetermineNotNull(Expression expr) {
    // Judge whether this predicate determines the parameter column is not null
    // e.g. WHERE col = 'str'
    if (expr == null) return false;
    if (!isSingleParamExpr(expr)) return false;
    if (!isSimpleBinaryExpression(expr.template(), false)) return false;

    return isSimpleExprDetermineNotNull0(expr.template());
  }

  private static boolean isSimpleExprDetermineNotNull0(SqlNode pred) {
    if (!ExprKind.Binary.isInstance(pred)) return false; // Do not consider other kinds of predicate

    // In binary predicate, cannot determine NotNull iff encountering `col IS NULL`
    if (colIsNullPredicate(pred)) return false;

    switch (pred.$(ExprFields.Binary_Op)) {
      case AND -> {
        return isSimpleExprDetermineNotNull0(pred.$(ExprFields.Binary_Left))
                || isSimpleExprDetermineNotNull0(pred.$(ExprFields.Binary_Right));
      }
      case OR -> {
        return isSimpleExprDetermineNotNull0(pred.$(ExprFields.Binary_Left))
                && isSimpleExprDetermineNotNull0(pred.$(ExprFields.Binary_Right));
      }
      default -> {
        return true;
      }
    }
  }

  public static boolean isSimpleIntArithmeticExpr(Expression expr) {
    // Only allow single colRef, which may appear multiple times
    if (expr == null) return false;
    if (!isConstantExpr(expr) && !isSingleParamExpr(expr)) return false;
    return isSimpleBinaryExpression(expr.template(), true);
  }

  public static boolean isSingleParamExpr(Expression expr) {
    if (expr == null || expr.colRefs().isEmpty()) return false;
    if (expr.colRefs().size() == 1) return true;

    for (SqlNode colRef : expr.colRefs())
      if (!colRef.toString().equals(expr.colRefs().get(0).toString()))
        return false;
    return true;
  }

  public static boolean isConstantExpr(Expression expr) {
    if (expr == null) return false;
    return expr.colRefs().isEmpty();
  }

  private static boolean isSimpleBinaryExpression(SqlNode pred, boolean intArithmetic) {
    if (ExprKind.ColRef.isInstance(pred)) return true;
    if (ExprKind.Literal.isInstance(pred))
      return !intArithmetic || pred.$(ExprFields.Literal_Kind) == LiteralKind.INTEGER;

    if (ExprKind.Binary.isInstance(pred)) {
      final SqlNode lhs = pred.$(ExprFields.Binary_Left);
      final SqlNode rhs = pred.$(ExprFields.Binary_Right);
      if (colIsNullPredicate(pred)) return ExprKind.ColRef.isInstance(lhs); // col IS NULL

      return isSimpleBinaryExpression(lhs, intArithmetic) && isSimpleBinaryExpression(rhs, intArithmetic);
    }
    return false;
  }

  public static boolean colIsNullPredicate(SqlNode pred) {
    return ExprKind.Binary.isInstance(pred)
            && pred.$(ExprFields.Binary_Op) == BinaryOpKind.IS
            && ExprKind.Literal.isInstance(pred.$(ExprFields.Binary_Right))
            && pred.$(ExprFields.Binary_Right).$(ExprFields.Literal_Kind) == LiteralKind.NULL;
  }

  public static boolean isColRef(Expression expr) {
    return ExprKind.ColRef.isInstance(expr.template());
  }

  public static boolean isColRefs(Expression expr) {
    final SqlNode ast = expr.template();
    if (ExprKind.ColRef.isInstance(ast)) return true;
    if (!ExprKind.Tuple.isInstance(ast)) return false;
    return all(ast.$(ExprFields.Tuple_Exprs), ExprKind.ColRef::isInstance);
  }

  public static Expression mkColRefExpr(Value value) {
    return Expression.mk(mkColRef(SqlContext.mk(2), value.qualification(), value.name()));
  }

  public static Expression mkColRefExpr() {
    return Expression.mk(mkColRef(SqlContext.mk(2), PLACEHOLDER_NAME, PLACEHOLDER_NAME));
  }

  public static Expression mkColRefsExpr(int count) {
    if (count == 1) return mkColRefExpr();

    final SqlContext sqlCtx = SqlContext.mk(count * 2 + 1);
    final TIntList refs = new TIntArrayList(count);
    for (int n = 0; n < count; ++n) {
      final SqlNode ref = mkColRef(sqlCtx, PLACEHOLDER_NAME, PLACEHOLDER_NAME);
      refs.add(ref.nodeId());
    }
    final SqlNodes refNodes = SqlNodes.mk(sqlCtx, refs);

    final SqlNode tuple = SqlNode.mk(sqlCtx, ExprKind.Tuple);
    tuple.$(ExprFields.Tuple_Exprs, refNodes);
    return Expression.mk(tuple);
  }

  public static Expression mkJoinCond(int numKeys) {
    if (numKeys <= 0) throw new IllegalArgumentException();

    final SqlContext sqlCtx = SqlContext.mk(6 * numKeys - 1);

    SqlNode expr = null;
    for (int i = 0; i < numKeys; i++) {
      final SqlNode lhsRef = mkColRef(sqlCtx, PLACEHOLDER_NAME, PLACEHOLDER_NAME);
      final SqlNode rhsRef = mkColRef(sqlCtx, PLACEHOLDER_NAME, PLACEHOLDER_NAME);
      final SqlNode eq = mkBinary(sqlCtx, BinaryOpKind.EQUAL, lhsRef, rhsRef);
      if (expr == null) expr = eq;
      else expr = mkBinary(sqlCtx, BinaryOpKind.AND, expr, eq);
    }
    return Expression.mk(expr);
  }

  static SqlNode normalizePredicate(SqlNode exprAst, SqlContext sqlCtx) {
    if (ExprKind.ColRef.isInstance(exprAst)) {
      final SqlNode literal = mkLiteral(sqlCtx, LiteralKind.BOOL, Boolean.TRUE);
      return mkBinary(sqlCtx, BinaryOpKind.IS, copyAst(exprAst, sqlCtx), literal);
    } else {
      return exprAst;
    }
  }

  static boolean isBoolConstant(SqlNode exprAst) {
    return ExprKind.Binary.isInstance(exprAst)
            && ExprKind.Literal.isInstance(exprAst.$(ExprFields.Binary_Left))
            && ExprKind.Literal.isInstance(exprAst.$(ExprFields.Binary_Right))
            // TODO: hack, need further modify
            && !(exprAst.$(ExprFields.Binary_Left).$(ExprFields.Literal_Kind) == LiteralKind.NULL
            && exprAst.$(ExprFields.Binary_Right).$(ExprFields.Literal_Kind) == LiteralKind.INTEGER
            && exprAst.$(ExprFields.Binary_Right).$(ExprFields.Literal_Value).equals((Integer) 1));
  }

  private static SqlNode mkInSubExpr(PlanContext plan, int nodeId) {
    final SqlNode query = translateAsAst(plan, plan.childOf(nodeId, 1), true);
    if (query == null) return null;

    final SqlContext sqlCtx = query.context();
    final SqlNode rhsExpr = SqlSupport.mkQueryExpr(sqlCtx, query);
    final SqlNode lhsExpr = copyAst(((InSubNode) plan.nodeAt(nodeId)).expr().template(), sqlCtx);
    return SqlSupport.mkBinary(sqlCtx, BinaryOpKind.IN_SUBQUERY, lhsExpr, rhsExpr);
  }

  private static SqlNode mkExistsExpr(PlanContext plan, int nodeId) {
    final SqlNode query = translateAsAst(plan, plan.childOf(nodeId, 1), true);
    if (query == null) return null;

    final SqlContext sqlCtx = query.context();
    final SqlNode queryExpr = SqlSupport.mkQueryExpr(sqlCtx, query);
    final SqlNode exists = SqlNode.mk(sqlCtx, ExprKind.Exists);
    exists.$(ExprFields.Exists_Subquery, queryExpr);
    return exists;
  }

  private static boolean mustBeQualified(PlanContext ctx, int nodeId) {
    if (ctx.kindOf(nodeId) == PlanKind.Input) return true;

    int parent = ctx.parentOf(nodeId), child = parent;
    while (ctx.isPresent(parent)) {
      final PlanKind parentKind = ctx.kindOf(parent);
      if (parentKind == PlanKind.SetOp) return false;
      if (parentKind == PlanKind.Proj || parentKind == PlanKind.Join) return true;
      if (parentKind.isFilter()) return ctx.childOf(parent, 0) == child;
      child = parent;
      parent = ctx.parentOf(parent);
    }
    return false;
  }

  public static boolean isSemanticError(PlanContext plan0, PlanContext plan1) {
    return new PlanError(plan0, plan1).isErrorTrees();
  }

  public static boolean isLiteralEq(PlanContext plan0, PlanContext plan1) {
    return new PlanEq(plan0, plan1).isEqTree();
  }

  public static boolean isLiteralEq(PlanContext plan0, int root0, PlanContext plan1, int root1) {
    return new PlanEq(plan0, plan1).isEqTree(root0, root1);
  }

  public static boolean isLiteralEq(String sql0, String sql1, String schema) {
    try {
      return new sqlsolver.sql.calcite.PlanEq(sql0, sql1, schema).isEqTree();
    } catch (Exception e) {
      return false;
    }
  }
}
