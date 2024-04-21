package sqlsolver.sql.ast;

import sqlsolver.sql.ast.constants.Category;

import java.util.List;

public interface SqlDataType {
  Category category();

  String name();

  int width();

  int precision();

  boolean unsigned();

  List<String> valuesList();

  boolean isArray();

  int storageSize();

  int[] dimensions();

  void formatAsDataType(StringBuilder builder, String dbType);

  void formatAsCastType(StringBuilder builder, String dbType);

  SqlDataType setUnsigned(boolean unsigned);

  SqlDataType setIntervalField(String intervalField);

  SqlDataType setValuesList(List<String> valuesList);

  SqlDataType setDimensions(int[] dimensions);

  static SqlDataType mk(Category category, String name, int width, int precision) {
    return new SqlDataTypeImpl(category, name, width, precision);
  }
}
