package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;

import java.util.ArrayList;
import java.util.List;

import static sqlsolver.sql.ast.SqlNodeFields.TableSource_Kind;

public enum TableSourceKind implements FieldDomain {
  SimpleSource,
  JoinedSource,
  DerivedSource;

  private final List<FieldKey<?>> fields = new ArrayList<>(5);

  @Override
  public List<FieldKey<?>> fields() {
    return fields;
  }

  @Override
  public boolean isInstance(SqlNode node) {
    return node != null && node.$(TableSource_Kind) == this;
  }

  @Override
  public <T, R extends T> FieldKey<R> field(String name, Class<T> clazz) {
    final FieldKey<R> field = new TableSourceField<>(name, clazz, this);
    fields.add(field);
    return field;
  }
}
