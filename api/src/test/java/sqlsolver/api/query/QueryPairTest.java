package sqlsolver.api.query;

import org.junit.jupiter.api.Test;
import sqlsolver.api.TestHelper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QueryPairTest {

  @Test
  void test() {
    List<String> lines = new ArrayList<>();
    lines.add("SELECT i, j FROM a");
    lines.add("SELECT T.COL1, T.COL2 FROM (SELECT i AS COL1, j AS COL2 FROM a) AS T");
    lines.add("SELECT x, y FROM b");
    lines.add("SELECT T.COL1, T.COL2 FROM (SELECT x AS COL1, y AS COL2 FROM b) AS T");

    final List<QueryPair> pairs = new ArrayList<>(lines.size() >> 1);
    for (int i = 0, bound = lines.size(); i < bound; i += 2) {
      String originSql0 = lines.get(i), originSql1 = lines.get(i + 1);
      String sql0 = "TEST sql0", sql1 = "TEST sql1";
      pairs.add(QueryPair.mk(i + 1, TestHelper.SCHEMA, sql0, sql1, originSql0, originSql1));
    }

    assertEquals(pairs.size(), lines.size() / 2);
    for (int i = 0, bound = pairs.size(); i < bound; i++) {
      QueryPair pair = pairs.get(i);
      assertEquals(pair.getSql0(), "TEST sql0");
      assertEquals(pair.getSql1(), "TEST sql1");
    }
  }
}
