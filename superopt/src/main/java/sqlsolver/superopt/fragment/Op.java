package sqlsolver.superopt.fragment;

import sqlsolver.common.utils.TreeNode;

public interface Op extends TreeNode<Symbols, Op>, Comparable<Op> {
  static Op mk(OpKind type) {
    return switch (type) {
      case INPUT -> new InputOp();
      case INNER_JOIN -> new InnerJoinOp();
      case CROSS_JOIN -> new CrossJoinOp();
      case LEFT_JOIN -> new LeftJoinOp();
      case RIGHT_JOIN -> new RightJoinOp();
      case FULL_JOIN -> new FullJoinOp();
      case SIMPLE_FILTER -> new SimpleFilterOp();
      case IN_SUB_FILTER -> new InSubFilterOp();
      case EXISTS_FILTER -> new ExistsFilterOp();
      case PROJ -> new ProjOp();
      case AGG -> new AggOp();
      case SORT -> new SortOp();
      case LIMIT -> new LimitOp();
      case UNION -> new UnionOp();
      case INTERSECT -> new IntersectOp();
      case EXCEPT -> new ExceptOp();
    };
  }

  static Op parse(String typeName){
    final OpKind opKind = OpKind.parse(typeName);
    final Op op = Op.mk(opKind);
    if (opKind == OpKind.PROJ && typeName.endsWith("*")) ((Proj)op).setDeduplicated(true);
    if (opKind.isSetOp() && typeName.endsWith("*")) ((SetOp)op).setDeduplicated(true);
    if (opKind == OpKind.AGG && typeName.endsWith("*")) ((Agg) op).setDeduplicated(true);
    if (opKind == OpKind.AGG) {
      switch (typeName) {
        case "Agg_sum" -> {
          ((Agg) op).setAggFuncKind(AggFuncKind.SUM);
        }
        case "Agg_average" -> {
          ((Agg) op).setAggFuncKind(AggFuncKind.AVERAGE);
        }
        case "Agg_count", "Agg_count*" -> {
          ((Agg) op).setAggFuncKind(AggFuncKind.COUNT);
        }
        case "Agg_max" -> {
          ((Agg) op).setAggFuncKind(AggFuncKind.MAX);
        }
        case "Agg_min" -> {
          ((Agg) op).setAggFuncKind(AggFuncKind.MIN);
        }
        default -> {
          if (!typeName.equals("Agg"))
            throw new IllegalArgumentException("unknown operator: " + typeName);
        }
      }
    }
    return op;
  }

  OpKind kind();

  Fragment fragment();

  void setFragment(Fragment fragment);

  void acceptVisitor(OpVisitor visitor);

  int shadowHash();

  Op copyTree();

  @Override
  default Op copy(Symbols context) {
    throw new UnsupportedOperationException();
  }

  @Override
  default int compareTo(Op o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final Op[] preds = predecessors(), otherPreds = o.predecessors();
    assert preds.length == otherPreds.length;

    for (int i = 0, bound = preds.length; i < bound; i++) {
      final Op pred = preds[i], otherPred = otherPreds[i];
      if (pred == null && otherPred == null) continue;
      if (pred == null /* && otherPred != null */) return -1;
      if (/* pred != null && */ otherPred == null) return 1;

      res = pred.compareTo(otherPred);
      if (res != 0) return res;
    }

    return 0;
  }
}
