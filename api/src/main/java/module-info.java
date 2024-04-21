module sqlsolver.api {
  exports sqlsolver.api;

  requires sqlsolver.sql;
  requires sqlsolver.common;
  requires sqlsolver.superopt;
  requires sqlsolver.stmt;
  requires calcite.core;
}