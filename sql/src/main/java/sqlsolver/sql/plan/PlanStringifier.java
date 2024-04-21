package sqlsolver.sql.plan;

import sqlsolver.sql.ast.constants.JoinKind;

import java.util.List;

import static sqlsolver.common.tree.TreeContext.NO_SUCH_NODE;
import static sqlsolver.sql.plan.PlanSupport.joinKindOf;

class PlanStringifier {
  private final PlanContext plan;
  private final ValuesRegistry values;
  private final StringBuilder builder;
  private final boolean oneLine;
  private final boolean compact;
  private int indentLevel;

  PlanStringifier(PlanContext plan, StringBuilder builder, boolean oneLine, boolean compact) {
    this.plan = plan;
    this.values = plan.valuesReg();
    this.builder = builder;
    this.oneLine = oneLine;
    this.compact = compact;
  }

  static String stringifyNode(PlanContext ctx, int id, boolean oneLine, boolean compact) {
    final PlanStringifier stringifier = new PlanStringifier(ctx, new StringBuilder(), oneLine, compact);
    stringifier.stringifyNode(id);
    return stringifier.builder.toString();
  }

  static String stringifyTree(PlanContext ctx, int id, boolean oneLine, boolean compact) {
    final PlanStringifier stringifier = new PlanStringifier(ctx, new StringBuilder(), oneLine, compact);
    stringifier.stringifyTree(id);
    return stringifier.builder.toString();
  }

  private void stringifyTree(int rootId) {
    stringifyNode(rootId);
    final int[] children = plan.childrenOf(rootId);
    int numChildren = 0;
    for (int child : children) if (child != NO_SUCH_NODE) numChildren++;

    ++indentLevel;
    if (numChildren > 0) {
      builder.append('(');
      stringifyTree(children[0]);
    }
    for (int i = 1; i < numChildren; ++i) {
      builder.append(',');
      stringifyTree(children[i]);
    }
    --indentLevel;
    if (numChildren > 0) builder.append(')');
  }

  private void stringifyNode(int nodeId) {
    appendIndent();
    switch (plan.kindOf(nodeId)) {
      case SetOp -> appendSetOp(nodeId);
      case Limit -> appendLimit(nodeId);
      case Sort -> appendSort(nodeId);
      case Agg -> appendAgg(nodeId);
      case Proj -> appendProj(nodeId);
      case Filter -> appendFilter(nodeId);
      case InSub -> appendInSub(nodeId);
      case Exists -> appendExists(nodeId);
      case Join -> appendJoin(nodeId);
      case Input -> appendInput(nodeId);
    }
  }

  private void appendSetOp(int nodeId) {
    final SetOpNode setOp = (SetOpNode) plan.nodeAt(nodeId);
    builder.append(setOp.opKind());
    appendNodeId(nodeId);
    if (setOp.deduplicated()) builder.append('*');
  }

  private void appendLimit(int nodeId) {
    final LimitNode limit = (LimitNode) plan.nodeAt(nodeId);
    builder.append("Limit");
    appendNodeId(nodeId);
    builder.append('{');
    if (limit.limit() != null) builder.append("limit=").append(limit.limit());
    if (limit.limit() != null && limit.offset() != null) builder.append(',');
    if (limit.offset() != null) builder.append("offset=").append(limit.offset());
    builder.append('}');
  }

  private void appendSort(int nodeId) {
    final SortNode sort = (SortNode) plan.nodeAt(nodeId);
    builder.append("Sort");
    appendNodeId(nodeId);
    builder.append("{[");
    for (Expression expr : sort.sortSpec()) builder.append(expr);
    builder.append("],refs=[");
    for (Expression expr : sort.sortSpec()) appendRefs(expr);
    builder.append("]}");
  }

