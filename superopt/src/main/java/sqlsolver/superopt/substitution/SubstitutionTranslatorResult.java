package sqlsolver.superopt.substitution;

import sqlsolver.sql.plan.Expression;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.superopt.fragment.AggFuncKind;
import sqlsolver.superopt.fragment.Symbol;

import java.util.HashMap;
import java.util.Map;

public class SubstitutionTranslatorResult {
  public final PlanContext src, tgt;
  final Map<Symbol, Expression> concretePreds; // p -> Expr
  final Map<Symbol, Object> specialProjAttrs; // e.g. SELECT 1 ...

  final Map<Symbol, AggFuncKind> concreteAggFuncs; // f -> SUM()
  Substitution rule;
  String error;

  public SubstitutionTranslatorResult(PlanContext src, PlanContext tgt) {
    this.src = src;
    this.tgt = tgt;
    this.concretePreds = new HashMap<>();
    this.specialProjAttrs = new HashMap<>();
    this.concreteAggFuncs = new HashMap<>();
  }

  public void setConcretePred(Symbol pred, Expression predExpr) {
    concretePreds.put(pred, predExpr);
  }

  public Expression getConcretePred(Symbol pred) {
    return concretePreds.get(pred);
  }

  public void setSpecialProjAttrs(Symbol attrs, Object expr) {
    specialProjAttrs.put(attrs, expr);
  }

  public Object getSpecialProjAttrs(Symbol attrs) {
    return specialProjAttrs.get(attrs);
  }

  public void setConcreteAggFunc(Symbol func, AggFuncKind funcKind) {
    concreteAggFuncs.put(func, funcKind);
  }

  public AggFuncKind getConcreteAggFunc(Symbol func) {
    return concreteAggFuncs.get(func);
  }

  public Substitution rule() {
    return rule;
  }

  public void setRule(Substitution rule) {
    this.rule = rule;
  }

  public String error() {
    return error;
  }
}
