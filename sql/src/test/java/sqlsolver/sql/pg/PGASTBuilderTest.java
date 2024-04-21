package sqlsolver.sql.pg;

import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.Test;
import sqlsolver.sql.pg.internal.PGParser;
import sqlsolver.sql.ast.SqlDataType;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.constants.Category;
import sqlsolver.sql.ast.SqlNodeFields;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PGASTBuilderTest {
  private static final PgAstParser PARSER = new PgAstParser();

  private static class TestHelper {
    private String sql;
    private SqlNode node;
    private final Function<PGParser, ParserRuleContext> rule;

    private TestHelper(Function<PGParser, ParserRuleContext> rule) {
      this.rule = rule;
    }

    private SqlNode sql(String sql) {
      if (sql != null) return (node = PARSER.parse(sql, rule));
      return null;
    }
  }

  @Test
  void testCharacterString() {
    final TestHelper helper = new TestHelper(PGParser::table_column_definition);
    final SqlNode node = helper.sql("\"a\" character varying [][]");
    final SqlDataType dataType = node.$(SqlNodeFields.ColDef_DataType);
    assertEquals(Category.STRING, dataType.category());
    assertArrayEquals(new int[] {0, 0}, dataType.dimensions());
  }
}
