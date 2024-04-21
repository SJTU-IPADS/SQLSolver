package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlCountAggFunction;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;

public class AggExprRewriter extends SqlNodePreprocess {

  private boolean needsHandle(SqlNodeList list) {
    for (SqlNode node : list) {
      if (getAggExprOf(node, true) != null) return true;
    }
    return false;
  }

  // find column references in a predicate recursively
  // columns: where found columns are saved
  private void findColumnsInPredicate(SqlNode pred, SqlNodeList columns) {
    if (pred instanceof SqlIdentifier) {
      addNodeNoDup(columns, pred);
    } else if (pred instanceof SqlBasicCall call) {
      for (SqlNode arg : call.getOperandList()) {
        findColumnsInPredicate(arg, columns);
      }
    }
  }

  // Agg(...agg1(e1)...aggK(eK)... from R
  // ->
  // Agg(...agg1(c1)...aggK(cK)... from
  //   Proj(..., e1 AS c1, ..., eK AS cK from R))
  // where e1...eK are expressions
  //
  // COUNT(*) is ignored
  @Override
  public SqlNode preprocess(SqlNode node) {
    if (node instanceof SqlSelect select) {
      List<SqlBasicCall> aggExprs = new ArrayList<>();
      List<SqlNode> exprs = new ArrayList<>();
      // find agg(expr) in outputs
      SqlNodeList outputs = select.getSelectList();
      SqlNode from = select.getFrom();
      if (needsHandle(outputs)) {
        // <initSelectList> are columns fetched by <select>
        //   including column references in GROUP BY/...
        //   except for agg(expr)
        SqlNodeList initSelectList = select.getGroup();
        if (initSelectList != null) {
          initSelectList = initSelectList.clone(SqlParserPos.ZERO);
        } else {
          initSelectList = new SqlNodeList(SqlParserPos.ZERO);
        }
        // add already projected columns to <initSelectList>
        for (SqlNode output : outputs) {
          SqlBasicCall aggExpr = getAggExprOf(output, false);
          if (aggExpr != null) {
            // currently not support things like "COUNT(DISTINCT a, b)"
            aggExprs.add(aggExpr);
            exprs.addAll(aggExpr.getOperandList());
          } else {
            // remove COUNT(*) in the inner SELECT
            if (!isCountStar(output))
              addNodeNoDup(initSelectList, output.clone(SqlParserPos.ZERO));
          }
        }
        // add columns referenced by WHERE to <initSelectList>
        // column references in subqueries (e.g. EXISTS) are not included
        findColumnsInPredicate(select.getWhere(), initSelectList);
        // there must be at least one aggExpr (complex expr)
        // wrap SELECT around <from>
        // setops are not currently supported
        from = wrapNodeWithSelectAs(from, initSelectList);
        SqlNode fromOrigin = from;
        // <fromOrigin> OR (<fromOrigin> AS ...)
        // expand AS if there is one
        if (from instanceof SqlBasicCall call) {
          if (call.getOperator() instanceof SqlAsOperator) {
            fromOrigin = call.operand(0);
          }
        }
        // Agg(Proj)
        // this condition should be true unless <from> is an input table
        if (fromOrigin instanceof SqlSelect proj) {
          SqlNodeList projOutputs = proj.getSelectList();
          if (projOutputs.size() > 0 && "*".equals(projOutputs.get(0).toString())) {
            // currently not support append columns after "*"
            return node;
          }
          // append columns (eK AS cK)
          List<String> columnNames = appendExprColumns(projOutputs, exprs);
          // replace exprs (eK) with columns (cK)
          replaceExprsWithColumns(aggExprs, columnNames);
        }
        // remove qualifications of SELECT list, GROUP BY
        removeQualifications(outputs);
        removeQualifications(select.getGroup());
      }
      // recursion
      select.setFrom(preprocess(from));
      select.setWhere(preprocess(select.getWhere()));
    } else if (node instanceof SqlBasicCall call) {
      List<SqlNode> args = call.getOperandList();
      for (int i = 0; i < args.size(); i++) {
        call.setOperand(i, preprocess(args.get(i)));
      }
    } else if (node instanceof SqlOrderBy orderBy) {
      // SqlOrderBy do not support setOperand
      // rebuild SqlOrderBy
      return new SqlOrderBy(SqlParserPos.ZERO, preprocess(orderBy.query),
              orderBy.orderList, orderBy.offset, orderBy.fetch);
    } else if (node instanceof SqlJoin join) {
      // SqlJoin
      // only left & right need to be replaced
      join.setLeft(preprocess(join.getLeft()));
      join.setRight(preprocess(join.getRight()));
    }
    // otherwise, stay unchanged
    return node;
  }

