package sqlsolver.stmt.dao.internal;

import sqlsolver.common.datasource.DbUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class DbDao {
  private final Map<String, PreparedStatement> caches = new HashMap<>();
  private Connection connection = null;

  protected Connection connection() {
    if (connection == null) connection = DbUtils.connection();
    return connection;
  }

  protected PreparedStatement prepare(String sql) throws SQLException {
    PreparedStatement ps = caches.get(sql);
    if (ps != null) return ps;

    final Connection conn = connection();
    caches.put(sql, ps = conn.prepareStatement(sql));
    return ps;
  }

  protected void begin() {
    try {
      final Connection conn = connection();
      conn.setAutoCommit(false);
    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  protected void commit() {
    try {
      final Connection conn = connection();
      conn.commit();
      conn.setAutoCommit(true);
    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }
}
