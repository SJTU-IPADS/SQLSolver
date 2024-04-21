package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlExtractFunction;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The rewriter class is for handling unresolved function:
 */
public class UnresolvedFunctionRewriter extends RecursiveRewriter {

  /**
   * Handle STRFTIME function.
   * STRFTIME -> EXTRACT
   */
  private SqlNode handleSTRFTIME(SqlNode node) {
    if (node instanceof SqlBasicCall call && Objects.equals(call.getOperator().getName(), "STRFTIME")) {
      final SqlNode argument0 = call.getOperandList().get(0);
      final SqlNode argument1 = call.getOperandList().get(1);
      SqlIntervalQualifier intervalQualifier = null;
      if (argument0 instanceof SqlCharStringLiteral charStringLiteral) {
        if (Objects.equals(charStringLiteral.getValueAs(String.class), "%Y")) {
          intervalQualifier = new SqlIntervalQualifier(TimeUnit.YEAR, -1, null, -1, SqlParserPos.ZERO);
        }
      }
      if (argument0 instanceof SqlCharStringLiteral charStringLiteral) {
        if (Objects.equals(charStringLiteral.getValueAs(String.class), "%m")) {
          intervalQualifier = new SqlIntervalQualifier(TimeUnit.MONTH, -1, null, -1, SqlParserPos.ZERO);
        }
      }
      if (argument0 instanceof SqlCharStringLiteral charStringLiteral) {
        if (Objects.equals(charStringLiteral.getValueAs(String.class), "%d")) {
          intervalQualifier = new SqlIntervalQualifier(TimeUnit.DAY, -1, null, -1, SqlParserPos.ZERO);
        }
      }
      if (intervalQualifier != null) {
        return new SqlBasicCall(new SqlExtractFunction("EXTRACT"),
                new ArrayList<>(List.of(intervalQualifier, argument1)),
                SqlParserPos.ZERO);
      }
    }
     return node;
  }

  /**
   * Handle POSITIVE function.
   * POSITIVE function has no actual meaning, just eliminate it.
   */
  private SqlNode handlePOSITIVE(SqlNode node) {
    if (node instanceof SqlBasicCall call && Objects.equals(call.getOperator().getName(), "POSITIVE")) {
      assert call.getOperandList().size() == 1;
      return call.getOperandList().get(0);
    }
    return node;
  }


  @Override
  public SqlNode handleNode(SqlNode node) {
    node = handleSTRFTIME(node);
    node = handlePOSITIVE(node);
    return node;
  }

}
