package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// SELECT ... FROM t WHERE p(r1, ...)
// ->
// SELECT ... FROM t LEFT JOIN r1' ON TRUE ... WHERE p(c1, ...)
// r1 --(add single_value, rename its column as c1)-> r1'

// SELECT ... FROM ... GROUP BY ... HAVING p(r1)
// ->
// SELECT ... FROM ... LEFT JOIN (c1 FROM r1) AS t1 ON TRUE
//   GROUP BY ..., c1 HAVING p(c1)

// SELECT * should be processed specially -> SELECT t.*

public class ScalarQueryRewriter extends SqlNodePreprocess {

  private int columnIndex = 0;
  private int tableIndex = 0;

  private String sqlToRewrite;

  private String newTableName() {
    return "ta" + tableIndex++;
  }

  private String newColumnName() {
    return "ca" + columnIndex++;
  }

  private boolean containsAlias(String name) {
    return sqlToRewrite.contains(name);
  }

  private String freshColumnAlias() {
    String alias = newColumnName();
    while (containsAlias(alias)) {
      alias = newColumnName();
    }
    return alias;
  }

  private String freshTableAlias() {
    String alias = newTableName();
    while (containsAlias(alias)) {
      alias = newTableName();
    }
    return alias;
  }

  private SqlIdentifier id(String name) {
    return new SqlIdentifier(name, SqlParserPos.ZERO);
  }

  private SqlBasicCall as(SqlNode node, String alias) {
    return new SqlBasicCall(new SqlAsOperator(), new SqlNode[]{node, id(alias)}, SqlParserPos.ZERO);
  }

  private boolean needsHandleJoinConditions(SqlNode from) {
    if (from instanceof SqlJoin join) {
      if (needsHandlePredicate(join.getCondition(), true)) return true;
      if (needsHandleJoinConditions(join.getLeft())) return true;
      return needsHandleJoinConditions(join.getRight());
    }
    return false;
  }

  private boolean needsHandlePredicate(SqlNode pred, boolean checksSelf) {
    if (pred instanceof SqlSelect select) {
      if (checksSelf && isSingleColumnSelect(select)) {
        // single-column scalar query
        return true;
      }
      // recursion
      return needsHandle(select.getWhere(), select.getFrom());
    } else if (pred instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      SqlKind kind = op.getKind();
      if (kind.equals(SqlKind.IN) || kind.equals(SqlKind.NOT_IN)) {
        if (needsHandlePredicate(call.operand(0), true)) return true;
        // queries are acceptable as the second operand of IN
        return needsHandlePredicate(call.operand(1), false);
      } else if (kind.equals(SqlKind.EXISTS)) {
        // queries are acceptable as the operand of EXISTS
        return needsHandlePredicate(call.operand(0), false);
      } else if (kind.equals(SqlKind.AS)) {
        // queries are acceptable as the first operand of AS
        return needsHandlePredicate(call.operand(0), false);
        // the second operand of AS must be an identifier
      } else if (kind.equals(SqlKind.UNION)) {
        // queries are acceptable as the first operand of AS
        return needsHandlePredicate(call.operand(0), false)
                || needsHandlePredicate(call.operand(1), false);
        // the second operand of AS must be an identifier
      } else {
        for (int i = 0; i < call.getOperandList().size(); i++) {
          SqlNode node = call.operand(i);
          if (needsHandlePredicate(node, true)) return true;
        }
      }
    }
    return false;
  }

  private String needsHandlePredicateNoFrom(SqlNode pred, boolean checksSelf) {
    if (pred instanceof SqlSelect select) {
      String value;
      if (checksSelf && !Objects.equals(value = singleColumnSelectValue(select), "") && select.getFrom() == null) {
        // single-column scalar query without from
        return value;
      }
    } else if (pred instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      SqlKind kind = op.getKind();
      if (kind.equals(SqlKind.IN) || kind.equals(SqlKind.NOT_IN)) {
        String value = "";
        if (!Objects.equals(value = needsHandlePredicateNoFrom(call.operand(0), true), "")) return value;
        // queries are acceptable as the second operand of IN
        return needsHandlePredicateNoFrom(call.operand(1), false);
      } else if (kind.equals(SqlKind.EXISTS)) {
        // queries are acceptable as the operand of EXISTS
        return needsHandlePredicateNoFrom(call.operand(0), false);
      } else if (kind.equals(SqlKind.AS)) {
        // queries are acceptable as the first operand of AS
        return needsHandlePredicateNoFrom(call.operand(0), false);
        // the second operand of AS must be an identifier
      } else {
        for (int i = 0; i < call.getOperandList().size(); i++) {
          SqlNode node = call.operand(i);
          String value = "";
          if(!Objects.equals(value = needsHandlePredicateNoFrom(node, true), "")) return value;
        }
      }
    }
    return "";
  }

