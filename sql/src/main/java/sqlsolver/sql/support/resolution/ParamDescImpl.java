package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.SqlNode;

import java.util.List;

class ParamDescImpl implements ParamDesc {
  private int index;

  private final transient SqlNode node;
  private final transient List<ParamModifier> modifiers;
  private final String exprString;

  ParamDescImpl(SqlNode expr, SqlNode node, List<ParamModifier> modifiers) {
    this.node = node;
    this.modifiers = modifiers;
    this.exprString = expr == null ? "param" : expr.toString();
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public SqlNode node() {
    return node;
  }

  @Override
  public List<ParamModifier> modifiers() {
    return modifiers;
  }

  @Override
  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public String toString() {
    return "{%d,%s}".formatted(index, exprString);
  }
}
