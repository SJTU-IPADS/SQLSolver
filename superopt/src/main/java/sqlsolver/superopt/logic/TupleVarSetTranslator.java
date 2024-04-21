package sqlsolver.superopt.logic;

import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Sort;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.sql.plan.Value;
import sqlsolver.superopt.uexpr.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A set translator that translates tuples into vars.
 */
public class TupleVarSetTranslator extends SetTranslator {
  // TODO: support NULL
  private final Sort SORT_TUPLE;
  private final NameSequence schemaNameSeq;
  private final Map<List<Value>, String> schemaNameMap;

  public TupleVarSetTranslator(Config config, TranslatorContext ctx, UTerm term) {
    super(config, ctx, term);
    SORT_TUPLE = ctx.z3.mkUninterpretedSort("Tuple");
    schemaNameSeq = NameSequence.mkIndexed("schema", 0);
    schemaNameMap = new HashMap<>();
  }

  @Override
  protected Expr trTable(UTable table, Function<UVarTerm, Expr> trVarTerm) {
    // translate to function (e.g. emp(x))
    final String tableName = table.tableName().toString();
    final UVar tuple = table.var();
    final UVarTerm vt = UVarTerm.mk(tuple.copy());
    final Expr tupleExpr = ctx.termVarMap.computeIfAbsent(vt, t -> ctx.newVar(SORT_TUPLE));
    FuncDecl funcDecl = ctx.funcs.get(tableName);
    if (funcDecl == null) {
      // create function definition upon first use
      funcDecl = ctx.z3.mkFuncDecl(tableName, SORT_TUPLE, ctx.z3.mkIntSort());
      ctx.funcs.put(tableName, funcDecl);
    }
    return ctx.z3.mkApp(funcDecl, tupleExpr);
  }

  @Override
  protected Expr trVarTerm(UVarTerm vt) {
    // map tuple to var
    final UVar projVar = vt.var();
    assert projVar.kind() == UVar.VarKind.PROJ && projVar.args().length == 1;
    final UVar tuple = projVar.args()[0];
    final Expr tupleExpr = ctx.termVarMap.computeIfAbsent(UVarTerm.mk(tuple), t -> ctx.newVar(SORT_TUPLE));
    // $i(x) -> function
    final String colName = projVar.name().toString();
    final List<Value> schema = ctx.varSchema.get(tuple);
    final String colType = schema.get(CalciteSupport.columnNameToIndex(colName)).type();
    final String funcNamePrefix = schemaNameMap.computeIfAbsent(schema, s -> schemaNameSeq.next());
    final String funcName = funcNamePrefix + colName;
    FuncDecl func = ctx.funcs.get(funcName);
    if (func == null) {
      // create function definition upon first use
      func = ctx.z3.mkFuncDecl(funcName, SORT_TUPLE, toValueType(colType).getSort(ctx.z3));
      ctx.funcs.put(funcName, func);
    }
    return ctx.z3.mkApp(func, tupleExpr);
  }
}
