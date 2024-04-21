package sqlsolver.stmt;

import sqlsolver.sql.schema.Schema;
import sqlsolver.stmt.internal.AppImpl;

import java.util.Collection;
import java.util.Properties;

/**
 * Basic information about an application. Also serve as the reader and cache for per-app
 * information (e.g. statements, schema, timing)
 */
public interface App {
  String name();

  String dbType();

  Schema schema(String tag, boolean patched);

  Properties dbProps();

  void setDbType(String dbType);

  void setSchema(String tag, Schema schema);

  void setDbConnProps(Properties props);

  default Schema schema(String tag) {
    return schema(tag, false);
  }

  static App of(String name) {
    return AppImpl.of(name);
  }

  static Collection<App> all() {
    return AppImpl.all();
  }

}
