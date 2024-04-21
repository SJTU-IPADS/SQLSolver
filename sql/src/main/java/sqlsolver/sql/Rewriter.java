package sqlsolver.sql;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.tools.*;

import org.apache.calcite.util.Litmus;
import sqlsolver.sql.tableSchema.ACCOUNT;
import sqlsolver.sql.tableSchema.BONUS;
import sqlsolver.sql.tableSchema.DEPT;
import sqlsolver.sql.tableSchema.EMP;
import sqlsolver.sql.tableSchema.*;

import java.util.*;


public class Rewriter {
  public enum rewriteType {HAVING, AGGARITH, AGGGROUPBY, WITHAS};

  public static final JavaTypeFactory typeFactory = new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
  public static final SchemaPlus defaultSchema = Frameworks.createRootSchema(true);

  private static final String aliasPrefix = "ca";

  private static final String tableAliasPrefix = "ta";

  private String origin_query = "";
  private Integer aliasNumber = 0;

  private Integer tableAliasNumber = 0;

  private FrameworkConfig config = Frameworks.newConfigBuilder().defaultSchema(defaultSchema).build();
  private Planner planner = Frameworks.getPlanner(config);

  public Rewriter(){
    addTableSchema();
  }

  public Rewriter(String query) {
    addTableSchema();
    origin_query = query;
  }

  public void addTableSchema(){
    SqlToRelConverter.config();
    defaultSchema.add("EMP", new EMP());
    defaultSchema.add("DEPT",new DEPT());
    defaultSchema.add("BONUS",new BONUS());
    defaultSchema.add("ACCOUNT",new ACCOUNT());
  }

  public SqlNode getSqlNode(String sql) throws SqlParseException, ValidationException, RelConversionException{
    final SqlNode parse = planner.parse(sql);
    return parse;
  }

  public static SqlNode asOperatorFirstOperand(SqlNode sqlNode) {
    if (sqlNode.getKind() == SqlKind.AS) {
      final SqlBasicCall basicCall = (SqlBasicCall) sqlNode;
      assert basicCall.getOperandList().size() == 2;
      final SqlNode firstOperand = basicCall.getOperandList().get(0);
      final SqlNode secondOperandNode = basicCall.getOperandList().get(1);
      assert secondOperandNode instanceof SqlIdentifier;
      return firstOperand;
    }
    return null;
  }

  public static SqlNode asOperatorSecondOperand(SqlNode sqlNode) {
    if (sqlNode.getKind() == SqlKind.AS) {
      final SqlBasicCall basicCall = (SqlBasicCall) sqlNode;
      assert basicCall.getOperandList().size() == 2;
      final SqlNode firstOperand = basicCall.getOperandList().get(0);
      final SqlNode secondOperandNode = basicCall.getOperandList().get(1);
      assert secondOperandNode instanceof SqlIdentifier;
      return secondOperandNode;
    }
    return null;
  }

  public static boolean isAggregateFunc(SqlNode sqlNode) {
    return sqlNode.getKind() == SqlKind.COUNT ||
            sqlNode.getKind() == SqlKind.MAX ||
            sqlNode.getKind() == SqlKind.MIN ||
            sqlNode.getKind() == SqlKind.SUM ||
            sqlNode.getKind() == SqlKind.AVG;
  }

  public static boolean isArithmetical(SqlNode sqlNode) {
    return sqlNode.getKind() == SqlKind.PLUS ||
            sqlNode.getKind() == SqlKind.MINUS ||
            sqlNode.getKind() == SqlKind.TIMES ||
            sqlNode.getKind() == SqlKind.DIVIDE;
  }

  private String newAliasVar() {
    return aliasPrefix+(aliasNumber++);
  }

  private String newAliasTable() {
    return tableAliasPrefix+(tableAliasNumber++);
  }

  private String operatorSymbol(SqlKind kind) throws Exception{
    switch (kind) {
      case GREATER_THAN -> { return ">"; }
      case LESS_THAN -> { return "<"; }
      case GREATER_THAN_OR_EQUAL -> { return ">="; }
      case LESS_THAN_OR_EQUAL ->  { return "<="; }
      default -> {
        throw new Exception("unhandled operator");
      }
    }
  }

