package sqlsolver.superopt.uexpr;

import java.util.Arrays;

import static sqlsolver.common.utils.ArraySupport.linearFind;

public class SchemaDesc {
  public final int[] components;

  SchemaDesc(int... components) {
    this.components = components;
  }

  SchemaDesc(SchemaDesc... components) {
    int numComponents = 0;
    for (SchemaDesc component : components) numComponents += component.components.length;

    this.components = new int[numComponents];
    int i = 0;
    for (SchemaDesc component : components) {
      final int length = component.components.length;
      System.arraycopy(component.components, 0, this.components, i, length);
      i += length;
    }
  }

  int indexOf(int component) {
    return linearFind(components, component, 0);
  }

  @Override
  public String toString() {
    return Arrays.toString(components);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SchemaDesc that = (SchemaDesc) o;
    return Arrays.equals(components, that.components);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(components);
  }
}
