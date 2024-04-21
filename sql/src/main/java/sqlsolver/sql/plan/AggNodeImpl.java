package sqlsolver.sql.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static sqlsolver.common.utils.Commons.coalesce;

class AggNodeImpl implements AggNode {
  private final boolean deduplicated;
  private final List<String> attrNames;
  private List<Expression> attrExprs;
  private final List<Expression> groupByExprs;
  private final Expression havingExpr;
  private String qualification;

  AggNodeImpl(
      boolean deduplicated,
      List<String> attrNames,
      List<Expression> attrExprs,
      List<Expression> groupByExprs,
      Expression havingExpr) {
    this.deduplicated = deduplicated;
    this.attrNames = Collections.unmodifiableList(attrNames);
    this.attrExprs = Collections.unmodifiableList(attrExprs);
    this.groupByExprs = Collections.unmodifiableList(coalesce(groupByExprs, emptyList()));
    this.havingExpr = havingExpr;
  }

  @Override
  public boolean deduplicated() {
    return deduplicated;
  }

  @Override
  public List<String> attrNames() {
    return attrNames;
  }

  @Override
  public List<Expression> attrExprs() {
    return attrExprs;
  }

  public void setAttrExprs(int index, Expression e) {
    ArrayList<Expression> tmp = new ArrayList<>();
    tmp.addAll(attrExprs);
    tmp.set(index, e);
    attrExprs = Collections.unmodifiableList(tmp);
  }

  @Override
  public List<Expression> groupByExprs() {
    return groupByExprs;
  }

  @Override
  public Expression havingExpr() {
    return havingExpr;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

}
