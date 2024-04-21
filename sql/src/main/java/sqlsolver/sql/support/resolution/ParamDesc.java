package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.SqlNode;

import java.util.List;

public interface ParamDesc {
  SqlNode node();

  List<ParamModifier> modifiers();

  int index();

  void setIndex(int idx);
}
