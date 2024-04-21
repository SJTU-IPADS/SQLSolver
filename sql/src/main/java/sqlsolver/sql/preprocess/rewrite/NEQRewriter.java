package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

/** NOT (A = B) -> A <> B */
public class NEQRewriter extends RecursiveRewriter {

  @Override
  public SqlNode handleNode(SqlNode node) {
    if (node instanceof SqlBasicCall call
            && call.getKind().equals(SqlKind.NOT)
            && call.operand(0) instanceof SqlBasicCall call0
            && call0.getKind().equals(SqlKind.EQUALS)) {
      return SqlStdOperatorTable.NOT_EQUALS.createCall(SqlParserPos.ZERO,
              call0.operand(0), call0.operand(1));
    }
    return node;
  }

}