  private boolean needsHandle(SqlNode where, SqlNode from) {
    return needsHandlePredicate(where, true) || needsHandleJoinConditions(from);
  }

  @Override
  public SqlNode preprocess(SqlNode node) {
    sqlToRewrite = node.toString().toLowerCase();
    return preprocess0(node);
  }

  public SqlNode preprocess0(SqlNode node) {
    if (node instanceof SqlSelect select) {
      SqlNode where = select.getWhere();
      SqlNode having = select.getHaving();
      SqlNodeList group = select.getGroup();
      SqlNode from = select.getFrom();
      // whether WHERE contains single-column scalar queries without FROM
      // example: SELECT * FROM T WHERE (SELECT 1)
      String value = "";
      if (!Objects.equals(value = needsHandlePredicateNoFrom(where, true), "")) {
        replaceAndAppendNoFrom(where, true, value);
      } else if (needsHandle(where, from) || needsHandle(having, from)) {
        Map<SqlSelect, String> aliasMap = new HashMap<>();
        Map<SqlSelect, String> groupAliasMap = new HashMap<>();
        // update SELECT list before updating FROM tables
        handleSelectStar(select);
        // handle join conditions & WHERE/HAVING predicates
        handleJoinConditions(from, aliasMap);
        replaceAndAppend(where, aliasMap, true);
        replaceAndAppend(having, groupAliasMap, true);
        for (String alias : groupAliasMap.values()) {
          group.add(id(alias));
        }
        // add left joins to FROM
        appendLeftJoinsAfterLast(select, groupAliasMap);
        appendLeftJoinsAdaptive(select, aliasMap);
      }
      select.setFrom(preprocess0(select.getFrom()));
    } else if (node instanceof SqlOrderBy orderBy) {
      return new SqlOrderBy(SqlParserPos.ZERO, preprocess0(orderBy.query),
              orderBy.orderList, orderBy.offset, orderBy.fetch);
    } else if (node instanceof SqlJoin join) {
      // SqlJoin
      return new SqlJoin(SqlParserPos.ZERO, preprocess0(join.getLeft()),
              join.isNaturalNode(), join.getJoinTypeNode(),
              preprocess0(join.getRight()), join.getConditionTypeNode(),
              preprocess0(join.getCondition()));
    } else if (node instanceof SqlBasicCall call) {
      List<SqlNode> args = call.getOperandList();
      for (int i = 0; i < args.size(); i++) {
        call.setOperand(i, preprocess0(args.get(i)));
      }
    }
    return node;
  }

  private void handleJoinConditions(SqlNode from, Map<SqlSelect, String> aliasMap) {
    if (from instanceof SqlJoin join) {
      replaceAndAppend(join.getCondition(), aliasMap, true);
      handleJoinConditions(join.getLeft(), aliasMap);
      handleJoinConditions(join.getRight(), aliasMap);
    }
  }

  private boolean isSingleColumnSelect(SqlSelect select) {
    SqlNodeList list = select.getSelectList();
    if (list.size() != 1) return false;
    SqlNode item = list.get(0);
    String itemStr = item.toString();
    // conservative, sometimes "*" actually returns one column
    if (itemStr.equals("*") || itemStr.endsWith(".*")) return false;
    return true;
  }

  private String singleColumnSelectValue(SqlSelect select) {
    SqlNodeList list = select.getSelectList();
    if (list.size() != 1) return "";
    SqlNode item = list.get(0);
    String itemStr = item.toString();
    // contains only numbers and letters
    String regex = "^[0-9a-zA-Z]+$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(itemStr);
    if(matcher.matches()) return itemStr;
    return "";
  }