  private SqlNode mkExpression(SqlNode node, Map<String, String> aliasMap, String aliasTable) throws Exception {
    final SqlParserPos newPos = new SqlParserPos(-1, -1);
    switch (node.getKind()) {
      case AND, OR,
            GREATER_THAN,
            LESS_THAN,
            GREATER_THAN_OR_EQUAL,
            LESS_THAN_OR_EQUAL -> {
        final SqlBasicCall basicCall = (SqlBasicCall) node;
        assert(basicCall.getOperandList().size() == 2);
        basicCall.setOperand(0, mkExpression(basicCall.operand(0), aliasMap, aliasTable));
        basicCall.setOperand(1, mkExpression(basicCall.operand(1), aliasMap, aliasTable));
        return basicCall;
      }
      case IDENTIFIER -> {
        return new SqlIdentifier(aliasTable + "." + aliasMap.get(node.toString()), newPos);
      }
      case LITERAL -> {
        return node;
      }
      case COUNT, MAX, MIN, SUM -> {
        final String alias = newAliasVar();
        aliasMap.put(node.toString(), alias);
        final SqlIdentifier newIdentifier = new SqlIdentifier(aliasTable + "." + alias, newPos);
        return newIdentifier;
      }
      default -> {
        throw new Exception("unhandled mkExpression: " + node.getKind());
      }
    }
  }

  private SqlNode processSelectHaveWhere(SqlNode node, Map<String, String> aliasMap, String aliasTable) throws Exception {
    return mkExpression(node, aliasMap, aliasTable);
  }

  private SqlNode processSelectHaveFrom(SqlNode node, Map<String, String> aliasMap) throws Exception {
    assert (node.getKind() == SqlKind.SELECT);
    assert (aliasMap.size() == 0);
    final SqlSelect sqlSelect = (SqlSelect) node;
    final SqlParserPos newPos = new SqlParserPos(-1, -1);
    final SqlNodeList selectList = sqlSelect.getSelectList();
    final SqlNodeList AliasSelectList = new SqlNodeList(newPos);
    SqlAsOperator asOperator = new SqlAsOperator();
    for(final SqlNode selectNode : selectList) {
      if(selectNode.getKind() == SqlKind.AS) {
        final SqlBasicCall basicCall = (SqlBasicCall) selectNode;
        assert basicCall.getOperandList().size() == 2;
        assert basicCall.getOperandList().get(1).getKind() == SqlKind.IDENTIFIER;
        final SqlNode firstOperand = basicCall.getOperandList().get(0);
        final SqlNode secondOperand = basicCall.getOperandList().get(1);
        aliasMap.put(firstOperand.toString(), secondOperand.toString());
        AliasSelectList.add(asOperator.createCall(newPos, firstOperand, new SqlIdentifier(secondOperand.toString(), newPos)));
      } else {
        final String aliasName = newAliasVar();
        final SqlIdentifier selectNodeAlias = new SqlIdentifier(aliasName, newPos);
        aliasMap.put(selectNode.toString(), aliasName);
        AliasSelectList.add(asOperator.createCall(newPos, selectNode, selectNodeAlias));
      }
    }
    return new SqlSelect(newPos, null, AliasSelectList, sqlSelect.getFrom(), sqlSelect.getWhere(),
            sqlSelect.getGroup(), null, null, null, null, null, null);
  }

  private SqlNode fixSelectHaveFrom(SqlNode node, Map<String, String> aliasMap) throws Exception {
    assert (node.getKind() == SqlKind.SELECT);
    final SqlSelect sqlSelect = (SqlSelect) node;
    final SqlParserPos newPos = new SqlParserPos(-1, -1);
    final SqlNodeList AliasSelectList = new SqlNodeList(newPos);
    SqlAsOperator asOperator = new SqlAsOperator();
    for(Map.Entry<String, String> entry : aliasMap.entrySet()) {
      final SqlIdentifier originNode = new SqlIdentifier(entry.getKey(), newPos);
      final SqlIdentifier selectNodeAlias = new SqlIdentifier(entry.getValue(), newPos);
      AliasSelectList.add(asOperator.createCall(newPos, originNode, selectNodeAlias));
    }
    return new SqlSelect(newPos, null, AliasSelectList, sqlSelect.getFrom(), sqlSelect.getWhere(),
            sqlSelect.getGroup(), null, null, null, null, null, null);
  }

