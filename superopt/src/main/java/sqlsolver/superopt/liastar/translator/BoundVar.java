package sqlsolver.superopt.liastar.translator;

import sqlsolver.superopt.uexpr.UVar;

import java.util.Objects;

public record BoundVar(UVar var, String table) {
  @Override
  public String toString() {
    if (table == null) {
      return var.toString();
    }
    return var.toString() + "[" + table + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BoundVar bv) {
      return var.equals(bv.var) && Objects.equals(table, bv.table);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