  private SqlSelect addSingleValue(SqlSelect select) {
    SqlNodeList list = select.getSelectList();
    assert list.size() == 1;
    SqlNode item = list.get(0);
    SqlNode item1 = SqlStdOperatorTable.SINGLE_VALUE.createCall(SqlParserPos.ZERO, item);
    list.set(0, item1);
    return select;
  }

  // find single-column scalar queries in <pred>
  // replace with aliases
  private SqlNode replaceAndAppend(SqlNode pred, Map<SqlSelect, String> aliasMap, boolean replacesSelf) {
    if (pred instanceof SqlSelect select) {
      if (replacesSelf && isSingleColumnSelect(select)) {
        // single-column scalar query
        String alias = freshColumnAlias();
        aliasMap.put(addSingleValue(select), alias);
        return id(alias);
      }
      // recursion
      return preprocess0(select);
    } else if (pred instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      SqlKind kind = op.getKind();
      if (kind.equals(SqlKind.IN) || kind.equals(SqlKind.NOT_IN)) {
        replaceAndAppend(call.operand(0), aliasMap, true);
        // queries are acceptable as the second operand of IN
        replaceAndAppend(call.operand(1), aliasMap, false);
      } else if (kind.equals(SqlKind.EXISTS)) {
        // queries are acceptable as the operand of EXISTS
        replaceAndAppend(call.operand(0), aliasMap, false);
      } else if (kind.equals(SqlKind.AS)) {
        // queries are acceptable as the first operand of AS
        replaceAndAppend(call.operand(0), aliasMap, false);
        // the second operand of AS must be an identifier
      } else {
        for (int i = 0; i < call.getOperandList().size(); i++) {
          SqlNode node = call.operand(i);
          SqlNode node1 = replaceAndAppend(node, aliasMap, true);
          if (node != node1) call.setOperand(i, node1);
        }
      }
    }
    return pred;
  }

  private SqlNode replaceAndAppendNoFrom(SqlNode pred, boolean replacesSelf, String value) {
    if (pred instanceof SqlSelect select) {
      if (replacesSelf && Objects.equals(value, singleColumnSelectValue(select)) && select.getFrom() == null) {
        // single-column scalar query
        Pattern pattern = Pattern.compile("^[+-]?\\d+$");
        Matcher matcher = pattern.matcher(value);
        assert matcher.matches();
        SqlNumericLiteral numericLiteral = SqlLiteral.createExactNumeric(value, SqlParserPos.ZERO);
        return numericLiteral;
      }
    } else if (pred instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      SqlKind kind = op.getKind();
      if (kind.equals(SqlKind.IN) || kind.equals(SqlKind.NOT_IN)) {
        replaceAndAppendNoFrom(call.operand(0), true, value);
        // queries are acceptable as the second operand of IN
        replaceAndAppendNoFrom(call.operand(1), false, value);
      } else if (kind.equals(SqlKind.EXISTS)) {
        // queries are acceptable as the operand of EXISTS
        replaceAndAppendNoFrom(call.operand(0), false, value);
      } else if (kind.equals(SqlKind.AS)) {
        // queries are acceptable as the first operand of AS
        replaceAndAppendNoFrom(call.operand(0), false, value);
        // the second operand of AS must be an identifier
      } else {
        for (int i = 0; i < call.getOperandList().size(); i++) {
          SqlNode node = call.operand(i);
          SqlNode node1 = replaceAndAppendNoFrom(node, true, value);
          if (node != node1) call.setOperand(i, node1);
        }
      }
    }
    return pred;
  }

  private SqlIdentifier addStar(SqlIdentifier id) {
    List<String> names = new ArrayList<>(id.names);
    names.add("");
    List<SqlParserPos> poses = new ArrayList<>();
    for (int i = 0; i < names.size(); i++) {
      poses.add(SqlParserPos.ZERO);
    }
    id.setNames(names, poses);
    return id;
  }

