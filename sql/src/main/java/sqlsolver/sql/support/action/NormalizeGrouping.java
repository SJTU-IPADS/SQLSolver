package sqlsolver.sql.support.action;

import sqlsolver.common.utils.Commons;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.support.resolution.Attribute;
import sqlsolver.sql.support.resolution.ResolutionSupport;
import sqlsolver.sql.ast.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sqlsolver.sql.support.resolution.ResolutionSupport.resolveAttribute;
import static sqlsolver.sql.util.RenumberListener.watch;

class NormalizeGrouping {
  static void normalize(SqlNode node) {
    for (SqlNode target : LocatorSupport.nodeLocator().accept(SqlKind.QuerySpec).gather(node)) normalizeGrouping(target);
  }

  private static void normalizeGrouping(SqlNode querySpec) {
    final SqlNodes groupBys = querySpec.$(SqlNodeFields.QuerySpec_GroupBy);
    if (Commons.isNullOrEmpty(groupBys)) return;

//    removeConstantGroupItem(groupBys);
    if (groupBys.isEmpty()) {
      querySpec.remove(SqlNodeFields.QuerySpec_GroupBy);
      return;
    }

    sortGroupItem(groupBys);
    convertHavingToWhere(querySpec);
    convertFullCoveringGroupingToDistinct(querySpec);
  }

  private static void removeConstantGroupItem(SqlNodes groupBys) {
    for (int i = 0; i < groupBys.size(); ) {
      final SqlNode groupItem = groupBys.get(i);
      if (NormalizationSupport.isConstant(groupItem.$(SqlNodeFields.GroupItem_Expr))) {
        groupBys.erase(groupItem.nodeId());
      } else {
        ++i;
      }
    }
  }

  private static void sortGroupItem(SqlNodes groupBys) {
    groupBys.sort(Comparator.comparing(SqlNode::toString));
  }

  private static void convertFullCoveringGroupingToDistinct(SqlNode querySpec) {
    if (querySpec.$(SqlNodeFields.QuerySpec_Having) != null) return;
    final SqlNodes groupBys = querySpec.$(SqlNodeFields.QuerySpec_GroupBy);

    final Set<Attribute> groupAttributes = new HashSet<>();
    for (SqlNode groupBy : groupBys) {
      final SqlNode expr = groupBy.$(SqlNodeFields.GroupItem_Expr);
      if (!ExprKind.ColRef.isInstance(expr)) return;

      final Attribute attribute = ResolutionSupport.resolveAttribute(expr);
      if (attribute == null) return;

      groupAttributes.add(attribute);
    }

    final SqlNodes projections = querySpec.$(SqlNodeFields.QuerySpec_SelectItems);
    for (SqlNode projection : projections) {
      final SqlNode expr = projection.$(SqlNodeFields.SelectItem_Expr);
      if (!ExprKind.ColRef.isInstance(expr)) return;

      final Attribute attribute = ResolutionSupport.resolveAttribute(expr);
      if (attribute == null) return;

      if (!groupAttributes.contains(attribute)) return;
    }

    querySpec.remove(SqlNodeFields.QuerySpec_GroupBy);
    querySpec.flag(SqlNodeFields.QuerySpec_Distinct);
  }

  private static void convertHavingToWhere(SqlNode querySpec) {
    final SqlNode having = querySpec.$(SqlNodeFields.QuerySpec_Having);
    if (having == null) return;

    final SqlNodes exprs =
        LocatorSupport.predicateLocator().primitive().conjunctive().breakdownExpr().gather(having);

    try (final var es = watch(querySpec.context(), exprs.nodeIds())) {
      for (SqlNode e : es) {
        convertHavingToWhere(querySpec, e);
      }
    }
  }

  private static void convertHavingToWhere(SqlNode querySpec, SqlNode expr) {
    final SqlNode agg = LocatorSupport.nodeLocator().accept(ExprKind.Aggregate).find(expr);
    if (agg != null) return;

    final SqlNodes colRefs = LocatorSupport.nodeLocator().accept(ExprKind.ColRef).gather(expr);
    final List<Attribute> outAttr = ResolutionSupport.getEnclosingRelation(querySpec).attributes();
    for (SqlNode colRef : colRefs) {
      final Attribute attr = ResolutionSupport.resolveAttribute(colRef);
      if (attr != null && outAttr.contains(attr)) return;
    }

    NormalizationSupport.detachExpr(expr);
    NormalizationSupport.conjunctExprTo(querySpec, SqlNodeFields.QuerySpec_Where, expr);
  }
}
