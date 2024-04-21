package sqlsolver.superopt.uexpr;

import org.apache.calcite.rel.RelNode;
import sqlsolver.common.utils.SetSupport;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UExprConcreteTranslationResult {
  final RelNode p0, p1;

  final Schema schema;
  UTerm srcExpr, tgtExpr;
  UVar srcOutVar, tgtOutVar;
  final Map<UVar, List<Value>> srcTupleVarSchemas;
  final Map<UVar, List<Value>> tgtTupleVarSchemas;

  public UExprConcreteTranslationResult(RelNode p0, RelNode p1, Schema schema) {
    this.p0 = p0;
    this.p1 = p1;
    this.schema = schema;
    this.srcTupleVarSchemas = new HashMap<>();
    this.tgtTupleVarSchemas = new HashMap<>();
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

  /**
   * Get schemas of all tuples.
   * Assume that each common tuple between source and target has the same schema
   * on source and target side.
   */
  public Map<UVar, List<Value>> getTupleVarSchemas() {
    final Map<UVar, List<Value>> result = new HashMap<>();
    for (Map.Entry<UVar, List<Value>> entry : SetSupport.union(srcTupleVarSchemas.entrySet(), tgtTupleVarSchemas.entrySet())) {
      // deep copy each column in schema
      final UVar var = entry.getKey().copy();
      final List<Value> schema = new ArrayList<>();
      for (Value column : entry.getValue()) {
        schema.add(column.copy());
      }
      // check if schema already exists in the result map
      final List<Value> schema0 = result.get(var);
      if (schema0 == null) {
        // not exists; enter the schema
        result.put(var, schema);
        continue;
      }
      // if different from the existing schema, inequivalent
      final List<Value> finalSchema = CalciteSupport.mergeTwoValueLists(schema, schema0);
      // TODO: type checking
      //assert finalSchema != null;
      if (finalSchema != null)
        result.put(var, finalSchema);
    }
    return result;
  }

  public void setSrcTupleVarSchema(UVar var, List<Value> schema) {
    srcTupleVarSchemas.put(var, schema);
  }

  public List<Value> srcTupleVarSchemaOf(UVar var) {
    return srcTupleVarSchemas.get(var);
  }

  public void setTgtTupleVarSchema(UVar var, List<Value> schema) {
    tgtTupleVarSchemas.put(var, schema);
  }

  public List<Value> tgtTupleVarSchemaOf(UVar var) {
    return tgtTupleVarSchemas.get(var);
  }

  public Map<UVar, List<Value>> getSrcSchema() {
    return srcTupleVarSchemas;
  }

  public Map<UVar, List<Value>> getTgtSchema() {
    return tgtTupleVarSchemas;
  }

  public void alignOutVar(UVar freshOutVar) {
    // Invariant: out var is BASE type
    assert srcOutVar.is(UVar.VarKind.BASE) && tgtOutVar.is(UVar.VarKind.BASE);

    tgtExpr.replaceVarInplace(tgtOutVar, freshOutVar, false);
    tgtTupleVarSchemas.put(freshOutVar, tgtTupleVarSchemas.get(tgtOutVar));
    tgtTupleVarSchemas.remove(tgtOutVar);
    tgtOutVar = freshOutVar.copy();

    srcExpr.replaceVarInplace(srcOutVar, freshOutVar, false);
    srcTupleVarSchemas.put(freshOutVar, srcTupleVarSchemas.get(srcOutVar));
    srcTupleVarSchemas.remove(srcOutVar);
    srcOutVar = freshOutVar.copy();
  }

  public void alignOutSchema() {
    // srcOutVar and tgtOutVar should be aligned
    assert srcOutVar.is(UVar.VarKind.BASE) && srcOutVar.equals(tgtOutVar);
    final List<Value> srcOutVarSchema = srcTupleVarSchemas.get(srcOutVar);
    final List<Value> tgtOutVarSchema = tgtTupleVarSchemas.get(tgtOutVar);
    if (srcOutVarSchema.size() != tgtOutVarSchema.size())
      return; // Hack for some cases with VALUES, which has different length of schemas
    // TODO: should be uncomment after rewrite of uexpr concrete translation
    // for (var schemaPair : zip(srcOutVarSchema, tgtOutVarSchema)) {
    //   final UVar srcOutProjVar = mkProjVarForOutVar(schemaPair.getLeft(), false);
    //   final UVar tgtOutProjVar = mkProjVarForOutVar(schemaPair.getRight(), true);
    //   if (!srcOutProjVar.equals(tgtOutProjVar)) {
    //     tgtExpr.replaceVarInplace(tgtOutProjVar, srcOutProjVar, false);
    //   }
    // }
  }

  // TODO: should be uncomment after rewrite of uexpr concrete translation
  // private UVar mkProjVarForOutVar(Value value, boolean isTarget) {
  //   final Column column = tryResolveColumn(isTarget ? p1 : p0, value);
  //   final UName projFullName = UName.mk(getFullName((column != null) ? column : value));
  //   return UVar.mkProj(projFullName, isTarget ? tgtOutVar : srcOutVar);
  // }
}
