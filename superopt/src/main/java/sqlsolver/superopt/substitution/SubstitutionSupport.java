package sqlsolver.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sqlsolver.sql.plan.PlanContext;
import sqlsolver.superopt.logic.LogicSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SubstitutionSupport {
  public static SubstitutionBank loadBank(Path path) throws IOException {
    final SubstitutionBank bank = new SubstitutionBankImpl();
    final List<String> lines = Files.readAllLines(path);

    int lineNum = 0;
    for (String line : lines) {
      ++lineNum;
      try {
        if (line.isEmpty() || !Character.isLetter(line.charAt(0))) continue;

        final Substitution rule = Substitution.parse(line);
        bank.add(rule);
        rule.setId(lineNum);

      } catch (Throwable ex) {
        System.err.println("failed to add rule: " + line);
        if (LogicSupport.dumpLiaFormulas)
          ex.printStackTrace();
        throw ex;
      }
    }

    return bank;
  }

  public static SubstitutionBank reduceBank(SubstitutionBank bank) {
    return new ReduceRuleBank(bank).reduce();
  }

  public static Pair<PlanContext, PlanContext> translateAsPlan(Substitution rule) {
    return new PlanTranslator(rule).translate();
  }

  public static Pair<PlanContext, PlanContext> translateAsPlan2(Substitution rule) {
    return new PlanTranslator2(rule).translate();
  }

  public static SubstitutionTranslatorResult translateAsSubstitution(PlanContext source, PlanContext target) {
    return new SubstitutionTranslator(source, target).translate();
  }
}
