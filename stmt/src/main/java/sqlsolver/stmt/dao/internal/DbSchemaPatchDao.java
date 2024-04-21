package sqlsolver.stmt.dao.internal;

import sqlsolver.sql.schema.SchemaPatch;
import sqlsolver.stmt.dao.SchemaPatchDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DbSchemaPatchDao extends DbDao implements SchemaPatchDao {
  private static final SchemaPatchDao INSTANCE = new DbSchemaPatchDao();

  private DbSchemaPatchDao() {}

  public static SchemaPatchDao instance() {
    return INSTANCE;
  }

  private static final String KEY_COLUMNS = "columnNames";
  private static final String KEY_APP = "app";
  private static final String KEY_TYPE = "type";
  private static final String KEY_TABLE_NAME = "tableName";
  private static final String KEY_SOURCE = "source";
  private static final String KEY_REFERENCE = "reference";

  private static final String FIND_BY_APP =
      String.format(
          "SELECT patch_app AS %s, patch_type AS %s, patch_table_name AS %s, patch_columns_name AS %s, patch_source AS %s, patch_reference AS %s "
              + "FROM sqlsolver_schema_patches "
              + "WHERE patch_app = ?",
          KEY_APP, KEY_TYPE, KEY_TABLE_NAME, KEY_COLUMNS, KEY_SOURCE, KEY_REFERENCE);
  private static final String DELETE_GENERATED =
      "DELETE FROM sqlsolver_schema_patches WHERE patch_source <> 'manual' AND patch_app = ?";
  private static final String INSERT =
      "INSERT OR REPLACE INTO sqlsolver_schema_patches (patch_app, patch_type, patch_table_name, patch_columns_name, patch_source, patch_reference) "
          + "VALUES (?, ?, ?, ?, ?, ?)";

  private static SchemaPatch inflate(ResultSet set) throws SQLException {
    final SchemaPatch.Type type = SchemaPatch.Type.valueOf(set.getString(KEY_TYPE));
    final String app = set.getString(KEY_APP);
    final String table = set.getString(KEY_TABLE_NAME);
    final List<String> columns = Arrays.asList(set.getString(KEY_COLUMNS).split(","));
    final String reference = set.getString(KEY_REFERENCE);

    return SchemaPatch.build(type, app, table, columns, reference);
  }

  @Override
  public void beginBatch() {
    begin();
  }

  @Override
  public void endBatch() {
    commit();
  }

  @Override
  public void truncate(String appName) {
    try {
      final PreparedStatement ps = prepare(DELETE_GENERATED);
      ps.setString(1, appName);
      ps.executeUpdate();

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public List<SchemaPatch> findByApp(String appName) {
    try {
      final PreparedStatement ps = prepare(FIND_BY_APP);
      ps.setString(1, appName);

      final ResultSet rs = ps.executeQuery();

      final List<SchemaPatch> patches = new ArrayList<>(50);
      while (rs.next()) patches.add(inflate(rs));

      return patches;

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public void save(SchemaPatch patch) {
    try {
      final PreparedStatement ps = prepare(INSERT);
      ps.setString(1, patch.schema());
      ps.setString(2, patch.type().name());
      ps.setString(3, patch.table());
      ps.setString(4, String.join(",", patch.columns()));
      ps.setString(5, null);
      ps.setString(6, patch.reference());

      ps.executeUpdate();

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }
}
