package sqlsolver.superopt.optimizer;

public enum PreprocessRule {
  EnforceInnerJoin(1, "EnforceInnerJoin"),
  ReduceSort(2, "ReduceSort"),
  ReduceDedup(3, "ReduceDedup"),
  ConvertExists(4, "ConvertExists"),
  FlipRightJoin(5, "FlipRightJoin");

  private final int ruleId;
  private final String desc;

  PreprocessRule(int ruleId, String desc) {
    this.ruleId = ruleId;
    this.desc = desc;
  }

  public int ruleId() {
    return ruleId;
  }

  public String desc() {
    return desc;
  }

  static String getDescByRuleId(int ruleId) {
    for (PreprocessRule r : PreprocessRule.values()) {
      if (r.ruleId() == ruleId) return r.desc();
    }
    return "unknown";
  }
}