  private String tailName(SqlIdentifier id) {
    return id.names.get(id.names.size() - 1);
  }

  // "[X.]A AS A" or "[X.]A" -> "[X.]A"
  // otherwise -> null
  private String getNodeNameWithoutUselessAs(SqlNode node) {
    if (node instanceof SqlBasicCall call) {
      if (call.getOperator() instanceof SqlAsOperator) {
        // ... AS ...
        if (call.operand(0) instanceof SqlIdentifier id1
                && call.operand(1) instanceof SqlIdentifier id2) {
          // A AS B
          if (tailName(id1).equals(tailName(id2))) {
            // [X.]A AS A
            return id1.toString();
          }
        }
      }
    } else if (node instanceof SqlIdentifier id) {
      return id.toString();
    }
    return null;
  }

  private boolean isDupNode(SqlNode n1, SqlNode n2) {
    String s1 = getNodeNameWithoutUselessAs(n1);
    String s2 = getNodeNameWithoutUselessAs(n2);
    if (s1 == null || s2 == null) {
      return false;
    }
    return s1.equals(s2);
  }

  // avoid "SELECT X.A, X.A [AS A]"
  private void addNodeNoDup(SqlNodeList list, SqlNode node) {
    for (SqlNode node0 : list) {
      if (isDupNode(node0, node)) {
        return;
      }
    }
    list.add(node);
  }

  private boolean isCountStar(SqlNode node) {
    if (node instanceof SqlBasicCall call
            && call.getOperator() instanceof SqlAsOperator)
      node = call.operand(0);
    return node.toString().equalsIgnoreCase("COUNT(*)");
  }

  private boolean isComplexExpr(SqlNode node) {
    return !(node instanceof SqlIdentifier);
  }

  /** If node is agg(expr) [AS ...], return agg(expr);
   * otherwise, return NULL. */
  private SqlBasicCall getAggExprOf(SqlNode node, boolean complex) {
    if (node instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      // if <call> = (... AS ...), expand AS
      // and take the first operand as <call>
      if (op instanceof SqlAsOperator) {
        SqlNode outputRaw = call.operand(0);
        if (outputRaw instanceof SqlBasicCall callRaw) {
          call = callRaw;
          op = call.getOperator();
        }
      }
      // <call> is AggFunc
      if (op instanceof SqlAggFunction) {
        List<SqlNode> args = call.getOperandList();
        // agg(x) or non-distinct COUNT(...)
        if (args.size() == 1
                || (args.size() > 1 && call.getOperator() instanceof SqlCountAggFunction
                && call.getFunctionQuantifier() == null)) {
          // COUNT(*) is excluded
          if (isCountStar(call))
            return null;
          for (SqlNode arg : args) {
            // "arg is not a column" when complex is true
            // "true" when complex is false
            if (!complex || isComplexExpr(arg)) {
              // agg(...expr...)
              return call;
            }
          }
        }
      }
    }
    return null;
  }

  // node can be a table/AS-expr
  private SqlIdentifier getAlias(SqlNode node) {
    if (node instanceof SqlIdentifier table) {
      return table;
    }
    if (node instanceof SqlBasicCall call) {
      if (call.getOperator() instanceof SqlAsOperator) {
        return call.operand(1);
      }
    }
    return null;
  }

  // R -> (SELECT <output> FROM R) AS <alias>
  private SqlBasicCall wrapNodeWithSelectAs(SqlNode node, SqlNodeList outputs, SqlIdentifier alias) {
    node = new SqlSelect(SqlParserPos.ZERO,
            new SqlNodeList(SqlParserPos.ZERO),
            outputs, node,
            null,null, null,
            new SqlNodeList(SqlParserPos.ZERO),
            null, null, null, null);
    return new SqlBasicCall(new SqlAsOperator(),
            new SqlNode[]{node, alias}, SqlParserPos.ZERO);
  }