  private void appendFromTables(SqlNodeList list, SqlNode from) {
    if (from instanceof SqlJoin join) {
      appendFromTables(list, join.getLeft());
      appendFromTables(list, join.getRight());
    } else if (from instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      if (op instanceof SqlAsOperator) {
        SqlIdentifier tableId = (SqlIdentifier) call.operand(1).clone(SqlParserPos.ZERO);
        list.add(addStar(tableId));
      }
    } else if (from instanceof SqlIdentifier id) {
      SqlIdentifier tableId = (SqlIdentifier) id.clone(SqlParserPos.ZERO);
      list.add(addStar(tableId));
    } else {
      assert false;
    }
  }

  private void handleSelectStar(SqlSelect select) {
    SqlNodeList list = select.getSelectList();
    SqlNodeList newList = new SqlNodeList(SqlParserPos.ZERO);
    for (SqlNode node : list) {
      if (node instanceof SqlIdentifier id
              && id.toString().equals("*")) {
        // * -> t1.*, ...
        SqlNode from = select.getFrom();
        appendFromTables(newList, from);
      } else {
        newList.add(node);
      }
    }
    select.setSelectList(newList);
  }

  private SqlSelect renameColumn(SqlSelect select, String alias) {
    SqlNodeList list = select.getSelectList();
    assert list.size() == 1;
    SqlNode item = list.get(0);
    SqlNode item1 = as(item, alias);
    list.set(0, item1);
    return select;
  }

  private SqlNode appendLeftJoins(SqlNode from, Map<SqlSelect, String> aliasMap) {
    for (Map.Entry<SqlSelect, String> entry : aliasMap.entrySet()) {
      SqlSelect table = entry.getKey();
      String columnAlias = entry.getValue();
      SqlNode sub = as(renameColumn(table, columnAlias), freshTableAlias());
      from = new SqlJoin(SqlParserPos.ZERO, from,
              SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
              JoinType.LEFT.symbol(SqlParserPos.ZERO),
              sub,
              JoinConditionType.ON.symbol(SqlParserPos.ZERO),
              SqlLiteral.createBoolean(true, SqlParserPos.ZERO));
    }
    return from;
  }

  private void appendLeftJoinsAtFirst(SqlJoin first, Map<SqlSelect, String> aliasMap) {
    // find the first table & append left joins
    SqlNode left = first.getLeft();
    if (left instanceof SqlJoin join) {
      appendLeftJoinsAtFirst(join, aliasMap);
    } else {
      first.setLeft(appendLeftJoins(left, aliasMap));
    }
  }

  private void appendLeftJoinsAtFirst(SqlSelect select, Map<SqlSelect, String> aliasMap) {
    SqlNode from = select.getFrom();
    // find the first table & append left joins
    if (from instanceof SqlJoin join) {
      appendLeftJoinsAtFirst(join, aliasMap);
    } else {
      select.setFrom(appendLeftJoins(from, aliasMap));
    }
  }

  private void appendLeftJoinsAtLast(SqlJoin last, Map<SqlSelect, String> aliasMap) {
    // find the last table & append left joins
    SqlNode right = last.getRight();
    if (right instanceof SqlJoin join) {
      appendLeftJoinsAtLast(join, aliasMap);
    } else {
      last.setRight(appendLeftJoins(right, aliasMap));
    }
  }

  private void appendLeftJoinsAfterLast(SqlSelect select, Map<SqlSelect, String> aliasMap) {
    // FROM R -> FROM (R) LEFT JOIN ...
    select.setFrom(appendLeftJoins(select.getFrom(), aliasMap));
  }

  private int getJoinSize(SqlNode node) {
    if (node instanceof SqlJoin join) {
      return getJoinSize(join.getLeft()) + getJoinSize(join.getRight());
    }
    return 1;
  }

  private void appendLeftJoinsAdaptive(SqlSelect select, Map<SqlSelect, String> aliasMap) {
    SqlNode from = select.getFrom();
    // find a suitable table & append left joins
    int joinSize = getJoinSize(from);
    if (joinSize == 2) {
      // fast path: append to the last table
      appendLeftJoinsAtLast((SqlJoin) from, aliasMap);
    } else {
      // normal path: append to the first table
      appendLeftJoinsAtFirst(select, aliasMap);
    }
  }

}
