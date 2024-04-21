module sqlsolver.superopt {
  exports sqlsolver.superopt;
  exports sqlsolver.superopt.constraint;
  exports sqlsolver.superopt.logic;
  exports sqlsolver.superopt.optimizer;
  exports sqlsolver.superopt.fragment;
  exports sqlsolver.superopt.substitution;
  exports sqlsolver.superopt.uexpr;
  exports sqlsolver.superopt.uexpr.normalizer;
  exports sqlsolver.superopt.liastar;
  exports sqlsolver.superopt.liastar.translator;
  exports sqlsolver.superopt.liastar.transformer;
  exports sqlsolver.superopt.liastar.destructor;
  exports sqlsolver.superopt.util;

  requires com.google.common;
  requires org.apache.commons.lang3;
  requires java.logging;
  requires sqlsolver.common;
  requires sqlsolver.sql;
  requires sqlsolver.stmt;
  requires java.sql;
  requires org.postgresql.jdbc;
  requires com.zaxxer.hikari;
  requires progressbar;
  requires trove4j;
  requires z3;
  requires com.microsoft.sqlserver.jdbc;
  requires calcite.core;
  requires mysql.connector.java;
  requires annotations;
}
