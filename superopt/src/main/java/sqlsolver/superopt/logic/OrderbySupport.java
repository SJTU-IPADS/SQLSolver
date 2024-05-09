package sqlsolver.superopt.logic;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.*;
import org.apache.calcite.rel.core.*;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.Planner;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.Table;
import sqlsolver.superopt.uexpr.UConst;
import sqlsolver.superopt.uexpr.UExprConcreteTranslationResult;
import sqlsolver.superopt.uexpr.UExprSupport;
import sqlsolver.superopt.util.Timeout;

import java.math.BigDecimal;
import java.util.*;

import static sqlsolver.sql.calcite.CalciteSupport.*;
import static sqlsolver.superopt.logic.LogicSupport.proveEqByLIAStarConcrete;

/**
 * This class use a heuristic algorithm to handle OrderBy cases.
 */
public class OrderbySupport {

  private RelNode plan0;
  private RelNode plan1;

  private Schema schema;

  OrderbySupport(RelNode p0, RelNode p1, Schema schema) {
    this.plan0 = p0;
    this.plan1 = p1;
    this.schema = schema;
  }

  /**
   * The interface that called by others.
   */
  public static VerificationResult sortHandler(RelNode p0, RelNode p1, Schema schema) {
    OrderbySupport os = new OrderbySupport(p0, p1, schema);
    return os.trRules();
  }

  /**
   * The main entrance of the algorithm.
   */
  private VerificationResult trRules() {
    try {
      // determine whether both are ordered or both are not ordered

      // if both logical plan are LIMIT 0, then they are equal
      if (bothReturnZeroTuples(plan0, plan1)) {
        return VerificationResult.EQ;
      }

      // delete useless limit
      deleteUselessLimit(plan0, true);
      deleteUselessLimit(plan1, false);

      // delete empty sort nodes after delete useless limit
      deleteEmptySort(plan0, true);
      deleteEmptySort(plan1, false);

      // delete useless sort
      deleteUselessSort(plan0, true);
      deleteUselessSort(plan1, false);

      // delete empty sort nodes after delete useless sort
      deleteEmptySort(plan0, true);
      deleteEmptySort(plan1, false);

      // if one plan has sort while the other doesn't, it is an unknown case
      if (hasNodeOfKind(plan0, Sort.class) != hasNodeOfKind(plan1, Sort.class)) {
        return VerificationResult.UNKNOWN;
      }

      // promote sort node, the concrete usage can be seen in comment
      promoteLimitSort(plan0, true);
      promoteLimitSort(plan1, false);

      // merge sort limit node
      mergeLimit(plan0, true);
      mergeLimit(plan1, false);

      return patternMatching(plan0, plan1);
    } catch (Exception e) {
      Timeout.bypassTimeout(e);
      if (LogicSupport.dumpLiaFormulas)
        e.printStackTrace();
      return VerificationResult.UNKNOWN;
    }
  }

  /*
   * Rules that handle the sort limit node.
   */

  /**
   * Delete nearby useless limit (limit number and offset number).
   * This function traverse the plan from the top and only consider one child node,
   * and for the sort node, find the bigger limit, and delete the offset and limit of it.
   */
  private void deleteUselessLimit(RelNode node, boolean isSource) {
    if (node == null) {
      return;
    }

    final RelNode root = isSource ? plan0 : plan1;
    int numChildren = node.getInputs().size();

    // Only consider the node that only contains one child
    if (numChildren != 1) {
      return;
    }

    if (node != root) {
      if (node instanceof Sort) {
        RelNode deleteNode = getBiggerLimit(root, node);
        if (deleteNode != null) {
          assert deleteNode.getInputs().size() == 1;
          Sort deleteSort = (Sort) deleteNode;
          // Delete the fetch and offset in the deleteNode
          if (deleteNode == root) {
            if (isSource) {
              plan0 = LogicalSort.create(deleteSort.getInput(), deleteSort.getCollation(), null, null);
            } else {
              plan1 = LogicalSort.create(deleteSort.getInput(), deleteSort.getCollation(), null, null);
            }
          } else {
            // The traverse process promise that the father only have one child.
            replaceNode(root, deleteSort, LogicalSort.create(deleteSort.getInput(), deleteSort.getCollation(), null, null));
          }
        }
      }
    }

    for (int i = 0; i < numChildren; ++i) {
      deleteUselessLimit(node.getInput(i), isSource);
    }
  }

