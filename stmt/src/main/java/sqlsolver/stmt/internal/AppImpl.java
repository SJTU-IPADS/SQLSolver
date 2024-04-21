package sqlsolver.stmt.internal;

import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;
import sqlsolver.stmt.App;
import sqlsolver.stmt.dao.SchemaPatchDao;
import sqlsolver.common.io.FileUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static sqlsolver.common.datasource.DbSupport.MySQL;
import static sqlsolver.common.datasource.DbSupport.PostgreSQL;

public class AppImpl implements App {
  private final String name;
  private String dbType;
  private final Map<String, Schema> schemas;
  private Properties connProps;

  private AppImpl(String name, String dbType) {
    this.name = name;
    this.dbType = dbType;
    this.schemas = new HashMap<>();
  }

  public static App of(String name) {
    return KNOWN_APPS.computeIfAbsent(name, it -> new AppImpl(it, MySQL));
  }

  public static Collection<App> all() {
    return KNOWN_APPS.values();
  }

  public String name() {
    return name;
  }

  public String dbType() {
    return dbType;
  }

  public Schema schema(String tag, boolean patched) {
    final Schema existing = schemas.get(tag);
    if (existing == null) {
      final Schema schema = readSchema(tag);
      if (schema == null) return null;
      if (patched) schema.patch(SchemaPatchDao.instance().findByApp(name));
      schemas.put(tag, schema);
      return schema;
    } else return existing;
  }

  @Override
  public Properties dbProps() {
    if (connProps == null) {
      final String dbName = name + "_base";
      connProps = DbSupport.dbProps(dbType, dbName);
    }

    return connProps;
  }

  @Override
  public void setDbType(String dbType) {
    this.dbType = dbType;
  }

  @Override
  public void setSchema(String tag, Schema schema) {
    schemas.put(tag, schema);
  }

  @Override
  public void setDbConnProps(Properties props) {
    this.connProps = props;
  }

  private Schema readSchema(String tag) {
    final String str = FileUtils.readFile("schemas", name + "." + tag + ".schema.sql");
    if (str == null) return null;
    return SchemaSupport.parseSchema(dbType, str);
  }

  private static final String[] APP_NAMES = {
    "spider_car_1",
    "spider_employee_hire_evaluation",
    "spider_tvshow",
    "spider_singer",
    "spider_course_teach",
    "spider_student_transcripts_tracking",
    "spider_wta_1",
    "spider_cre_Doc_Template_Mgt",
    "spider_poker_player",
    "spider_world_1",
    "spider_pets_1",
    "spider_flight_2",
    "spider_concert_singer",
    "spider_orchestra",
    "spider_dog_kennels",
    "spider_network_1",
    "spider_voter_1",
    "spider_museum_visit",
    "spider_battle_death",
    "broadleaf",
    "tpcc",
    "tpch",
    "job",
    "calcite_test",
    "diaspora",
    "discourse",
    "eladmin",
    "fatfreecrm",
    "febs",
    "forest_blog",
    "gitlab",
    "guns",
    "halo",
    "homeland",
    "lobsters",
    "publiccms",
    "pybbs",
    "redmine",
    "refinerycms",
    "sagan",
    "shopizer",
    "solidus",
    "spree",
    "useful_rewrite_example"
  };

  private static final Set<String> PG_APPS = Set.of("discourse", "gitlab", "homeland");

  private static final Map<String, App> KNOWN_APPS =
      Arrays.stream(APP_NAMES)
          .map(it -> new AppImpl(it, (PG_APPS.contains(it) ? PostgreSQL : MySQL)))
          .collect(Collectors.toMap(App::name, identity()));
}
