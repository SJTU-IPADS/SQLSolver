package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.*;

public class HavingAggRewriter extends RecursiveRewriter {

  // SELECT <cols1> FROM t GROUP BY <cols3> HAVING P(<cols2>)
  // ->
  // SELECT <cols1>' FROM (SELECT <cols1>, <cols2>, <cols3> FROM t GROUP BY <cols3>)
  //   AS ... WHERE P(<cols2>')
  // where cols1 & cols2 may contain items like agg(col)
  //   and cols1' & cols2' should use aliases instead of expressions
  // cols3 should be included in the inner SELECT
  //   since "WHERE P" may reference cols3

  private Map<SqlNode, String> aggAliases;
  private int tableAliasIndex, columnAliasIndex;

  public HavingAggRewriter() {
    setAllowsMultipleApplications(true);
  }

  private SqlIdentifier newTableAlias() {
    String name = "TA" + (tableAliasIndex++);
    return new SqlIdentifier(name, SqlParserPos.ZERO);
  }

  private String newColumnName() {
    return "COL" + columnAliasIndex++;
  }

  private boolean containsAgg(SqlNode node) {
    // partial
    String s = node.toString();
    return s.contains("COUNT")
            || s.contains("SUM");
  }

  @Override
  public boolean prepare(SqlNode node) {
    tableAliasIndex = 0;
    columnAliasIndex = 0;
    aggAliases = new HashMap<>();
    return true;
  }

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlSelect select) {
      SqlNode pred = select.getHaving();
      if (pred != null && containsAgg(pred)) {
        // extract aliases of cols1 as cols1'
        // add new aliases to cols1 if alias is absent for some column
        SqlNodeList selectListInner = select.getSelectList();
        SqlNodeList cols1 = extractAliases(selectListInner);
        // extract cols2
        List<SqlNode> cols2 = extractOutputsFromExpression(pred);
        // insert cols2 into the inner SELECT list
        for (SqlNode col : cols2) {
          selectListInner.add(col);
        }
        // insert GROUP BY columns into the inner SELECT list
        for (SqlNode col : select.getGroup()) {
          selectListInner.add(col);
        }
        // remove HAVING of the inner query
        select.setHaving(null);
        // construct the WHERE clause
        SqlNode newPred = substAliasesInNode(pred, aggAliases);
        // construct the outer query
        SqlBasicCall as = new SqlBasicCall(new SqlAsOperator(),
                new SqlNode[]{select, newTableAlias()}, SqlParserPos.ZERO);
        return new SqlSelect(SqlParserPos.ZERO,
                new SqlNodeList(SqlParserPos.ZERO),
                cols1, as, newPred, null, null,
                new SqlNodeList(SqlParserPos.ZERO),
                null, null, null, null);
      }
    }
    return node;
  }

  // return a list of "colName" or "agg AS ..."
  // also update aggAliases
  private List<SqlNode> extractOutputsFromExpression(SqlNode pred) {
    if (pred instanceof SqlBasicCall call) {
      if (call.getOperator() instanceof SqlAggFunction) {
        String colName = newColumnName();
        SqlIdentifier alias = new SqlIdentifier(colName, SqlParserPos.ZERO);
        aggAliases.put(call, colName);
        SqlBasicCall as = new SqlBasicCall(new SqlAsOperator(),
                new SqlNode[]{call, alias}, SqlParserPos.ZERO);
        return Collections.singletonList(as);
      } else {
        List<SqlNode> list = new ArrayList<>();
        for (SqlNode operand : call.getOperandList()) {
          list.addAll(extractOutputsFromExpression(operand));
        }
        return list;
      }
    }
    // currently not recognize "colName" as a predicate
    return new ArrayList<>();
  }

  // replace SqlNodes with corresponding aliases
  private SqlNode substAliasesInNode(SqlNode node, Map<SqlNode, String> aliases) {
    // replace node with its alias
    if (aliases.containsKey(node)) {
      return new SqlIdentifier(aliases.get(node), SqlParserPos.ZERO);
    }
    // recursion
    if (node instanceof SqlBasicCall call) {
      int i = 0;
      for (SqlNode operand : call.getOperandList()) {
        call.setOperand(i++, substAliasesInNode(operand, aliases));
      }
    }
    return node;
  }

  private void removeQualification(SqlNode node) {
    if (node instanceof SqlIdentifier id) {
      String name = id.names.get(id.names.size() - 1);
      List<String> names = Collections.singletonList(name);
      List<SqlParserPos> poses = Collections.singletonList(SqlParserPos.ZERO);
      id.setNames(names, poses);
    }
  }

  private SqlNodeList extractAliases(SqlNodeList nodes) {
    SqlNodeList res = new SqlNodeList(SqlParserPos.ZERO);
    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      SqlNode node = nodes.get(i);
      if (node instanceof SqlIdentifier) {
        // id itself as its alias
        node = node.clone(SqlParserPos.ZERO);
        removeQualification(node);
        res.add(node);
      } else if (node instanceof SqlBasicCall call) {
        SqlOperator op = call.getOperator();
        if (op instanceof SqlAsOperator) {
          // extract the alias
          res.add(call.operand(1));
        } else {
          // need a new alias
          String colName = newColumnName();
          SqlIdentifier alias = new SqlIdentifier(colName, SqlParserPos.ZERO);
          res.add(alias);
          SqlBasicCall as = new SqlBasicCall(new SqlAsOperator(),
                  new SqlNode[]{call, alias}, SqlParserPos.ZERO);
          nodes.set(i, as);
        }
      }
    }
    return res;
  }

}
