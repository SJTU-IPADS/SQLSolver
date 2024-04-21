package sqlsolver.sql.preprocess.rewrite;

import com.google.common.collect.Iterables;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelReferentialConstraint;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.Litmus;
import sqlsolver.sql.Rewriter;
import sqlsolver.sql.calcite.CalciteSupport;

import java.util.Map;

/**
 * The rewriter class is for handling aggregation that select group list
 * is larger than group by list.
 * e.g. SELECT A, B, COUNT(*) FROM T GROUP BY A
 * if A is primary key, it can be transformed into:
 * SELECT A, B, COUNT(*) FROM T GROUP BY A, B
 */
public class AggSelectLargerGroupRewriter extends RecursiveRewriter {

  private Map<SqlNode, SqlNode> aliasToNode;

  /**
   * Check whether node match the case
   */
  private boolean isTheCase(SqlSelect sqlSelect) {
    final SqlNodeList selectList = sqlSelect.getSelectList();
    final SqlNodeList groupBys = sqlSelect.getGroup();
    if(selectList == null || groupBys == null) return false;

    SqlNodeList selectNotAggregatedList = new SqlNodeList(SqlParserPos.ZERO);
    SqlNodeList selectNotAggregatedAliasList = new SqlNodeList(SqlParserPos.ZERO);
    for(final SqlNode select : selectList) {
      SqlNode targetSelect = select;
      SqlNode targetAlias = null;
      if(select.getKind() == SqlKind.AS) {
        targetSelect = Rewriter.asOperatorFirstOperand(select);
        targetAlias = Rewriter.asOperatorSecondOperand(select);
        aliasToNode.put(targetAlias, targetSelect);
      }

      // for aggregated function, just ignore them.
      if (targetSelect instanceof SqlBasicCall basicCall
          && CalciteSupport.isAggOperator(basicCall.getOperator())) continue;

      // consider not aggregated function and its alias
      selectNotAggregatedList.add(targetSelect);
      if (targetAlias != null) {
        selectNotAggregatedAliasList.add(targetAlias);
      }
    }

    boolean groupNotEqualToSelect = false;

    // check whether select list have col that group list don't have
    for (final SqlNode group : groupBys) {
      groupNotEqualToSelect = true;
      for (final SqlNode notAggregatedNode : Iterables.concat(selectNotAggregatedList, selectNotAggregatedAliasList)) {
        if (group.equalsDeep(notAggregatedNode, Litmus.IGNORE)) {
          groupNotEqualToSelect = false;
        }
      }
      if (groupNotEqualToSelect) break;
    }

    // TODO: check whether every matched select list is primary key.
    if (groupNotEqualToSelect) {

    }

    return false;
  }

  @Override
  public SqlNode handleNode(SqlNode node) {
    aliasToNode.clear();
    if (node instanceof SqlSelect select && isTheCase(select)) {

    }
    return node;
  }
}
