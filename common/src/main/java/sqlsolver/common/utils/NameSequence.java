package sqlsolver.common.utils;

import java.util.Set;

public interface NameSequence {
  String next();

  default String nextUnused(Set<String> usedNames) {
    String name = next();
    while (usedNames.contains(name)) name = next();
    return name;
  }

  static NameSequence mkIndexed(String prefix, int baseIndex) {
    return new IndexedNameSequence(prefix, baseIndex);
  }
}
