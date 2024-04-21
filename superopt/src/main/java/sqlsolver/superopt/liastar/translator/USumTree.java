package sqlsolver.superopt.liastar.translator;

import static sqlsolver.superopt.liastar.translator.LiaStarTranslatorSupport.findTopLevelSums;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sqlsolver.common.utils.Commons;
import sqlsolver.superopt.uexpr.*;

/**
 * The structure of summations in a U-expression. There must be a "root" node (which has no bound
 * vars) for any given U-expression.
 */
public class USumTree {
  // bound vars
  private final Set<BoundVar> bvs;
  private final List<USumTree> children;

  public USumTree(UTerm uexp) {
    bvs = new HashSet<>();
    children = buildForestFromUExpr(uexp);
  }

  public USumTree(Set<BoundVar> bvs, List<USumTree> children) {
    this.bvs = bvs;
    this.children = children;
  }

  /**
   * Copy and collect all the nodes (including the root).
   *
   * @return a list containing all the copied nodes
   */
  public List<Set<BoundVar>> getAllNodes() {
    List<Set<BoundVar>> result = new ArrayList<>();
    result.add(new HashSet<>(bvs));
    for (USumTree child : children) {
      result.addAll(child.getAllNodes());
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(sb, 0);
    return sb.toString();
  }

  private void toString(StringBuilder sb, int depth) {
    // print the current layer
    if (depth > 0) {
      // indent
      sb.append("   ".repeat(depth - 1));
      sb.append("|- ");
    }
    Commons.joining("{", "", ",", "", "}\n", bvs, sb);
    // print successive layers
    for (USumTree child : children) {
      child.toString(sb, depth + 1);
    }
  }

  /**
   * Build a summation "forest" (i.e. a list of sum trees) given a U-expression. For example,
   * ∑{x}(...) + ∑{y}(...) results in [{x}, {y}], where "..." does not contain summations while {x}
   * and {y} stand for single-node trees.
   */
  private static List<USumTree> buildForestFromUExpr(UTerm uexp) {
    // find top-level summations
    final List<USum> topLevelSums = findTopLevelSums(uexp);
    // build a SumTree for each summation recursively
    final List<USumTree> forest = new ArrayList<>();
    for (USum sum : topLevelSums) {
      final Set<BoundVar> bvs = new HashSet<>();
      final UTerm body = sum.body();
      for (UVar var : sum.boundedVars()) {
        bvs.add(buildBV(var, body));
      }
      final List<USumTree> children = buildForestFromUExpr(sum.body());
      forest.add(new USumTree(bvs, children));
    }
    return forest;
  }

  private static BoundVar buildBV(UVar var, UTerm sumBody) {
    if (sumBody instanceof UMul mul) {
      for (UTerm term : mul.subTerms()) {
        if (term instanceof UTable table && table.var().equals(var)) {
          return new BoundVar(var.copy(), table.tableName().toString());
        }
      }
    } else {
      if (sumBody instanceof UTable table && table.var().equals(var)) {
        return new BoundVar(var.copy(), table.tableName().toString());
      }
    }
    return new BoundVar(var.copy(), null);
  }
}
