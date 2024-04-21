module sqlsolver.common {
  exports sqlsolver.common.utils;
  exports sqlsolver.common.tree;
  exports sqlsolver.common.field;
  exports sqlsolver.common.datasource;
  exports sqlsolver.common.io;

  requires com.google.common;
  requires org.apache.commons.lang3;
  requires annotations;
  requires trove4j;
  requires com.zaxxer.hikari;
  requires java.sql;
}
