package sqlsolver.sql.copreprocess;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.*;

// For each item in the SELECT list:
// column -> column
// ... AS a -> ... AS a
// ... -> ... AS a
//
// Finally, wrap with SELECT c0, c1, ...
// Note that the SQL should not have an outer ORDER BY
public class ColumnAliasCoRewriter extends SqlNodeCoPreprocess {

  private int columnIndex = 0;

  private String newColumnAlias() {
    return "CA" + columnIndex++;
  }

  private String freshColumnAlias(Collection<String> aliases) {
    String newAlias = newColumnAlias();
    while (aliases.contains(newAlias)) {
      newAlias = newColumnAlias();
    }
    return newAlias;
  }

  private boolean isSelectStar(SqlNodeList list) {
    if (list.size() == 1) {
      if (list.get(0) instanceof SqlIdentifier id
              && id.names.size() == 1)
        return id.names.get(0).isEmpty();
    }
    return false;
  }

  // ... AS a -> a
  // ... -> null
  private String getAlias(SqlNode node) {
    if (node instanceof SqlIdentifier id) {
      return tailName(id);
    } else if (node instanceof SqlBasicCall call
            && call.getOperator() instanceof SqlAsOperator) {
      return tailName(call.operand(1));
    }
    return null;
  }

  // if each pair of corresponding output both do not have aliases or
  //   have the same alias, do not handle
  private boolean needsHandle(SqlNodeList list0, SqlNodeList list1) {
    // SELECT * is not handled
    if (isSelectStar(list0) || isSelectStar(list1)) return false;
    // SELECTs with different output counts are not handled
    int size = list0.size();
    if (size != list1.size()) return false; // NEQ
    for (int i = 0; i < size; i++) {
      SqlNode node0 = list0.get(i), node1 = list1.get(i);
      String alias0 = getAlias(node0), alias1 = getAlias(node1);
      boolean needs;
      if (alias0 == null) needs = alias1 != null;
      else needs = !alias0.equals(alias1);
      if (needs) return true;
    }
    return false;
  }

  @Override
  public SqlNode[] coPreprocess(SqlNode[] nodes) {
    if (nodes[0].toString().contains("VALUES")) return nodes;
    if (nodes[1].toString().contains("VALUES")) return nodes;
    if (nodes[0] instanceof SqlSelect select0 && nodes[1] instanceof SqlSelect select1) {
      if (select0.getOrderList() == null && select1.getOrderList() == null) {
        if (needsHandle(select0.getSelectList(), select1.getSelectList())) {
          nodes[0] = preprocess(select0);
          nodes[1] = preprocess(select1);
        }
      }
    }
    return nodes;
  }

  private SqlNode preprocess(SqlSelect select) {
    List<String> aliases = addAliases(select.getSelectList());
    SqlNodeList outerList = renameColumns(aliases);
    return new SqlSelect(SqlParserPos.ZERO,
            new SqlNodeList(SqlParserPos.ZERO),
            outerList, as(select, "OUTERMOST_TABLE"),
            null,null, null,
            new SqlNodeList(SqlParserPos.ZERO),
            null, null, null, null);
  }

  private String tailName(SqlIdentifier id) {
    return id.names.get(id.names.size() - 1);
  }

  private SqlIdentifier id(String name) {
    return new SqlIdentifier(name, SqlParserPos.ZERO);
  }

  private SqlBasicCall as(SqlNode node, String alias) {
    return new SqlBasicCall(new SqlAsOperator(), new SqlNode[]{node, id(alias)}, SqlParserPos.ZERO);
  }

  private Set<String> getExistingAliases(SqlNodeList list) {
    Set<String> existingAliases = new HashSet<>();
    for (SqlNode item : list) {
      if (item instanceof SqlIdentifier id) {
        existingAliases.add(tailName(id));
      } else if (item instanceof SqlBasicCall call) {
        if (call.getOperator() instanceof SqlAsOperator) {
          existingAliases.add(tailName(call.operand(1)));
        }
      }
    }
    return existingAliases;
  }

  // add aliases if missing & return the alias list
  // column -> column
  // ... AS a -> ... AS a
  // ... -> ... AS a
  private List<String> addAliases(SqlNodeList list) {
    Set<String> existingAliases = getExistingAliases(list);
    List<String> aliases = new ArrayList<>();
    // add aliases to non-aliased outputs
    for (int i = 0; i < list.size(); i++) {
      SqlNode item = list.get(i);
      if (item instanceof SqlIdentifier id) {
        aliases.add(tailName(id));
        continue;
      } else if (item instanceof SqlBasicCall call) {
        if (call.getOperator() instanceof SqlAsOperator) {
          aliases.add(tailName(call.operand(1)));
          continue;
        }
      }
      // add an alias
      String alias = freshColumnAlias(existingAliases);
      aliases.add(alias);
      list.set(i, as(item, alias));
    }
    return aliases;
  }

  // x, y, z ... -> x AS c0, y AS c1, z AS c2 ...
  private SqlNodeList renameColumns(List<String> columnNames) {
    SqlNodeList list = new SqlNodeList(SqlParserPos.ZERO);
    for (int i = 0; i < columnNames.size(); i++) {
      String columnName = columnNames.get(i);
      list.add(as(id(columnName), "OUT_C" + i));
    }
    return list;
  }

}
