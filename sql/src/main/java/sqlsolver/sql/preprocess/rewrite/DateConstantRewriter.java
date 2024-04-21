package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlDateLiteral;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateConstantRewriter extends RecursiveRewriter {

  private static final LocalDate DATE_ORIGIN = LocalDate.of(1993, 1, 1);

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlDateLiteral dateNode) {
      String dateStr = dateNode.getValue().toString();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      LocalDate date = LocalDate.parse(dateStr, formatter);
      long diff = ChronoUnit.DAYS.between(DATE_ORIGIN, date);
      return SqlLiteral.createExactNumeric(String.valueOf(diff), SqlParserPos.ZERO);
    }
    return node;
  }

}
