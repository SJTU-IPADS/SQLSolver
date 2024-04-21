package sqlsolver.sql.plan;

import java.util.List;

public interface Exporter extends Qualified {
  boolean deduplicated();

  List<String> attrNames();

  List<Expression> attrExprs();

  void setAttrExprs(int i, Expression expression);
}
