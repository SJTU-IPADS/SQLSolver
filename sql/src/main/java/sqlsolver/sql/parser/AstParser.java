package sqlsolver.sql.parser;

import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.mysql.MySQLAstParser;
import sqlsolver.sql.pg.PgAstParser;

import java.util.Properties;

public interface AstParser {
  SqlNode parse(String string);

  default void setProperties(Properties props) {}

  static AstParser ofDb(String dbType) {
    if (DbSupport.MySQL.equals(dbType)) return new MySQLAstParser();
    else if (DbSupport.PostgreSQL.equals(dbType)) return new PgAstParser();
    else throw new IllegalArgumentException();
  }

  static AstParser mysql() {
    return ofDb(DbSupport.MySQL);
  }

  static AstParser postgresql() {
    return ofDb(DbSupport.PostgreSQL);
  }
}
