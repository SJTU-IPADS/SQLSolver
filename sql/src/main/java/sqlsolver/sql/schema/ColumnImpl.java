package sqlsolver.sql.schema;

import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlDataType;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodeFields;
import sqlsolver.sql.ast.constants.Category;
import sqlsolver.sql.ast.constants.ConstraintKind;

import java.util.*;

class ColumnImpl implements Column {
  private final String table;
  private final String name;
  private final String rawDataType;
  private final SqlDataType dataType;
  private final EnumSet<Flag> flags;
  private List<Constraint> constraints;
  private List<SchemaPatch> patches;

  ColumnImpl(String table, String name, String rawDataType, SqlDataType dataType) {
    this.table = table;
    this.name = name;
    this.rawDataType = rawDataType;
    this.dataType = dataType;
    this.flags = EnumSet.noneOf(Flag.class);

    if (dataType.category() == Category.BOOLEAN) flags.add(Flag.IS_BOOLEAN);
    else if (dataType.category() == Category.ENUM) flags.add(Flag.IS_ENUM);
  }

  static ColumnImpl build(String table, SqlNode colDef) {
    final String colName = SqlSupport.simpleName(colDef.$(SqlNodeFields.ColDef_Name).$(SqlNodeFields.ColName_Col));
    final String rawDataType = colDef.$(SqlNodeFields.ColDef_RawType);
    final SqlDataType dataType = colDef.$(SqlNodeFields.ColDef_DataType);

    final ColumnImpl column = new ColumnImpl(table, colName, rawDataType, dataType);

    if (colDef.isFlag(SqlNodeFields.ColDef_Generated)) column.flag(Flag.GENERATED);
    if (colDef.isFlag(SqlNodeFields.ColDef_Default)) column.flag(Flag.HAS_DEFAULT);
    if (colDef.isFlag(SqlNodeFields.ColDef_AutoInc)) column.flag(Flag.AUTO_INCREMENT);

    return column;
  }

  void flag(Flag... flags) {
    this.flags.addAll(Arrays.asList(flags));
  }

  @Override
  public String tableName() {
    return table;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String rawDataType() {
    return rawDataType;
  }

  @Override
  public SqlDataType dataType() {
    return dataType;
  }

  @Override
  public Collection<Constraint> constraints() {
    return constraints == null ? Collections.emptyList() : constraints;
  }

  @Override
  public Collection<SchemaPatch> patches() {
    return patches == null ? Collections.emptyList() : patches;
  }

  @Override
  public boolean isFlag(Flag flag) {
    return flags.contains(flag);
  }

  void addConstraint(Constraint constraint) {
    if (constraints == null) constraints = new ArrayList<>(3);
    constraints.add(constraint);
    if (constraint.kind() == null) flags.add(Flag.INDEXED);
    else
      switch (constraint.kind()) {
        case PRIMARY:
          flags.add(Flag.PRIMARY);
          flags.add(Flag.NOT_NULL);
        case UNIQUE:
          flags.add(Flag.UNIQUE);
          flags.add(Flag.INDEXED);
          break;
        case NOT_NULL:
          flags.add(Flag.NOT_NULL);
          break;
        case FOREIGN:
          flags.add(Flag.FOREIGN_KEY);
          flags.add(Flag.INDEXED);
          break;
        case CHECK:
          flags.add(Flag.HAS_CHECK);
          break;
      }
  }

  void addPatch(SchemaPatch patch) {
    if (patches == null) patches = new ArrayList<>(2);
    patches.add(patch);
    switch (patch.type()) {
      case NOT_NULL -> flags.add(Flag.NOT_NULL);
      case INDEX -> flags.add(Flag.INDEXED);
      case BOOLEAN -> flags.add(Flag.IS_BOOLEAN);
      case ENUM -> flags.add(Flag.IS_ENUM);
      case UNIQUE -> flags.add(Flag.UNIQUE);
      case FOREIGN_KEY -> flags.add(Flag.FOREIGN_KEY);
    }
  }

  @Override public String toString() {
    return table + "." + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnImpl column = (ColumnImpl) o;
    return Objects.equals(table, column.table) && Objects.equals(name, column.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, name);
  }

  @Override
  public StringBuilder toDdl(String dbType, StringBuilder buffer) {
     buffer.append(SqlSupport.quoted(dbType, name)).append(' ').append(dataType.toString());
     if (isFlag(Flag.PRIMARY)) buffer.append(' ').append("PRIMARY KEY");
     else if (isFlag(Flag.UNIQUE)) buffer.append(' ').append("UNIQUE");
     if (isFlag(Flag.NOT_NULL) && !isFlag(Flag.PRIMARY)) buffer.append(' ').append("NOT NULL");
     return buffer;
  }
}
