package sqlsolver.sql.util;

import sqlsolver.common.field.FieldKey;
import sqlsolver.sql.ast.*;
import sqlsolver.sql.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static sqlsolver.common.utils.ArraySupport.linearFind;

public class SqlCopier {
  private int[] tracks;
  private SqlContext toCtx;
  private SqlNode root;

  private int[] destination;

  public SqlCopier track(int... nodeIds) {
    this.tracks = nodeIds;
    return this;
  }

  public SqlCopier to(SqlContext toCtx) {
    this.toCtx = toCtx;
    return this;
  }

  public SqlCopier root(SqlNode root) {
    this.root = root;
    return this;
  }

  public SqlNode go() {
    if (toCtx == null) toCtx = root.context();
    if (tracks != null) destination = new int[tracks.length];

    final SqlNode copied = copy0(root);
    if (tracks != null) System.arraycopy(destination, 0, tracks, 0, destination.length);
    return copied;
  }

  private SqlNode copy0(SqlNode node) {
    final int newNodeId = toCtx.mkNode(node.kind());

    if (destination != null) {
      final int idx = linearFind(tracks, node.nodeId(), 0);
      if (idx >= 0) destination[idx] = newNodeId;
    }

    if (SqlKind.TableSource.isInstance(node)) {
      toCtx.setFieldOf(newNodeId, SqlNodeFields.TableSource_Kind, node.$(SqlNodeFields.TableSource_Kind));
    }
    if (SqlKind.Expr.isInstance(node)) {
      toCtx.setFieldOf(newNodeId, SqlNodeFields.Expr_Kind, node.$(SqlNodeFields.Expr_Kind));
    }

    for (Map.Entry<FieldKey<?>, Object> pair : node.entrySet()) {
      final FieldKey key = pair.getKey();
      final Object value = pair.getValue();
      final Object copiedValue;
      if (value instanceof SqlNode) {
        copiedValue = copy0((SqlNode) value);

      } else if (value instanceof SqlNodes) {
        final SqlNodes nodes = (SqlNodes) value;
        final List<SqlNode> newChildren = new ArrayList<>(nodes.size());
        for (SqlNode sqlNode : nodes) newChildren.add(copy0(sqlNode));
        copiedValue = SqlNodes.mk(toCtx, newChildren);

      } else {
        copiedValue = value;
      }

      toCtx.setFieldOf(newNodeId, key, copiedValue);
    }

    return SqlNode.mk(toCtx, newNodeId);
  }
}
