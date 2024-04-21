package sqlsolver.sql.ast;

public interface AdditionalInfo<T extends AdditionalInfo<T>> {
  interface Key<T extends AdditionalInfo<T>> {
    T init(SqlContext sql);
  }

  void relocateNode(int oldId, int newId);

  void deleteNode(int nodeId);
}
