package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The rewriter class is for transforming limit 0 into filter false.
 */
public class LimitZeroRewriter extends RecursiveRewriter {
  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlOrderBy orderBy) {
      final SqlNode fetch = orderBy.fetch;
      if (fetch instanceof SqlNumericLiteral literal
              && literal.isInteger()
              && literal.getValueAs(BigDecimal.class).intValue() == 0){
        // only consider when the query is SqlSelect.
        if (orderBy.query instanceof SqlSelect select) {
          select.setWhere(SqlLiteral.createBoolean(false, SqlParserPos.ZERO));
          return select;
        }
      }
    }
    return node;
  }
}
