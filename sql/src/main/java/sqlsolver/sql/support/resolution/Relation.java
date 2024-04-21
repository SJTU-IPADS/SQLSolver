package sqlsolver.sql.support.resolution;

import sqlsolver.sql.ast.SqlNode;
import sqlsolver.sql.ast.SqlKind;
import sqlsolver.sql.ast.TableSourceKind;

import java.util.List;

public interface Relation {
  SqlNode rootNode(); // invariant: isRelationBoundary(rootNode())

  String qualification();

  List<Relation> inputs();

  List<Attribute> attributes();

  Attribute resolveAttribute(String qualification, String name);

  static boolean isRelationRoot(SqlNode node) {
    return SqlKind.Query.isInstance(node) || TableSourceKind.SimpleSource.isInstance(node);
  }
}
