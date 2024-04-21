package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;
import sqlsolver.sql.Rewriter;

import java.util.List;

public class AggSelectGroupRewriter extends RecursiveRewriter {
  /** for select list is the subset of group by
   *  e.g SELECT A, COUNT(*) FROM T GROUP BY A, B
   *  SELECT <cols1> FROM t GROUP BY <cols1>, <cols2>, ...
   *  ->
   *  SELECT <cols1>' FROM (SELECT <cols1>, <cols2>, ... FROM t GROUP BY <cols1>, <cols2>, ...)
   *  WHERE cols1, cols2, ... are all columns
   *  and cols1' should use aliases instead of expressions
   */
  private int tableAliasIndex, columnAliasIndex;

  public AggSelectGroupRewriter() {
    setAllowsMultipleApplications(true);
  }

  @Override
  public boolean prepare(SqlNode node) {
    tableAliasIndex = 0;
    columnAliasIndex = 0;
    return true;
  }

  private String tailName(SqlIdentifier id) {
    return id.names.get(id.names.size() - 1);
  }

  private SqlIdentifier tail(SqlIdentifier id) {
    return new SqlIdentifier(tailName(id), SqlParserPos.ZERO);
  }

  private SqlIdentifier newTableAlias() {
    String name = "ASG" + (tableAliasIndex++);
    return new SqlIdentifier(name, SqlParserPos.ZERO);
  }

  private String newColumnName() {
    return "ASGCOL" + columnAliasIndex++;
  }

  // record all the col and its alias, the 2th and 3rd argument are the results
  private void getColListAndAlias(SqlNodeList nodeList, SqlNodeList selectColList, SqlNodeList selectColAliasList) {
    for(final SqlNode select : nodeList) {
      if(select.getKind() == SqlKind.AS) {
        final SqlNode firstOperand = Rewriter.asOperatorFirstOperand(select);
        final SqlNode secondOperand = Rewriter.asOperatorSecondOperand(select);
        if(firstOperand instanceof SqlIdentifier) {
          selectColList.add(firstOperand);
          // only record col's alias
          selectColAliasList.add(secondOperand);
        }
      } else if(select instanceof SqlIdentifier)
        selectColList.add(select);
    }
  }

  // get groupBy list have col that select list dont have
  private SqlNodeList getGroupByNotInNodeList(SqlNodeList groupBys, SqlNodeList selectColList, SqlNodeList selectColAliasList) {
    SqlNodeList result = new SqlNodeList(SqlParserPos.ZERO);
    final List<String> selectColStrList = selectColList.getList().stream().map(Object::toString).toList();
    final List<String> selectColStrAliasList = selectColAliasList.getList().stream().map(Object::toString).toList();
    for(final SqlNode groupBy : groupBys) {
      if(groupBy instanceof SqlIdentifier) {
        if(!selectColStrList.contains(groupBy.toString()) && !selectColStrAliasList.contains(groupBy.toString()))
          result.add(groupBy);
      }
    }
    return result;
  }

  private SqlNodeList getSelectOuter(SqlNodeList selectList) {
    SqlNodeList result = new SqlNodeList(SqlParserPos.ZERO);
    for(int i = 0, bound = selectList.size(); i < bound; i++) {
      SqlNode toAddItem = selectList.get(i).clone(SqlParserPos.ZERO);
      if(toAddItem.getKind() == SqlKind.AS) {
        toAddItem = Rewriter.asOperatorSecondOperand(toAddItem);
      } else if(toAddItem instanceof SqlIdentifier id) {
        toAddItem = tail(id);
      }
      else  {
        // for the case is not SqlIdentifier, it must be AS if this select has outer select
        String newColAlias = newColumnName();
        SqlIdentifier newAs = new SqlIdentifier(newColAlias, SqlParserPos.ZERO);
        SqlBasicCall as = new SqlBasicCall(new SqlAsOperator(),
                new SqlNode[]{toAddItem, newAs}, SqlParserPos.ZERO);
        selectList.set(i, as);
        toAddItem = newAs;
      }
      result.add(toAddItem);
    }
    return result;
  }

  private boolean isTheCase(SqlNode node) {
    if(node.getKind() == SqlKind.SELECT) {
      final SqlSelect sqlSelect = (SqlSelect) node;
      final SqlNodeList selectList = sqlSelect.getSelectList();
      final SqlNodeList groupBys = sqlSelect.getGroup();
      if(selectList == null || groupBys == null) return false;

      SqlNodeList selectColList = new SqlNodeList(SqlParserPos.ZERO);
      SqlNodeList selectColAliasList = new SqlNodeList(SqlParserPos.ZERO);

      // record all the col and its alias
      getColListAndAlias(selectList, selectColList, selectColAliasList);

      // check whether groupBy list have col that select list dont have
      return getGroupByNotInNodeList(groupBys, selectColList, selectColAliasList).size() != 0;
    }
    return false;
  }

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlSelect select && isTheCase(node)) {

        SqlNodeList selectListInner = select.getSelectList();
        SqlNodeList selectListOuter = getSelectOuter(selectListInner);
        SqlNodeList groupBys = select.getGroup();

        SqlNodeList newSelectListInner = selectListInner.clone(SqlParserPos.ZERO);
        SqlNodeList selectColList = new SqlNodeList(SqlParserPos.ZERO);
        SqlNodeList selectColAliasList = new SqlNodeList(SqlParserPos.ZERO);

        getColListAndAlias(selectListInner, selectColList, selectColAliasList);
        // for the groupByOwns, it only contains the Col that has no alias
        SqlNodeList groupByOwns = getGroupByNotInNodeList(groupBys, selectColList, selectColAliasList);

        for(SqlNode groupByOwn : groupByOwns) {
          newSelectListInner.add(groupByOwn);
        }

        // construct the outer query
        select.setSelectList(newSelectListInner);
        SqlBasicCall as = new SqlBasicCall(new SqlAsOperator(),
                new SqlNode[]{select, newTableAlias()}, SqlParserPos.ZERO);
        return new SqlSelect(SqlParserPos.ZERO,
                             new SqlNodeList(SqlParserPos.ZERO),
                             selectListOuter, as, null, null, null,
                             new SqlNodeList(SqlParserPos.ZERO),
                             null, null, null, null);

    }
    return node;
  }

}
