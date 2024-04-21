package sqlsolver.sql.ast;

import gnu.trove.list.TIntList;
import sqlsolver.common.tree.LabeledTreeContextBase;
import sqlsolver.common.utils.Lazy;
import sqlsolver.sql.schema.Schema;

import java.util.IdentityHashMap;
import java.util.Map;

import static sqlsolver.common.tree.TreeSupport.checkIsValidChild;
import static sqlsolver.common.tree.TreeSupport.checkNodePresent;

public class SqlContextImpl extends LabeledTreeContextBase<SqlKind> implements SqlContext {
  private Schema schema;
  private String dbType;
  private final Lazy<Map<AdditionalInfo.Key<?>, AdditionalInfo<?>>> additionalInfo;

  protected SqlContextImpl(int expectedNumNodes, Schema schema) {
    super(expectedNumNodes);
    this.schema = schema;
    this.additionalInfo = Lazy.mk(IdentityHashMap::new);
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public String dbType() {
    return dbType;
  }

  @Override
  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  @Override
  public void setDbType(String dbType) {
    this.dbType = dbType;
  }

  @Override
  public void displaceNode(int oldNodeId, int newNodeId) {
    checkNodePresent(this, oldNodeId);
    checkNodePresent(this, newNodeId);
    final int oldParent = parentOf(oldNodeId);
    checkIsValidChild(this, oldParent, newNodeId);

    for (Object child : fieldsOf(oldNodeId).values()) unsetParent(child);
    setParentOf(newNodeId, oldParent);

    nodes[oldNodeId] = nodes[newNodeId];
    nodes[newNodeId] = null;
    for (int i = 1, bound = maxNodeId; i <= bound; ++i) {
      final Nd<SqlKind> n = nodes[i];
      if (n != null && n.parentId() == newNodeId) n.setParent(oldNodeId);
    }

    notifyRelocate(newNodeId, oldNodeId);
  }

  @Override
  public <T extends AdditionalInfo<T>> T getAdditionalInfo(AdditionalInfo.Key<T> key) {
    return (T) additionalInfo.get().computeIfAbsent(key, ignored -> key.init(this));
  }

  @Override
  public void removeAdditionalInfo(AdditionalInfo.Key<?> key) {
    if (additionalInfo.isInitialized()) additionalInfo.get().remove(key);
  }

  @Override
  public void clearAdditionalInfo() {
    if (additionalInfo.isInitialized()) additionalInfo.get().clear();
  }

  @Override
  protected void relocate(int from, int to) {
    super.relocate(from, to);
    notifyRelocate(from, to);
  }

  @Override
  public void deleteNode(int nodeId) {
    super.deleteNode(nodeId);

    if (additionalInfo.isInitialized())
      for (AdditionalInfo<?> info : additionalInfo.get().values()) {
        info.deleteNode(nodeId);
      }
  }

  private void unsetParent(Object obj) {
    if (obj instanceof SqlNode) nodes[((SqlNode) obj).nodeId()].setParent(NO_SUCH_NODE);
    if (obj instanceof SqlNodes) {
      final TIntList ids = ((SqlNodes) obj).nodeIds();
      for (int i = 0, bound = ids.size(); i < bound; ++i) nodes[ids.get(i)].setParent(NO_SUCH_NODE);
    }
  }

  private void notifyRelocate(int from, int to) {
    if (additionalInfo.isInitialized())
      for (AdditionalInfo<?> info : additionalInfo.get().values()) {
        info.relocateNode(from, to);
      }
  }
}