  /**
   * Remove useless sort.
   * For those sort nodes that followed by literal, like ORDER BY 'a', delete the literal.
   * e.g. SELECT x, y FROM T ORDER BY 'a' -> order($2)-proj(x, y, 'a')
   */
  private void deleteUselessSort(RelNode node, boolean isSource) {
    if (node == null) {
      return;
    }

    final RelNode root = isSource ? plan0 : plan1;
    int numChildren = node.getInputs().size();

    if (node instanceof Sort sort) {
      // This situation handles the case like ORDERBY 'a', where an orderby is followed by literal.
      final List<RexNode> sortExps = sort.getSortExps();
      final List<RelFieldCollation> sortCollations = sort.getCollation().getFieldCollations();
      final List<RelFieldCollation> newSortCollations = new ArrayList<>();
      assert sortCollations.size() == sortExps.size();
      final RelNode child = sort.getInput();
      // Only consider the case that the child is projection
      if (child instanceof Project proj) {
        for (int i = 0; i < sortExps.size(); i++) {
          final RexNode sortExp = sortExps.get(i);
          if (sortExp instanceof RexInputRef rexInputRef) {
            if (proj.getProjects().get(rexInputRef.getIndex()) instanceof RexLiteral) {
              continue;
            }
          }
          newSortCollations.add(sortCollations.get(i));
        }

        // If sortCollations is not equal to newSortCollations, then this node needs to be changed
        if (sortCollations.size() != newSortCollations.size()) {
          if (newSortCollations.size() == 0) {
            deleteSortLimit(root, node);
          } else {
            replaceNode(root, node, LogicalSort.create(sort.getInput(),
                    RelCollations.of(newSortCollations), sort.offset, sort.fetch));
          }
        }
      }
    }

    for (int i = 0; i < numChildren; ++i) {
      deleteUselessSort(node.getInput(i), isSource);
    }
  }

  /**
   * Promote Sort node.
   * <p> </p>
   * <code>
   * SELECT xxx FROM (SELECT xxx FROM (SELECT xxx FROM xxx ORDER BY exp LIMIT params)) ORDER BY exp
   * <p>-></p>
   * SELECT xxx FROM (SELECT xxx FROM (SELECT xxx FROM xxx)) ORDER BY exp LIMIT params
   * </code>
   */
  private void promoteLimitSort(RelNode node, boolean isSource) {
    if (node == null) {
      return;
    }

    RelNode root = isSource ? plan0 : plan1;
    int numChildren = node.getInputs().size();

    for (int i = 0; i < numChildren; ++i) {
      promoteLimitSort(node.getInput(i), isSource);
    }

    if (!(node instanceof Sort)) {
      return;
    }

    // find the match upperSort
    final Sort sort = (Sort) node;
    final Sort upperSort = (Sort) getParentSortProjs(root, node, node);
    if (upperSort == null) {
      return;
    }

    // Only consider the same sort and has no offset and fetch
    if (!isSortSame(sort, upperSort) || upperSort.offset != null || upperSort.fetch != null) {
      return;
    }

    // replace upperSort with node
    final LogicalSort newSort = LogicalSort.create(upperSort.getInput(), upperSort.getCollation(), sort.offset, sort.fetch);
    if (upperSort == root) {
      if (isSource) {
        plan0 = newSort;
      } else {
        plan1 = newSort;
      }
    } else {
      replaceNode(root, upperSort, newSort);
    }

    // update local variable
    root = isSource ? plan0 : plan1;

    // delete node
    deleteSortLimit(root, node);
  }

