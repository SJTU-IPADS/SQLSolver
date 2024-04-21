package sqlsolver.sql.plan;

import org.junit.jupiter.api.Test;
import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotNullInferenceTest {
  @Test
  void testJoin() {
    final String schemaDef =
        "Create Table t (i int NOT NULL, j int); Create Table s (m int NOT NULL, n int)";
    final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, schemaDef);

    final String sql0 = "Select * From t Inner Join s On t.i = s.m";
    final SqlNode ast0 = SqlSupport.parseSql(DbSupport.MySQL, sql0);
    final PlanContext plan0 = PlanSupport.assemblePlan(ast0, schema);
    final int projNode0 = plan0.root();
    final Values values0 = plan0.valuesReg().valuesOf(projNode0);
    final NotNullInference inference0 = new NotNullInference(plan0);
    assertTrue(inference0.isNotNullAt(values0.get(0), projNode0));
    assertFalse(inference0.isNotNullAt(values0.get(1), projNode0));
    assertTrue(inference0.isNotNullAt(values0.get(2), projNode0));
    assertFalse(inference0.isNotNullAt(values0.get(3), projNode0));

    final String sql1 = "Select * From t Left Join s On t.i = s.n";
    final SqlNode ast1 = SqlSupport.parseSql(DbSupport.MySQL, sql1);
    final PlanContext plan1 = PlanSupport.assemblePlan(ast1, schema);
    final int projNode1 = plan1.root();
    final Values values1 = plan1.valuesReg().valuesOf(projNode1);
    final NotNullInference inference1 = new NotNullInference(plan1);
    assertTrue(inference1.isNotNullAt(values1.get(0), projNode0));
    assertFalse(inference1.isNotNullAt(values1.get(1), projNode0));
    assertFalse(inference1.isNotNullAt(values1.get(2), projNode0));
    assertFalse(inference1.isNotNullAt(values1.get(3), projNode0));
  }

  @Test
  void testFilter() {
    final String schemaDef = "Create Table t (i int NOT NULL, j int)";
    final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, schemaDef);

    final String sql0 = "Select * From t Where t.j = 10";
    final SqlNode ast0 = SqlSupport.parseSql(DbSupport.MySQL, sql0);
    final PlanContext plan0 = PlanSupport.assemblePlan(ast0, schema);
    final int projNode0 = plan0.root();
    final Values values0 = plan0.valuesReg().valuesOf(projNode0);
    final NotNullInference inference0 = new NotNullInference(plan0);
    assertTrue(inference0.isNotNullAt(values0.get(0), projNode0));
    assertTrue(inference0.isNotNullAt(values0.get(1), projNode0));
  }

  @Test
  void testAgg() {
    final String schemaDef = "Create Table t (i int NOT NULL, j int)";
    final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, schemaDef);

    final String sql0 = "Select * From t Group By t.i";
    final SqlNode ast0 = SqlSupport.parseSql(DbSupport.MySQL, sql0);
    final PlanContext plan0 = PlanSupport.assemblePlan(ast0, schema);
    final int projNode0 = plan0.root();
    final Values values0 = plan0.valuesReg().valuesOf(projNode0);
    final NotNullInference inference0 = new NotNullInference(plan0);
    assertTrue(inference0.isNotNullAt(values0.get(0), projNode0));
    assertFalse(inference0.isNotNullAt(values0.get(1), projNode0));
  }
}