  private SqlNode processSelectHave(SqlNode node) throws Exception {
    //TODO: not completed
    if(node.getKind() == SqlKind.SELECT) {
      final SqlSelect sqlSelect = (SqlSelect) node;
      final SqlParserPos newPos = new SqlParserPos(-1, -1);
      final SqlNodeList selectList = sqlSelect.getSelectList();
      final SqlNodeList originSelectList = new SqlNodeList(newPos);
      final SqlBasicCall have = (SqlBasicCall) sqlSelect.getHaving();
      final String aliasTable = newAliasTable();
      Map<String, String> aliasMap = new HashMap<>();
      SqlNode newFromSqlSelect = processSelectHaveFrom(node, aliasMap);
      final SqlNode newWhere = processSelectHaveWhere(have, aliasMap, aliasTable);
      newFromSqlSelect = fixSelectHaveFrom(node, aliasMap);
      final SqlAsOperator asOperator = new SqlAsOperator();

      for(final SqlNode selectNode : selectList) {
        if(selectNode.getKind() == SqlKind.AS) {
          final SqlBasicCall as = (SqlBasicCall) selectNode;
          final SqlNode operand1 = as.operand(0);
          assert aliasMap.get(operand1.toString()) != null;
          final String aliasName = aliasTable + "." + aliasMap.get(operand1.toString());
          final SqlIdentifier newAs = new SqlIdentifier(aliasName, newPos);
          originSelectList.add(newAs);
        } else {
          assert aliasMap.get(selectNode.toString()) != null;
          final String aliasName = aliasTable + "." + aliasMap.get(selectNode.toString());
          final SqlIdentifier newAs = new SqlIdentifier(aliasName, newPos);
          originSelectList.add(newAs);
        }
      }

      final SqlIdentifier newAs = new SqlIdentifier(aliasTable, newPos);
      final SqlSelect newSqlSelect = new SqlSelect(newPos, null, originSelectList, asOperator.createCall(newPos,newFromSqlSelect, newAs), newWhere,
                    null, null, null, null, null, null, null);
      return newSqlSelect;
    }
    throw new Exception("unhandled case");
  }

  private SqlNode processAggregateFrom(SqlNode node, SqlNodeList colList) throws Exception {
    assert (node.getKind() == SqlKind.SELECT);
    assert (colList.size() == 0);
    final SqlSelect sqlSelect = (SqlSelect) node;
    final SqlParserPos newPos = new SqlParserPos(-1, -1);
    final SqlNodeList selectList = sqlSelect.getSelectList();
    final SqlNodeList AliasSelectList = new SqlNodeList(newPos);
    SqlAsOperator asOperator = new SqlAsOperator();

    for (final SqlNode selectNode : selectList) {
      // outerSelectList = colList
      // innerSelectList = AliasSelectList
      // agg + a AS b -> SELECT ca0 + ca1 AS b FROM (SELECT agg AS ca0, a AS ca1)
      // ... AS b -> SELECT b FROM (SELECT ... AS b)
      // col -> SELECT col FROM (SELECT col)
      if (selectNode.getKind() == SqlKind.AS) {
        final SqlBasicCall basicCall = (SqlBasicCall) selectNode;
        assert basicCall.getOperandList().size() == 2;
        final SqlNode firstOperand = basicCall.getOperandList().get(0);
        final SqlNode secondOperandNode = basicCall.getOperandList().get(1);
        assert secondOperandNode instanceof SqlIdentifier;
        SqlIdentifier secondOperand = (SqlIdentifier) secondOperandNode;
        if (isArithmetical(firstOperand)) {
          final SqlBasicCall arithmeticalCall = (SqlBasicCall) firstOperand;
          assert arithmeticalCall.getOperandList().size() == 2;
          final SqlNode arFirstOperand = arithmeticalCall.getOperandList().get(0);
          final SqlNode arSecondOperand = arithmeticalCall.getOperandList().get(1);
          final String aliasFirstName = newAliasVar();
          final SqlIdentifier aliasFirstOperand = new SqlIdentifier(aliasFirstName, newPos);
          final String aliasSecondName = newAliasVar();
          final SqlIdentifier aliasSecondOperand = new SqlIdentifier(aliasSecondName, newPos);
          String aliasName = secondOperand.names.get(secondOperand.names.size() - 1);
          SqlNode alias = new SqlIdentifier(aliasName, newPos);
          SqlNode arithNew = new SqlBasicCall(arithmeticalCall.getOperator(),
                  new SqlNode[]{aliasFirstOperand, aliasSecondOperand}, newPos);
          SqlNode columnOut = asOperator.createCall(newPos, arithNew, alias);
          colList.add(columnOut);
          AliasSelectList.add(asOperator.createCall(newPos, arFirstOperand, aliasFirstOperand));
          AliasSelectList.add(asOperator.createCall(newPos, arSecondOperand, aliasSecondOperand));
        } else {
          colList.add(secondOperand);
          AliasSelectList.add(selectNode);
        }
      } else {
        if (selectNode instanceof SqlIdentifier id) {
          String name = id.names.get(id.names.size() - 1);
          colList.add(new SqlIdentifier(name, newPos));
          AliasSelectList.add(selectNode);
        } else {
          final String aliasName = newAliasVar();
          final SqlIdentifier selectNodeAlias = new SqlIdentifier(aliasName, newPos);
          colList.add(new SqlIdentifier(aliasName, newPos));
          AliasSelectList.add(asOperator.createCall(newPos, selectNode, selectNodeAlias));
        }
      }
    }
    return new SqlSelect(newPos, null, AliasSelectList, sqlSelect.getFrom(), sqlSelect.getWhere(),
            sqlSelect.getGroup(), sqlSelect.getHaving(), null, null, null, null, null);
  }

