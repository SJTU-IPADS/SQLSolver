package sqlsolver.sql.plan;

import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlNodeFields;
import sqlsolver.sql.support.locator.LocatorSupport;
import sqlsolver.sql.ast.SqlNodes;
import sqlsolver.sql.ast.*;

import java.util.List;

import static sqlsolver.sql.SqlSupport.copyAst;
import static sqlsolver.sql.support.locator.LocatorSupport.gatherColRefs;

public class ExpressionImpl implements Expression {
  private SqlNode template;
  private final List<SqlNode> internalRefs;
  private final List<SqlNode> colRefs;

  ExpressionImpl(SqlNode ast) {
    final SqlContext tempCtx = SqlContext.mk(8);
    this.template = SqlSupport.copyAst(ast, tempCtx);
    this.internalRefs = SqlNodes.mk(tempCtx, LocatorSupport.gatherColRefs(template));
    this.colRefs = SqlNodes.mk(ast.context(), LocatorSupport.gatherColRefs(ast));
    // To make a template, we extract all the col-refs, and replace the
    // interpolate "#.#" to the original position.
    // e.g., "t.x > 10" becomes "#.# > 10"
    for (SqlNode colRef : internalRefs) putPlaceholder(colRef);
  }

  ExpressionImpl(SqlNode ast, List<SqlNode> colRefs) {
    this.template = ast;
    this.colRefs = colRefs;
    this.internalRefs = colRefs;
  }

  private ExpressionImpl(SqlNode ast, List<SqlNode> internalRefs, List<SqlNode> colRefs) {
    this.template = ast;
    this.internalRefs = internalRefs;
    this.colRefs = colRefs;
  }

  @Override
  public SqlNode template() {
    return template;
  }

  @Override
  public List<SqlNode> colRefs() {
    return colRefs;
  }

  @Override
  public List<SqlNode> internalRefs() {
    return internalRefs;
  }

  public Expression setTemplate(SqlNode ast) {
    template = ast;
    return this;
  }

  @Override
  public SqlNode interpolate(SqlContext ctx, Values values) {
    if (internalRefs.isEmpty() && (values == null || values.isEmpty()))
      return SqlSupport.copyAst(template, ctx);

    if (values == null || internalRefs.size() != values.size())
      throw new PlanException("mismatched # of values during interpolation");

    for (int i = 0, bound = internalRefs.size(); i < bound; i++) {
      final SqlNode colName = internalRefs.get(i).$(ExprFields.ColRef_ColName);
      final Value value = values.get(i);
      colName.$(SqlNodeFields.ColName_Table, value.qualification());
      colName.$(SqlNodeFields.ColName_Col, value.name());
    }

    final SqlNode newAst = SqlSupport.copyAst(template, ctx);
    for (SqlNode colRef : internalRefs) putPlaceholder(colRef);
    return newAst;
  }

  @Override
  public Expression copy() {
    return new ExpressionImpl(template, internalRefs, colRefs);
  }

  @Override
  public String toString() {
    return template.toString();
  }

  private void putPlaceholder(SqlNode colRef) {
    final SqlNode colName = colRef.$(ExprFields.ColRef_ColName);
    colName.$(SqlNodeFields.ColName_Table, PlanSupport.PLACEHOLDER_NAME);
    colName.$(SqlNodeFields.ColName_Col, PlanSupport.PLACEHOLDER_NAME);
  }
}
