package sqlsolver.sql.plan;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;
import sqlsolver.common.tree.UniformTreeContextBase;
import sqlsolver.common.utils.COW;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.ast.SqlNode;

import java.util.HashMap;
import java.util.Map;

import static sqlsolver.common.tree.TreeSupport.checkNodePresent;

class PlanContextImpl extends UniformTreeContextBase<PlanKind> implements PlanContext {
  private int root;
  private final Schema schema;
  private final COW<TObjectIntMap<PlanNode>> nodeReg;
  private final ValuesRegistryImpl valuesReg;
  private final InfoCacheImpl infoCache;
  private final COW<TIntIntMap> subQueryPlanReg;
  private final COW<Map<SqlNode, Integer>> subQueryPlanRegSqlNode;

  protected PlanContextImpl(int root, int expectedNumNodes, Schema schema) {
    super(new PlanNd[(expectedNumNodes <= 0 ? 16 : expectedNumNodes) + 1], 2);
    this.schema = schema;
    this.nodeReg = new COW<>(mkIdentityMap(), null);
    this.valuesReg = new ValuesRegistryImpl(this);
    this.infoCache = new InfoCacheImpl();
    this.subQueryPlanReg = new COW<>(new TIntIntHashMap(2), null);
    this.subQueryPlanRegSqlNode = new COW<>(new HashMap<>(), null);
  }

  private PlanContextImpl(PlanContextImpl other) {
    super(copyNodesArray((PlanNd[]) other.nodes), 2);
    this.root = other.root;
    this.maxNodeId = other.maxNodeId;
    this.schema = other.schema;
    this.nodeReg = new COW<>(other.nodeReg.forRead(), PlanContextImpl::copyIdentityMap);
    this.valuesReg = new ValuesRegistryImpl(other.valuesReg, this);
    this.infoCache = new InfoCacheImpl(other.infoCache);
    this.subQueryPlanReg = new COW<>(other.subQueryPlanReg.forRead(), TIntIntHashMap::new);
    this.subQueryPlanRegSqlNode = new COW<>(other.subQueryPlanRegSqlNode.forRead(), HashMap::new);
  }

  @Override
  public int root() {
    return root;
  }

  @Override
  public PlanContext setRoot(int root) {
    this.root = root;
    return this;
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public PlanNode nodeAt(int id) {
    checkNodePresent(this, id);
    return ((PlanNd) nodes[id]).planNode;
  }

  @Override
  public int nodeIdOf(PlanNode node) {
    return nodeReg.forRead().get(node);
  }

  @Override
  public int bindNode(PlanNode node) {
    final int newNodeId = mkNode(node.kind());
    ((PlanNd) nodes[newNodeId]).planNode = node;
    nodeReg.forWrite().put(node, newNodeId);
    return newNodeId;
  }

  @Override
  public void deleteNode(int nodeId) {
    nodeReg.forWrite().remove(nodeId);
    valuesReg.deleteNode(nodeId);
    infoCache.deleteNode(nodeId);
    super.deleteNode(nodeId);
  }

  public void myDeleteNode(int nodeId) {
    nodeReg.forWrite().remove(nodeId);
    valuesReg.deleteNode(nodeId);
    infoCache.deleteNode(nodeId);
    super.deleteNode(nodeId, parentOf(nodeId));
  }

  public int parent(int nodeId) {
    if (nodeId == root)
      return NO_SUCH_NODE;
    else
      return parentOf(nodeId);
  }

  @Override
  public void compact() {
    super.compact();
    infoCache.cleanTemporary();
  }

  @Override
  protected void relocate(int from, int to) {
    nodeReg.forWrite().put(nodeAt(from), to);
    valuesReg.deleteNode(to);
    valuesReg.relocateNode(from, to);
    infoCache.deleteNode(to);
    infoCache.renumberNode(from, to);
    if (root == from) root = to;
    super.relocate(from, to);
  }

  @Override
  public ValuesRegistry valuesReg() {
    return valuesReg;
  }

  @Override
  public InfoCache infoCache() {
    return infoCache;
  }

  @Override
  public void setSubQueryPlanRootId(int sqlNodeId, int rootId) {
    subQueryPlanReg.forWrite().put(sqlNodeId, rootId);
  }

  @Override
  public int getSubQueryPlanRootId(int sqlNodeId) {
    return subQueryPlanReg.forRead().get(sqlNodeId);
  }

  @Override
  public void setSubQueryPlanRootIdBySqlNode(SqlNode sqlNode, int rootId) {
    subQueryPlanRegSqlNode.forWrite().put(sqlNode, rootId);
  }

  @Override
  public int getSubQueryPlanRootIdBySqlNode(SqlNode sqlNode) {
    return subQueryPlanRegSqlNode.forRead().get(sqlNode);
  }

  @Override
  public PlanContext copy() {
    return new PlanContextImpl(this);
  }

  @Override
  protected Nd<PlanKind> mk(PlanKind planKind) {
    return new PlanNd(planKind);
  }

  @Override
  public String toString() {
    return PlanSupport.stringifyTree(this, root());
  }

  private static <K> TObjectIntMap<K> mkIdentityMap() {
    return new TObjectIntCustomHashMap<>(IdentityHashingStrategy.INSTANCE);
  }

  private static <K> TObjectIntMap<K> copyIdentityMap(TObjectIntMap<K> other) {
    return new TObjectIntCustomHashMap<>(IdentityHashingStrategy.INSTANCE, other);
  }

  private static PlanNd[] copyNodesArray(PlanNd[] nds) {
    final PlanNd[] copiedNodes = new PlanNd[nds.length];
    for (int i = 0; i < nds.length; i++) {
      if (nds[i] != null) copiedNodes[i] = new PlanNd(nds[i]);
    }
    return copiedNodes;
  }

  private static class PlanNd extends Nd<PlanKind> {
    private PlanNode planNode;

    protected PlanNd(PlanKind planKind) {
      super(planKind);
    }

    protected PlanNd(PlanNd other) {
      super(other);
      this.planNode = other.planNode;
    }
  }
}
