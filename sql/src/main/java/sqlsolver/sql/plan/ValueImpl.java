package sqlsolver.sql.plan;

final public class ValueImpl implements Value {
  private final int id;
  private String qualification;
  private final String name;
  private String type;
  private boolean isNotNull;

  public ValueImpl(int id, String qualification, String name) {
    this.id = id;
    this.qualification = qualification;
    this.name = name;
    this.type = TYPE_INT;
    this.isNotNull = false;
  }

  public ValueImpl(int id, String qualification, String name, String type) {
    this.id = id;
    this.qualification = qualification;
    this.name = name;
    this.type = type;
    this.isNotNull = false;
  }

  public ValueImpl(int id, String qualification, String name, String type, boolean isNotNull) {
    this.id = id;
    this.qualification = qualification;
    this.name = name;
    this.type = type;
    this.isNotNull = isNotNull;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public boolean isNotNull() {
    return isNotNull;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public void setNotNull(boolean isNotNull) {
    this.isNotNull = isNotNull;
  }

  @Override
  public Value copy() {
    return new ValueImpl(id, qualification, name, type, isNotNull);
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof Value)) return false;
    return this.id() == ((Value) obj).id();
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(id);
  }

  @Override
  public String toString() {
    return qualification + "." + name;
  }
}
