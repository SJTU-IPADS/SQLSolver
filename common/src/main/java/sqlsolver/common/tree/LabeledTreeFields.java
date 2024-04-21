package sqlsolver.common.tree;

import sqlsolver.common.field.Fields;

public interface LabeledTreeFields<Kind> extends Fields {
  Kind kind();

  int parent();
}
