package sqlsolver.superopt.util;

import sqlsolver.sql.ast.constants.JoinKind;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.sql.plan.PlanKind;
import sqlsolver.superopt.fragment.*;
import sqlsolver.superopt.fragment.*;

import java.util.HashSet;
import java.util.Set;

import static java.lang.Integer.min;
import static sqlsolver.sql.plan.PlanSupport.isDedup;
import static sqlsolver.sql.plan.PlanSupport.joinKindOf;

public class Fingerprint {
  private final String fingerprint;

  Fingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }

  public String fingerprint() {
    return fingerprint;
  }

  public static Fingerprint mk(Fragment fragment) {
    final OpFingerprintBuilder builder = new OpFingerprintBuilder();
    return builder.build(fragment.root(), 4);
  }

  public static Set<Fingerprint> mk(PlanContext plan, int node) {
    final Set<Fingerprint> fingerprints = new HashSet<>();
    for (int limit = 1; limit <= 4; ++limit) {
      final PlanFingerprintBuilder builder = new PlanFingerprintBuilder(plan);
      fingerprints.addAll(builder.build(node, limit));
    }
    return fingerprints;
  }

  private static char getOpIdentifier(OpKind kind, boolean dedup) {
    switch (kind) {
      case PROJ:
        return dedup ? 'q' : 'p';
      case SIMPLE_FILTER:
        return 'f';
      case IN_SUB_FILTER:
        return 's';
      case INNER_JOIN:
        return 'j';
      case LEFT_JOIN:
        return 'l';
      case AGG:
        return 'a';
      case UNION:
        return dedup ? 'v' : 'u';
      default:
        return '?';
    }
  }

  private static void repeatChar(StringBuilder builder, char c, int count) {
    for (int i = 0; i < count; ++i) builder.append(c);
  }

  private static void popChars(StringBuilder buffer, int count) {
    buffer.delete(buffer.length() - count, buffer.length());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fingerprint)) return false;
    final Fingerprint that = (Fingerprint) o;
    return fingerprint.equals(that.fingerprint);
  }

  @Override
  public int hashCode() {
    return fingerprint.hashCode();
  }

  @Override
  public String toString() {
    return fingerprint;
  }

  private static class OpFingerprintBuilder {
    private final StringBuilder builder = new StringBuilder();
    private Fingerprint fingerprint;

    private Fingerprint build(Op op, int limit) {
      if (limit <= 0 || op.kind() == OpKind.INPUT) {
        return fingerprint = new Fingerprint(builder.toString());
      }

      final boolean dedup =
          op.kind() == OpKind.PROJ && ((Proj) op).deduplicated()
              || op.kind() == OpKind.UNION && ((Union) op).deduplicated();

      builder.append(getOpIdentifier(op.kind(), dedup));
      build(op.predecessors()[0], limit - 1);

      return fingerprint;
    }
  }

  private static class PlanFingerprintBuilder {
    private final PlanContext plan;
    private final StringBuilder builder;
    private final Set<Fingerprint> fingerprints;

    private PlanFingerprintBuilder(PlanContext plan) {
      this.plan = plan;
      this.builder = new StringBuilder();
      this.fingerprints = new HashSet<>();
    }

    private Set<Fingerprint> build(int node, int budget) {
      final PlanKind nodeKind = plan.kindOf(node);
      if (budget <= 0 || nodeKind == PlanKind.Input) {
        fingerprints.add(new Fingerprint(builder.toString()));
        return fingerprints;
      }

      if (nodeKind.isFilter()) {
        final int chainChild = locateFilterChainChild(node);
        final int[] counts = countFilters(node);
        final int tot = counts[0], subTot = counts[1];
        final int limit = min(tot, budget);

        for (int cnt = 1; cnt <= limit; ++cnt) {
          for (int subCnt = 0, subBudget = min(cnt, subTot); subCnt <= subBudget; ++subCnt) {
            repeatChar(builder, getOpIdentifier(OpKind.SIMPLE_FILTER, false), cnt - subCnt);
            repeatChar(builder, getOpIdentifier(OpKind.IN_SUB_FILTER, false), subCnt);
            // Suppose we have P,S,S,S,J. We should not make out PSSJ.

            if (cnt == subCnt && subCnt < subTot)
              fingerprints.add(new Fingerprint(builder.toString()));
            else build(chainChild, budget - cnt);

            popChars(builder, cnt);
          }
        }

      } else if (nodeKind == PlanKind.Join) {
        final int treeChild = locateJoinTreeChild(node);
        final int[] counts = countJoins(node);
        final int total = counts[0], leftJoins = counts[1];
        mkFingerprintForJoin(total, leftJoins, budget, treeChild);

      } else if (nodeKind == PlanKind.Proj || nodeKind == PlanKind.SetOp) {
        builder.append(identifierOf(node));
        build(plan.childOf(node, 0), budget - 1);
        popChars(builder, 1);
      } else if (nodeKind == PlanKind.Agg) {
        builder.append(identifierOf(node));
        build(plan.childOf(plan.childOf(node, 0), 0), budget - 1);
        popChars(builder, 1);
      }

      return fingerprints;
    }

    private char identifierOf(int node) {
      final PlanKind kind = plan.kindOf(node);
      if (kind == PlanKind.Proj) return getOpIdentifier(OpKind.PROJ, isDedup(plan, node));
      else if (kind == PlanKind.SetOp) return getOpIdentifier(OpKind.UNION, isDedup(plan, node));
      else if (kind == PlanKind.Agg) return getOpIdentifier(OpKind.AGG, false);
      else if (kind == PlanKind.Filter) return getOpIdentifier(OpKind.SIMPLE_FILTER, false);
      else if (kind == PlanKind.InSub) return getOpIdentifier(OpKind.IN_SUB_FILTER, false);
      else if (kind == PlanKind.Join)
        return joinKindOf(plan, node) == JoinKind.INNER_JOIN
            ? getOpIdentifier(OpKind.INNER_JOIN, false)
            : getOpIdentifier(OpKind.LEFT_JOIN, false);
      else assert false : kind;
      return '?';
    }

    private int locateFilterChainChild(int node) {
      while (plan.kindOf(node).isFilter()) node = plan.childOf(node, 0);
      return node;
    }

    private int locateJoinTreeChild(int node) {
      while (plan.kindOf(node) == PlanKind.Join) node = plan.childOf(node, 0);
      return node;
    }

    private int[] countFilters(int node) {
      int total = 0, subquery = 0;
      while (plan.kindOf(node).isFilter()) {
        ++total;
        if (plan.kindOf(node).isSubqueryFilter()) ++subquery;
        node = plan.childOf(node, 0);
      }
      return new int[] {total, subquery};
    }

    private int[] countJoins(int node) {
      int total = 0, leftJoin = 0;
      while (plan.kindOf(node) == PlanKind.Join) {
        ++total;
        if (joinKindOf(plan, node) == JoinKind.LEFT_JOIN) ++leftJoin;
        node = plan.childOf(node, 0);
      }
      return new int[] {total, leftJoin};
    }

    private void mkFingerprintForJoin(int joins, int leftJoins, int budget, int joinTreeChild) {
      if (budget == 0 || joins == 0) {
        build(joinTreeChild, budget);
        return;
      }

      if (leftJoins > 0) {
        builder.append(getOpIdentifier(OpKind.LEFT_JOIN, false));
        mkFingerprintForJoin(joins - 1, leftJoins - 1, budget - 1, joinTreeChild);
        popChars(builder, 1);
      }

      builder.append(getOpIdentifier(OpKind.INNER_JOIN, false));
      mkFingerprintForJoin(joins - 1, leftJoins, budget - 1, joinTreeChild);
      popChars(builder, 1);
    }
  }
}
