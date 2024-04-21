package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.*;

// SELECT agg(x) + agg(y), agg(z), d FROM r GROUP BY ...
// ->
// SELECT a + b, c, d FROM (SELECT agg(x) AS a, agg(y) AS b, agg(z) AS c, d FROM r GROUP BY ...) AS t
//
// SINGLE_VALUE is regarded as an ordinary function
public class CompoundAggRewriter extends SqlNodePreprocess {

  private int tableIndex = 0, columnIndex = 0;

  private String newTableAlias() {
    return "TA" + tableIndex++;
  }

  private String freshTableAlias(String alias) {
    String newAlias = newTableAlias();
    while (newAlias.equals(alias)) {
      newAlias = newTableAlias();
    }
    return newAlias;
  }

  // r -> r AS ta0
  // SELECT FROM ta0 -> (SELECT FROM ta0) AS ta1
  private SqlNode wrapTableWithAlias(SqlNode table) {
    String alias = null;
    if (table instanceof SqlSelect select) {
      if (select.getFrom() instanceof SqlBasicCall call) {
        if (call.getOperator() instanceof SqlAsOperator) {
          SqlIdentifier id = call.operand(1);
          alias = id.toString();
        }
      }
    }
    String newAlias = freshTableAlias(alias);
    SqlIdentifier id = new SqlIdentifier(newAlias, SqlParserPos.ZERO);
    return new SqlBasicCall(new SqlAsOperator(), new SqlNode[]{table, id}, SqlParserPos.ZERO);
  }

  private String newColumnAlias() {
    return "CA" + columnIndex++;
  }

  private String freshColumnAlias(Set<String> names) {
    String newAlias;
    boolean dup;
    do {
      dup = false;
      newAlias = newColumnAlias();
      if (names.contains(newAlias)) dup = true;
    } while (dup);
    return newAlias;
  }

  private String tailName(SqlIdentifier id) {
    return id.names.get(id.names.size() - 1);
  }

  private SqlIdentifier tail(SqlIdentifier id) {
    return new SqlIdentifier(tailName(id), SqlParserPos.ZERO);
  }

  private SqlIdentifier id(String name) {
    return new SqlIdentifier(name, SqlParserPos.ZERO);
  }

  private SqlBasicCall as(SqlNode node, String alias) {
    return new SqlBasicCall(new SqlAsOperator(), new SqlNode[]{node, id(alias)}, SqlParserPos.ZERO);
  }

  private boolean isAggOp(SqlOperator op) {
    return !op.equals(SqlStdOperatorTable.SINGLE_VALUE)
            && (op instanceof SqlAggFunction);
  }

  private boolean containsAgg(SqlNode item) {
    if (item instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      // ignore window functions
      if (op instanceof SqlOverOperator) {
        return false;
      }
      // base case
      if (isAggOp(op)) return true;
      // recursion
      for (SqlNode subitem : call.getOperandList()) {
        if (containsAgg(subitem)) return true;
      }
    }
    return false;
  }

  private boolean needsHandle(SqlNodeList list) {
    for (SqlNode item : list) {
      // ignore AS
      if (item instanceof SqlBasicCall call
              && call.getOperator() instanceof SqlAsOperator) {
        item = call.operand(0);
      }
      if (item instanceof SqlBasicCall call) {
        // complex expressions containing agg
        SqlOperator op = call.getOperator();
        if (!isAggOp(op)) {
          if (containsAgg(item)) return true;
        }
      }
    }
    return false;
  }

  @Override
  public SqlNode preprocess(SqlNode node) {
    if (node instanceof SqlSelect select) {
      SqlSelect newSelect = select;
      if (needsHandle(select.getSelectList())) {
        SqlNodeList outerList = new SqlNodeList(SqlParserPos.ZERO);
        SqlNodeList innerList = new SqlNodeList(SqlParserPos.ZERO);
        Set<String> columnAliases = new HashSet<>();
        // find existing aliases
        for (SqlNode item : select.getSelectList()) {
          if (item instanceof SqlIdentifier id) {
            columnAliases.add(tailName(id));
          } else if (item instanceof SqlBasicCall call
                  && call.getOperator() instanceof SqlAsOperator) {
            columnAliases.add(tailName(call.operand(1)));
          }
        }
        // handle each item
        Map<String, String> aggAlias = new HashMap<>();
        for (SqlNode item : select.getSelectList()) {
          handleItem(item.clone(SqlParserPos.ZERO), columnAliases, outerList, innerList, aggAlias);
        }
        select.setSelectList(innerList);
        newSelect = new SqlSelect(SqlParserPos.ZERO,
                new SqlNodeList(SqlParserPos.ZERO),
                outerList, wrapTableWithAlias(select),
                null,null, null,
                new SqlNodeList(SqlParserPos.ZERO),
                null, null, null, null);
      }
      // recursion
      select.setFrom(preprocess(select.getFrom()));
      return newSelect;
    } else if (node instanceof SqlBasicCall call) {
      // recursion
      List<SqlNode> args = call.getOperandList();
      for (int i = 0; i < args.size(); i++) {
        call.setOperand(i, preprocess(args.get(i)));
      }
    } else if (node instanceof SqlOrderBy orderBy) {
      // recursion
      return new SqlOrderBy(SqlParserPos.ZERO, preprocess(orderBy.query),
              orderBy.orderList, orderBy.offset, orderBy.fetch);
    } else if (node instanceof SqlJoin join) {
      // recursion
      join.setLeft(preprocess(join.getLeft()));
      join.setRight(preprocess(join.getRight()));
    }
    return node;
  }

  // r.a -> (outer) a; (inner) r.a
  // f(agg(x), agg(y)) -> (outer) f(a, b); (inner) agg(x) AS a, agg(y) AS b
  // each agg(x) is aliased once only
  private void handleItem(SqlNode item, Set<String> aliases,
                          SqlNodeList outerList, SqlNodeList innerList, Map<String, String> aggAlias) {
    if (item instanceof SqlIdentifier id) {
      outerList.add(tail(id));
      innerList.add(id);
    } else if (item instanceof SqlBasicCall) {
      outerList.add(aliasAggs(item, aliases, innerList, aggAlias));
    }
  }

  // Precondition: <item> should not contain column references.
  // replace each agg occurrence in <item> in-place
  // record their aliases in <aggAlias>
  // update <innerList>
  private SqlNode aliasAggs(SqlNode item, Set<String> aliases,
                            SqlNodeList innerList, Map<String, String> aggAlias) {
    if (item instanceof SqlBasicCall call) {
      if (isAggOp(call.getOperator())) {
        String itemStr = item.toString();
        if (!aggAlias.containsKey(itemStr)) {
          String alias = freshColumnAlias(aliases);
          aggAlias.put(itemStr, alias);
          innerList.add(as(item, alias));
        }
        return id(aggAlias.get(itemStr));
      } else {
        for (int i = 0; i < call.getOperandList().size(); i++) {
          call.setOperand(i, aliasAggs(call.getOperandList().get(i), aliases, innerList, aggAlias));
        }
      }
    } else if (item instanceof SqlCase cas) {
      SqlNodeList whens = cas.getWhenOperands();
      SqlNodeList thens = cas.getThenOperands();
      int size = whens.size();
      assert size == thens.size();
      for (int i = 0; i < size; i++) {
        whens.set(i, aliasAggs(whens.get(i), aliases, innerList, aggAlias));
        thens.set(i, aliasAggs(thens.get(i), aliases, innerList, aggAlias));
      }
      cas.setOperand(3, aliasAggs(cas.getElseOperand(), aliases, innerList, aggAlias));
    }
    return item;
  }

}
