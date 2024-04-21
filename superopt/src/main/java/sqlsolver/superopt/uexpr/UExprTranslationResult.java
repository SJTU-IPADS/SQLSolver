package sqlsolver.superopt.uexpr;

import sqlsolver.superopt.fragment.Symbol;
import sqlsolver.superopt.substitution.Substitution;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sqlsolver.common.utils.IterableSupport.linearFind;

public class UExprTranslationResult {
  final Substitution rule;
  final Map<Symbol, TableDesc> symToTable;
  final Map<Symbol, AttrsDesc> symToAttrs;
  final Map<Symbol, PredDesc> symToPred;
  final Map<Symbol, FuncDesc> symToFunc;
  final Map<UVar, SchemaDesc> varToSchema;
  final Map<Symbol, SchemaDesc> symToSchema;
  UTerm srcExpr, tgtExpr;
  UVar srcOutVar, tgtOutVar;

  public UExprTranslationResult(Substitution rule) {
    this.rule = rule;
    this.symToTable = new HashMap<>(8);
    this.symToAttrs = new HashMap<>(16);
    this.symToPred = new HashMap<>(8);
    this.symToFunc = new HashMap<>(8);
    this.varToSchema = new HashMap<>(8);
    this.symToSchema = new HashMap<>(8);
  }

  void setVarSchema(UVar var, int... components) {
    varToSchema.put(var, new SchemaDesc(components));
  }

  public void setVarSchema(UVar var, SchemaDesc... schema) {
    assert schema.length != 0;
    if (schema.length == 1) varToSchema.put(var, schema[0]);
    else varToSchema.put(var, new SchemaDesc(schema));
  }

  void setSymSchema(Symbol sym, int... components) {
    symToSchema.put(sym, new SchemaDesc(components));
  }

  void setSymSchema(Symbol sym, SchemaDesc... schema) {
    if (schema.length == 1) symToSchema.put(sym, schema[0]);
    else symToSchema.put(sym, new SchemaDesc(schema));
  }

  public Collection<TableDesc> tableTerms() {
    return symToTable.values();
  }

  public Substitution rule() {
    return rule;
  }

  public UTerm sourceExpr() {
    return srcExpr;
  }

  public UTerm targetExpr() {
    return tgtExpr;
  }

  public UVar sourceOutVar() {
    return srcOutVar;
  }

  public UVar targetOutVar() {
    return tgtOutVar;
  }

  public void setSrcExpr(UTerm srcExpr) {
    this.srcExpr = srcExpr;
  }

  public void setTgtExpr(UTerm tgtExpr) {
    this.tgtExpr = tgtExpr;
  }

  public void setSrcOutVar(UVar srcOutVar) {
    this.srcOutVar = srcOutVar;
  }

  public void setTgtOutVar(UVar tgtOutVar) {
    this.tgtOutVar = tgtOutVar;
  }

  public SchemaDesc schemaOf(UVar var) {
    return varToSchema.get(var);
  }

  public SchemaDesc schemaOf(Symbol sym) {
    return symToSchema.get(sym);
  }

  public TableDesc tableDescOf(Symbol sym) {
    return symToTable.get(sym);
  }

  public AttrsDesc attrsDescOf(Symbol sym) {
    return symToAttrs.get(sym);
  }

  public PredDesc predDescOf(Symbol sym) {
    return symToPred.get(sym);
  }

  public FuncDesc funcDescOf(Symbol sym) {
    return symToFunc.get(sym);
  }

  public String tableNameOf(Symbol sym) {
    final TableDesc tableDesc = symToTable.get(sym);
    return tableDesc == null ? null : tableDesc.term().tableName().toString();
  }

  public String attrsNameOf(Symbol sym) {
    final AttrsDesc attrsDesc = symToAttrs.get(sym);
    return attrsDesc == null ? null : attrsDesc.name().toString();
  }

  public String predNameOf(Symbol sym) {
    final PredDesc predDesc = symToPred.get(sym);
    return predDesc == null ? null : predDesc.name().toString();
  }

  public String funcNameOf(Symbol sym) {
    final FuncDesc funcDesc = symToFunc.get(sym);
    return funcDesc == null ? null : funcDesc.name().toString();
  }

  public void alignOutVar() {
    if (srcOutVar == null || tgtOutVar == null) return;
    if (srcOutVar.kind() != tgtOutVar.kind() || srcOutVar.args().length != tgtOutVar.args().length) return;
    if (srcOutVar.equals(tgtOutVar)) return;

    for (int i = 0, bound = srcOutVar.args().length; i < bound; ++i) {
      final SchemaDesc srcSchemaDesc = varToSchema.get(srcOutVar.args()[i]);
      final SchemaDesc tgtSchemaDesc = varToSchema.get(tgtOutVar.args()[i]);
      if (srcSchemaDesc == null || tgtSchemaDesc == null) return;

      // Sym tends to be schema sym, or may be other kind
      final List<Symbol> srcSchemaSyms = rule.constraints().sourceSymbols().symbolsOf(Symbol.Kind.SCHEMA);
      final List<Symbol> tgtSchemaSyms = rule.constraints().targetSymbols().symbolsOf(Symbol.Kind.SCHEMA);
      final Symbol srcSchemaSym = linearFind(srcSchemaSyms, s -> schemaOf(s).equals(srcSchemaDesc));
      final Symbol tgtSchemaSym = linearFind(tgtSchemaSyms, s -> schemaOf(s).equals(tgtSchemaDesc));
      if (srcSchemaSym == null || tgtSchemaSym == null) return;

      // if (!rule.constraints().isEq(srcSchemaSym, tgtSchemaSym)) return;
      final Symbol instantiationSchema = rule.constraints().instantiationOf(tgtSchemaSym);
      if (!rule.constraints().isEq(srcSchemaSym, instantiationSchema)) return;
    }

    // Out vars have eq schemas but with different schema id, then we try to align them.
    // This is used to support translating concrete plan to template in `SubstitutionTranslator`,
    // where we may not find aligned schema instantiations and fail to have aligned out vars.
    // (inspired by calcite rule #128)
    tgtExpr.replaceVarInplace(tgtOutVar, srcOutVar, false);
    tgtOutVar = srcOutVar.copy();
  }
}
