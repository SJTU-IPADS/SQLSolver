package sqlsolver.sql.ast.constants;

import java.util.Set;

public interface DataTypeName {
  // integral
  String TINYINT = "tinyint";
  String INT = "int";
  String INTEGER = "integer";
  String SMALLINT = "smallint";
  String MEDIUMINT = "mediumint";
  String BIGINT = "bigint";
  String SMALLSERIAL = "smallserial";
  String SERIAL = "serial";
  String BIGSERIAL = "bigserial";
  // bit-string
  String BIT = "bit";
  String BIT_VARYING = "bit varying";
  // fraction
  String REAL = "real";
  String DOUBLE = "double";
  String FLOAT = "float";
  String DECIMAL = "decimal";
  String NUMERIC = "numeric";
  String FIXED = "fixed";
  Set<String> FRACTION_TYPES = Set.of(REAL, DOUBLE, FLOAT, DECIMAL, NUMERIC, FIXED);
  // boolean
  String BOOLEAN = "boolean";
  // enum
  String ENUM = "enum";
  String SET = "set";
  // string
  String CHAR = "char";
  String VARCHAR = "varchar";
  String BINARY = "binary";
  String VARBINARY = "varbinary";
  String TINYTEXT = "tinytext";
  String TEXT = "text";
  String MEDIUMTEXT = "mediumtext";
  String BIGTEXT = "bigtext";
  // time
  String YEAR = "year";
  String DATE = "date";
  String TIME = "time";
  String TIMETZ = "timetz";
  String TIMESTAMP = "timestamp";
  String TIMESTAMPTZ = "timestamptz";
  String DATETIME = "datetime";
  Set<String> TIME_TYPE = Set.of(YEAR, DATE, TIME, TIMETZ, TIMESTAMP, TIMESTAMPTZ, DATETIME);
  // blob
  String TINYBLOB = "tinyblob";
  String BLOB = "blob";
  String MEDIUMBLOB = "mediumblob";
  String LONGBLOB = "longblob";
  // json
  String JSON = "json";
  // geo
  String GEOMETRY = "geometry";
  String GEOMETRYCOLLECTION = "geometrycollection";
  String POINT = "point";
  String MULTIPOINT = "multipoint";
  String LINESTRING = "linestring";
  String MULTILINESTRING = "multilinestring";
  String POLYGON = "polygon";
  String MULTIPOLYGON = "multipolygon";
  String BOX = "box";
  String CIRCLE = "circle";
  String LINE = "line";
  String LSEG = "lseg";
  String PATH = "path";
  Set<String> GEOMETRY_TYPES =
      Set.of(
          GEOMETRY,
          GEOMETRYCOLLECTION,
          POINT,
          MULTIPOINT,
          LINESTRING,
          MULTILINESTRING,
          POLYGON,
          MULTIPOLYGON,
          BOX,
          CIRCLE,
          LINE,
          LSEG,
          PATH);
  // interval
  String INTERVAL = "interval";
  // net
  String CIDR = "cidr";
  String INET = "inet";
  String MACADDR = "macaddr";
  Set<String> NET_TYPES = Set.of(CIDR, INET, MACADDR);
  // money
  String MONEY = "money";
  // uuid
  String UUID = "uuid";
  // xml
  String XML = "xml";
  String PG_LSN = "pg_lsn";
  String TSVECTOR = "tsvector";
  String TSQUERY = "tsquery";
  String TXID_SNAPSHOT = "txid_snapshot";
}
