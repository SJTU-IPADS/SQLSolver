package sqlsolver.common.utils;

public interface TreeContext<C extends TreeContext<C>> {
  C dup();
}
