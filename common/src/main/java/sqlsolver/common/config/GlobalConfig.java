package sqlsolver.common.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GlobalConfig {
  /** Z3 timeout used by SQLSolver, in millis. */
  public static final int SQLSOLVER_Z3_TIMEOUT;

  static  {
    Properties properties = new Properties();
    try (FileInputStream fileInputStream = new FileInputStream("sqlsolver.properties")) {
      properties.load(fileInputStream);
    } catch (IOException e) {
      System.err.println("Failed to load the configuration file. SQLSolver will use its defaults.");
    }
    SQLSOLVER_Z3_TIMEOUT = Integer.parseInt(properties.getProperty("sqlsolver.z3.timeout", "10000"));
  }
}