  /**
   * Merge Limit.
   * The algorithm mentioned in the thesis.
   * <p/>
   * Merge sort nodes that are related to UNION/FULL JOIN/LEFT JOIN.
   * <p/>
   * e.g.
   * <code> (R1 order by expression limit number_rows offset offset_value)
   * [union all|full join|left join] R2
   * order by expression limit number_rows offset offset_value
   * <p>-></p>
   * R1 [union all|full join|left join] R2
   * order by expression limit number_rows offset offset_value </code>
   * <p/>
   * Merge nearby sort nodes.
   * <p/>
   * e.g.
   * <code> (R order by expression limit number_rows1 offset offset_value1)
   * order by expression limit number_rows2 offset offset_value2
   * <p>-></p>
   * R order by expression limit number_rows3 offset offset_value3 </code> <p/>
   * when (offset_value2 + number_rows2) <= number_rows1, number_rows3 = number_rows2,
   * otherwise, number_rows3 = MAX((number_rows1 - number_rows2), 0).
   */
  private void mergeLimit(RelNode node, boolean isSource) {
    if (node == null) {
      return;
    }

    final RelNode root = isSource ? plan0 : plan1;
    final int numChildren = node.getInputs().size();

    // Merge sort nodes that are related to UNION/FULL JOIN/LEFT JOIN.
    if ((node != root)
            && ((node instanceof Union && ((Union) node).all)
            || (node instanceof Join && ((Join) node).getJoinType() == JoinRelType.LEFT)
            || (node instanceof Join && ((Join) node).getJoinType() == JoinRelType.FULL))) {
      final RelNode upperSortLimit = getParentSortProjs(root, node, node);
      if (upperSortLimit != null) {
        if ((node instanceof Union && ((Union) node).all)
                || (node instanceof Join && ((Join) node).getJoinType() == JoinRelType.FULL)) {
          // The union all && full join case: consider all children.
          for (int i = 0; i < numChildren; ++i) {
            final RelNode childSortLimit = getChildSortProjs(root, node.getInput(i), node);
            if (childSortLimit == null) {
              continue;
            }
            if (canMergeSortLimit(childSortLimit, upperSortLimit)) {
              deleteSortLimit(root, childSortLimit);
            }
          }
        } else {
          // The left join case: consider first children.
          final RelNode childSortLimit = getChildSortProjs(root, node.getInput(0), node);
          if (canMergeSortLimit(childSortLimit, upperSortLimit)) {
            deleteSortLimit(root, childSortLimit);
          }
        }
      }
    }

    // Merge nearby sort nodes.
    if ((node != root) && (node instanceof Sort sort)) {
      final Sort upperSortLimit = (Sort) getParentSortProjs(root, node, node);
      if (upperSortLimit != null) {
        if (isSortSame(sort, upperSortLimit)
                && isTargetKind(sort.fetch, RexLiteral.class)
                && isTargetKind(sort.offset, RexLiteral.class)
                && isTargetKind(upperSortLimit.fetch, RexLiteral.class)
                && isTargetKind(upperSortLimit.offset, RexLiteral.class)) {
          final RexLiteral sortFetch = (RexLiteral) sort.fetch;
          final RexLiteral sortOffset = (RexLiteral) sort.offset;
          final RexLiteral upperSortLimitFetch = (RexLiteral) upperSortLimit.fetch;
          final RexLiteral upperSortLimitOffset = (RexLiteral) upperSortLimit.offset;
          if (sortFetch.getType().getSqlTypeName() == SqlTypeName.INTEGER
                  && sortOffset.getType().getSqlTypeName() == SqlTypeName.INTEGER
                  && upperSortLimitFetch.getType().getSqlTypeName() == SqlTypeName.INTEGER
                  && upperSortLimitOffset.getType().getSqlTypeName() == SqlTypeName.INTEGER) {
            // Just use integer rather than big decimal due to the simplicity of integer.
            final int sortFetchVal = RexLiteral.intValue(sortFetch);
            final int sortOffsetVal = RexLiteral.intValue(sortOffset);
            final int upperSortLimitFetchVal = RexLiteral.intValue(upperSortLimitFetch);
            final int upperSortLimitOffsetVal = RexLiteral.intValue(upperSortLimitOffset);

            final int newOffsetVal = sortOffsetVal + upperSortLimitOffsetVal;
            int newFetchVal = -1;
            if (upperSortLimitFetchVal + upperSortLimitOffsetVal <= sortFetchVal) {
              newFetchVal = upperSortLimitFetchVal;
            } else {
              newFetchVal = Math.max((sortFetchVal - upperSortLimitOffsetVal), 0);
            }

            // Construct the newSort.
            final RexBuilder rexBuilder = new RexBuilder(CalciteSupport.JAVA_TYPE_FACTORY);
            final RelNode newSort = LogicalSort.create(upperSortLimit.getInput(),
                    upperSortLimit.getCollation(),
                    rexBuilder.makeBigintLiteral(BigDecimal.valueOf(newOffsetVal)),
                    rexBuilder.makeBigintLiteral(BigDecimal.valueOf(newFetchVal)));

            // Delete the childSort.
            deleteSortLimit(root, sort);

            // Replace the upperSort.
            if (upperSortLimit == root) {
              if (isSource) {
                plan0 = newSort;
              } else {
                plan1 = newSort;
              }
            } else {
              replaceNode(root, upperSortLimit, newSort);
            }
          }
        }
      }
    }


    for (int i = 0; i < numChildren; ++i) {
      mergeLimit(node.getInput(i), isSource);
    }
  }

