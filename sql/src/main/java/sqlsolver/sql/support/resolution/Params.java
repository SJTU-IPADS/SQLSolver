package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.AdditionalInfo;
import sqlsolver.sql.ast.SqlNode;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Params extends AdditionalInfo<Params> {
  AdditionalInfo.Key<Params> PARAMS = ParamsImpl::new;

  int numParams();

  ParamDesc paramOf(SqlNode node);

  void forEach(Consumer<ParamDesc> consumer);

  boolean forEach(Predicate<ParamDesc> consumer);

  JoinGraph joinGraph();
}
