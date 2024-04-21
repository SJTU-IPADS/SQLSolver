package sqlsolver.api.schema;

import org.junit.jupiter.api.Test;
import sqlsolver.api.TestHelper;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.schema.Schema;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaProviderTest {

  @Test
  void testValidSqlSchema() {
     final String testSchema =
         ""
         + "CREATE TABLE a ( i INT PRIMARY KEY, j INT, k INT );"
         + "CREATE TABLE b ( x INT PRIMARY KEY, y INT, z INT );"
         + "CREATE TABLE c ( u INT PRIMARY KEY, v CHAR(10), w DECIMAL(1, 10) );"
         + "CREATE TABLE d ( p INT, q CHAR(10), r DECIMAL(1, 10), UNIQUE KEY (p), FOREIGN KEY (p) REFERENCES c (u) );";
     final Schema schema = TestHelper.parseSchema(testSchema);
     assertNotNull(schema);
     assertTrue(schema.tables().size() != 0);
  }

  @Test
  void testInvalidSqlSchema() {
    final String testSchema =
        ""
        + "CREATE TABLE a ( i INT PRIMARY KEY, INT, k INT );"
        + "CREATE TABLE b ( x INT PRIMARY KEY, y INT, );";
    // mute parsing error information
    SqlSupport.muteParsingError();
    final Schema schema = TestHelper.parseSchema(testSchema);
    // unmute parsing error information
    SqlSupport.unMuteParsingError();
    assertNotNull(schema);
    assertTrue(schema.tables().size() == 0);
  }
}
