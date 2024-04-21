package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.preprocess.handler.CastHandler;

import java.util.List;
import java.util.Objects;

/**
 * The rewriter class is for handling cast cases.
 */
public class CastRewriter extends RecursiveRewriter {

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlBasicCall basicCall
            && Objects.equals(basicCall.getOperator().getName(), "CAST")) {
      final SqlNode secondOperand = basicCall.getOperandList().get(1);
      if (secondOperand instanceof SqlDataTypeSpec dataType) {
        switch (dataType.getTypeName().toString()) {
          case "SIGNED" -> {
            // for firstOperand boolean case, transform it into CASE WHEN because boolean cannot be cast into integer in calcite.
            final SqlNode firstOperand = basicCall.getOperandList().get(0);
            if (CalciteSupport.isBooleanSqlNode(firstOperand)) {
              return SqlCase.createSwitched(SqlParserPos.ZERO,
                      null,
                      SqlNodeList.of(firstOperand),
                      SqlNodeList.of(SqlLiteral.createExactNumeric("1", SqlParserPos.ZERO)),
                      SqlNodeList.of(SqlLiteral.createExactNumeric("0", SqlParserPos.ZERO)));
            }
            // for SIGNED case, transform it into INTEGER case.
            basicCall.setOperand(1, new SqlDataTypeSpec(new SqlBasicTypeNameSpec(SqlTypeName.INTEGER, SqlParserPos.ZERO), SqlParserPos.ZERO));
          }
          default -> {
          }
        }
      }
    }
    return node;
  }
}
