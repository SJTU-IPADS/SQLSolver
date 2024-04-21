package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;

import java.util.ArrayList;
import java.util.List;

public enum SqlKind implements FieldDomain {
  Void,
  Invalid,
  Name2,
  Name3,
  TableName,
  ColName,
  CreateTable,
  AlterSeq,
  AlterTable,
  AlterTableAction,
  ColDef,
  Reference,
  IndexDef,
  KeyPart,
  SetOp,
  Query,
  QuerySpec,
  SelectItem,
  Expr,
  OrderItem,
  GroupItem,
  WindowSpec,
  WindowFrame,
  FrameBound,
  TableSource,
  IndexHint,
  Statement;

  private final List<FieldKey<?>> fields = new ArrayList<>(5);

  @Override
  public boolean isInstance(SqlNode node) {
    return node != null && node.kind() == this;
  }

  @Override
  public List<FieldKey<?>> fields() {
    return fields;
  }

  @Override
  public <T, R extends T> FieldKey<R> field(String name, Class<T> clazz) {
    final FieldKey<R> field = new SqlNodeField<>(name, clazz, this);
    fields.add(field);
    return field;
  }
}
