package sqlsolver.superopt.logic;

import com.microsoft.z3.*;
import sqlsolver.sql.plan.Value;
import sqlsolver.sql.schema.Schema;
import sqlsolver.superopt.uexpr.UTerm;
import sqlsolver.superopt.uexpr.UVar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static sqlsolver.superopt.logic.SetTranslator.TranslatorContext;

/**
 * Decide whether two terms are equal
 * when summations are always within squash/negation.
 */
public class SetSolver {
  private final UTerm term1, term2;
  private final Map<UVar, List<Value>> varSchema;
  private final Schema tableSchema;

  private static final List<SetTranslator.Config> TRANSLATOR_CONFIGS;

  static {
    TRANSLATOR_CONFIGS = new ArrayList<>();
    TRANSLATOR_CONFIGS.add(new SetTranslator.Config(SetTranslator.Config.VAR_MODE_COLUMN_AS_VAR, false));
    TRANSLATOR_CONFIGS.add(new SetTranslator.Config(SetTranslator.Config.VAR_MODE_COLUMN_AS_VAR, true));
  }

  public SetSolver(UTerm term1, UTerm term2, Map<UVar, List<Value>> varSchema, Schema tableSchema) {
    this.term1 = term1;
    this.term2 = term2;
    this.varSchema = varSchema;
    this.tableSchema = tableSchema;
  }

  /**
   * Decide equivalence of preset two U-expressions
   * by deciding satisfiability of a FOL formula.
   * Summations should always be within squash/negation,
   * or the solver does not know the equivalence.
   */
  public VerificationResult proveEq() {
    int count = 0;
    for (SetTranslator.Config config : TRANSLATOR_CONFIGS) {
      final VerificationResult result = proveEq(config);
      if (LogicSupport.dumpLiaFormulas) {
        System.out.println("Set solver result (config #" + (++count) + "): " + result);
      }
      if (result == VerificationResult.EQ)
        return VerificationResult.EQ;
      else if (result == VerificationResult.NEQ)
        return VerificationResult.NEQ;
    }
    return VerificationResult.UNKNOWN;
  }

  private VerificationResult proveEq(SetTranslator.Config config) {
    // TODO: add IC if necessary
    try (final Context z3 = new Context()) {
      try {
        // translate to FOL
        final TranslatorContext ctx = new TranslatorContext(z3, varSchema, tableSchema);
        final ArithExpr arithExp1 = SetTranslator.mk(config, ctx, term1).translate();
        final ArithExpr arithExp2 = SetTranslator.mk(config, ctx, term2).translate();
        if (LogicSupport.dumpLiaFormulas) {
          System.out.println("Term 1:");
          System.out.println(term1.toPrettyString());
          System.out.println("Term 2:");
          System.out.println(term2.toPrettyString());
          System.out.println("FOL formula 1:");
          System.out.println(arithExp1);
          System.out.println("FOL formula 2:");
          System.out.println(arithExp2);
        }
        // solve
        final Solver solver = ctx.getSolver();
        solver.add(z3.mkNot(z3.mkEq(arithExp1, arithExp2)));
        return trResult(solver.check());
      } catch (Throwable e) {
        return VerificationResult.UNKNOWN;
      }
    }
  }

  private VerificationResult trResult(Status res) {
    if (res == Status.UNSATISFIABLE) return VerificationResult.EQ;
    else if (res == Status.SATISFIABLE) return VerificationResult.NEQ;
    else return VerificationResult.UNKNOWN;
  }
}