  private SqlIdentifier merge(SqlIdentifier... ids) {
    StringBuilder sb = new StringBuilder();
    for (SqlIdentifier id : ids) {
      sb.append(tailName(id));
    }
    List<String> names = new ArrayList<>();
    names.add(sb.toString());
    return new SqlIdentifier(names, SqlParserPos.ZERO);
  }

  // node may be T1,T2,...,Tn, where this returns the name T1T2...Tn
  private SqlIdentifier mergeAliases(SqlNode node) {
    if (node instanceof SqlJoin join) {
      SqlIdentifier leftAlias = mergeAliases(join.getLeft());
      SqlIdentifier rightAlias = mergeAliases(join.getRight());
      return merge(leftAlias, rightAlias);
    }
    return getAlias(node);
  }

  private SqlIdentifier renameAlias(SqlIdentifier id) {
    int index = id.names.size() - 1;
    String name = id.names.get(index) + "_PRIME";
    return id.setName(index, name);
  }

  // R (i.e. <node>) is wrapped when
  //   R is an input table/subquery or JOIN of two tables
  // R -> (SELECT ... FROM R) AS ...
  // <initSelectList> is the original select list of the outer SELECT
  //   that excludes agg(expr) & COUNT(*)
  private SqlNode wrapNodeWithSelectAs(SqlNode node, SqlNodeList initSelectList) {
    if (node instanceof SqlIdentifier || node instanceof SqlBasicCall
            || node instanceof SqlSelect) {
      // T -> (SELECT <initSelectList> FROM T) AS T_PRIME
      SqlIdentifier alias = renameAlias(getAlias(node));
      return wrapNodeWithSelectAs(node, initSelectList, alias);
    } else if (node instanceof SqlJoin) {
      // T1, T2, ... , Tn
      // ->
      // (SELECT <initSelectList> FROM T1, T2, ... , Tn) AS T1T2...Tn
      SqlIdentifier alias = mergeAliases(node);
      return wrapNodeWithSelectAs(node, initSelectList, alias);
    }
    return node;
  }

  private boolean containsColumnName(SqlNodeList outputs, String name) {
    assert name != null;
    for (SqlNode output : outputs) {
      if (output instanceof SqlBasicCall call) {
        // ... AS COL
        if (call.getOperator() instanceof SqlAsOperator) {
          if (name.equals(call.operand(1).toString())) return true;
        }
      } else if (output instanceof SqlIdentifier id) {
        // TABLE.COL
        if (name.equals(tailName(id))) return true;
      }
    }
    return false;
  }

  /** append "eK AS cK" in Proj */
  private List<String> appendExprColumns(SqlNodeList projOutputs, List<SqlNode> exprs) {
    List<String> columnNames = new ArrayList<>();
    int index = 0;
    for (SqlNode expr : exprs) {
      String columnName = "EXPRC" + (index++);
      while (containsColumnName(projOutputs, columnName)) {
        columnName = "EXPRC" + (index++);
      }
      columnNames.add(columnName);
      SqlIdentifier columnId = new SqlIdentifier(columnName, SqlParserPos.ZERO);
      SqlBasicCall column = new SqlBasicCall(new SqlAsOperator(),
              new SqlNode[]{expr, columnId}, SqlParserPos.ZERO); // eK AS cK
      projOutputs.add(column);
    }
    return columnNames;
  }

  // agg(eK) -> agg(cK)
  private void replaceExprsWithColumns(List<SqlBasicCall> aggExprs, List<String> columnNames) {
    for (int i = 0; i < aggExprs.size(); i++) {
      SqlBasicCall aggExpr = aggExprs.get(i);
      String columnName = columnNames.get(i);
      SqlIdentifier column = new SqlIdentifier(columnName, SqlParserPos.ZERO);
      aggExpr.setOperand(0, column);
    }
  }

  private void removeQualifications(SqlNodeList list) {
    if (list == null) return;
    for (SqlNode node : list) {
      removeQualification(node);
    }
  }

  // t.x [AS a] -> x [AS a]
  private void removeQualification(SqlNode node) {
    SqlNode innerNode = node;
    if (node instanceof SqlBasicCall call
            && call.getOperator() instanceof SqlAsOperator) {
      innerNode = call.operand(0);
    }
    if (innerNode instanceof SqlIdentifier id) {
      List<String> names = new ArrayList<>();
      names.add(tailName(id));
      List<SqlParserPos> poses = new ArrayList<>();
      poses.add(SqlParserPos.ZERO);
      id.setNames(names, poses);
    }
  }

}
