package sqlsolver.sql.support.resolution;

import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import sqlsolver.sql.schema.Column;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class JoinGraphImpl implements JoinGraph {
  private final MutableValueGraph<Relation, JoinKeyImpl> graph;
  private List<Set<Relation>> scc;

  JoinGraphImpl() {
    graph = ValueGraphBuilder.undirected().allowsSelfLoops(false).build();
  }

  @Override
  public Set<Relation> tables() {
    return graph.nodes();
  }

  @Override
  public Set<Relation> getJoined(Relation t) {
    return graph.adjacentNodes(t);
  }

  @Override
  public JoinKey getJoinKey(Relation t0, Relation t1) {
    final JoinKeyImpl joinKey = graph.edgeValue(t0, t1).orElse(null);
    if (joinKey != null && joinKey.rhsTable().equals(t0)) return joinKey.reversed();
    else return joinKey;
  }

  @Override
  public List<Set<Relation>> getSCC() {
    if (this.scc != null) return this.scc;

    final Set<Relation> checked = new HashSet<>(graph.nodes().size());
    final List<Set<Relation>> scc = new ArrayList<>(2);
    for (Relation node : graph.nodes()) {
      if (checked.contains(node)) continue;
      final Set<Relation> reachable = Graphs.reachableNodes(graph.asGraph(), node);
      checked.addAll(reachable);
      scc.add(reachable);
    }

    return this.scc = scc;
  }

  @Override
  public void addTable(Relation tbl) {
    requireNonNull(tbl);
    graph.addNode(tbl);
    scc = null;
  }

  @Override
  public void addJoin(Relation leftTbl, Column leftCol, Relation rightTbl, Column rightCol) {
    requireNonNull(leftTbl);
    requireNonNull(leftCol);
    requireNonNull(rightTbl);
    requireNonNull(rightCol);

    graph.putEdgeValue(leftTbl, rightTbl, new JoinKeyImpl(leftTbl, rightTbl, leftCol, rightCol));
    scc = null;
  }

  @Override
  public void relocateNode(int oldId, int newId) {}

  @Override
  public void deleteNode(int nodeId) {}

  private static class JoinKeyImpl implements JoinKey {
    private final Relation leftTbl, rightTbl;
    private final Column leftCol, rightCol;
    private JoinKey reversed;

    private JoinKeyImpl(Relation leftTbl, Relation rightTbl, Column leftCol, Column rightCol) {
      this.leftTbl = leftTbl;
      this.rightTbl = rightTbl;
      this.leftCol = leftCol;
      this.rightCol = rightCol;
    }

    @Override
    public Column lhsKey() {
      return leftCol;
    }

    @Override
    public Column rhsKey() {
      return rightCol;
    }

    @Override
    public Relation lhsTable() {
      return leftTbl;
    }

    @Override
    public Relation rhsTable() {
      return rightTbl;
    }

    private JoinKey reversed() {
      if (reversed == null) reversed = new JoinKeyImpl(rightTbl, leftTbl, rightCol, leftCol);
      return reversed;
    }

    @Override
    public String toString() {
      return "%s.%s=%s.%s"
          .formatted(
              leftTbl.qualification(), leftCol.name(), rightTbl.qualification(), rightCol.name());
    }
  }
}
