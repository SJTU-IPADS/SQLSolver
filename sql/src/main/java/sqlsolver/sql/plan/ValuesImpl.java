package sqlsolver.sql.plan;

import sqlsolver.common.utils.DelegateList;

import java.util.ArrayList;
import java.util.List;

class ValuesImpl extends DelegateList<Value> implements Values {
  private final List<Value> values;

  ValuesImpl() {
    this(new ArrayList<>());
  }

  ValuesImpl(List<Value> values) {
    this.values = values;
  }

  @Override
  protected List<Value> delegation() {
    return values;
  }
}
