package sqlsolver.sql.calcite;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelReferentialConstraint;
import org.apache.calcite.rel.RelReferentialConstraintImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.mapping.IntPair;
import sqlsolver.sql.ast.constants.Category;
import sqlsolver.sql.schema.Column;
import sqlsolver.sql.schema.Constraint;

import java.util.*;

import static sqlsolver.sql.ast.constants.ConstraintKind.*;

public class CalciteTable implements Table {

  private final Map<String, SqlTypeWrapper> columns = new LinkedHashMap<>();

  public CalciteTable(sqlsolver.sql.schema.Table table) {
    final List<Column> allColumns = table.columns().stream().toList();
    for (Column column : allColumns) {
      final boolean nullable = !column.isFlag(Column.Flag.NOT_NULL);
      addColumn(column.name(), column.dataType().category(), nullable);
    }
  }

  public void addColumn(String col, Category category, boolean nullable) {
    SqlTypeName typeName = null;
    switch (category) {
      case INTEGRAL -> {
        typeName = SqlTypeName.INTEGER;
      }
      case FRACTION -> {
        typeName = SqlTypeName.DECIMAL;
      }
      case BOOLEAN -> {
        typeName = SqlTypeName.BOOLEAN;
      }
      case STRING -> {
        typeName = SqlTypeName.VARCHAR;
      }
      case BIT_STRING, BLOB -> {
        typeName = SqlTypeName.BINARY;
      }
      case TIME -> {
        typeName = SqlTypeName.DATE;
      }
      default -> {
        typeName = SqlTypeName.ANY;
      }
    }
    columns.put(col, new SqlTypeWrapper(typeName, nullable));
  }

  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder b = typeFactory.builder();

    for (Map.Entry<String, SqlTypeWrapper> column : columns.entrySet()) {
      final SqlTypeWrapper wrappedType = column.getValue();
      final RelDataType typeWithNullability =
              typeFactory.createTypeWithNullability(
                      typeFactory.createSqlType(wrappedType.sqlTypeName),
                      wrappedType.nullable);
      b.add(column.getKey(), typeWithNullability);
    }
    return b.build();
  }

  @Override
  public boolean isRolledUp(String s) {
    return false;
  }

  @Override
  public boolean rolledUpColumnValidInsideAgg(String s, SqlCall sqlCall, SqlNode sqlNode, CalciteConnectionConfig calciteConnectionConfig) {
    return false;
  }

  public Statistic getStatistic() {
    return Statistics.of(null);
  }

  public Schema.TableType getJdbcTableType() {
    return Schema.TableType.STREAM;
  }

  public Table stream() {
    return null;
  }

  private record SqlTypeWrapper(SqlTypeName sqlTypeName, boolean nullable) {
  }
}
