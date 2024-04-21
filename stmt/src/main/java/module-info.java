module sqlsolver.stmt {
  requires sqlsolver.common;
  requires sqlsolver.sql;
  requires sqlite.jdbc;
  requires java.sql;
  requires com.google.common;
  requires org.apache.commons.lang3;

  exports sqlsolver.stmt;
}
