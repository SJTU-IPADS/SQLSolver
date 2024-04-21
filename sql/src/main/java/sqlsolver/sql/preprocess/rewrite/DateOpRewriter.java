package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.DateString;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The rewriter class is for handling Date operators:
 * date_sub(Date, Interval) will be calculated into date(Date)
 * date + interval will be calculated into date(Date)
 */
public class DateOpRewriter extends RecursiveRewriter {

  /**
   * Calculate the result of given dateStr and field and amount.
   */
  private String dateAdd(String dateStr, int field, int amount) {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date date = format.parse(dateStr);
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(date);
      calendar.add(field, amount);
      Date newDate = calendar.getTime();
      return format.format(newDate);
    } catch (ParseException e) {
      assert false;
    }
    return null;
  }

  /**
   * Calculate the result of two SqlNodes including date/interval literal.
   */
  private SqlBasicCall dateAdd(SqlBasicCall basicCall, SqlIntervalLiteral intNode) {
    final String dateStr = basicCall.operand(0).toString().replace("'", "");
    int field = -1;
    switch (intNode.getTypeName()) {
      case INTERVAL_DAY -> field = Calendar.DAY_OF_MONTH;
      case INTERVAL_MONTH -> field = Calendar.MONTH;
      case INTERVAL_YEAR -> field = Calendar.YEAR;
      default -> {
        assert false; // unsupported
      }
    }
    int amount = Integer.parseInt(intNode.getValue().toString());
    String newDateStr = dateAdd(dateStr, field, amount);
    basicCall.setOperand(0, SqlLiteral.createCharString(newDateStr, SqlParserPos.ZERO));
    return basicCall;
  }

  /**
   * Calculate the result of given SqlCharLiteralString
   */
  private SqlBasicCall dateAdd(SqlCharStringLiteral argument0, SqlCharStringLiteral argument1) {
    final String dateStr = argument0.getValueAs(String.class);
    final String[] cals = argument1.getValueAs(String.class).split(" ");
    if (cals.length != 2) {
      return null;
    }
    int amount = Integer.parseInt(cals[0]);
    int field = -1;
    switch (cals[1]) {
      case "day", "days" -> field = Calendar.DAY_OF_MONTH;
      case "month", "months" -> field = Calendar.MONTH;
      case "year", "years" -> field = Calendar.YEAR;
      default -> {
        assert false; // unsupported
      }
    }
    final SqlUnresolvedFunction dateFunction = new SqlUnresolvedFunction(new SqlIdentifier(new ArrayList<>(List.of("DATE")),
            null,
            SqlParserPos.ZERO,
            null),
            null,
            null,
            null,
            null,
            SqlFunctionCategory.TIMEDATE);
    final String newDateStr = dateAdd(dateStr, field, amount);
    return new SqlBasicCall(dateFunction, new ArrayList<>(List.of(SqlCharStringLiteral.createCharString(newDateStr, SqlParserPos.ZERO))), SqlParserPos.ZERO);
  }

  /**
   * Calculate data_sub and fold it into a date constant.
   */
  private SqlNode foldDateSub(SqlCall dateSub) {
    if (dateSub.operand(0) instanceof SqlCharStringLiteral && dateSub.operand(1) instanceof SqlNumericLiteral) {
      String dateStr = ((SqlCharStringLiteral) dateSub.operand(0)).getNlsString().getValue();
      int amount = -((SqlNumericLiteral) dateSub.operand(1)).intValue(true);
      String newDateStr = dateAdd(dateStr, Calendar.DAY_OF_MONTH, amount);
      return SqlLiteral.createDate(new DateString(newDateStr), SqlParserPos.ZERO);
    } else {
      return dateSub;
    }
  }

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlBasicCall call) {
      final SqlOperator op = call.getOperator();
      if (op instanceof SqlUnresolvedFunction funcOp) {
        switch (funcOp.getName()) {
          case "DATE_SUB" -> {
            // DATE_SUB case
            return foldDateSub(call);
          }
          case "DATE" -> {
            // DATE case, e.g. DATE('1993-07-01', '+3 month')
            if (call.getOperandList().size() == 2
                    && call.operand(0) instanceof SqlCharStringLiteral argument0
                    && call.operand(1) instanceof SqlCharStringLiteral argument1) {
              return dateAdd(argument0, argument1);
            }
          }
        }
      } else if (op.getKind().equals(SqlKind.PLUS)) {
        if (call.operand(0) instanceof SqlBasicCall basicCall
                && Objects.equals(basicCall.getOperator().toString(), "DATE")
                && call.operand(1) instanceof SqlIntervalLiteral intNode) {
          // date 'yyyy-MM-dd' + interval ...
          return dateAdd(basicCall, intNode);
        }
      }
    }
    return node;
  }

}
