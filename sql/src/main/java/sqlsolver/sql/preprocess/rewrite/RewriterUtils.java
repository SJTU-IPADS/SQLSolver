package sqlsolver.sql.preprocess.rewrite;

import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.parser.SqlParserPos;

public class RewriterUtils {
  /** Return the last name in id (i.e. aN in "a1 DOT a2 DOT ... DOT aN"). */
  public static String tailName(SqlIdentifier id) {
    return id.names.get(id.names.size() - 1);
  }

  /** Return an unqualified copy of id. */
  public static SqlIdentifier tail(SqlIdentifier id) {
    return new SqlIdentifier(tailName(id), SqlParserPos.ZERO);
  }
}
