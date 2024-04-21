package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.AdditionalInfo;
import sqlsolver.sql.ast.SqlNode;

public interface Relations extends AdditionalInfo<Relations> {
  AdditionalInfo.Key<Relations> RELATION = RelationsImpl::new;

  Relation enclosingRelationOf(SqlNode node);
}
