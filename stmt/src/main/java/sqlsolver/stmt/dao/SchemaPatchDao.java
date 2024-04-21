package sqlsolver.stmt.dao;

import sqlsolver.sql.schema.SchemaPatch;
import sqlsolver.stmt.dao.internal.DbSchemaPatchDao;

import java.util.List;

public interface SchemaPatchDao {
  List<SchemaPatch> findByApp(String appName);

  void save(SchemaPatch patch);

  void truncate(String app);

  void beginBatch();

  void endBatch();

  static SchemaPatchDao instance() {
    return DbSchemaPatchDao.instance();
  }
}
