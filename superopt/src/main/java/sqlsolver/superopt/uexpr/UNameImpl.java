package sqlsolver.superopt.uexpr;

record UNameImpl(String str) implements UName {
  @Override
  public UName copy() {
    return new UNameImpl(str);
  }

  @Override
  public String toString() {
    return str;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof UName)) return false;
    final UName that = (UName) obj;
    return this.toString().equals(that.toString());
  }

  @Override
  public int hashCode() {
    return str.hashCode();
  }
}
