package sqlsolver.sql.support.action;

import org.junit.jupiter.api.Test;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NormalizationTest {
  @Test
  void testRemoveBoolConstant() {
    {
      final SqlNode sql = TestHelper.parseSql("select a from t where 1=1");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t`", sql.toString());
    }
    {
      final SqlNode sql = TestHelper.parseSql("select a from t where true and b");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t` WHERE `b`", sql.toString());
    }
    {
      final SqlNode sql = TestHelper.parseSql("select a from t where 1+1 = 2 and (c or 1 between 0 and 2)");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t` WHERE `c`", sql.toString());
    }
    {
      final SqlNode sql =
          TestHelper.parseSql("select a from t where now() = 0 or rand() = 10 or field(k, 1, 2) = 3");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t` WHERE RAND() = 10 OR FIELD(`k`, 1, 2) = 3", sql.toString());
    }
  }

  @Test
  void testRemoveTextFunc() {
    final SqlNode sql = TestHelper.parseSql(("select a from t where a like concat('%', concat('1', '%'))"));
    Clean.clean(sql);
    assertEquals("SELECT `a` FROM `t` WHERE `a` LIKE '%1%'", sql.toString());
  }

  @Test
  void testNormalizeTuple() {
    final SqlNode sql = TestHelper.parseSql("select a from t where (a, b) in ((1,2),(3,4))");
    NormalizeTuple.normalize(sql);
    assertEquals("SELECT `a` FROM `t` WHERE (`a`, `b`) IN (?)", sql.toString());
  }

  @Test
  void testNormalizeBool() {
    final SqlNode sql =
        TestHelper.parseSql("select * from a where a.i or (a.j is false and (a.j = a.i) is false)");
    NormalizeBool.normalize(sql);
    assertEquals(
        "SELECT * FROM `a` WHERE `a`.`i` = TRUE OR NOT `a`.`j` = TRUE AND NOT `a`.`j` = `a`.`i`",
        sql.toString());
  }

  @Test
  void testNormalizeJoinCondition() {
    final SqlNode sql =
        TestHelper.parseSql(
            "select * from a join b on a.i = b.x and a.j=3 join c on b.y=c.v and b.z=c.w and c.u<10 where a.j=b.y");
    NormalizeJoinCond.normalize(sql);
    assertEquals(
        "SELECT * FROM `a` INNER JOIN `b` ON `a`.`i` = `b`.`x` AND `a`.`j` = `b`.`y` INNER JOIN `c` ON `b`.`y` = `c`.`v` AND `b`.`z` = `c`.`w` WHERE `a`.`j` = 3 AND `c`.`u` < 10",
        sql.toString());
  }

  @Test
  void testNormalizeConstantTable() {
    final SqlNode sql =
        TestHelper.parseSql(
            "select sub.j from a inner join (select 1 as j) as sub on a.i = sub.j where sub.j = 1 order by sub.j");
    InlineLiteralTable.normalize(sql);
    Clean.clean(sql);
    final SqlContext ctx = sql.context();
    ctx.deleteDetached(ctx.root());
    ctx.compact();
    assertEquals("SELECT 1 FROM `a` WHERE `a`.`i` = 1", sql.toString());
  }

  @Test
  void testNormalizeGrouping() {
    final SqlNode sql =
        TestHelper.parseSql(
            "Select a.i, a.j From a Group By a.i, a.j, 3, 2 + 3 HAVING a.i > 10 AND (count(a.i) > 1 OR a.j < 5)");
    NormalizeGrouping.normalize(sql);
    final SqlContext ctx = sql.context();
    ctx.deleteDetached(ctx.root());
    ctx.compact();
    assertEquals(
        "SELECT DISTINCT `a`.`i`, `a`.`j` FROM `a` WHERE `a`.`i` > 10 HAVING COUNT(`a`.`i`) > 1 OR `a`.`j` < 5",
        sql.toString());
  }

  @Test
  void testNormalizeRightJoin() {
    final SqlNode sql = TestHelper.parseSql("Select a.i From a Right Join b On a.i = b.x");
    NormalizeRightJoin.normalize(sql);
    final SqlContext ctx = sql.context();
    ctx.deleteDetached(ctx.root());
    ctx.compact();
    assertEquals("SELECT `a`.`i` FROM `b` LEFT JOIN `a` ON `a`.`i` = `b`.`x`", sql.toString());
  }
}
