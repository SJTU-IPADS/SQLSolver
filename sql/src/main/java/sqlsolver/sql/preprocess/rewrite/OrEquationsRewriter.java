package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.*;

/*
 * X = C1 OR X = C2 OR ... OR X = Ck
 * ->
 * X IN (C1, C2, ..., Ck)
 */
public class OrEquationsRewriter extends RecursiveRewriter {

  private SqlNode toOrNode(List<SqlNode> orList) {
    assert !orList.isEmpty();
    SqlNode orNode = orList.get(0);
    for (int i = 1, bound = orList.size(); i < bound; i++) {
      orNode = SqlStdOperatorTable.OR.createCall(SqlParserPos.ZERO,
              orNode, orList.get(i));
    }
    return orNode;
  }

  // x = C1 OR ... OR x = Ck OR other
  // ->
  // x IN (C1, ..., Ck) OR other
  private SqlNode toInList(List<SqlNode> orList) {
    Map<String, SqlNodeList> inLists = new HashMap<>();
    List<SqlNode> otherList = new ArrayList<>();
    for (SqlNode node : orList) {
      if (node instanceof SqlBasicCall call) {
        SqlKind kind = call.getKind();
        if (kind.equals(SqlKind.EQUALS)) {
          SqlNode left = call.operand(0);
          SqlNode right = call.operand(1);
          if (left instanceof SqlLiteral) {
            // C = x -> x = C
            SqlNode tmp = left;
            left = right;
            right = tmp;
          }
          if (right instanceof SqlLiteral) {
            // x = C -> x IN (C)
            String key = left.toString();
            SqlNodeList inArgs = inLists.get(key);
            if (inArgs == null) {
              inArgs = new SqlNodeList(SqlParserPos.ZERO);
              // the first operand of IN is x
              inArgs.add(left);
              // the second operand of IN is a list
              inArgs.add(new SqlNodeList(SqlParserPos.ZERO));
              inLists.put(key, inArgs);
            }
            SqlNodeList newList = (SqlNodeList) inArgs.get(1);
            newList.add(right);
            continue;
          }
        }
      }
      otherList.add(node);
    }
    List<SqlNode> newOrList = new ArrayList<>();
    for (SqlNodeList inList : inLists.values()) {
      // for each left value, generate an IN list
      newOrList.add(SqlStdOperatorTable.IN.createCall(inList));
    }
    // collect the rest of nodes
    if (!otherList.isEmpty()) newOrList.addAll(otherList);
    // generate disjunction of IN lists and the other nodes
    return toOrNode(newOrList);
  }

  // recursively recognize consecutive ORs
  private List<SqlNode> collectOrList(SqlNode node) {
    if (node instanceof SqlBasicCall call
            && call.getKind().equals(SqlKind.OR)) {
      List<SqlNode> list = collectOrList(call.operand(0));
      list.addAll(collectOrList(call.operand(1)));
      return list;
    }
    List<SqlNode> list = new ArrayList<>();
    list.add(node);
    return list;
  }

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlBasicCall call
            && call.getKind().equals(SqlKind.OR)) {
      return toInList(collectOrList(node));
    }
    return node;
  }

}
