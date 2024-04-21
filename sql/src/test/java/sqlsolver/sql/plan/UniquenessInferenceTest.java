package sqlsolver.sql.plan;

import org.junit.jupiter.api.Test;
import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.schema.Schema;
import sqlsolver.sql.schema.SchemaSupport;
import sqlsolver.sql.SqlSupport;
import sqlsolver.sql.ast.SqlNode;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniquenessInferenceTest {
  @Test
  void testJoin() {
    final String schemaDef =
        "Create Table t (i int primary key, j int); Create Table s (m int primary key, n int)";
    final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, schemaDef);

    final String sql0 = "Select * From t Inner Join s On t.i = s.m";
    final SqlNode ast0 = SqlSupport.parseSql(DbSupport.MySQL, sql0);
    final PlanContext plan0 = PlanSupport.assemblePlan(ast0, schema);
    final int projNode0 = plan0.root();
    final Values values0 = plan0.valuesReg().valuesOf(projNode0);
    final UniquenessInference inference0 = new UniquenessInference(plan0);
    assertTrue(inference0.isUniqueCoreAt(singletonList(values0.get(0)), projNode0));
    assertFalse(inference0.isUniqueCoreAt(singletonList(values0.get(1)), projNode0));
    assertTrue(inference0.isUniqueCoreAt(singletonList(values0.get(2)), projNode0));

    final String sql1 = "Select * From t Inner Join s On t.i = s.n";
    final SqlNode ast1 = SqlSupport.parseSql(DbSupport.MySQL, sql1);
    final PlanContext plan1 = PlanSupport.assemblePlan(ast1, schema);
    final int projNode1 = plan1.root();
    final Values values1 = plan1.valuesReg().valuesOf(projNode1);
    final UniquenessInference inference1 = new UniquenessInference(plan1);
    assertFalse(inference1.isUniqueCoreAt(singletonList(values1.get(0)), projNode1));
    assertFalse(inference1.isUniqueCoreAt(singletonList(values1.get(1)), projNode1));
    assertFalse(inference1.isUniqueCoreAt(singletonList(values1.get(2)), projNode1));
    assertTrue(inference1.isUniqueCoreAt(asList(values0.get(0), values1.get(2)), projNode1));
  }

  @Test
  void testFilter() {
    final String schemaDef = "Create Table t (i int, j int, primary key(i,j)); ";
    final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, schemaDef);

    final String sql0 = "Select * From t Where t.j = 3";
    final SqlNode ast0 = SqlSupport.parseSql(DbSupport.MySQL, sql0);
    final PlanContext plan0 = PlanSupport.assemblePlan(ast0, schema);
    final int projNode0 = plan0.root();
    final Values values0 = plan0.valuesReg().valuesOf(projNode0);
    final UniquenessInference inference0 = new UniquenessInference(plan0);
    assertTrue(inference0.isUniqueCoreAt(singletonList(values0.get(0)), projNode0));
    assertFalse(inference0.isUniqueCoreAt(singletonList(values0.get(1)), projNode0));
    assertTrue(inference0.isUniqueCoreAt(values0, projNode0));
  }

  @Test
  void testAgg() {
    final String schemaDef = "Create Table t (i int, j int); ";
    final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, schemaDef);

    final String sql = "Select * From t Group By t.i";
    final SqlNode ast = SqlSupport.parseSql(DbSupport.MySQL, sql);
    final PlanContext plan = PlanSupport.assemblePlan(ast, schema);
    final int aggNode = plan.root();
    final Values values0 = plan.valuesReg().valuesOf(aggNode);
    final UniquenessInference inference0 = new UniquenessInference(plan);
    assertTrue(inference0.isUniqueCoreAt(singletonList(values0.get(0)), aggNode));
    assertFalse(inference0.isUniqueCoreAt(singletonList(values0.get(1)), aggNode));
  }
}
