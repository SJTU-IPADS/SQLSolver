package sqlsolver.sql.ast;

import sqlsolver.sql.ast.constants.*;
import sqlsolver.sql.ast.constants.*;

import java.util.List;

import static sqlsolver.common.utils.Commons.coalesce;
import static sqlsolver.common.utils.Commons.trimTrailing;
import static sqlsolver.common.datasource.DbSupport.MySQL;
import static sqlsolver.common.datasource.DbSupport.PostgreSQL;
import static sqlsolver.sql.ast.SqlNodeFields.*;
import static sqlsolver.sql.ast.TableSourceFields.*;

public class SqlFormatter implements SqlVisitor{
  private static final String INDENT_STR = "  ";
  private static final String UNKNOWN_PLACEHOLDER = "<??>";

  protected final StringBuilder builder = new StringBuilder();
  private final boolean oneLine;

  private int indent = 0;

  public SqlFormatter(boolean oneLine) {
    this.oneLine = oneLine;
  }

  private static char quotation(SqlNode node) {
    if (PostgreSQL.equals(node.dbType())) {
      return '"';
    } else {
      return '`';
    }
  }

  private static String quotation2(SqlNode node) {
    if (PostgreSQL.equals(node.dbType())) {
      return "\"";
    } else {
      return "`";
    }
  }

  protected SqlFormatter append(Object o) {
    builder.append(o);
    return this;
  }

  protected SqlFormatter append(int i) {
    builder.append(i);
    return this;
  }

  protected SqlFormatter append(char c) {
    builder.append(c);
    return this;
  }

  protected SqlFormatter append(String s) {
    builder.append(s);
    return this;
  }

  private void breakLine0() {
    append('\n');
  }

  private void increaseIndent() {
    if (!oneLine) ++indent;
  }

  private void decreaseIndent() {
    if (!oneLine) --indent;
  }

  private void insertIndent() {
    append(INDENT_STR.repeat(indent));
  }

  private void breakLine(boolean spaceIfOneLine) {
    if (!oneLine) {
      breakLine0();
      insertIndent();
    } else if (spaceIfOneLine) append(' ');
  }

  private void breakLine() {
    breakLine(true);
  }

  private void appendName(SqlNode node, String name, boolean withDot) {
    if (name == null) return;
    append(quotation(node)).append(name).append(quotation(node));
    if (withDot) append('.');
  }

  @Override
  public boolean enter(SqlNode node) {
    if (node.kind() == SqlKind.Invalid) {
      append(UNKNOWN_PLACEHOLDER);
      return false;
    }
    return true;
  }

  @Override
  public boolean enterCreateTable(SqlNode createTable) {
    append("CREATE TABLE ");

    safeVisit(createTable.$(CreateTable_Name));

    append(" (");
    increaseIndent();

    for (var colDef : createTable.$(CreateTable_Cols)) {
      breakLine();
      safeVisit((SqlNode) colDef);
      append(',');
    }

    for (var conDef : createTable.$(CreateTable_Cons)) {
      breakLine();
      safeVisit((SqlNode) conDef);
      append(',');
    }

    trimTrailing(builder, 1);
    decreaseIndent();
    breakLine();
    insertIndent();
    append(')');

    final String engine = createTable.$(CreateTable_Engine);
    if (engine != null) append(" ENGINE = '").append(engine).append('\'');
    return false;
  }

  @Override
  public boolean enterTableName(SqlNode tableName) {
    appendName(tableName, tableName.$(TableName_Schema), true);
    appendName(tableName, tableName.$(TableName_Table), false);

    return false;
  }

  @Override
  public boolean enterColumnDef(SqlNode colDef) {
    safeVisit(colDef.$(ColDef_Name));
    append(' ').append(colDef.$(ColDef_RawType));

    if (colDef.isFlag(ColDef_Cons, ConstraintKind.UNIQUE)) append(" UNIQUE");
    if (colDef.isFlag(ColDef_Cons, ConstraintKind.PRIMARY)) append(" PRIMARY KEY");
    if (colDef.isFlag(ColDef_Cons, ConstraintKind.NOT_NULL)) append(" NOT NULL");
    if (colDef.isFlag(ColDef_AutoInc)) append(" AUTO_INCREMENT");

    final var references = colDef.$(ColDef_Ref);
    if (references != null) safeVisit(references);

    return false;
  }