  /**
   * Check whether the given two plans are matched using the algorithm that delete the sort node.
   * This algorithm is mentioned in paper.
   */
  private VerificationResult patternMatching(RelNode p0, RelNode p1) {
    boolean hasOrderBy0 = hasNodeOfKind(p0, Sort.class);
    boolean hasOrderBy1 = hasNodeOfKind(p1, Sort.class);

    if (hasOrderBy0 != hasOrderBy1) {
      return VerificationResult.UNKNOWN;
    }

    if (!hasOrderBy0) {
      return proveEqByLIAStarConcrete(p0, p1, this.schema);
    }

    final List<RelNode> sortNodes0 = new ArrayList<>();
    final List<RelNode> sortNodes1 = new ArrayList<>();
    getAllNodesOfKind(p0, Sort.class, sortNodes0);
    getAllNodesOfKind(p1, Sort.class, sortNodes1);
    for (final RelNode sortNode0 : sortNodes0) {
      for (final RelNode sortNode1 : sortNodes1) {
        final boolean isSameSort = isSameSortLimitFetch((Sort) sortNode0, (Sort) sortNode1);
        // if two sort nodes have different semantics, continue.
        if (!isSameSort) {
          continue;
        }

        // if two sort have different schema size, continue.
        if (getOutputSizeOfRelNode(sortNode0) != getOutputSizeOfRelNode(sortNode1)) {
          continue;
        }

        final String[] tempQueries = getNewTempTableAndProjString(getOutputSizeOfRelNode(sortNode0),
                sortNode0.getRowType().getFieldList());
        RelNode[] subTree0 = splitPlanContext(p0, sortNode0, tempQueries);
        RelNode[] subTree1 = splitPlanContext(p1, sortNode1, tempQueries);

        if (subTree0.length != subTree1.length) {
          return VerificationResult.UNKNOWN;
        }

        boolean isEqual = true;
        for (int i = 0; i < 2; ++i) {
          RelNode subPlan0 = subTree0[i];
          RelNode subPlan1 = subTree1[i];
          if (subPlan0 == null && subPlan1 == null) break;
          if (subPlan0 == null || subPlan1 == null) return VerificationResult.UNKNOWN;

          VerificationResult matchingFlag = patternMatching(subPlan0, subPlan1);
          if (matchingFlag == VerificationResult.UNKNOWN) {
            isEqual = false;
            break;
          }
          if (matchingFlag == VerificationResult.NEQ) {
            isEqual = false;
            break;
          }
        }

        if (isEqual)
          // successfully find a case that the splits plan are EQ.
          return VerificationResult.EQ;
        // restore the plan tree.
        if (subTree0[1] != null && subTree0[2] != null) {
          replaceNode(p0, subTree0[2], subTree0[1]);
        }
        if (subTree1[1] != null && subTree1[2] != null) {
          replaceNode(p1, subTree1[2], subTree1[1]);
        }
      }
    }

    return VerificationResult.UNKNOWN;
  }

