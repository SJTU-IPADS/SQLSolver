package sqlsolver.superopt.optimizer;

import sqlsolver.common.utils.BaseCongruentClass;

import java.util.Collection;
import java.util.Set;

class OptGroup extends BaseCongruentClass<SubPlan> {
  protected OptGroup(Memo congruence) {
    super(congruence);
  }

  @Override
  protected void merge(BaseCongruentClass<SubPlan> other) {
    super.merge(other);

    ((MinCostSet) elements).evicted().addAll(((OptGroup) other).evicted());

    final Memo memo = (Memo) this.congruence;
    for (String key : ((OptGroup) other).evicted()) {
      ((OptGroup) memo.eqClassAt(key)).elements = elements;
    }
  }

  private Set<String> evicted() {
    return ((MinCostSet) elements).evicted();
  }

  @Override
  protected Collection<SubPlan> mkCollection() {
    return new MinCostSet();
  }
}
