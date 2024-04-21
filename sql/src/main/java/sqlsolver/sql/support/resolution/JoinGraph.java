package sqlsolver.sql.support.resolution;

import sqlsolver.sql.schema.Column;
import sqlsolver.sql.ast.AdditionalInfo;

import java.util.List;
import java.util.Set;

public interface JoinGraph extends AdditionalInfo<JoinGraph> {
  AdditionalInfo.Key<JoinGraph> JOIN_GRAPH = JoinGraphBuilder::build;

  Set<Relation> tables();

  Set<Relation> getJoined(Relation t);

  JoinKey getJoinKey(Relation t0, Relation t1);

  List<Set<Relation>> getSCC();

  void addTable(Relation tbl);

  void addJoin(Relation leftTbl, Column leftCol, Relation rightTbl, Column rightCol);

  interface JoinKey {
    Column lhsKey();

    Column rhsKey();

    Relation lhsTable();

    Relation rhsTable();
  }
}