  /**
   * Delete empty sort nodes.
   * For those empty sort nodes, which doesn't have order by expression, limit and fetch, simply delete them.
   */
  private void deleteEmptySort(RelNode node, boolean isSource) {
    if (node == null) {
      return;
    }

    final RelNode root = isSource ? plan0 : plan1;

    if (node instanceof Sort sort) {
      if (sort.fetch == null
              && sort.offset == null
              && sort.getSortExps().size() == 0) {
        if (node == root) {
          if (isSource) {
            plan0 = sort.getInput();
          } else {
            plan1 = sort.getInput();
          }
        } else {
          deleteSortLimit(root, node);
        }
      }
    }

    for (final RelNode child : node.getInputs()) {
      deleteEmptySort(child, isSource);
    }
  }

  /*
   * Helper functions for OrderBy algorithm.
   */

  /**
   * Whether a logical plan is LIMIT 0.
   */
  private boolean isFetchZero(RelNode p) {
    if (p instanceof Sort node) {
      RexNode fetch = node.fetch;
      if (fetch == null) {
        return false;
      }
      if (fetch instanceof RexLiteral literal && literal.getType().getSqlTypeName() == SqlTypeName.INTEGER) {
        final int value = RexLiteral.intValue(literal);
        return value == 0;
      }
    }
    return false;
  }

  /**
   * Whether two logical plan return zero tuples.
   */
  private boolean bothReturnZeroTuples(RelNode p0, RelNode p1) {
    boolean isFetchZero0 = isFetchZero(p0);
    boolean isFetchZero1 = isFetchZero(p1);
    if ((isFetchZero0 && isFetchZero1)) return true;
    // skip the sort node and get the u-expression of sub-nodes
    RelNode p0NoSort = p0;
    RelNode p1NoSort = p1;
    while (p0NoSort instanceof Sort sort) {
      p0NoSort = sort.getInput();
    }
    while (p1NoSort instanceof Sort sort) {
      p1NoSort = sort.getInput();
    }
    try {
      final UExprConcreteTranslationResult uExprsWithIC =
              UExprSupport.translateQueryToUExpr(p0NoSort, p1NoSort, schema,
                      UExprSupport.UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE
                              | UExprSupport.UEXPR_FLAG_NO_EXPLAIN_PREDICATES);
      if (uExprsWithIC != null) {
        if (!isEqualTwoValueList(uExprsWithIC.srcTupleVarSchemaOf(uExprsWithIC.sourceOutVar()), uExprsWithIC.tgtTupleVarSchemaOf(uExprsWithIC.targetOutVar()))) {
          return false;
        }
        return uExprsWithIC.sourceExpr().equals(UConst.zero()) && uExprsWithIC.targetExpr().equals(UConst.zero());
      }
    } catch (Exception e) {
      Timeout.bypassTimeout(e);
      return false;
    }
    return false;
  }

  /**
   * Find a bigger limit related to `source`.
   * The target limit must be ancestor of `source`, and proceed the finding process from the top to the bottom,
   * which finds the matched Limit that is close to the source.
   * e.g. <code>SELECT * FROM (SELECT * FROM T LIMIT 3) LIMIT 5
   * <p>-></p>
   * SELECT * FROM (SELECT * FROM T LIMIT 3)</code>
   */
  private RelNode getBiggerLimit(RelNode plan, RelNode source) {
    assert (source instanceof Sort);
    RelNode target = plan;
    RelNode result = null;
    while (target != source) {
      int numChildren = target.getInputs().size();
      if (numChildren != 1) return null;
      if (target instanceof Sort sort) {
        final RexNode targetOffset = sort.offset;
        final RexNode targetLimit = sort.fetch;
        final RexNode sourceOffset = ((Sort) source).offset;
        final RexNode sourceLimit = ((Sort) source).fetch;
        // All the offsets and limits should be literal
        if (targetOffset != null || sourceOffset != null) {
          return null;
        }
        // All the limits should be literal
        if (sourceLimit instanceof RexLiteral sourceLiteral
                && sourceLiteral.getType().getSqlTypeName() == SqlTypeName.INTEGER
                && targetLimit instanceof RexLiteral targetLiteral
                && targetLiteral.getType().getSqlTypeName() == SqlTypeName.INTEGER) {
          final int sourceValue = RexLiteral.intValue(sourceLiteral);
          final int targetValue = RexLiteral.intValue(targetLiteral);
          if (targetValue >= sourceValue) {
            result = target;
          }
        }
      }
      target = target.getInput(0);
    }
    return result;

  }

