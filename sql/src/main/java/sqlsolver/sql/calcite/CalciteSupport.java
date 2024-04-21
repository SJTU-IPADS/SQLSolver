package sqlsolver.sql.calcite;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.*;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlLibrary;
import org.apache.calcite.sql.fun.SqlLibraryOperatorTableFactory;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.babel.SqlBabelParserImpl;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.util.SqlOperatorTables;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.tools.*;
import sqlsolver.common.utils.IterableSupport;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.ast.constants.ConstraintKind;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.plan.ValueImpl;
import sqlsolver.sql.schema.Constraint;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;
import sqlsolver.sql.schema.Table;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.calcite.sql.SqlKind.*;
import static sqlsolver.common.datasource.DbSupport.MySQL;
import static sqlsolver.sql.plan.Value.*;

/**
 * Calcite related supporter.
 * Convert the calcite data structure to the corresponding sqlsolver built-in data structure.
 */
public abstract class CalciteSupport {

  /**
   * Constant of type factory.
   */
  public static final JavaTypeFactory JAVA_TYPE_FACTORY = new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);

  /**
   * Constant of user defined functions.
   */
  public static final List<SqlOperator> USER_DEFINED_FUNCTIONS = new ArrayList<>();

  /**
   * Constant of temporary table's name prefix.
   */
  public static final String TEMP_TABLE_PREFIX = "TEMP_";

  /**
   * Constant of temporary table's name sequence.
   */
  public static final NameSequence TEMP_TABLE_NAME_SEQUENCE = NameSequence.mkIndexed(TEMP_TABLE_PREFIX, 0);

  /**
   * Constant of temporary column's name prefix.
   */
  public static final String TEMP_COL_PREFIX = "temp_";

  /**
   * Constant of temporary column's name sequence.
   */
  public static final NameSequence TEMP_COL_NAME_SEQUENCE = NameSequence.mkIndexed(TEMP_COL_PREFIX, 0);

  /**
   * Used for value ID.
   */
  private static int nextId = 0;

  /**
   * Default database type for schema.
   */
  private static final String DB_TYPE = MySQL;

  /*
   * Support info
   */

  public static boolean supportsAggFunc(SqlKind kind) {
    return kind == COUNT
            || kind == SUM
            || kind == AVG
            || kind == MAX
            || kind == MIN
            || kind == STDDEV_SAMP
            || kind == STDDEV_POP
            || kind == VAR_SAMP
            || kind == VAR_POP
            || kind == SINGLE_VALUE;
  }

  /*
   * Schema related functions
   */

  /**
   * Given a string of schema.
   * Return the Schema object.
   */
  public static Schema getSchema(String content) {
    return SchemaSupport.parseSchema(DB_TYPE, content);
  }

  /**
   * Given a string of schema.
   * Return the Calcite Schema object.
   */
  public static CalciteSchema getCalciteSchema(String content) {
    return getCalciteSchema(getSchema(content));
  }

  /**
   * Given a schema object.
   * Return the Calcite Schema object.
   */
  public static CalciteSchema getCalciteSchema(Schema schema) {
    CalciteSchema calciteSchema = CalciteSchema.createRootSchema(false, false);

    for (Table table : schema.tables()) {
      CalciteTable calciteTable = new CalciteTable(table);
      calciteSchema.add(table.name(), calciteTable);
    }

    return calciteSchema;
  }

  /*
    value related functions
   */

  /**
   * Get the value list by the calcite table:
   * e.g. T(a0 int, a1 int) -> value list: [T.a0, T.a1]
   */
  public static List<Value> getValueListByCalciteTable(RelOptTable table) {
    return getValueListByCalciteTable(table, null);
  }

  public static List<Value> getValueListByCalciteTable(RelOptTable table, Schema schema) {
    assert table.getQualifiedName().size() == 1;
    final String tableName = table.getQualifiedName().get(0);
    final List<String> colNames = table.getRowType().getFieldNames();
    final List<Value> values = new ArrayList<>();
    for (String colName : colNames) {
      final int id = ++nextId;
      final Value value = Value.mk(id, tableName, colName);
      // NOT NULL from table IC
      if (schema != null && tableName != null) {
        boolean isNotNull = false;
        for (Constraint constraint : schema.table(tableName).constraints(ConstraintKind.NOT_NULL)) {
          if (constraint.columns().size() == 1
                  && constraint.columns().get(0).name().equals(colName)) {
            isNotNull = true;
            break;
          }
        }
        value.setNotNull(isNotNull);
      }
      values.add(value);
    }
    return values;
  }

  /** i -> "$i" */
  public static String indexToColumnName(int index) {
    return "$" + index;
  }

  /** "$i" -> i */
  public static int columnNameToIndex(String columnName) {
    final String errorMsg = "column name should be like $i where i is a natural number";
    if (!columnName.startsWith("$"))
      throw new IllegalArgumentException(errorMsg);
    try {
      return Integer.parseInt(columnName.substring(1));
    } catch (Throwable e) {
      throw new IllegalArgumentException(errorMsg);
    }
  }

  /**
   * Get the value list by expr list:
   * e.g. expr[$1, $4], originList[a, b, c, d, e, f, g] -> [b, e]
   * expr[RexCall, $4], originList[a, b, c, d, e, f, g] -> [$0, e]
   */
  public static List<Value> getValueListByIndexExpr(List<RexNode> expr, List<Value> originList) {
    final List<Value> result = new ArrayList<>();
    int curIndex = 0;
    for (RexNode index : expr) {
      // for the case of index
      if (index instanceof RexInputRef inputRef && originList.get(inputRef.getIndex()).qualification() != null) {
        assert inputRef.getIndex() < originList.size();
        result.add(originList.get(inputRef.getIndex()));
      } else {
        // name the other case for "${index}", e.g. $1
        int id = ++nextId;
        final Value value = Value.mk(id, null, indexToColumnName(curIndex), index.getType().getSqlTypeName().getName(), isNotNullExpr(index, originList));
        result.add(value);
      }
      curIndex++;
    }
    return result;
  }

  /**
   * Decide whether expr must be non-NULL w.r.t refList.
   * This method guarantees that when it claims that expr is NOT NULL,
   * it must actually be NOT NULL.
   */
  public static boolean isNotNullExpr(RexNode expr, List<Value> refList) {
    switch (expr.getKind()) {
      case LITERAL -> {
        return !expr.getType().isNullable();
      }
      case INPUT_REF -> {
        final int index = columnNameToIndex(((RexInputRef) expr).getName());
        return refList.get(index).isNotNull();
      }
      case PLUS, MINUS, TIMES, EXTRACT -> {
        final RexCall call = (RexCall) expr;
        return IterableSupport.all(call.getOperands(), op -> isNotNullExpr(op, refList));
      }
      case CASE -> {
        final RexCall call = (RexCall) expr;
        final List<RexNode> ops = call.getOperands();
        if (ops.size() % 2 != 1) return false;
        int opIndex = 0;
        // THEN terms should be all non-NULL
        for (RexNode op : ops) {
          if (opIndex % 2 == 1 && !isNotNullExpr(op, refList)) return false;
          opIndex++;
        }
        // ELSE term should be non-NULL
        return isNotNullExpr(ops.get(opIndex - 1), refList);
      }
      // TODO: more operators
    }
    return false;
  }

  /**
   * Get the value list by size:
   * e.g. size: 4 -> [$0, $1, $2, $3]
   */
  public static List<Value> getValueListBySize(int size) {
    final List<Value> result = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      // name the case for "${i}", e.g. $1
      final int id = ++nextId;
      final Value value = new ValueImpl(id, null, indexToColumnName(i));
      result.add(value);
    }
    return result;
  }

  /**
   * Get the value by Integer index.
   * e.g. 1 -> $1
   */
  public static Value getValueByIndex(int index) {
    // name the case for "${i}", e.g. $1
    return new ValueImpl(++nextId, null, indexToColumnName(index));
  }

  /**
   * Get the value list by Integer List.
   * e.g. [1, 3, 4] -> [$1, $3, $4]
   * "schema" provides info about each column ($i).
   */
  public static List<Value> getValueListByIndexes(List<Integer> indexes, List<Value> schema) {
    final List<Value> result = new ArrayList<>();
    for (Integer index : indexes) {
      // name the case for "${i}", e.g. $1
      final int id = ++nextId;
      final Value refValue = schema.get(index);
      final Value value = new ValueImpl(id, null, indexToColumnName(index), refValue.type(), refValue.isNotNull());
      result.add(value);
    }
    return result;
  }

  /**
   * Get the value list by aggregate node.
   * This function is for aggregate node especially
   * due to the particularity of the aggregate node.
   */
  public static List<Value> getValueListByAgg(List<Integer> groupIndex, List<AggregateCall> aggCallList, List<Value> originList) {
    final int aggCallSize = aggCallList.size();
    final int groupSize = groupIndex.size();
    final List<Value> result = new ArrayList<>();
    for (int i = 0, bound = aggCallSize + groupSize; i < bound; i++) {
      if (i < groupSize) {
        final Integer index = groupIndex.get(i);
        assert index < originList.size();
        result.add(originList.get(index));
      } else {
        // name the case for "${i}", e.g. $1
        final int id = ++nextId;
        final AggregateCall aggCall = aggCallList.get(i - groupSize);
        final SqlKind aggFunc = aggCall.getAggregation().getKind();
        final List<Integer> argList = aggCall.getArgList();
        final boolean isNotNull;
        if (aggFunc == COUNT) {
          isNotNull = true;
        } else if (supportsAggFunc(aggFunc) && argList.size() == 1) {
          final int arg = argList.get(0);
          isNotNull = groupSize > 0 && originList.get(arg).isNotNull();
        } else {
          isNotNull = false;
        }
        final Value value = Value.mk(id, null, indexToColumnName(i));
        value.setNotNull(isNotNull);
        result.add(value);
      }
    }
    return result;
  }

  /**
   * Get index string by given qualification, name and value list
   */
  public static String getIndexStringByInfo(String qualification, String name, List<Value> valueList) {
    for (int i = 0; i < valueList.size(); i++) {
      final Value value = valueList.get(i);
      if (Objects.equals(value.qualification(), qualification)
              && Objects.equals(value.name(), name)) {
        return indexToColumnName(i);
      }
    }
    return null;
  }

  /**
   * Get the true column by index value.
   * e.g. $1, [T.a, T.b, T.c] -> T.b
   */
  public static Value getColValByIndexVal(Value indexVal, List<Value> target) {
    final String indexStr = indexVal.toString();

    final Pattern pattern = Pattern.compile("\\$(\\d+)");
    final Matcher matcher = pattern.matcher(indexStr);

    if (matcher.find()) {
      String match = matcher.group(1);
      int number = Integer.parseInt(match);
      return target.get(number);
    }
    return null;
  }

  /**
   * Check whether two value lists equal in terms of their size.
   */
  public static boolean isEqualTwoValueList(List<Value> valueList0, List<Value> valueList1) {
    return valueList0.size() == valueList1.size();
  }

  /**
   * Check whether two value lists equal in terms of value type.
   */
  public static boolean isEqualTypeTwoValueList(List<Value> valueList0, List<Value> valueList1) {
    if (valueList0.size() != valueList1.size()) return false;
    for (int i = 0; i < valueList0.size(); i++) {
      final Value value0 = valueList0.get(i);
      final Value value1 = valueList1.get(i);
      if (!Objects.equals(value0.type(), value1.type())) return false;
    }
    return true;
  }

  /**
   * Check whether two value lists exactly equal.
   */
  public static boolean isExactlyEqualTwoValueList(List<Value> valueList0, List<Value> valueList1) {
    if (valueList0.size() != valueList1.size()) return false;
    for (int i = 0; i < valueList0.size(); i++) {
      final Value value0 = valueList0.get(i);
      final Value value1 = valueList1.get(i);
      if (!Objects.equals(value0.qualification(), value1.qualification())
          || !Objects.equals(value0.name(), value1.name())
          || !Objects.equals(value0.type(), value1.type())) return false;
    }
    return true;
  }

  /**
   * Merge two value lists, or return null if they are incompatible.
   */
  public static List<Value> mergeTwoValueLists(List<Value> valueList0, List<Value> valueList1) {
    if (valueList0.size() != valueList1.size()) return null;
    final int size = valueList0.size();
    final List<Value> newValueList = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      final Value col0 = valueList0.get(i).copy();
      final Value col1 = valueList1.get(i).copy();
      final String type0 = col0.type(), type1 = col1.type();
      if (type0.equals(type1)) {
        newValueList.add(col0);
      } else if (TYPE_NULL.equals(type0)) {
        newValueList.add(col1);
      } else if (TYPE_NULL.equals(type1)) {
        newValueList.add(col0);
      } else if (isNumberType(type0) && isNumberType(type1)) {
        // both number type -> the type with higher precision
        final int pl0 = getNumberPrecisionLevel(type0);
        final int pl1 = getNumberPrecisionLevel(type1);
        if (pl0 <= pl1) newValueList.add(col1);
        else newValueList.add(col0);
      } else if (isStringType(type0) && isStringType(type1)) {
        // CHAR and VARCHAR -> VARCHAR
        if (TYPE_VARCHAR.equals(type0)) newValueList.add(col0);
        else newValueList.add(col1);
      } else {
        // not supported; consider to be incompatible
        //System.out.println(type0 + ", " + type1);
        return null;
      }
    }
    return newValueList;
  }

  /*
   * Planner related functions
   */

  /**
   * Get calcite planner.
   */
  public static Planner getPlanner(CalciteSchema schema) {
    // parser config
    Frameworks.ConfigBuilder builder = Frameworks.newConfigBuilder()
            .defaultSchema(schema.plus())
            .parserConfig(SqlParser.config().withCaseSensitive(false).withParserFactory(SqlBabelParserImpl.FACTORY))
            .operatorTable(SqlOperatorTables.chain(SqlLibraryOperatorTableFactory.INSTANCE.getOperatorTable(SqlLibrary.BIG_QUERY,
                    SqlLibrary.SPARK,
                    SqlLibrary.MYSQL,
                    SqlLibrary.STANDARD), SqlOperatorTables.of(USER_DEFINED_FUNCTIONS)))
            .sqlValidatorConfig(SqlValidator.Config.DEFAULT.withTypeCoercionEnabled(true));
    FrameworkConfig config = builder.build();
    return Frameworks.getPlanner(config);
  }

  /*
   * AST related functions
   */

  /**
   * Get calcite AST.
   */
  public static SqlNode parseAST(String string, Planner planner) {
    try {
      SqlNode sqlNode = planner.parse(string);
      return planner.validate(sqlNode);
    } catch (SqlParseException | ValidationException e) {
      System.err.println(e.getMessage());
      return null;
    }
  }

  /**
   * Whether an operator is aggregation operator
   */
  public static boolean isAggOperator(SqlOperator operator) {
    final String operatorName = operator.getName().toUpperCase();
    if (operatorName.equals("COUNT")
            || operatorName.equals("SUM")
            || operatorName.equals("AVG")
            || operatorName.equals("MAX")
            || operatorName.equals("MIN")
            || operatorName.equals("VAR_POP")
            || operatorName.equals("VAR_SAMP")
            || operatorName.equals("STDDEV_POP")
            || operatorName.equals("STDDEV_SAMP")) return true;
    return false;
  }

  /**
   * Whether a SqlNode return boolean type.
   */
  public static boolean isBooleanSqlNode(SqlNode node) {
    final Set<SqlKind> booleans = EnumSet.of(IN, NOT_IN, EQUALS, NOT_EQUALS,
                                             LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL,
                                             IS_FALSE, IS_TRUE, IS_NOT_FALSE, IS_NOT_TRUE,
                                             IS_DISTINCT_FROM, IS_NOT_DISTINCT_FROM,
                                             IS_NULL, IS_NOT_NULL, IS_UNKNOWN);
    return node.isA(booleans);
  }

  /**
   * Add a user defined function.
   */
  public static void addUserDefinedFunction(String name, int argumentNum) {
    final SqlOperator custom_function = new SqlFunction(name,
            SqlKind.OTHER_FUNCTION,
            ReturnTypes.INTEGER,
            null,
            OperandTypes.family(Collections.nCopies(argumentNum, SqlTypeFamily.ANY)),
            SqlFunctionCategory.USER_DEFINED_FUNCTION);
    USER_DEFINED_FUNCTIONS.add(custom_function);
  }

  /*
   * RelNode (Calcite's logical plan) related functions
   */

  /**
   * Get calcite RelNode.
   */
  public static RelNode parseRel(SqlNode ast, Planner planner) {
    try {
      return planner.rel(ast).rel;
    } catch (Exception | Error ex) {
      System.err.println(ex.getMessage());
      return null;
    }
  }

  /**
   * Check whether a plan tree has target kind.
   */
  public static boolean hasNodeOfKind(RelNode plan, Class kind) {
    if (kind.isInstance(plan)) return true;
    for (RelNode children : plan.getInputs()) {
      if (hasNodeOfKind(children, kind)) return true;
    }
    return false;
  }

  /**
   * Get the node of kind in a given plan.
   */
  public static RelNode getNodeOfKind(RelNode plan, Class kind) {
    if (kind.isInstance(plan)) return plan;
    for (RelNode children : plan.getInputs()) {
      final RelNode target = getNodeOfKind(children, kind);
      if (target != null) return target;
    }
    return null;
  }

  /**
   * Get all nodes of kind in a given plan.
   * The nodes will be stored in results.
   */
  public static void getAllNodesOfKind(RelNode plan, Class kind, List<RelNode> results) {
    if (kind.isInstance(plan)) {
      results.add(plan);
    }
    for (RelNode children : plan.getInputs()) {
      getAllNodesOfKind(children, kind, results);
    }
  }

  /**
   * Find target node's father.
   */
  public static RelNode getFatherOfTarget(RelNode plan, RelNode target) {
    if (plan == null || target == null) {
      return null;
    }

    for (final RelNode child : plan.getInputs()) {
      if (child == target) {
        return plan;
      }
    }

    for (final RelNode child : plan.getInputs()) {
      final RelNode result = getFatherOfTarget(child, target);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  /**
   * Get output schema list size of a RelNode
   */
  public static int getOutputSizeOfRelNode(RelNode node) {
    if (node instanceof TableScan tableScan) {
      return getValueListByCalciteTable(tableScan.getTable()).size();
    }
    if (node instanceof Filter filter) {
      return getOutputSizeOfRelNode(filter.getInput());
    }
    if (node instanceof Project project) {
      return project.getProjects().size();
    }
    if (node instanceof Join join) {
      return getOutputSizeOfRelNode(join.getLeft()) + getOutputSizeOfRelNode(join.getRight());
    }
    if (node instanceof SetOp setOp) {
      // randomly select one
      return getOutputSizeOfRelNode(setOp.getInput(0));
    }
    if (node instanceof Aggregate aggregate) {
      return aggregate.getGroupCount() + aggregate.getAggCallList().size();
    }
    if (node instanceof Sort sort) {
      return getOutputSizeOfRelNode(sort.getInput());
    }

    // should not reach here.
    assert false;
    return -1;
  }

  /**
   * Check whether a RelNode is a scalar query.
   */
  public static boolean checkRelNodeScalar(RelNode node) {
    if (node instanceof Aggregate aggregate) {
      return aggregate.getAggCallList().size() == 1
              && aggregate.getAggCallList().get(0).getAggregation().getName().equalsIgnoreCase("SINGLE_VALUE");
    }
    return false;
  }

  /**
   * DeepCopy a RelNode's tree
   */
  public static RelNode deepCopy(RelNode node) {
    final int inputSize = node.getInputs().size();
    for (int i = 0; i < inputSize; i++) {
      node.replaceInput(i, deepCopy(node.getInput(i)));
    }

    return node.copy(node.getTraitSet(), node.getInputs());
  }

  /*
   * RexNode (Calcite's expression) related functions
   */

  /**
   * Create the RexInputRef List by Index list
   */
  public static List<RexNode> getRexInputRefByIndexs(List<Integer> indexs) {
    List<RexNode> result = new ArrayList<>();
    for (Integer index : indexs) {
      // For the type here, only create a unknown type because the attribute of type is useless
      final RexInputRef rexInputRef = new RexInputRef(index, new JavaTypeFactoryImpl().createUnknownType());
      result.add(rexInputRef);
    }
    return result;
  }

  /**
   * Check whether RexNode is unary kind.
   */
  public static boolean isUnaryRexNode(RexNode node) {
    final SqlKind kind = node.getKind();
    return kind == NOT
            || kind == PLUS_PREFIX
            || kind == MINUS_PREFIX
            || kind == IS_TRUE
            || kind == IS_FALSE
            || kind == IS_NOT_TRUE
            || kind == IS_NOT_FALSE
            || kind == IS_NULL
            || kind == IS_NOT_NULL
            || kind == IS_UNKNOWN;
  }

  /**
   * Check whether RexNode is Binary kind.
   */
  public static boolean isBinaryRexNode(RexNode node) {
    final SqlKind kind = node.getKind();
    return kind == AND
            || kind == OR
            || kind == EQUALS
            || kind == NOT_EQUALS
            || kind == LESS_THAN
            || kind == GREATER_THAN
            || kind == GREATER_THAN_OR_EQUAL
            || kind == LESS_THAN_OR_EQUAL;
  }

  /**
   * Check whether RexNode is target kind.
   */
  public static boolean isTargetKind(RexNode node, Class target) {
    return (node != null) && (target.isInstance(node));
  }

  /**
   * Get RexNode related column
   */
  public static Value getRexNodeColumn(RexNode expr, RelNode node) {
    if (expr instanceof RexInputRef rexInputRef) {
      final int index = rexInputRef.getIndex();
      if (node instanceof TableScan tableScan) {
        return getValueListByCalciteTable(tableScan.getTable()).get(index);
      }
      if (node instanceof Filter filter) {
        return getRexNodeColumn(expr, filter.getInput());
      }
      if (node instanceof Project project) {
        return getRexNodeColumn(project.getProjects().get(index), project.getInput());
      }
      if (node instanceof Join join) {
        if (index >= getOutputSizeOfRelNode(join.getLeft())) {
          return getRexNodeColumn(new RexInputRef(index - getOutputSizeOfRelNode(join.getLeft()), rexInputRef.getType())
                  , join.getRight());
        } else {
          return getRexNodeColumn(expr, join.getLeft());
        }
      }
      if (node instanceof SetOp setop) {
        // since the schema of two tables that use SetOp must be the same, so it only needs to select a random one.
        return getRexNodeColumn(expr, setop.getInputs().get(0));
      }
      if (node instanceof Aggregate aggregate) {
        // assume that the group sets are always selected in front of aggCalls
        if (index >= aggregate.getGroupCount())
          return null;
        return getRexNodeColumn(expr, aggregate.getInput());
      }
      if (node instanceof Sort sort) {
        return getRexNodeColumn(expr, sort.getInput());
      }
    }

    return null;
  }

  /*
   * Temporary table/column related functions
   */

  /**
   * Change type into SQLSolver's schema's type
   */
  private static String typeConversion(String type, boolean isNullable) {
    final String postfix = isNullable ? "" : " not null";
    switch (type) {
      case "VARCHAR" -> {
        return "varchar(20)" + postfix;
      }
      case "INTEGER" -> {
        return "int" + postfix;
      }
    }
    return type;
  }

  /**
   * For a given column number, create a new temporary table with the column number.
   * Return the CREATE sql and SELECT * sql.
   */
  public static String[] getNewTempTableAndProjString(int columnSize, List<RelDataTypeField> types) {
    final List<String> columns = new ArrayList<>();
    final String tableName = TEMP_TABLE_NAME_SEQUENCE.next();
    String[] result = new String[2];

    for (int i = 0; i < columnSize; i++) {
      columns.add(TEMP_COL_NAME_SEQUENCE.next());
    }

    result[0] = "CREATE TABLE " + tableName + " (";
    for (int i = 0; i < columns.size(); i++) {
      if (i == columns.size() - 1) {
        result[0] += columns.get(i) + " " +
                typeConversion(types.get(i).getType().toString(), types.get(i).getType().isNullable()) + ");";
      } else {
        result[0] += columns.get(i) + " " +
                typeConversion(types.get(i).getType().toString(), types.get(i).getType().isNullable()) + ",";
      }
    }
    result[1] = "SELECT * FROM " + tableName;

    return result;
  }
}
