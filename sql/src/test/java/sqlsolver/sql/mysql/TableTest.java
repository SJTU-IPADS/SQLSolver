package sqlsolver.sql.mysql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;
import sqlsolver.sql.schema.Table;

import static org.junit.jupiter.api.Assertions.*;

public class TableTest {
  @Test
  @DisplayName("[Stmt.Table] from CREATE TABLE")
  void test() {
    final String createTable =
        ""
            + "create table `public`.t ("
            + "`i` int(10) primary key references b(x),"
            + "j varchar(512) NOT NULL DEFAULT 'a',"
            + "k int AUTO_INCREMENT CHECK (k < 100),"
            + "index (j(100)),"
            + "unique (j DESC) using rtree,"
            + "constraint fk_cons foreign key fk (k) references b(y)"
            + ") ENGINE = 'myisam';";
    final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, createTable);
    final Table table = schema.table("t");

    {
      assertEquals("public", table.schema());
      assertEquals("t", table.name());
      assertEquals("myisam", table.engine());
      assertEquals(3, table.columns().size());
      assertEquals(7, table.constraints().size());
    }

    {
      final Column col0 = table.column("i");
      assertTrue(col0.isFlag(Column.Flag.PRIMARY));
      assertTrue(col0.isFlag(Column.Flag.FOREIGN_KEY));
      assertFalse(col0.isFlag(Column.Flag.AUTO_INCREMENT));
      assertFalse(col0.isFlag(Column.Flag.HAS_CHECK));
    }

    {
      final Column col1 = table.column("j");
      assertFalse(col1.isFlag(Column.Flag.PRIMARY));
      assertTrue(col1.isFlag(Column.Flag.NOT_NULL));
      assertTrue(col1.isFlag(Column.Flag.HAS_DEFAULT));
      assertTrue(col1.isFlag(Column.Flag.INDEXED));
      assertTrue(col1.isFlag(Column.Flag.UNIQUE));
    }

    {
      final Column col2 = table.column("k");
      assertFalse(col2.isFlag(Column.Flag.PRIMARY));
      assertTrue(col2.isFlag(Column.Flag.HAS_CHECK));
      assertTrue(col2.isFlag(Column.Flag.AUTO_INCREMENT));
      assertTrue(col2.isFlag(Column.Flag.FOREIGN_KEY));
    }
  }
}
