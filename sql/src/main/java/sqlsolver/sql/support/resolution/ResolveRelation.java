package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.*;
import sqlsolver.sql.schema.Table;
import sqlsolver.sql.ast.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static sqlsolver.common.utils.ListSupport.flatMap;
import static sqlsolver.common.utils.ListSupport.map;
import static sqlsolver.sql.SqlSupport.mkColRef;
import static sqlsolver.sql.SqlSupport.selectItemNameOf;

class ResolveRelation implements SqlVisitor {
  private final SqlContext ctx;
  private RelationsImpl relations;

  ResolveRelation(SqlContext ctx) {
    this.ctx = requireNonNull(ctx);
  }

  void resolve(RelationsImpl relations) {
    this.relations = relations;

    final SqlNode rootNode = SqlNode.mk(ctx, ctx.root());
    rootNode.accept(new SetupInfo());
    rootNode.accept(new SetupInput());
    rootNode.accept(new SetupAttrs());
  }

  private class SetupInfo implements SqlVisitor {
    @Override
    public boolean enter(SqlNode node) {
      if (Relation.isRelationRoot(node)) {
        relations.bindRelationRoot(node);
      }
      return true;
    }
  }

  private class SetupInput implements SqlVisitor {
    @Override
    public boolean enterSimpleTableSource(SqlNode tableSource) {
      final RelationImpl relation = relations.enclosingRelationOf(tableSource);
      final RelationImpl outerRelation = ((RelationImpl) ResolutionSupport.getOuterRelation(relation));
      outerRelation.addInput(relation);
      return false;
    }

    @Override
    public boolean enterDerivedTableSource(SqlNode tableSource) {
      final RelationImpl relation = relations.enclosingRelationOf(tableSource.$(TableSourceFields.Derived_Subquery));
      final RelationImpl outerRelation = relations.enclosingRelationOf(tableSource);
      outerRelation.addInput(relation);
      return true;
    }

    @Override
    public void leaveSetOp(SqlNode union) {
      final RelationImpl lhs = relations.enclosingRelationOf(union.$(SqlNodeFields.SetOp_Left));
      final RelationImpl rhs = relations.enclosingRelationOf(union.$(SqlNodeFields.SetOp_Right));
      final RelationImpl outerRelation = relations.enclosingRelationOf(union);
      outerRelation.addInput(lhs);
      outerRelation.addInput(rhs);
    }
  }

  private class SetupAttrs implements SqlVisitor {
    @Override
    public void leaveSimpleTableSource(SqlNode simpleTableSource) {
      final RelationImpl relation = relations.enclosingRelationOf(simpleTableSource);
      final Table table = ctx.schema().table(simpleTableSource.$(TableSourceFields.Simple_Table).$(SqlNodeFields.TableName_Table));
      final List<Attribute> attrs = map(table.columns(), c -> new ColumnAttribute(relation, c));
      relation.setAttributes(attrs);
    }

    @Override
    public void leaveQuery(SqlNode query) {
      final RelationImpl rel = relations.enclosingRelationOf(query);
      final SqlNode body = query.$(SqlNodeFields.Query_Body);
      if (SqlKind.QuerySpec.isInstance(body)) {
        final SqlNodes items = body.$(SqlNodeFields.QuerySpec_SelectItems);
        final List<Attribute> attributes = new ArrayList<>(items.size());
        for (SqlNode item : items) {
          if (ExprKind.Wildcard.isInstance(item.$(SqlNodeFields.SelectItem_Expr)))
            expandWildcard(item.$(SqlNodeFields.SelectItem_Expr), rel, attributes);
          else
            attributes.add(new ExprAttribute(rel, selectItemNameOf(item), item.$(SqlNodeFields.SelectItem_Expr)));
        }

        rel.setAttributes(attributes);

      } else if (SqlKind.SetOp.isInstance(body)) {
        final List<Relation> inputs = rel.inputs();
        rel.setAttributes(flatMap(inputs, Relation::attributes));

      } else assert false;
    }

    private void expandWildcard(SqlNode wildcard, Relation owner, List<Attribute> dest) {
      final SqlNode tableName = wildcard.$(ExprFields.Wildcard_Table);
      final String qualification = tableName == null ? null : tableName.$(SqlNodeFields.TableName_Table);
      final SqlContext ctx = wildcard.context();

      for (Relation inputRel : owner.inputs()) {
        if (qualification == null || qualification.equals(inputRel.qualification())) {
          for (Attribute inputAttr : inputRel.attributes()) {
            // A shadow node
            final SqlNode colRef = mkColRef(ctx, inputRel.qualification(), inputAttr.name());
            ctx.setParentOf(colRef.nodeId(), wildcard.nodeId());

            dest.add(new ExprAttribute(owner, inputAttr.name(), colRef));
          }
        }
      }
    }
  }
}
