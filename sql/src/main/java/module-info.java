module sqlsolver.sql {
  exports sqlsolver.sql;
  exports sqlsolver.sql.util;
  exports sqlsolver.sql.support.action;
  exports sqlsolver.sql.support.locator;
  exports sqlsolver.sql.support.resolution;
  exports sqlsolver.sql.schema;
  exports sqlsolver.sql.plan;
  exports sqlsolver.sql.ast;
  exports sqlsolver.sql.ast.constants;
  exports sqlsolver.sql.copreprocess;
  exports sqlsolver.sql.plan.normalize;
  exports sqlsolver.sql.preprocess.rewrite;
  exports sqlsolver.sql.preprocess.handler;
  exports sqlsolver.sql.calcite;

  requires sqlsolver.common;
  requires org.antlr.antlr4.runtime;
  requires org.apache.commons.lang3;
  requires com.google.common;
  requires trove4j;
  requires calcite.core;
  requires avatica.core;
  requires calcite.babel;
}