  /**
   * Find the parent sort of target where the middle nodes must be PROJ.
   */
  private RelNode getParentSortProjs(RelNode plan, RelNode node, RelNode target) {
    assert target != null;

    if (node == null) {
      return null;
    }

    final RelNode father = getFatherOfTarget(plan, node);

    // Only when node is PROJ, go up.
    if (node == target || node instanceof Project) {
      return getParentSortProjs(plan, father, target);
    }

    // When node is sort, find the node.
    if (node instanceof Sort sort) {
      return sort;
    }

    return null;
  }

  /**
   * Find the child sort of target where the middle nodes must be PROJ.
   */
  private RelNode getChildSortProjs(RelNode plan, RelNode node, RelNode target) {
    assert target != null;

    if (node == null) {
      return null;
    }

    // Only when node is PROJ, go down.
    if (node == target || node instanceof Project) {
      return getChildSortProjs(plan, node.getInput(0), target);
    }

    // When node is sort, find the node.
    if (node instanceof Sort sort) {
      return sort;
    }

    return null;
  }

  /**
   * Check whether two Sort node has the same OrderBy expression.
   * NOTE: This function can only handle column cases and need to add more features.
   */
  private boolean isSortSame(Sort sort1, Sort sort2) {
    final List<RexNode> sortExps1 = sort1.getSortExps();
    final List<RexNode> sortExps2 = sort2.getSortExps();
    final List<RelFieldCollation> sortCollations1 = sort1.getCollation().getFieldCollations();
    final List<RelFieldCollation> sortCollations2 = sort2.getCollation().getFieldCollations();

    assert sortCollations1.size() == sortExps1.size();
    assert sortCollations2.size() == sortExps2.size();

    // Firstly check whether the size are the same.
    if (sortExps1.size() != sortExps2.size()) {
      return false;
    }

    // Secondly Check whether every sortExp are the same.
    // Only need to consider that whether every sortExp's index are the same.
    for (int i = 0; i < sortExps1.size(); i++) {
      final RexNode sortExp1 = sortExps1.get(i);
      final RexNode sortExp2 = sortExps2.get(i);
      final RelFieldCollation sortCollation1 = sortCollations1.get(i);
      final RelFieldCollation sortCollation2 = sortCollations2.get(i);
      if (sortExp1 instanceof RexInputRef rexInputRef1
              && sortExp2 instanceof RexInputRef rexInputRef2) {
        if (rexInputRef1.getIndex() != rexInputRef2.getIndex()
                || sortCollation1.direction != sortCollation2.direction) {
          return false;
        }
      } else {
        return false;
      }
    }


    return true;
  }

  /**
   * Replace the target node with another node
   */
  private void replaceNode(RelNode plan, RelNode target, RelNode replace) {
    final RelNode father = getFatherOfTarget(plan, target);

    if (father == null) {
      // target should be root.
      assert target == plan;
      if (plan == plan0) {
        plan0 = replace;
      } else {
        plan1 = replace;
      }
      return;
    }

    int targetIndex = -1;
    for (int i = 0; i < father.getInputs().size(); i++) {
      if (father.getInput(i) == target) {
        targetIndex = i;
        break;
      }
    }
    father.replaceInput(targetIndex, replace);
  }

  /**
   * Delete the sort limit node in the plan.
   */
  private void deleteSortLimit(RelNode plan, RelNode target) {
    assert target instanceof Sort;
    final Sort sort = (Sort) target;
    replaceNode(plan, sort, sort.getInput());
  }

