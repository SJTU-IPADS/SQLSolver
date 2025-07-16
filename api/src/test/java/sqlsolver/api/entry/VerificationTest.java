package sqlsolver.api.entry;

import org.junit.jupiter.api.Test;
import sqlsolver.api.TestHelper;
import sqlsolver.superopt.logic.LogicSupport;
import sqlsolver.superopt.logic.VerificationResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationTest {

  @Test
  void test() {
    final String schema = "CREATE TABLE dept\n" +
            "(\n" +
            "    deptno int not null,\n" +
            "    name   varchar(10)     not null,\n" +
            "    primary key (deptno)\n" +
            ");";
    final String sql0 = "SELECT dept.deptno FROM dept AS dept WHERE dept.deptno = 7 AND dept.deptno = 8";
    final String sql1 = "SELECT dept.deptno FROM dept WHERE dept.deptno = 7 AND dept.deptno = 8";

    LogicSupport.dumpLiaFormulas = true;
    final VerificationResult result = Verification.verify(sql0, sql1, schema);
    assertEquals(result.toString(), "EQ");
  }

  @Test
  void testList() {
    final List<String> lines1 = new ArrayList<>();
    final List<String> lines2 = new ArrayList<>();
    lines1.add("SELECT i, j FROM a");
    lines2.add("SELECT T.COL1, T.COL2 FROM (SELECT i AS COL1, j AS COL2 FROM a) AS T");
    lines1.add("SELECT x, y FROM b");
    lines2.add("SELECT T.COL1, T.COL2 FROM (SELECT x AS COL1, y AS COL2 FROM b) AS T");
    lines1.add("SELECT p, q FROM d WHERE p IN (10, 20)");
    lines2.add("SELECT p, q FROM d WHERE p = 10 OR p = 20");

    final List<VerificationResult> results = Verification.verify(lines1, lines2, TestHelper.TEST_SCHEMA);
    for (VerificationResult result : results) {
      assertEquals(result.toString(), "EQ");
    }
  }
}
