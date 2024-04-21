package sqlsolver.sql.plan;

import java.util.List;

public interface Values extends List<Value> {
  static Values mk() {
    return new ValuesImpl();
  }

  static Values mk(List<Value> values) {
    return new ValuesImpl(values);
  }
}
