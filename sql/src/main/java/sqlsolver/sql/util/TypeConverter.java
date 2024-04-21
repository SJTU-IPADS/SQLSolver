package sqlsolver.sql.util;

public class TypeConverter {

  public static boolean isConvertibleStringToInt(String str) {
    try {
      int num = Integer.parseInt(str);
      return true;
    } catch(NumberFormatException e) {
      return false;
    }
  }
}