  private SqlNode processAggregate(SqlNode node) throws Exception {
    //TODO: not completed
    if(node.getKind() == SqlKind.SELECT) {
      final SqlParserPos newPos = new SqlParserPos(-1, -1);
      final String aliasTable = newAliasTable();
      final SqlAsOperator asOperator = new SqlAsOperator();
      SqlNodeList columnList = new SqlNodeList(newPos);

      final SqlNode newSelectFrom = processAggregateFrom(node, columnList);

      final SqlIdentifier newAs = new SqlIdentifier(aliasTable, newPos);
      final SqlSelect newSqlSelect = new SqlSelect(newPos, null, columnList, asOperator.createCall(newPos, newSelectFrom, newAs), null,
              null, null, null, null, null, null, null);
      return newSqlSelect;
    }
    throw new Exception("unhandled case");
  }

  /** for select list are all agg */
  private Boolean isAggGroupByCase(SqlNode node) {
    if(node.getKind() == SqlKind.SELECT) {
      final SqlSelect sqlSelect = (SqlSelect) node;
      final SqlNodeList selectList = sqlSelect.getSelectList();
      final SqlNodeList groupBys = sqlSelect.getGroup();
      boolean flag = false;
      for(final SqlNode select : selectList) {
        if(select.getKind() == SqlKind.AS) {
          final SqlNode firstOperand = asOperatorFirstOperand(select);
          if(!isAggregateFunc(firstOperand))
            return false;
        } else if(!isAggregateFunc(select))
          return false;
        flag = true;
      }
      if(flag) {
        for(final SqlNode groupBy : groupBys) {
          for(final SqlNode select : selectList) {
            if(select.equals(groupBy)) return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  private SqlNode processAggregateGroupByFrom(SqlNode node, SqlNodeList colList) throws Exception {
    assert (node.getKind() == SqlKind.SELECT);
    assert (colList.size() == 0);
    final SqlSelect sqlSelect = (SqlSelect) node;
    final SqlParserPos newPos = new SqlParserPos(-1, -1);
    final SqlNodeList selectList = sqlSelect.getSelectList();
    final SqlNodeList AliasSelectList = new SqlNodeList(newPos);
    final SqlNodeList groupBys = sqlSelect.getGroup();
    SqlAsOperator asOperator = new SqlAsOperator();

    for(final SqlNode groupByNode : groupBys) {
      assert groupByNode.getKind() != SqlKind.AS;
      assert groupByNode.getKind() == SqlKind.IDENTIFIER;
      if(groupByNode instanceof SqlIdentifier id) {
        AliasSelectList.add(groupByNode);
      }
    }

    for (final SqlNode selectNode : selectList) {
      // outerSelectList = colList
      // innerSelectList = AliasSelectList
      // SELECT agg, agg ... GROUP BY col -> SELECT col, agg, agg ...
      // ... AS b -> SELECT b FROM (SELECT ... AS b)
      // col -> SELECT col FROM (SELECT col)
      if (selectNode.getKind() == SqlKind.AS) {
        final SqlBasicCall basicCall = (SqlBasicCall) selectNode;
        assert basicCall.getOperandList().size() == 2;
        final SqlNode secondOperandNode = basicCall.getOperandList().get(1);
        assert secondOperandNode instanceof SqlIdentifier;
        SqlIdentifier secondOperand = (SqlIdentifier) secondOperandNode;
        colList.add(secondOperand);
        AliasSelectList.add(selectNode);
      } else {
        if (selectNode instanceof SqlIdentifier id) {
          String name = id.names.get(id.names.size() - 1);
          colList.add(new SqlIdentifier(name, newPos));
          AliasSelectList.add(selectNode);
        } else {
          final String aliasName = newAliasVar();
          final SqlIdentifier selectNodeAlias = new SqlIdentifier(aliasName, newPos);
          colList.add(new SqlIdentifier(aliasName, newPos));
          AliasSelectList.add(asOperator.createCall(newPos, selectNode, selectNodeAlias));
        }
      }
    }

    return new SqlSelect(newPos, null, AliasSelectList, sqlSelect.getFrom(), sqlSelect.getWhere(),
            sqlSelect.getGroup(), sqlSelect.getHaving(), null, null, null, null, null);
  }

  private SqlNode processAggregateGroupBy(SqlNode node) throws Exception {
    //TODO: not completed
    if(node.getKind() == SqlKind.SELECT) {
      final SqlParserPos newPos = new SqlParserPos(-1, -1);
      final String aliasTable = newAliasTable();
      final SqlAsOperator asOperator = new SqlAsOperator();
      final SqlSelect sqlSelect = (SqlSelect) node;
      SqlNodeList columnList = new SqlNodeList(newPos);

      final SqlNode newSelectFrom = processAggregateGroupByFrom(node, columnList);


      final SqlIdentifier newAs = new SqlIdentifier(aliasTable, newPos);
      final SqlSelect newSqlSelect = new SqlSelect(newPos, null, columnList, asOperator.createCall(newPos, newSelectFrom, newAs), null,
              null, null, null, null, null, null, null);
      return newSqlSelect;
    }
    throw new Exception("unhandled case");
  }

  private SqlNode processWithAs(SqlNode node) throws Exception {
    final SqlWith withAs = (SqlWith) node;
    SqlWithItem asNode;
    if(withAs.getOperandList().get(0) instanceof SqlNodeList list) {
      assert list.size() == 1;
      asNode = (SqlWithItem) list.get(0);
    }
    else
      asNode = (SqlWithItem) withAs.getOperandList().get(0);
    SqlNode body = withAs.getOperandList().get(1);
    final SqlNode targetNode = new SqlBasicCall(new SqlAsOperator(), new SqlNode[]{asNode.query, asNode.name}, SqlParserPos.ZERO);
    body = replaceNode(body, asNode.name, targetNode);
    return body;
  }

  private SqlNode replaceNode(SqlNode node, SqlNode replaced, SqlNode target) throws Exception {
    if(node == null)
      return null;
    if(replaced.equalsDeep(node, Litmus.IGNORE))
      return target;
    switch (node.getKind()) {
      case SELECT -> {
        SqlSelect select = (SqlSelect) node;
        SqlNodeList selectList = select.getSelectList();
        SqlNodeList newSelectList = new SqlNodeList(SqlParserPos.ZERO);
        if(selectList != null) {
          for(SqlNode selectListItem : selectList)
            newSelectList.add(replaceNode(selectListItem, replaced, target));
          select.setSelectList(newSelectList);
        }
        SqlNodeList groupList = select.getGroup();
        SqlNodeList newGroupList = new SqlNodeList(SqlParserPos.ZERO);
        if(groupList != null) {
          for(SqlNode groupListItem : groupList)
            newGroupList.add(replaceNode(groupListItem, replaced, target));
          select.setGroupBy(newGroupList);
        }
        select.setFrom(replaceNode(select.getFrom(), replaced, target));
        select.setWhere(replaceNode(select.getWhere(), replaced, target));
        select.setHaving(replaceNode(select.getHaving(), replaced, target));
      }
      case IDENTIFIER, LITERAL -> {
        return node;
      }
      case IN, AS, LESS_THAN -> {
        SqlBasicCall in = (SqlBasicCall) node;
        for(int i = 0; i < in.getOperandList().size(); i++)
          in.setOperand(i, replaceNode(in.getOperandList().get(i), replaced, target));
      }
      default -> {
        throw new Exception("Unsupported replaceNode type: " + node.getKind());
      }
    }
    return node;
  }

  public String transform(SqlNode root, rewriteType type) throws Exception{
    switch (type) {
      case HAVING -> {
        return processSelectHave(root).toString().replace("\n", " ").replace("`", "");
      }
      case AGGARITH -> {
        return processAggregate(root).toString().replace("\n", " ").replace("`", "");
      }
      case AGGGROUPBY -> {
        if(!isAggGroupByCase(root))
          return origin_query;
        return processAggregateGroupBy(root).toString().replace("\n", " ").replace("`", "");
      }
      case WITHAS -> {
        return processWithAs(root).toString().replace("\n", " ").replace("`", "");
      }
      default -> {
        throw new Exception("Unknown transform type");
      }
    }
  }
}
