package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlRowOperator;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.List;

// SELECT consts
// ->
// SELECT * FROM VALUES ((consts)) AS t0
public class ConstantRowRewriter extends SqlNodePreprocess {

  private SqlIdentifier star() {
    return new SqlIdentifier(List.of(""), SqlParserPos.ZERO);
  }

  private SqlIdentifier id(String name) {
    return new SqlIdentifier(name, SqlParserPos.ZERO);
  }

  private SqlBasicCall as(SqlNode node, String alias) {
    return new SqlBasicCall(new SqlAsOperator(), new SqlNode[]{node, id(alias)}, SqlParserPos.ZERO);
  }

  @Override
  public SqlNode preprocess(SqlNode node) {
    if (node instanceof SqlSelect select
            && select.getFrom() == null
            && select.getWhere() == null
            && select.getGroup() == null
            && select.getHaving() == null
            && select.getFetch() == null
            && select.getOffset() == null
            && select.getWindowList().size() == 0) {
      boolean isConstList = true;
      for (SqlNode expr : select.getSelectList()) {
        if (!(expr instanceof SqlLiteral)) {
          isConstList = false;
          break;
        }
      }
      // only operate on list of constants
      if (isConstList) {
        SqlNode row = new SqlRowOperator(" ").createCall(select.getSelectList());
        SqlNode values = new SqlValuesOperator().createCall(SqlParserPos.ZERO, row);
        SqlNode as = as(values, "t0");
        return new SqlSelect(SqlParserPos.ZERO, SqlNodeList.EMPTY,
                SqlNodeList.of(star()), as, null, null,
                null, SqlNodeList.EMPTY, null, null, null, null);
      }
    }
    return node;
  }

}
