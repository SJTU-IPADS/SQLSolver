package sqlsolver.common.utils;

class IndexedNameSequence implements NameSequence {
  private final String prefix;
  private int nextId;

  IndexedNameSequence(String prefix, int nextId) {
    this.prefix = prefix;
    this.nextId = nextId;
  }

  @Override
  public String next() {
    return prefix + nextId++;
  }
}