  private void appendAgg(int nodeId) {
    final AggNode agg = (AggNode) plan.nodeAt(nodeId);
    final List<String> attrNames = agg.attrNames();
    final List<Expression> attrExprs = agg.attrExprs();
    final List<Expression> groupBys = agg.groupByExprs();
    final Expression having = agg.havingExpr();

    builder.append("Agg");
    appendNodeId(nodeId);
    builder.append("{[");
    appendSelectItems(attrExprs, attrNames);
    builder.append(']');
    if (!groupBys.isEmpty()) {
      builder.append(",group=[");
      for (Expression expr : groupBys) builder.append(expr).append(',');
      builder.append(']');
    }
    if (having != null) {
      builder.append(",having=").append(having);
    }
    builder.append(",refs=[");
    for (Expression expr : attrExprs) appendRefs(expr);
    for (Expression expr : groupBys) appendRefs(expr);
    if (having != null) appendRefs(having);
    builder.append("]");
    if (agg.qualification() != null) {
      builder.append(",qual=").append(agg.qualification());
    }
    builder.append("}");
  }

  private void appendProj(int nodeId) {
    final ProjNode proj = (ProjNode) plan.nodeAt(nodeId);
    final List<Expression> exprs = proj.attrExprs();
    final List<String> names = proj.attrNames();

    builder.append("Proj");
    if (PlanSupport.isDedup(plan, nodeId)) builder.append('*');
    appendNodeId(nodeId);
    builder.append("{[");
    appendSelectItems(exprs, names);
    builder.append("],refs=[");
    for (Expression expr : exprs) appendRefs(expr);
    builder.append("]");
    if (proj.qualification() != null) {
      builder.append(",qual=").append(proj.qualification());
    }
    builder.append("}");
  }

  private void appendFilter(int nodeId) {
    final SimpleFilterNode filter = (SimpleFilterNode) plan.nodeAt(nodeId);
    final Expression predicate = filter.predicate();
    builder.append("Filter");
    appendNodeId(nodeId);
    builder.append('{').append(predicate).append(",refs=[");
    appendRefs(predicate);
    builder.append("]}");
  }

  private void appendInSub(int nodeId) {
    final InSubNode filter = (InSubNode) plan.nodeAt(nodeId);
    final Expression expr = filter.expr();
    builder.append("InSub");
    appendNodeId(nodeId);
    builder.append('{').append(expr).append(",refs=[");
    appendRefs(expr);
    builder.append("]}");
  }

  private void appendExists(int nodeId) {
    builder.append("Exists");
    appendNodeId(nodeId);
  }

  private void appendJoin(int nodeId) {
    final JoinNode join = (JoinNode) plan.nodeAt(nodeId);
    final Expression joinCond = join.joinCond();
    builder.append(stringifyJoinKind(joinKindOf(plan, nodeId)));
    appendNodeId(nodeId);
    builder.append('{').append(joinCond).append(",refs=[");
    if(joinCond != null) appendRefs(joinCond);
    builder.append("]}");
  }

  private void appendInput(int nodeId) {
    final InputNode input = (InputNode) plan.nodeAt(nodeId);
    builder.append("Input");
    appendNodeId(nodeId);
    builder.append('{').append(input.table().name());
    builder.append(" AS ").append(input.qualification()).append("}");
  }

  private void appendNodeId(int nodeId) {
    if (!compact) builder.append(nodeId);
  }

  private void appendSelectItems(List<Expression> exprs, List<String> names) {
    for (int i = 0, bound = exprs.size(); i < bound; i++) {
      final StringBuilder builder = this.builder.append(exprs.get(i));
      // if (!names.get(i).startsWith(SYN_NAME_PREFIX))
      builder.append(" AS ").append(names.get(i)).append(',');
    }
  }

  private void appendRefs(Expression expr) {
    for (Value ref : values.valueRefsOf(expr)) builder.append(ref).append(',');
  }

  private void appendIndent() {
    if (oneLine || indentLevel == 0) return;
    builder.append('\n');
    for (int i = 0; i < indentLevel; i++) builder.append(' ').append(' ');
  }

  private static String stringifyJoinKind(JoinKind kind) {
    return switch (kind) {
      case CROSS_JOIN -> "CrossJoin";
      case INNER_JOIN -> "InnerJoin";
      case LEFT_JOIN -> "LeftJoin";
      case RIGHT_JOIN -> "RightJoin";
      case FULL_JOIN -> "FullJoin";
      default -> throw new IllegalArgumentException("unknown join kind");
    };
  }
}
