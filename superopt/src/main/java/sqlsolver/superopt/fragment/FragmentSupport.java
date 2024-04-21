package sqlsolver.superopt.fragment;

import sqlsolver.superopt.fragment.pruning.*;
import sqlsolver.superopt.fragment.pruning.*;
import sqlsolver.superopt.util.Hole;

import java.util.List;
import java.util.Set;

import static sqlsolver.common.utils.IterableSupport.linearFind;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.superopt.fragment.Op.mk;
import static sqlsolver.superopt.fragment.OpKind.*;

public class FragmentSupport {
  private static final int DEFAULT_MAX_OPS = 4;
  private static final List<Op> DEFAULT_OP_SET;
  private static final Set<Rule> BASIC_PRUNING_RULES;
//  private static final Set<Rule> EXTENDED_PRUNING_RULES;

  static {
    DEFAULT_OP_SET =
        map(List.of(INNER_JOIN, LEFT_JOIN, SIMPLE_FILTER, PROJ, PROJ, IN_SUB_FILTER), Op::mk);
//        map(List.of(AGG, AGG, INNER_JOIN, LEFT_JOIN, SIMPLE_FILTER, PROJ, PROJ, IN_SUB_FILTER, UNION, UNION), Op::mk);
//        map(List.of(AGG, AGG, AGG, AGG, AGG, AGG, INNER_JOIN, LEFT_JOIN, SIMPLE_FILTER, PROJ, PROJ, IN_SUB_FILTER, UNION, UNION), Op::mk);
    BASIC_PRUNING_RULES =
        Set.of(
            new MalformedJoin(),
            new MalformedSubquery(),
//            new MalformedUnion(),
//            new MalformedAgg(),
            new NonLeftDeepJoin(),
            new TooManyJoin(),
            new TooManySubqueryFilter(),
            new TooManySimpleFilter(),
            new TooManyProj(),
//            new TooManyUnion(),
//            new TooManyAgg(),
//            new TooDeepUnion(),
//            new TooDeepAgg(),
            // new NotOnRootAgg(),
            new ReorderedFilter(),
            new MeaninglessDedup(),
            new MeaninglessUnionDedup(),
            new DiffUnionInputs());
//    EXTENDED_PRUNING_RULES =
//        Set.of(
//            new MalformedJoin(),
//            new MalformedSubquery(),
//            new NonLeftDeepJoin(),
//            // There are some hard-coded optimization in Rewriter (TopDownOptimizer). Some rules
//            // overlapped with those stuff. We preclude them here.
//            new MeaninglessDedup(), // dedup in IN-subquery context
//            // (since IN-sub enforce a set-semantic context)
//            new TooManyJoin(), // more than 2 join (since we don't change join order)
//            new ReorderedFilter(), // InSub before a Filter
//            new TooManySimpleFilter(), // More than 2 simple filters
//            new TooManySubqueryFilter(), // More than 2 subquery filters
//            // (since filters in a chain can be freely reordered and combined)
//            new TooManyProj() // More than 2 Proj (since they can be collapsed)
//            );

    ((Proj) linearFind(DEFAULT_OP_SET, it -> it.kind() == PROJ)).setDeduplicated(true);
//    ((Union) linearFind(DEFAULT_OP_SET, it -> it.kind() == UNION)).setDeduplicated(true);
//    ((Agg)DEFAULT_OP_SET.get(0)).setAggFuncKind(AggFuncKind.MAX);
//    ((Agg)DEFAULT_OP_SET.get(1)).setAggFuncKind(AggFuncKind.COUNT);
//    ((Agg)DEFAULT_OP_SET.get(1)).setDeduplicated(true);
//    ((Agg)DEFAULT_OP_SET.get(2)).setAggFuncKind(AggFuncKind.SUM);
//    ((Agg)DEFAULT_OP_SET.get(3)).setAggFuncKind(AggFuncKind.MAX);
//    ((Agg)DEFAULT_OP_SET.get(4)).setAggFuncKind(AggFuncKind.MIN);
//    ((Agg)DEFAULT_OP_SET.get(5)).setAggFuncKind(AggFuncKind.AVERAGE);
  }

  public static int countOps(Op op) {
    if (op.kind() == INPUT) return 0;

    int count = 0;
    for (Op predecessor : op.predecessors()) {
      count += countOps(predecessor);
    }
    return count + 1;
  }

  public static int countInput(Op op) {
    if (op.kind() == INPUT) return 1;

    int count = 0;
    for (Op predecessor : op.predecessors()) {
      count += countOps(predecessor);
    }
    return count;
  }

  /** Fill holes with Input operator and call setFragment on each operator. */
  public static Fragment setupFragment(Fragment fragment) {
    for (Hole<Op> hole : FragmentUtils.gatherHoles(fragment)) hole.fill(mk(INPUT));
    fragment.acceptVisitor(OpVisitor.traverse(it -> it.setFragment(fragment)));
    return fragment;
  }
}
