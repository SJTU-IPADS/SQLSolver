package sqlsolver.superopt.uexpr;

public interface UTable extends UAtom {
  UName tableName();

  @Override
  default UKind kind() {
    return UKind.TABLE;
  }

  static UTable mk(UName tableName, UVar var) {
    // assert var.is(UVar.VarKind.BASE);
    return new UTableImpl(tableName, var);
  }
}
