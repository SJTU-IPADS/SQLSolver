package sqlsolver.superopt.logic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Expr;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.uexpr.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static sqlsolver.superopt.util.Z3Support.assembleFunction;
import static sqlsolver.superopt.util.Z3Support.mkExprByTypeString;

/**
 * A set translator that translates tuple columns into vars.
 */
public class ColumnVarSetTranslator extends SetTranslator {
  public ColumnVarSetTranslator(Config config, TranslatorContext ctx, UTerm term) {
    super(config, ctx, term);
  }

  @Override
  protected Expr trTable(UTable table, Function<UVarTerm, Expr> trVarTerm) {
    // translate to function (e.g. emp(x0, x1, ..., x8))
    final String tableName = table.tableName().toString();
    final UVar tuple = table.var();
    final List<Value> schema = ctx.varSchema.get(tuple);
    final int arity = schema.size();
    final List<Expr> columns = new ArrayList<>();
    final List<PredefinedFunctions.ValueType> columnTypes = new ArrayList<>();
    for (int i = 0; i < arity; i++) {
      final String columnName = CalciteSupport.indexToColumnName(i);
      final UVar argVar = UVar.mkProj(UName.mk(columnName), tuple.copy());
      final UVarTerm vt = UVarTerm.mk(argVar);
      columns.add(trVarTerm.apply(vt));
      final String columnType = schema.get(i).type();
      columnTypes.add(toValueType(columnType));
    }
    return assembleFunction(tableName, columns, PredefinedFunctions.ValueType.INT, columnTypes, ctx.z3, ctx.funcs);
  }

  @Override
  protected Expr trVarTerm(UVarTerm vt) {
    return ctx.termVarMap.computeIfAbsent(vt, t -> newVar(vt));
  }

  private Expr newVar(UVarTerm vt) {
    // existing mapping
    final Expr record = ctx.termVarMap.get(vt);
    if (record != null) return record;
    // new mapping
    // checks
    final UVar var = vt.var();
    if (var.kind() != UVar.VarKind.PROJ)
      throw new UnsupportedOperationException("only accepts PROJ vars");
    assert var.args().length == 1;
    // get var type info
    final int index = CalciteSupport.columnNameToIndex(var.name().toString());
    final UVar tuple = var.args()[0];
    final List<Value> schema = ctx.varSchema.get(tuple);
    assert schema != null;
    final String varType = schema.get(index).type();
    // construct new z3 var according to type info
    final Expr exp = mkExprByTypeString(ctx.varNameSeq.next(), varType, ctx.z3);
    if (Value.TYPE_NAT.equals(varType)) {
      ctx.solver.add(ctx.z3.mkLe(ctx.z3.mkInt(0), (ArithExpr) exp));
    }
    return exp;
  }
}
