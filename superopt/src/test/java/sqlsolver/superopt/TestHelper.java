package sqlsolver.superopt;

import sqlsolver.common.utils.Lazy;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanSupport;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;

import java.nio.file.Path;

import static sqlsolver.common.datasource.DbSupport.MySQL;

public abstract class TestHelper {
  private static final String TEST_SCHEMA =
      ""
          + "CREATE TABLE a ( i INT PRIMARY KEY, j INT, k INT );"
          + "CREATE TABLE b ( x INT PRIMARY KEY, y INT, z INT );"
          + "CREATE TABLE c ( u INT PRIMARY KEY, v CHAR(10), w DECIMAL(1, 10) );"
          + "CREATE TABLE d ( p INT, q CHAR(10), r DECIMAL(1, 10), UNIQUE KEY (p), FOREIGN KEY (p) REFERENCES c (u) );";
  private static final Lazy<Schema> SCHEMA =
      Lazy.mk(() -> SchemaSupport.parseSchema(MySQL, TEST_SCHEMA));

  public static SqlNode parseSql(String sql) {
    return SqlSupport.parseSql(MySQL, sql);
  }

  public static PlanContext parsePlan(String sql) {
    return PlanSupport.assemblePlan(parseSql(sql), SCHEMA.get());
  }

  public static Path dataDir() {
    return Path.of(System.getProperty("sqlsolver.data_dir", "sqlsolver_data"));
  }
}
