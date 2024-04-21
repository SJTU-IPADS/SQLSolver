package sqlsolver.sql.plan;

import java.util.List;

public interface ExpressionsReg {
  void bindValueRefs(Expression expr, List<Value> valueRefs);

  Values valueRefsOf(Expression expr);
}
