package sqlsolver.sql.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ProjNodeImpl implements ProjNode {
  private boolean deduplicated;
  private final List<String> attrNames;
  private List<Expression> expressions;
  private String qualification;

  ProjNodeImpl(boolean deduplicated, List<String> attrNames, List<Expression> expressions) {
    this.deduplicated = deduplicated;
    this.attrNames = Collections.unmodifiableList(attrNames);
    this.expressions = Collections.unmodifiableList(expressions);
  }

  @Override
  public boolean deduplicated() {
    return deduplicated;
  }

  @Override
  public void setDeduplicated(boolean deduplicated) {
    this.deduplicated = deduplicated;
  }

  @Override
  public List<String> attrNames() {
    return attrNames;
  }

  @Override
  public List<Expression> attrExprs() {
    return expressions;
  }

  public void setAttrExprs(int index, Expression e) {
    ArrayList<Expression> tmp = new ArrayList<>();
    tmp.addAll(expressions);
    tmp.set(index, e);
    expressions = Collections.unmodifiableList(tmp);
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
