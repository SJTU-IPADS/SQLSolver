package sqlsolver.sql.schema;

import java.util.List;

record SchemaPatchImpl(Type type, String schema, String table, List<String> columns, String reference) implements SchemaPatch {}
