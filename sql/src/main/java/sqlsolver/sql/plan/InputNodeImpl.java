package sqlsolver.sql.plan;

import sqlsolver.sql.schema.Table;

class InputNodeImpl implements InputNode {
  private final Table table;
  private String qualification;

  InputNodeImpl(Table table, String qualification) {
    this.table = table;
    this.qualification = qualification;
  }

  @Override
  public Table table() {
    return table;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

}
