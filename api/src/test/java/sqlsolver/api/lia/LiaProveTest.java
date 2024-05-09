package sqlsolver.api.lia;

import org.junit.jupiter.api.Test;
import sqlsolver.api.entry.Verification;
import sqlsolver.sql.calcite.CalciteSupport;
import sqlsolver.superopt.logic.VerificationResult;
import sqlsolver.superopt.substitution.Substitution;
import sqlsolver.superopt.substitution.SubstitutionBank;
import sqlsolver.superopt.substitution.SubstitutionSupport;
import sqlsolver.superopt.uexpr.UExprTranslationResult;
import sqlsolver.superopt.logic.LogicSupport;
import sqlsolver.superopt.uexpr.UExprSupport;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static sqlsolver.api.TestHelper.dataDir;

public class LiaProveTest {


  @Test
  void testLiaOnTPCCSparkRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.tpcc.spark.txt");
    int targetId = -1;
    int[] eqCases = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "tpcc");
  }

  @Test
  void testLiaOnTPCCCalciteRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.tpcc.calcite.txt");
    int targetId = -1;
    int[] eqCases = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19};
    final Set<Integer> timeouts = Set.of(18);
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "tpcc");
  }

  @Test
  void testLiaOnTPCHSparkRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.tpch.spark.txt");
    int targetId = -1;
    int[] eqCases = new int[]{1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18, 20, 21, 22};
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "tpch");
  }

  @Test
  void testLiaOnTPCHCalciteRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.tpch.calcite.txt");
    int targetId = 5;
    int[] eqCases = new int[]{};
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "tpch");
  }

  @Test
  void testLiaOnTPCHDinSqlRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.tpch.dinsql.txt");
    int targetId = -1;
    int[] eqCases = new int[]{4, 6, 9, 15, 18};
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "tpch");
  }

  @Test
  void testLiaOnTPCHSparkCalciteRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.tpch.spark.calcite.txt");
    int targetId = -1;
    int[] eqCases = new int[]{};
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "tpch");
  }

  @Test
  void testLiaOnJobSparkRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.job.spark.txt");
    int targetId = -1;
    long timeout = 1800;
    int[] eqCases = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113};
    // timeout cases may become EQ after timeout
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "job", timeout);
  }

  @Test
  void testLiaOnJobCalciteRules() throws IOException {
    Path testCasesPath = dataDir().resolve("prepared").resolve("rules.job.calcite.txt");
    int targetId = -1;
    int[] eqCases = new int[]{};
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "job");
  }

  @Test
  void testLiaOnCalciteRules() throws IOException {
    Path testCasesPath = dataDir().resolve("calcite").resolve("calcite_tests");
    int targetId = -1;
    int[] eqCases = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232};
    final Set<Integer> timeouts = Set.of();
    CalciteSupport.addUserDefinedFunction("UNIX_TIMESTAMP", 1);
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "calcite_test");
  }

  @Test
  void testLiaOnSparkRules() throws IOException {
    Path testCasesPath = dataDir().resolve("db_rule_instances").resolve("spark_tests");
    int targetId = -1;
    int[] eqCases = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 48, 49, 50, 52, 56, 57, 58, 59, 60, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 124, 125, 126};
    final Set<Integer> timeouts = Set.of();
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "calcite_test");
  }

  @Test
  void testLiaOnBuggyRules() throws IOException {
    Path testCasesPath = dataDir().resolve("db_rule_instances").resolve("buggy_tests");
    int targetId = -1;
    int[] eqCases = new int[]{1};
    final Set<Integer> timeouts = Set.of();
    LogicSupport.setDumpLiaFormulas(true);
    System.setOut(new PrintStream("log"));
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, "spider_pets_1");
  }

  @Test
  void testMultipleCases() {
    Path testCasesPath = dataDir().resolve("neighbor_result");
    try {
      final PrintStream stream = new PrintStream("log");
      System.setOut(stream);
      System.setErr(stream);
      final List<String> fileNames = new ArrayList<>();
      Files.walk(testCasesPath)
              .filter(Files::isRegularFile)
              .forEach(path -> fileNames.add(path.getFileName().toString()));
      final Set<String> haveTested = Set.of("network_1", "student_transcripts_tracking", "real_estate_properties", "flight_2");
      for (final String fileName : fileNames) {
        if (haveTested.contains(fileName)) continue;
        System.out.println("Handling case for: " + fileName);
        Path testCase = testCasesPath.resolve(fileName);
        int targetId = -1;
        int[] eqCases = new int[]{};
        final Set<Integer> timeouts = Set.of();
        testLiaOnRulesConcrete(testCase, targetId, eqCases, timeouts, "spider_" + fileName);
      }
    } catch (IOException ignored) {

    }

  }

  List<Integer> transferVerificationResult(List<VerificationResult> results) {
    final List<Integer> integers = new ArrayList<>();
    for (final VerificationResult result : results) {
      int integer = -2;
      switch (result) {
        case EQ -> integer = 1;
        case NEQ -> integer = 0;
        case UNKNOWN -> integer = -1;
        case TIMEOUT -> integer = -2;
      }
      integers.add(integer);
    }
    return integers;
  }

  @Test
  void testLiaOnSpiderRules() throws IOException {
    String[] appNames = new String[]{
            "spider_car_1",
            "spider_employee_hire_evaluation",
            "spider_tvshow",
            "spider_singer",
            "spider_course_teach", // instructor
            "spider_student_transcripts_tracking", // course
            "spider_wta_1",
            "spider_cre_Doc_Template_Mgt",
            "spider_poker_player",
            "spider_world_1",
            "spider_pets_1",
            "spider_flight_2", // no schema files
            "spider_concert_singer",
            "spider_orchestra",
            "spider_dog_kennels",
            "spider_network_1", // takes
            "spider_voter_1",
            "spider_museum_visit",
            "spider_battle_death",
            "spider_real_estate_properties"
    };

    Map<String, List<Integer>> sqlExecResults = new HashMap<String, List<Integer>>();
    for (String appName : appNames) {
      Path expectPath = dataDir().resolve("spider/expect").resolve(appName + ".expect.txt");
      sqlExecResults.put(appName + ".expect.txt", Files.readAllLines(expectPath).stream().map(Integer::parseInt).collect(Collectors.toList()));
    }

    Map<String, List<Integer>> expectExceptionCases = new HashMap<String, List<Integer>>();
    Map<String, List<Integer>> finalExceptionCases = new HashMap<String, List<Integer>>();
    Path expectExceptionPath = dataDir().resolve("spider").resolve("expect_error.txt");
    List<String> expectExceptions = Files.readAllLines(expectExceptionPath);
    for (String expectException : expectExceptions) {
      String[] parts = expectException.split("\\s+");
      String appName = parts[0];
      for (int i = 1; i < parts.length; i++) {
        expectExceptionCases.computeIfAbsent(appName, k -> new ArrayList<>()).add(Integer.valueOf(parts[i]));
      }
    }

    String targetApp = null;
    HashMap<String, List<Integer>> sqlsolverResultMap = new HashMap<>();
    try (FileWriter fileWriter1 = new FileWriter(dataDir().resolve("error.txt").toString())) {
      try (FileWriter fileWriter = new FileWriter(dataDir().resolve("unsound.txt").toString())) {
        for (String appName : appNames) {
          System.out.println(appName + " begin");
          if (targetApp != null && !targetApp.equals(appName))
            continue;
          Path testCasesPath = dataDir().resolve("spider/cases").resolve("rules." + appName + ".sql.txt");
          List<String> totalCase = Files.readAllLines(testCasesPath);
          List<String> sqlList0 = new ArrayList<>();
          List<String> sqlList1 = new ArrayList<>();
          String schema = Files.readString(dataDir().resolve("schemas").resolve(appName + ".base.schema.sql"));
          assert (totalCase.size() % 2 == 0);
          for (int i = 0; i < totalCase.size(); i++) {
            if (i % 2 == 0) {
              sqlList0.add(totalCase.get(i));
            } else {
              sqlList1.add(totalCase.get(i));
            }
          }
          List<VerificationResult> results = Verification.verify(sqlList0, sqlList1, schema);
          List<Integer> sqlsolverResult = transferVerificationResult(results);
          sqlsolverResultMap.put(appName, sqlsolverResult);
          System.out.println(appName + "\n" + sqlsolverResult);
          System.out.println(sqlExecResults.get(appName + ".expect.txt"));
          for (int i = 0; i < sqlsolverResult.size(); i++) {
            if (!sqlsolverResult.get(i).equals(sqlExecResults.get(appName + ".expect.txt").get(i)) && sqlsolverResult.get(i) >= 0) {
              System.out.println("mismatch at " + (i + 1));
              fileWriter.append(appName).append("\t").append(String.valueOf(i + 1)).append("\t").append(sqlList0.get(i)).append("\t").append(sqlList1.get(i)).append("\t")
                      .append(String.valueOf(sqlExecResults.get(appName + ".expect.txt").get(i))).append("\t")
                      .append(String.valueOf(sqlsolverResult.get(i))).append("\n");
            }
            if (sqlsolverResult.get(i) < 0) {
              finalExceptionCases.computeIfAbsent(appName, k -> new ArrayList<>()).add(i + 1);
              fileWriter1.append(appName).append("\t").append(String.valueOf(i + 1)).append("\t").append(sqlList0.get(i)).append("\t").append(sqlList1.get(i)).append("\t")
                      .append(String.valueOf(sqlExecResults.get(appName + ".expect.txt").get(i))).append("\t")
                      .append(String.valueOf(sqlsolverResult.get(i))).append("\n");
            }
          }
        }
      }
    }
  }

  void testLiaOnRulesConcrete(Path testCasesPath, int targetId, int[] eqCases, Set<Integer> timeouts, String appName) throws IOException {
    testLiaOnRulesConcrete(testCasesPath, targetId, eqCases, timeouts, appName, -1);
  }

  void testLiaOnRulesConcrete(Path testCasesPath, int targetId, int[] eqCases, Set<Integer> timeouts, String appName, long timeout) throws IOException {
    List<String> totalCase = Files.readAllLines(testCasesPath);
    List<String> sqlList0 = new ArrayList<>();
    List<String> sqlList1 = new ArrayList<>();
    String schema = Files.readString(dataDir().resolve("schemas").resolve(appName + ".base.schema.sql"));

    assert (totalCase.size() % 2 == 0);
    for (int i = 0; i < totalCase.size(); i++) {
      if (i % 2 == 0) {
        sqlList0.add(totalCase.get(i));
      } else {
        sqlList1.add(totalCase.get(i));
      }
    }

    assert (sqlList0.size() == sqlList1.size());

    for (int i = 0; i < sqlList0.size(); i++) {
      if (timeouts.contains(i + 1)) {
        sqlList0.set(i, "");
        sqlList1.set(i, "");
      }
    }

    List<VerificationResult> results = Verification.verify(sqlList0, sqlList1, schema, timeout);

    for (int t : timeouts) {
      results.set(t - 1, VerificationResult.UNKNOWN);
    }

    for (int eqCase : eqCases) {
      if (timeouts.contains(eqCase)) {
        System.out.println("warning: case " + eqCase + " becomes TIMEOUT!\n");
      } else if (results.get(eqCase - 1) != VerificationResult.EQ)
        System.out.println("warning: case " + eqCase + " becomes NEQ!\n");
    }

    System.out.println(results + "\n");

    int totalEqs = 0;

    for (VerificationResult result : results) {
      if (result == VerificationResult.EQ) totalEqs++;
    }

    System.out.println("Total EQs: " + totalEqs);
  }

  @Test
  void testLiaOnCraftRules() throws IOException {
    final Path ruleFilePath = dataDir().resolve("prepared").resolve("rules.test.txt");
    final SubstitutionBank rules = SubstitutionSupport.loadBank(ruleFilePath);
    int targetId = -1;
    int startId = -1;
    for (Substitution rule : rules.rules()) {
      rule.toString();
      if (targetId > 0 && rule.id() != targetId) continue;
      if (targetId < 0 && startId > 0 && rule.id() < startId) continue;
      final UExprTranslationResult uExprs =
              UExprSupport.translateToUExpr(rule, UExprSupport.UEXPR_FLAG_INTEGRITY_CONSTRAINT_REWRITE);
      if (targetId > 0) {
        System.out.println("Rewritten UExpressions: ");
        System.out.println("[[q0]](" + uExprs.sourceOutVar() + ") := " + uExprs.sourceExpr());
        System.out.println("[[q1]](" + uExprs.targetOutVar() + ") := " + uExprs.targetExpr());
        LogicSupport.setDumpLiaFormulas(true);
      }
      final VerificationResult result = LogicSupport.proveEqByLIAStar(rule);
      System.out.println("Rule id: " + rule.id() + " is " + result.toString());
    }
  }
}