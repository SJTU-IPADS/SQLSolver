package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


// a OP b -> a * 10^n OP b * 10^n
public class NumericComparisonRewriter extends RecursiveRewriter {

  private List<SqlOperator> comparisons;

  public NumericComparisonRewriter() {
    comparisons = new ArrayList<>();
    comparisons.add(SqlStdOperatorTable.EQUALS);
    comparisons.add(SqlStdOperatorTable.LESS_THAN);
    comparisons.add(SqlStdOperatorTable.LESS_THAN_OR_EQUAL);
    comparisons.add(SqlStdOperatorTable.GREATER_THAN);
    comparisons.add(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL);
  }

  private int getScale(SqlNode node) {
    if (node instanceof SqlBasicCall call) {
      assert call.operandCount() == 2;
      SqlOperator op = call.getOperator();
      SqlNode left = call.operand(0), right = call.operand(1);
      int scaleLeft = getScale(left), scaleRight = getScale(right);
      switch (op.getKind()) {
        case PLUS, MINUS -> { return Math.max(scaleLeft, scaleRight); }
        case TIMES -> { return scaleLeft + scaleRight; }
      }
    } else if (node instanceof SqlNumericLiteral literal) {
      int scale = literal.getScale();
      assert scale >= 0;
      return scale;
    }
    return 0;
  }

  private SqlNode upScale(SqlNode node, int scale) {
    if (node instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      SqlNode left = call.operand(0), right = call.operand(1);
      switch (op.getKind()) {
        case PLUS, MINUS -> {
          left = upScale(left, scale);
          right = upScale(right, scale);
        }
        case TIMES -> {
          int scaleLeft = getScale(left);
          left = upScale(left, scaleLeft);
          right = upScale(right, scale - scaleLeft);
        }
      }
      call.setOperand(0, left);
      call.setOperand(1, right);
    } else if (node instanceof SqlNumericLiteral literal) {
      BigDecimal decimal = literal.bigDecimalValue().scaleByPowerOfTen(scale);
      return SqlLiteral.createExactNumeric(decimal.toString(), SqlParserPos.ZERO);
    } else {
      SqlNumericLiteral literal = SqlLiteral.createExactNumeric("1" + "0".repeat(scale), SqlParserPos.ZERO);
      return SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO,
              node, literal);
    }
    return node;
  }

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlBasicCall call) {
      SqlOperator op = call.getOperator();
      if (comparisons.contains(op)) {
        try {
          SqlNode left = call.operand(0), right = call.operand(1);
          int scale = Math.max(getScale(left), getScale(right));
          if (scale > 0) {
            call.setOperand(0, upScale(left, scale));
            call.setOperand(1, upScale(right, scale));
          }
        } catch (Throwable ignored) {}
      }
    }
    return node;
  }

}
