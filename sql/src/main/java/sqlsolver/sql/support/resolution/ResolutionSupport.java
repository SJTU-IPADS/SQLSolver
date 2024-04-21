package sqlsolver.sql.support.resolution;

import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.Table;
import sqlsolver.sql.ast.*;

import static sqlsolver.common.tree.TreeSupport.nodeEquals;
import static sqlsolver.common.utils.ListSupport.tail;
import static sqlsolver.common.utils.IterableSupport.any;
import static sqlsolver.common.utils.IterableSupport.linearFind;
import static sqlsolver.sql.support.resolution.Relations.RELATION;

public abstract class ResolutionSupport {
  private ResolutionSupport() {}

  static boolean limitClauseAsParam = false;

  static int scopeRootOf(SqlNode node) {
    if (Relation.isRelationRoot(node)) return node.nodeId();

    final SqlContext ctx = node.context();
    int cursor = node.nodeId();
    while (ctx.kindOf(cursor) != SqlKind.Query) cursor = ctx.parentOf(cursor);
    return cursor;
  }

  public static void setLimitClauseAsParam(boolean limitClauseAsParam) {
    ResolutionSupport.limitClauseAsParam = limitClauseAsParam;
  }

  public static Relation getEnclosingRelation(SqlNode node) {
    return node.context().getAdditionalInfo(RELATION).enclosingRelationOf(node);
  }

  public static boolean isDirectTable(Relation relation) {
    return TableSourceKind.SimpleSource.isInstance(relation.rootNode());
  }

  public static boolean isServedAsInput(Relation relation) {
    final SqlNode node = relation.rootNode();
    final SqlNode parent = node.parent();
    return SqlKind.TableSource.isInstance(node)
        || SqlKind.TableSource.isInstance(parent)
        || SqlKind.SetOp.isInstance(parent) && nodeEquals(parent.$(SqlNodeFields.SetOp_Left), node);
  }

  public static Table tableOf(Relation relation) {
    if (!isDirectTable(relation)) return null;
    final Schema schema = relation.rootNode().context().schema();
    final String tableName = relation.rootNode().$(TableSourceFields.Simple_Table).$(SqlNodeFields.TableName_Table);
    return schema.table(tableName);
  }

  public static SqlNode tableSourceOf(Relation relation) {
    final SqlNode node = relation.rootNode();
    if (SqlKind.TableSource.isInstance(node)) return node;
    final SqlNode parent = node.parent();
    if (SqlKind.TableSource.isInstance(parent)) return parent;
    return null;
  }

  public static SqlNode queryOf(Relation relation) {
    final SqlNode node = relation.rootNode();
    if (SqlKind.Query.isInstance(node)) return node;
    else return null;
  }

  public static ParamDesc paramOf(SqlNode node) {
    return node.context().getAdditionalInfo(Params.PARAMS).paramOf(node);
  }

  public static Relation getOuterRelation(Relation relation) {
    final SqlNode parent = relation.rootNode().parent();
    return parent == null ? null : getEnclosingRelation(parent);
  }

  public static Attribute resolveAttribute(Relation relation, String qualification, String name) {
    for (Relation input : relation.inputs()) {
      final Attribute attr = input.resolveAttribute(qualification, name);
      if (attr != null) return attr;
    }
    return null;
  }

  public static Attribute resolveAttribute(SqlNode colRef) {
    if (!ExprKind.ColRef.isInstance(colRef)) return null;

    final Relation relation = getEnclosingRelation(colRef);
    getEnclosingRelation(colRef);
    final String qualification = colRef.$(ExprFields.ColRef_ColName).$(SqlNodeFields.ColName_Table);
    final String name = colRef.$(ExprFields.ColRef_ColName).$(SqlNodeFields.ColName_Col);

    Attribute attr = resolveAttribute(relation, qualification, name);
    if (attr != null) return attr;

    if (qualification == null) {
      final FieldKey<?> clause = getClauseOfExpr(colRef);
      if (clause == SqlNodeFields.Query_OrderBy || clause == SqlNodeFields.QuerySpec_GroupBy || clause == SqlNodeFields.QuerySpec_Having) {
        attr = linearFind(relation.attributes(), it -> name.equals(it.name()));
        if (attr != null) return attr;
      }
    }

    // dependent col-ref
    Relation outerRelation = relation;
    while (tableSourceOf(outerRelation) == null
        && (outerRelation = getOuterRelation(outerRelation)) != null) {
      attr = resolveAttribute(outerRelation, qualification, name);
      if (attr != null) return attr;
    }

    return null;
  }

  public static Attribute deRef(Attribute attribute) {
    final SqlNode expr = attribute.expr();
    return ExprKind.ColRef.isInstance(expr) ? resolveAttribute(expr) : null;
  }

  public static Attribute traceRef(Attribute attribute) {
    final Attribute ref = deRef(attribute);
    return ref == null ? attribute : traceRef(ref);
  }

  public static boolean isElementParam(ParamDesc param) {
    return any(param.modifiers(), it -> it.type() == ParamModifier.Type.ARRAY_ELEMENT || it.type() == ParamModifier.Type.TUPLE_ELEMENT);
  }

  public static boolean isCheckNull(ParamDesc param) {
    final ParamModifier.Type lastModifierType = tail(param.modifiers()).type();
    return lastModifierType == ParamModifier.Type.CHECK_NULL || lastModifierType == ParamModifier.Type.CHECK_NULL_NOT;
  }

  private static FieldKey<?> getClauseOfExpr(SqlNode expr) {
    assert SqlKind.Expr.isInstance(expr);

    SqlNode parent = expr.parent(), child = expr;
    while (SqlKind.Expr.isInstance(parent)) {
      child = parent;
      parent = parent.parent();
    }

    if (SqlKind.OrderItem.isInstance(parent)) return SqlNodeFields.Query_OrderBy;
    if (SqlKind.GroupItem.isInstance(parent)) return SqlNodeFields.QuerySpec_GroupBy;
    if (SqlKind.SelectItem.isInstance(parent)) return SqlNodeFields.QuerySpec_SelectItems;
    if (TableSourceKind.JoinedSource.isInstance(parent)) return TableSourceFields.Joined_On;
    if (nodeEquals(parent.$(SqlNodeFields.Query_Offset), child)) return SqlNodeFields.Query_Offset;
    if (nodeEquals(parent.$(SqlNodeFields.Query_Limit), child)) return SqlNodeFields.Query_Offset;
    if (nodeEquals(parent.$(SqlNodeFields.QuerySpec_Where), child)) return SqlNodeFields.QuerySpec_Where;
    if (nodeEquals(parent.$(SqlNodeFields.QuerySpec_Having), child)) return SqlNodeFields.QuerySpec_Having;

    return null;
  }
}
