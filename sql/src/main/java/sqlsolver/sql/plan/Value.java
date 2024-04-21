package sqlsolver.sql.plan;

public interface Value extends Qualified {
  String TYPE_NAT = "NATURAL"; // some values are always non-negative (e.g. multiplicity of a tuple)
  String TYPE_INT = "INTEGER";
  String TYPE_BIGINT = "BIGINT";
  String TYPE_BOOL = "BOOLEAN";
  String TYPE_DECIMAL = "DECIMAL"; // numbers with point (XX.YY)
  String TYPE_DOUBLE = "DOUBLE";
  String TYPE_CHAR = "CHAR";
  String TYPE_VARCHAR = "VARCHAR";
  String TYPE_NULL = "NULL";

  int id();

  String name();

  String type();

  boolean isNotNull();

  void setType(String type);

  void setNotNull(boolean isNotNull);

  Value copy();

  static boolean isIntegralType(String type) {
    return TYPE_BOOL.equals(type)
            || TYPE_NAT.equals(type) || TYPE_INT.equals(type)
            || TYPE_BIGINT.equals(type);
  }

  static boolean isRealType(String type) {
    return TYPE_DECIMAL.equals(type) || TYPE_DOUBLE.equals(type);
  }

  static boolean isNumberType(String type) {
    return isIntegralType(type) || isRealType(type);
  }

  static int getNumberPrecisionLevel(String type) {
    switch (type) {
      case TYPE_BOOL -> {
        return 0;
      }
      case TYPE_NAT, TYPE_INT -> {
        return 1;
      }
      case TYPE_BIGINT -> {
        return 2;
      }
      case TYPE_DECIMAL, TYPE_DOUBLE -> {
        return 3;
      }
    }
    assert false;
    return -1;
  }

  static boolean isStringType(String type) {
    return TYPE_CHAR.equals(type) || TYPE_VARCHAR.equals(type);
  }

  static Value mk(String qualification, String name) {
    return new ValueImpl(-1, qualification, name);
  }

  static Value mk(int id, String qualification, String name) {
    return new ValueImpl(id, qualification, name);
  }

  static Value mk(int id, String qualification, String name, String type) {
    return new ValueImpl(id, qualification, name, type);
  }

  static Value mk(int id, String qualification, String name, String type, boolean isNotNull) {
    return new ValueImpl(id, qualification, name, type, isNotNull);
  }
}
