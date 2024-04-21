package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.constants.JoinKind;

import java.util.List;

import static sqlsolver.common.utils.Commons.coalesce;

public interface TableSourceFields {
  //// Simple
  FieldKey<SqlNode> Simple_Table = TableSourceKind.SimpleSource.nodeField("Table");
  FieldKey<List<String>> Simple_Partition = TableSourceKind.SimpleSource.field("Partitions", List.class);
  FieldKey<String> Simple_Alias = TableSourceKind.SimpleSource.textField("Alias");
  // mysql only
  FieldKey<SqlNodes> Simple_Hints = TableSourceKind.SimpleSource.nodesField("Hints");
  //// Joined
  FieldKey<SqlNode> Joined_Left = TableSourceKind.JoinedSource.nodeField("Left");
  FieldKey<SqlNode> Joined_Right = TableSourceKind.JoinedSource.nodeField("Right");
  FieldKey<JoinKind> Joined_Kind = TableSourceKind.JoinedSource.field("Kind", JoinKind.class);
  FieldKey<SqlNode> Joined_On = TableSourceKind.JoinedSource.nodeField("On");
  FieldKey<List<String>> Joined_Using = TableSourceKind.JoinedSource.field("Using", List.class);
  //// Derived
  FieldKey<SqlNode> Derived_Subquery = TableSourceKind.DerivedSource.nodeField("Subquery");
  FieldKey<String> Derived_Alias = TableSourceKind.DerivedSource.textField("Alias");
  FieldKey<Boolean> Derived_Lateral = TableSourceKind.DerivedSource.boolField("Lateral");
  FieldKey<List<String>> Derived_InternalRefs = TableSourceKind.DerivedSource.field("InternalRefs", List.class);

  static String tableSourceNameOf(SqlNode node) {
    if (!SqlKind.TableSource.isInstance(node)) return null;

    if (TableSourceKind.SimpleSource.isInstance(node))
      return coalesce(node.$(Simple_Alias), node.$(Simple_Table).$(SqlNodeFields.TableName_Table));
    else if (TableSourceKind.DerivedSource.isInstance(node)) return node.$(Derived_Alias);

    return null;
  }

  static String tableNameOf(SqlNode node) {
    return !TableSourceKind.SimpleSource.isInstance(node) ? null : node.$(Simple_Table).$(SqlNodeFields.TableName_Table);
  }
}
