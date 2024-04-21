package sqlsolver.sql.ast;

import sqlsolver.sql.ast.constants.Category;
import sqlsolver.sql.ast.constants.DataTypeName;

import java.util.Collections;
import java.util.List;

import static sqlsolver.common.datasource.DbSupport.MySQL;
import static sqlsolver.common.datasource.DbSupport.PostgreSQL;

class SqlDataTypeImpl implements SqlDataType {
  private final Category category;
  private final String name;
  private final int width;
  private final int precision;
  private boolean unsigned;
  private String intervalField;
  private List<String> valuesList;
  private int[] dimensions;

  private static final int[] EMPTY_INT_ARRAY = new int[0];

  SqlDataTypeImpl(Category category, String name, int width, int precision) {
    this.category = category;
    this.name = name;
    this.width = width;
    this.precision = precision;
    this.valuesList = Collections.emptyList();
    this.dimensions = EMPTY_INT_ARRAY;
  }

  @Override
  public Category category() {
    return category;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int width() {
    return width;
  }

  @Override
  public int precision() {
    return precision;
  }

  @Override
  public boolean unsigned() {
    return unsigned;
  }

  @Override
  public List<String> valuesList() {
    return valuesList;
  }

  @Override
  public boolean isArray() {
    return dimensions != null && dimensions.length > 0;
  }

  @Override
  public int storageSize() {
    switch (name) {
      case DataTypeName.TINYINT:
      case DataTypeName.BOOLEAN:
      case DataTypeName.ENUM:
      case DataTypeName.SET:
      case DataTypeName.YEAR:
        return 1;
      case DataTypeName.SMALLINT:
      case DataTypeName.SMALLSERIAL:
        return 2;
      case DataTypeName.MEDIUMINT:
      case DataTypeName.SERIAL:
      case DataTypeName.INTEGER:
      case DataTypeName.INT:
      case DataTypeName.REAL:
      case DataTypeName.FLOAT:
      case DataTypeName.DATE:
        return 4;
      case DataTypeName.DATETIME:
      case DataTypeName.DOUBLE:
      case DataTypeName.BIGSERIAL:
      case DataTypeName.BIGINT:
      case DataTypeName.TIMESTAMP:
      case DataTypeName.TIMESTAMPTZ:
      case DataTypeName.TIME:
      case DataTypeName.MACADDR:
      case DataTypeName.MONEY:
        return 8;
      case DataTypeName.TIMETZ:
        return 12;
      case DataTypeName.INTERVAL:
      case DataTypeName.UUID:
        return 16;
      case DataTypeName.BIT:
      case DataTypeName.BIT_VARYING:
        return (width - 1) / 8 + 1;

      case DataTypeName.DECIMAL:
      case DataTypeName.NUMERIC:
      case DataTypeName.FIXED:
        final int numIntegralDigit = width - precision;
        final int numFractionalDigit = precision;
        return 4
            * (numIntegralDigit / 9
                + (numIntegralDigit % 9 == 0 ? 0 : 1)
                + numFractionalDigit / 9
                + (numFractionalDigit % 9 == 0 ? 0 : 1));

        // string
      case DataTypeName.CHAR:
      case DataTypeName.VARCHAR:
      case DataTypeName.BINARY:
      case DataTypeName.VARBINARY:
        return width;

      case DataTypeName.TINYTEXT:
      case DataTypeName.TINYBLOB:
        return 255;
      case DataTypeName.TEXT:
      case DataTypeName.BLOB:
        return 65535;
      case DataTypeName.MEDIUMTEXT:
      case DataTypeName.MEDIUMBLOB:
        return 16777215;
      case DataTypeName.BIGTEXT:
      case DataTypeName.LONGBLOB:
        return Integer.MAX_VALUE;

      case DataTypeName.JSON:
      case DataTypeName.XML:
        return 1024;

      case DataTypeName.CIDR:
      case DataTypeName.INET:
        return 19;

      default:
        return 128;
    }
  }

  @Override
  public int[] dimensions() {
    return dimensions;
  }

  @Override
  public void formatAsDataType(StringBuilder builder, String dbType) {
    formatTypeBody(builder, dbType);

    if (valuesList != null && !valuesList.isEmpty())
      builder.append('(').append(String.join(", ", valuesList)).append(')');

    if (dimensions != null && dimensions.length != 0)
      for (int dimension : dimensions) {
        builder.append('[');
        if (dimension != 0) builder.append(dimension);
        builder.append(']');
      }

    if (unsigned) builder.append(" UNSIGNED");
  }

  @Override
  public void formatAsCastType(StringBuilder builder, String dbType) {
    if (MySQL.equals(dbType)) {
      if (category == Category.INTEGRAL)
        if (unsigned) builder.append("UNSIGNED ");
        else builder.append("SIGNED ");

      formatTypeBody(builder, dbType);
    } else { // postgres use same rule for dataType and castType
      formatAsDataType(builder, dbType);
    }
  }

  @Override
  public SqlDataType setUnsigned(boolean unsigned) {
    this.unsigned = unsigned;
    return this;
  }

  @Override
  public SqlDataType setIntervalField(String intervalField) {
    this.intervalField = intervalField;
    return this;
  }

  @Override
  public SqlDataType setValuesList(List<String> valuesList) {
    this.valuesList = valuesList;
    return this;
  }

  @Override
  public SqlDataType setDimensions(int[] dimensions) {
    this.dimensions = dimensions;
    return this;
  }

  private void formatTypeBody(StringBuilder builder, String dbType) {
    builder.append(name.toUpperCase());

    if (intervalField != null) builder.append(' ').append(intervalField);

    if (width != -1 && precision != -1)
      builder.append('(').append(width).append(", ").append(precision).append(')');
    else if (width != -1) builder.append('(').append(width).append(')');
    else if (precision != -1) builder.append('(').append(precision).append(')');
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    if (dimensions != null && dimensions.length > 0) formatAsDataType(builder, PostgreSQL);
    else formatAsDataType(builder, MySQL);
    return builder.toString();
  }
}