  @Override
  public boolean enterName2(SqlNode name2) {
    appendName(name2, name2.$(Name2_0), true);
    appendName(name2, name2.$(Name2_1), false);
    return false;
  }

  @Override
  public boolean enterName3(SqlNode name3) {
    appendName(name3, name3.$(Name3_0), true);
    appendName(name3, name3.$(Name3_1), true);
    appendName(name3, name3.$(Name3_2), false);
    return false;
  }

  @Override
  public boolean enterColumnName(SqlNode colName) {
    appendName(colName, colName.$(ColName_Schema), true);
    appendName(colName, colName.$(ColName_Table), true);
    appendName(colName, colName.$(ColName_Col), false);

    return false;
  }

  @Override
  public boolean enterCommonName(SqlNode commonName) {
    appendName(commonName, commonName.$(Name3_0), true);
    appendName(commonName, commonName.$(Name3_1), true);
    appendName(commonName, commonName.$(Name3_2), false);
    return false;
  }

  @Override
  public boolean enterReferences(SqlNode ref) {
    append(" REFERENCES ");
    safeVisit(ref.$(Reference_Table));

    final var columns = ref.$(Reference_Cols);
    if (columns != null) {
      try (final var ignored = withParen(true)) {
        for (SqlNode column : columns) {
          safeVisit(column);
          append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
      }
    }

    return false;
  }

  @Override
  public boolean enterIndexDef(SqlNode indexDef) {
    final var constraint = indexDef.$(IndexDef_Cons);
    final var type = indexDef.$(IndexDef_Kind);
    final var name = indexDef.$(IndexDef_Name);
    final var keys = indexDef.$(IndexDef_Keys);
    final var refs = indexDef.$(IndexDef_Refs);

    if (constraint != null)
      switch (constraint) {
        case PRIMARY -> append("PRIMARY ");
        case UNIQUE -> append("UNIQUE ");
        case FOREIGN -> append("FOREIGN ");
      }

    if (type != null)
      switch (type) {
        case FULLTEXT -> append("FULLTEXT ");
        case SPATIAL -> append("SPATIAL ");
      }

    append("KEY ");

    if (name != null) appendName(indexDef, name, false);

    try (final var ignored = withParen(true)) {
      for (SqlNode key : keys) {
        safeVisit(key);
        append(", ");
      }
      builder.delete(builder.length() - 2, builder.length());
    }

    if (refs != null) safeVisit(refs);

    if (type != null)
      switch (type) {
        case BTREE -> append(" USING BTREE ");
        case RTREE -> append(" USING RTREE ");
        case HASH -> append(" USING HASH ");
      }

    return false;
  }

  @Override
  public boolean enterKeyPart(SqlNode keyPart) {
    final String columnName = keyPart.$(KeyPart_Col);
    final Integer length = keyPart.$(KeyPart_Len);
    final KeyDirection direction = keyPart.$(KeyPart_Direction);
    final SqlNode expr = keyPart.$(KeyPart_Expr);

    if (columnName != null) appendName(keyPart, columnName, false);
    if (length != null) append('(').append(length).append(')');
    if (direction != null) append(' ').append(direction);
    if (expr != null)
      try (final var ignored = withParen(true)) {
        safeVisit(expr);
      }

    return false;
  }

  @Override
  public boolean enterVariable(SqlNode variable) {
    append(variable.$(ExprFields.Variable_Scope).prefix());
    append(variable.$(ExprFields.Variable_Name));

    final SqlNode assignment = variable.$(ExprFields.Variable_Assignment);
    if (assignment != null) {
      append(" = ");
      safeVisit(assignment);
    }

    return false;
  }

  @Override
  public boolean enterLiteral(SqlNode literal) {
    final Object value = literal.$(ExprFields.Literal_Value);
    switch (literal.$(ExprFields.Literal_Kind)) {
      case TEXT -> append('\'').append(value).append('\'');
      case INTEGER, LONG, FRACTIONAL, HEX -> append(value);
      case BOOL -> append(value.toString().toUpperCase());
      case NULL -> append("NULL");
      case NOT_NULL -> append("NOT NULL");
      case UNKNOWN -> append("UNKNOWN");
      case TEMPORAL -> builder
          .append(literal.$(ExprFields.Literal_Unit).toUpperCase())
          .append(" '")
          .append(value)
          .append('\'');
    }

    return false;
  }

  @Override
  public boolean enterFuncCall(SqlNode funcCall) {
    final SqlNode name = funcCall.$(ExprFields.FuncCall_Name);
    final SqlNodes args = coalesce(funcCall.$(ExprFields.FuncCall_Args), SqlNodes.mkEmpty());

    final String schemaName = name.$(Name2_0);
    final String funcName = name.$(Name2_1);

    if (schemaName == null && "extract".equalsIgnoreCase(funcName)) {
      append("EXTRACT");
      try (final var ignored = withParen(true)) {
        if (args.size() != 2) append(UNKNOWN_PLACEHOLDER);
        else {
          safeVisit(args.get(0));
          append(" FROM ");
          safeVisit(args.get(1));
        }
      }
      return false;
    }

    if (schemaName == null && "position".equalsIgnoreCase(funcName)) {
      append("POSITION");
      try (final var ignored = withParen(true)) {
        if (args.size() != 2) append(UNKNOWN_PLACEHOLDER);
        else {
          safeVisit(args.get(0));
          append(" IN ");
          safeVisit(args.get(1));
        }
      }
      return false;
    }

    if (schemaName == null && "trim".equalsIgnoreCase(funcName)) {
      append("TRIM");
      try (final var ignored = withParen(true)) {
        if (args.size() != 3) append(UNKNOWN_PLACEHOLDER);
        else {
          final SqlNode arg0 = args.get(0);
          final SqlNode arg1 = args.get(1);
          final SqlNode arg2 = args.get(2);

          if (!SqlKind.Void.isInstance(arg0)) {
            safeVisit(arg0); // LEADING/TRAILING/BOTH
            append(' ');
          }
          if (!SqlKind.Void.isInstance(arg1)) safeVisit(arg1);
          if (!SqlKind.Void.isInstance(arg2)) {
            if (!SqlKind.Void.isInstance(arg1)) append(' ');
            append("FROM ");
            safeVisit(arg2);
          }
        }
      }
      return false;
    }

    if (schemaName == null && "overlay".equalsIgnoreCase(funcName)) {
      append("OVERLAY");
      try (final var ignored = withParen(true)) {
        if (args.size() < 3) append(UNKNOWN_PLACEHOLDER);
        else {
          final SqlNode arg0 = args.get(0);
          final SqlNode arg1 = args.get(1);
          final SqlNode arg2 = args.get(2);
          final SqlNode arg3 = args.size() > 3 ? args.get(3) : null;
          safeVisit(arg0);
          append(" PLACING ");
          safeVisit(arg1);
          append(" FROM ");
          safeVisit(arg2);
          if (arg3 != null) {
            append(" FOR ");
            safeVisit(arg3);
          }
        }
      }
      return false;
    }

    if (schemaName == null && args.isEmpty() && PostgreSQL.equals(funcCall.dbType())) {
      final String upperCase = funcName.toUpperCase();
      if (upperCase.startsWith("CURRENT")
          || "SESSION_USER".equals(upperCase)
          || "USER".equals(upperCase)
          || "LOCALTIME".equals(upperCase)
          || "LOCALTIMESTAMP".equals(upperCase)) {
        append(upperCase);
        return false;
      }
    }

    appendName(name, schemaName, true);
    // we choose not quote the function name for beauty and convention
    // in most case this doesn't cause problem
    append(funcName.toUpperCase());

    appendNodes(args, true, true);
    return false;
  }

  @Override
  public boolean enterCollation(SqlNode collation) {
    final SqlNode expr = collation.$(ExprFields.Collate_Expr);
    try (final var ignored = withParen(needParen(collation, expr, true))) {
      safeVisit(expr);
    }
    append(" COLLATE ");
    if (PostgreSQL.equals(collation.dbType())) {
      collation.$(ExprFields.Collate_Collation).accept(this);
    } else {
      append('\'').append(collation.$(ExprFields.Collate_Collation).$(ExprFields.Symbol_Text)).append('\'');
    }
    return false;
  }

  @Override
  public boolean enterParamMarker(SqlNode paramMarker) {
    if (PostgreSQL.equals(paramMarker.dbType())
        && !paramMarker.isFlag(ExprFields.Param_ForceQuestion)) {
      append('$').append(coalesce(paramMarker.$(ExprFields.Param_Number), 1));
    } else {
      append('?');
    }
    return false;
  }

  @Override
  public boolean enterUnary(SqlNode unary) {
    final String op = unary.$(ExprFields.Unary_Op).text();
    append(op);
    if (op.length() > 1) append(' ');

    final SqlNode expr = unary.$(ExprFields.Unary_Expr);
    try (final var ignored = withParen(needParen(unary, expr, false))) {
      safeVisit(expr);
    }

    return false;
  }

  @Override
  public boolean enterGroupingOp(SqlNode groupingOp) {
    append("GROUPING");
    appendNodes(coalesce(groupingOp.$(ExprFields.GroupingOp_Exprs), SqlNodes.mkEmpty()));
    return false;
  }

  @Override
  public boolean enterTuple(SqlNode tuple) {
    if (tuple.isFlag(ExprFields.Tuple_AsRow)) append("ROW");
    appendNodes(coalesce(tuple.$(ExprFields.Tuple_Exprs), SqlNodes.mkEmpty()), true, true);
    return false;
  }

  @Override
  public boolean enterMatch(SqlNode match) {
    append("MATCH ");
    appendNodes(match.$(ExprFields.Match_Cols), false);

    append(" AGAINST (");
    safeVisit(match.$(ExprFields.Match_Expr));

    final MatchOption option = match.$(ExprFields.Match_Option);
    if (option != null) append(' ').append(option.optionText());
    append(')');

    return false;
  }

  @Override
  public boolean enterCast(SqlNode cast) {
    if (PostgreSQL.equals(cast.dbType())) {
      safeVisit(cast.$(ExprFields.Cast_Expr));
      append("::");
      cast.$(ExprFields.Cast_Type).formatAsCastType(builder, PostgreSQL);

    } else {
      append("CAST(");

      safeVisit(cast.$(ExprFields.Cast_Expr));

      append(" AS ");
      final SqlDataType castType = cast.$(ExprFields.Cast_Type);
      castType.formatAsCastType(builder, cast.dbType());

      if ((MySQL.equals(cast.dbType()) || cast.dbType() == null)
          && cast.isFlag(ExprFields.Cast_IsArray))
        append(" ARRAY");
      append(')');
    }
    return false;
  }

  @Override
  public boolean enterDefault(SqlNode _default) {
    append("DEFAULT(");
    safeVisit(_default.$(ExprFields.Default_Col));
    append(')');
    return false;
  }

  @Override
  public boolean enterValues(SqlNode values) {
    append("VALUES(");
    safeVisit(values.$(ExprFields.Values_Expr));
    append(')');
    return false;
  }

  @Override
  public boolean enterSymbol(SqlNode symbol) {
    append(symbol.$(ExprFields.Symbol_Text).toUpperCase());
    return false;
  }

  @Override
  public boolean enterInterval(SqlNode interval) {
    append("INTERVAL ");
    final SqlNode expr = interval.$(ExprFields.Interval_Expr);
    try (final var ignored = withParen(needParen(interval, expr, false))) {
      safeVisit(expr);
    }
    append(' ').append(interval.$(ExprFields.Interval_Unit));

    return false;
  }

  @Override
  public boolean enterWildcard(SqlNode wildcard) {
    final SqlNode table = wildcard.$(ExprFields.Wildcard_Table);
    if (table != null) {
      safeVisit(table);
      append('.');
    }
    append('*');
    return false;
  }

  @Override
  public boolean enterAggregate(SqlNode aggregate) {
    append(aggregate.$(ExprFields.Aggregate_Name).toUpperCase()).append('(');
    if (aggregate.isFlag(ExprFields.Aggregate_Distinct)) append("DISTINCT ");

    appendNodes(aggregate.$(ExprFields.Aggregate_Args), false, true);
    final SqlNodes order = aggregate.$(ExprFields.Aggregate_Order);

    if (order != null && !order.isEmpty()) {
      append(" ORDER BY ");
      appendNodes(order, false);
    }

    final String sep = aggregate.$(ExprFields.Aggregate_Sep);
    if (sep != null) append(" SEPARATOR '").append(sep).append('\'');

    append(')');

    final String windowName = aggregate.$(ExprFields.Aggregate_WindowName);
    final SqlNode windowSpec = aggregate.$(ExprFields.Aggregate_WindowSpec);
    if (windowName != null) {
      append(" OVER ");
      appendName(aggregate, windowName, false);
    }
    if (windowSpec != null) {
      append(" OVER ");
      safeVisit(windowSpec);
    }
    return false;
  }

  @Override
  public boolean enterExists(SqlNode exists) {
    append("EXISTS ");
    safeVisit(exists.$(ExprFields.Exists_Subquery));
    return false;
  }

  @Override
  public boolean enterJoinedTableSource(SqlNode joinedTableSource) {
    final SqlNode left = joinedTableSource.$(Joined_Left);
    final SqlNode right = joinedTableSource.$(Joined_Right);
    final JoinKind joinKind = joinedTableSource.$(Joined_Kind);
    final SqlNode on = joinedTableSource.$(Joined_On);
    final List<String> using = joinedTableSource.$(Joined_Using);

    safeVisit(left);

    breakLine();

    append(joinKind.text()).append(' ');
    final boolean needParen = TableSourceKind.JoinedSource.isInstance(right);
    try (final var ignored = withParen(needParen)) {
      if (needParen) {
        increaseIndent();
        breakLine(false);
      }

      safeVisit(right);

      if (needParen) {
        decreaseIndent();
        breakLine(false);
      }
    }

    if (on != null || using != null) {
      increaseIndent();
      breakLine();
      if (on != null) {
        append("ON ");
        safeVisit(on);
      } else {
        append("USING ");
        appendStrings(using, true, quotation2(joinedTableSource));
      }
      decreaseIndent();
    }

    return false;
  }

  @Override
  public boolean enterConvertUsing(SqlNode convertUsing) {
    append("CONVERT(");
    safeVisit(convertUsing.$(ExprFields.ConvertUsing_Expr));
    append(" USING ");

    String charset = convertUsing.$(ExprFields.ConvertUsing_Charset).$(ExprFields.Symbol_Text);
    if (!charset.equalsIgnoreCase("binary") && !charset.equalsIgnoreCase("default"))
      charset = "'" + charset + "'";

    append(charset);
    append(')');
    return false;
  }

  @Override
  public boolean enterCase(SqlNode _case) {
    append("CASE");
    final SqlNode cond = _case.$(ExprFields.Case_Cond);
    if (cond != null) {
      append(' ');
      try (final var ignored = withParen(needParen(_case, cond, false))) {
        safeVisit(cond);
      }
    }

    increaseIndent();
    for (SqlNode when : _case.$(ExprFields.Case_Whens)) {
      breakLine();
      safeVisit(when);
    }

    final SqlNode _else = _case.$(ExprFields.Case_Else);
    if (_else != null) {
      breakLine();
      append("ELSE ");
      try (final var ignored = withParen(needParen(_case, _else, false))) {
        safeVisit(_else);
      }
    }

    decreaseIndent();
    breakLine();
    append("END");

    return false;
  }

  @Override
  public boolean enterWhen(SqlNode when) {
    append("WHEN ");
    final SqlNode cond = when.$(ExprFields.When_Cond);
    try (final var ignored = withParen(needParen(when, cond, false))) {
      safeVisit(cond);
    }

    append(" THEN ");
    final SqlNode expr = when.$(ExprFields.When_Expr);
    try (final var ignored = withParen(needParen(when, expr, false))) {
      safeVisit(expr);
    }
    return false;
  }

  @Override
  public boolean enterWindowFrame(SqlNode windowFrame) {
    append(windowFrame.$(WindowFrame_Unit)).append(' ');
    final SqlNode start = windowFrame.$(WindowFrame_Start);
    final SqlNode end = windowFrame.$(WindowFrame_End);

    if (end != null) append("BETWEEN ");
    safeVisit(start);
    if (end != null) {
      append(" AND ");
      safeVisit(end);
    }

    final WindowExclusion exclusion = windowFrame.$(WindowFrame_Exclusion);
    if (exclusion != null) append(" EXCLUDE ").append(exclusion.text());

    return false;
  }

  @Override
  public boolean enterFrameBound(SqlNode frameBound) {
    if (frameBound.$(FrameBound_Direction) == null) append("CURRENT ROW");
    else {
      safeVisit(frameBound.$(FrameBound_Expr));
      append(' ').append(frameBound.$(FrameBound_Direction));
    }

    return false;
  }

  @Override
  public boolean enterWindowSpec(SqlNode windowSpec) {
    final String alias = windowSpec.$(WindowSpec_Alias);
    if (alias != null) {
      appendName(windowSpec, alias, false);
      append(" AS ");
    }

    append("(");

    final String name = windowSpec.$(WindowSpec_Name);
    if (name != null) appendName(windowSpec, name, false);

    final SqlNodes partition = windowSpec.$(WindowSpec_Part);
    if (partition != null && !partition.isEmpty()) {
      append(" PARTITION BY ");
      appendNodes(partition, false);
    }

    final SqlNodes order = windowSpec.$(WindowSpec_Order);
    if (order != null && !order.isEmpty()) {
      append(" ORDER BY ");
      appendNodes(order, false);
    }

    final SqlNode frame = windowSpec.$(WindowSpec_Frame);
    if (frame != null) {
      append(' ');
      safeVisit(frame);
    }

    append(')');

    return false;
  }

  @Override
  public boolean enterBinary(SqlNode binary) {
    final SqlNode left = binary.$(ExprFields.Binary_Left);
    try (final var ignored = withParen(needParen(binary, left, true))) {
      safeVisit(left);
    }

    final BinaryOpKind op = binary.$(ExprFields.Binary_Op);

    if (op.isLogic()) breakLine();
    else append(' ');

    append(op.text()).append(' ');

    final SubqueryOption subqueryOption = binary.$(ExprFields.Binary_SubqueryOption);
    if (subqueryOption != null) append(subqueryOption).append(' ');

    final SqlNode right = binary.$(ExprFields.Binary_Right);

    final boolean needParen =
        op == BinaryOpKind.MEMBER_OF || ExprKind.Array.isInstance(right) || needParen(binary, right, false);
    final boolean needIndent = needParen && op.isLogic();

    try (final var ignored0 = withParen(needParen)) {
      if (needIndent) {
        increaseIndent();
        breakLine(false);
      }

      safeVisit(right);

      if (needIndent) {
        decreaseIndent();
        breakLine(false);
      }
    }

    return false;
  }

  @Override
  public boolean enterTernary(SqlNode ternary) {
    final TernaryOp operator = ternary.$(ExprFields.Ternary_Op);
    final SqlNode left = ternary.$(ExprFields.Ternary_Left);
    final SqlNode middle = ternary.$(ExprFields.Ternary_Middle);
    final SqlNode right = ternary.$(ExprFields.Ternary_Right);

    try (final var ignored = withParen(needParen(ternary, left, false))) {
      safeVisit(left);
    }

    append(' ').append(operator.text0()).append(' ');
    try (final var ignored = withParen(needParen(ternary, middle, false))) {
      safeVisit(middle);
    }

    append(' ').append(operator.text1()).append(' ');
    try (final var ignored = withParen(needParen(ternary, right, false))) {
      safeVisit(right);
    }

    return false;
  }

  @Override
  public boolean enterOrderItem(SqlNode orderItem) {
    safeVisit(orderItem.$(OrderItem_Expr));
    final KeyDirection direction = orderItem.$(OrderItem_Direction);
    if (direction != null) append(' ').append(direction.name());
    return false;
  }

  @Override
  public boolean enterSelectItem(SqlNode selectItem) {
    safeVisit(selectItem.$(SelectItem_Expr));

    final String alias = selectItem.$(SelectItem_Alias);
    if (alias != null) {
      append(" AS ");
      appendName(selectItem, alias, false);
    }
    return false;
  }

  @Override
  public boolean enterIndexHint(SqlNode indexHint) {
    final IndexHintType type = indexHint.$(IndexHint_Kind);
    append(type).append(" INDEX");

    final IndexHintTarget target = indexHint.$(IndexHint_Target);
    if (target != null) append(" FOR ").append(target.text());

    append(' ');
    try (final var ignored = withParen(true)) {
      final List<String> names = indexHint.$(IndexHint_Names);

      if (names != null && !names.isEmpty()) {
        for (String name : names) {
          if (name.equalsIgnoreCase("primary")) append("PRIMARY");
          else appendName(indexHint, name, false);
          append(", ");
        }
        trimTrailing(builder, 2);
      }
    }

    return false;
  }

  @Override
  public boolean enterSimpleTableSource(SqlNode simpleTableSource) {
    safeVisit(simpleTableSource.$(Simple_Table));

    final List<String> partitions = simpleTableSource.$(Simple_Partition);
    if (partitions != null && !partitions.isEmpty()) {
      append(" PARTITION ");
      appendStrings(partitions, true, quotation2(simpleTableSource));
    }

    final String alias = simpleTableSource.$(Simple_Alias);
    if (alias != null) {
      append(" AS ");
      appendName(simpleTableSource, alias, false);
    }

    final SqlNodes hints = simpleTableSource.$(Simple_Hints);
    if ((simpleTableSource.dbType() == null || MySQL.equals(simpleTableSource.dbType()))
        && hints != null && !hints.isEmpty()) {
      append(' ');
      appendNodes(hints, false);
    }

    return false;
  }

  @Override
  public boolean enterQuery(SqlNode query) {
    safeVisit(query.$(Query_Body));

    final SqlNodes orderBy = query.$(Query_OrderBy);
    if (orderBy != null) {
      breakLine();
      append("ORDER BY");
      increaseIndent();
      breakLine();
      appendNodes(orderBy, false);
      decreaseIndent();
    }

    final SqlNode offset = query.$(Query_Offset);
    final SqlNode limit = query.$(Query_Limit);

    if (limit != null) {
      breakLine();
      append("LIMIT ");
      safeVisit(limit);
      if (offset != null) {
        append(" OFFSET ");
        safeVisit(offset);
      }
    }

    return false;
  }

  @Override
  public boolean enterQuerySpec(SqlNode querySpec) {
    final boolean distinct = querySpec.isFlag(QuerySpec_Distinct);
    final SqlNodes selectItems = querySpec.$(QuerySpec_SelectItems);
    final SqlNode from = querySpec.$(QuerySpec_From);
    final SqlNode where = querySpec.$(QuerySpec_Where);
    final SqlNodes groupBy = querySpec.$(QuerySpec_GroupBy);
    final OLAPOption olapOption = querySpec.$(QuerySpec_OlapOption);
    final SqlNode having = querySpec.$(QuerySpec_Having);
    final SqlNodes windows = querySpec.$(QuerySpec_Windows);

    append("SELECT");
    if (distinct) append(" DISTINCT");
    if (distinct && PostgreSQL.equals(querySpec.dbType())) {
      final SqlNodes distinctOn = querySpec.$(QuerySpec_DistinctOn);
      if (distinctOn != null && !distinctOn.isEmpty()) {
        append(" ON ");
        appendNodes(distinctOn, true, true);
      }
    }

    increaseIndent();
    breakLine();
    appendNodes(selectItems, false);
    decreaseIndent();

    if (from != null) {
      breakLine();
      append("FROM ");
      increaseIndent();
      safeVisit(from);
      decreaseIndent();
    }

    if (where != null) {
      breakLine();
      append("WHERE");
      increaseIndent();
      breakLine();
      safeVisit(where);
      decreaseIndent();
    }

    if (groupBy != null) {
      breakLine();
      append("GROUP BY");
      increaseIndent();
      breakLine();
      appendNodes(groupBy, false);
      if (olapOption != null) {
        breakLine();
        append(olapOption.text());
      }
      decreaseIndent();
    }

    if (having != null) {
      breakLine();
      append("HAVING");
      increaseIndent();
      breakLine();
      safeVisit(having);
      decreaseIndent();
    }

    if (windows != null) {
      breakLine();
      append("WINDOW");
      increaseIndent();
      breakLine();
      appendNodes(windows, false);
      decreaseIndent();
    }

    return false;
  }

  @Override
  public boolean enterQueryExpr(SqlNode queryExpr) {
    try (final var ignored = withParen(true)) {
      increaseIndent();
      breakLine(false);
      safeVisit(queryExpr.$(ExprFields.QueryExpr_Query));
      decreaseIndent();
      breakLine(false);
    }
    return false;
  }

  @Override
  public boolean enterSetOp(SqlNode setOp) {
    try (final var ignored = withParen(true)) {
      safeVisit(setOp.$(SetOp_Left));
    }

    breakLine();
    append(setOp.$(SetOp_Kind));

    final SetOpOption option = setOp.$(SetOp_Option);
    if (option != null) append(' ').append(option);
    breakLine();

    try (final var ignored = withParen(true)) {
      safeVisit(setOp.$(SetOp_Right));
    }

    return false;
  }

  @Override
  public boolean enterDerivedTableSource(SqlNode derivedTableSource) {
    if (derivedTableSource.isFlag(Derived_Lateral)) append("LATERAL ");
    try (final var ignored = withParen(true)) {
      increaseIndent();
      breakLine(false);
      safeVisit(derivedTableSource.$(Derived_Subquery));
      decreaseIndent();
      breakLine(false);
    }

    final String alias = derivedTableSource.$(Derived_Alias);
    if (alias != null) {
      append(" AS ");
      appendName(derivedTableSource, alias, false);
    }

    final List<String> internalRefs = derivedTableSource.$(Derived_InternalRefs);
    if (internalRefs != null && !internalRefs.isEmpty()) {
      breakLine();
      appendStrings(internalRefs, true, quotation2(derivedTableSource));
    }

    return false;
  }

  @Override
  public boolean enterArray(SqlNode array) {
    append("ARRAY[");
    appendNodes(array.$(ExprFields.Array_Elements), false, true);
    append(']');
    return false;
  }

  private SqlFormatter.DumbAutoCloseable withParen(boolean addParen) {
    return addParen ? new SqlFormatter.ParenCtx() : SqlFormatter.DumbAutoCloseable.INSTANCE;
  }

  private static class DumbAutoCloseable implements AutoCloseable {
    private static final SqlFormatter.DumbAutoCloseable INSTANCE = new SqlFormatter.DumbAutoCloseable();

    @Override
    public void close() {}
  }

  private class ParenCtx extends SqlFormatter.DumbAutoCloseable {
    ParenCtx() {
      append('(');
    }

    @Override
    public void close() {
      append(')');
    }
  }

  private void safeVisit(SqlNode node) {
    if (node == null) append(UNKNOWN_PLACEHOLDER);
    else node.accept(this);
  }

  private void appendStrings(List<String> exprs, boolean withParen, String surround) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) append("()");
      return;
    }

    if (withParen) append('(');

    for (String arg : exprs) {
      if (arg == null) continue;
      if (surround != null) append(surround);
      append(arg);
      if (surround != null) append(surround);
      append(", ");
    }

    trimTrailing(builder, 2);

    if (withParen) append(')');
  }

  private void appendNodes(SqlNodes exprs, boolean withParen, boolean noBreak) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) append("()");
      return;
    }

    if (withParen) append('(');

    for (int i = 0; i < exprs.size() - 1; i++) {
      final SqlNode expr = exprs.get(i);
      safeVisit(expr);
      append(',');
      if (noBreak) append(' ');
      else breakLine();
    }

    final SqlNode last = exprs.get(exprs.size() - 1);
    safeVisit(last);

    if (withParen) append(')');
  }

  private void appendNodes(SqlNodes exprs, boolean withParen) {
    appendNodes(exprs, withParen, false);
  }

  private void appendNodes(SqlNodes exprs) {
    appendNodes(exprs, true);
  }

  private static boolean needParen(SqlNode parent, SqlNode child, boolean isLeftChild) {
    if (parent == null || child == null) return false;

    final int parentPrecedence = ExprFields.getOperatorPrecedence(parent);
    final int childPrecedence = ExprFields.getOperatorPrecedence(child);
    if (parentPrecedence == -1 || childPrecedence == -1) return false;
    if (parentPrecedence > childPrecedence) return true;
    if (parentPrecedence == childPrecedence) return !isLeftChild;
    return false;
  }

  public String toString() {
    return builder.toString();
  }
}