  /**
   * Check whether the childSort can match the parentSort so that the child can be merged.
   * This function must ensure that the childSortLimit and parentSortLimit are totally the same.
   */
  boolean canMergeSortLimit(RelNode childSortLimit, RelNode parentSortLimit) {
    if (childSortLimit == null || parentSortLimit == null)
      return false;

    assert childSortLimit instanceof Sort;
    assert parentSortLimit instanceof Sort;

    final Sort childSort = (Sort) childSortLimit;
    final Sort parentSort = (Sort) parentSortLimit;

    // Check whether sort expressions are the same.
    if (!isSortSame(childSort, parentSort)) {
      return false;
    }

    // Check whether offset, fetch are the same. Only consider literal here.
    return checkTwoRexNodeIntegerEqual(childSort.offset, parentSort.offset)
            && checkTwoRexNodeIntegerEqual(childSort.fetch, parentSort.fetch);
  }

  /**
   * Check two RexNode are literally equal in Integer format.
   * If they are all null, return EQ.
   * If they have same integer values, return EQ.
   * Otherwise, return NEQ.
   */
  private boolean checkTwoRexNodeIntegerEqual(RexNode node1, RexNode node2) {
    boolean result = false;

    if (node1 == null && node2 == null) {
      return true;
    }

    if (node1 == null || node2 == null) {
      return false;
    }

    if (node1 instanceof RexLiteral nodeLiteral1
            && node2 instanceof RexLiteral nodeLiteral2) {
      if (nodeLiteral1.getType().getSqlTypeName() == SqlTypeName.INTEGER
              && nodeLiteral2.getType().getSqlTypeName() == SqlTypeName.INTEGER) {
        final int nodeInteger1 = RexLiteral.intValue(nodeLiteral1);
        final int nodeInteger2 = RexLiteral.intValue(nodeLiteral2);
        result = nodeInteger1 == nodeInteger2;
      }
    }


    return result;
  }

  /**
   * Check whether two sort node have literally same sort (isSortSame), limit and fetch.
   */
  private boolean isSameSortLimitFetch(Sort sort1, Sort sort2) {
    return isSortSame(sort1, sort2)
            && checkTwoRexNodeIntegerEqual(sort1.fetch, sort2.fetch)
            && checkTwoRexNodeIntegerEqual(sort1.offset, sort2.offset);
  }

  /**
   * Split the plan into two parts according to the given node.
   * Because the node's kind is sort, it will only have one child.
   */
  private RelNode[] splitPlanContext(RelNode plan, RelNode node, String[] tempQueries) {
    if (node == null || plan == null) {
      return new RelNode[0];
    }

    assert node instanceof Sort;

    RelNode[] results = new RelNode[3];
    results[0] = null;
    results[1] = null;
    results[2] = null;

    if (node == plan) {
      // if node == root, delete root and return root's first children.
      results[0] = node.getInput(0);
      return results;
    } else {
      // else split the plan into two parts, and fill the hole with a simulation node
      final RelNode simulationNode = getSimulationBySort(tempQueries);
      replaceNode(plan, node, simulationNode);
      results[0] = plan;
      results[1] = node;
      // results[2] store the simulation node for restoring the plan.
      results[2] = simulationNode;
      return results;
    }
  }

  /**
   * Construct a simulation node of a given sort.
   * This simulation node should be a Proj node for a tableScan which output size is equal to the given sort.
   * The sort related tempQueries have already been given.
   */
  private RelNode getSimulationBySort(String[] tempQueries) {
    final String createQuery = tempQueries[0];
    final String projQuery = tempQueries[1];
    final Schema tempSchema = getSchema(createQuery);
    final CalciteSchema tempSchemaPlus = getCalciteSchema(tempSchema);
    final Planner planner = getPlanner(tempSchemaPlus);

    // add the table of tempSchema into this.schema
    final List<Table> toAddTables = new ArrayList<>(tempSchema.tables());
    this.schema.addTables(toAddTables);

    return parseRel(parseAST(projQuery, planner), planner);
  }

}
