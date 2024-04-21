package sqlsolver.common.tree;

import sqlsolver.common.field.FieldKey;

import java.util.Map;
import java.util.NoSuchElementException;

import static sqlsolver.common.utils.ArraySupport.linearFind;

public interface TreeSupport {
  static void checkNodePresent(TreeContext<?> context, int nodeId) {
    if (!context.isPresent(nodeId))
      throw new NoSuchElementException("no such node in this tree: " + nodeId);
  }

  static void checkParentNotSet(TreeContext<?> context, int childId) {
    final int existingParent = context.parentOf(childId);
    if (existingParent != TreeContext.NO_SUCH_NODE)
      throw new IllegalStateException("cannot set parent: already has parent");
  }

  static void checkIsValidChild(TreeContext<?> context, int parentId, int childId) {
    final int existingParent = context.parentOf(childId);
    if (existingParent != TreeContext.NO_SUCH_NODE && isDescendant(context, childId, parentId))
      throw new IllegalStateException("cannot set parent: loop incurred");
  }

  static boolean nodeEquals(UniformTreeNode<?, ?, ?> n0, UniformTreeNode<?, ?, ?> n1) {
    if (n0 == n1) return true;
    if (n0 == null ^ n1 == null) return false;
    return n0.context() == n1.context() && n0.nodeId() == n1.nodeId();
  }

  static boolean nodeEquals(LabeledTreeNode<?, ?, ?> n0, LabeledTreeNode<?, ?, ?> n1) {
    if (n0 == n1) return true;
    if (n0 == null ^ n1 == null) return false;
    return n0.context() == n1.context() && n0.nodeId() == n1.nodeId();
  }

  static int countNodes(TreeContext<?> context) {
    int count = 0;
    for (int nodeId = 1, bound = context.maxNodeId(); nodeId <= bound; nodeId++) {
      if (!context.isPresent(nodeId)) ++count;
    }
    return count;
  }

  static int rootOf(TreeContext<?> context, int nodeId) {
    int parent = context.parentOf(nodeId);
    while (parent != TreeContext.NO_SUCH_NODE) {
      nodeId = parent;
      parent = context.parentOf(nodeId);
    }
    return nodeId;
  }

  static boolean isDetached(TreeContext<?> context, int rootId, int nodeId) {
    return rootOf(context, nodeId) != rootId;
  }

  static boolean isDescendant(TreeContext<?> context, int rootId, int toCheckNodeId) {
    int n = toCheckNodeId;
    while (n != TreeContext.NO_SUCH_NODE) {
      if (n == rootId) return true;
      n = context.parentOf(n);
    }
    return false;
  }

  static void deleteDetached(TreeContext<?> context, int rootId) {
    for (int i = 1, bound = context.maxNodeId(); i <= bound; ++i) {
      if (context.isPresent(i) && isDetached(context, rootId, i)) {
        context.deleteNode(i);
      }
    }
  }

  static int indexOfChild(UniformTreeContext<?> context, int nodeId) {
    final int parent = context.parentOf(nodeId);
    final int[] children = context.childrenOf(parent);
    return linearFind(children, nodeId, 0);
  }

  static int locate(UniformTreeContext<?> context, int childId) {
    final int parentId = context.parentOf(childId);
    final int[] children = context.childrenOf(parentId);
    return linearFind(children, childId, 0);
  }

  static FieldKey<?> locate(LabeledTreeContext<?> context, int childId) {
    final int parentId = context.parentOf(childId);
    final LabeledTreeFields<?> fields = context.fieldsOf(parentId);

    for (Map.Entry<FieldKey<?>, Object> pair : fields.entrySet()) {
      final Object value = pair.getValue();

      if (matchAstNode(value, childId)) return pair.getKey();

      if (value instanceof Iterable)
        for (Object o : (Iterable<?>) value)
          if (matchAstNode(o, childId)) {
            return pair.getKey();
          }
    }

    return null;
  }

  static <Kind> int copyTree(UniformTreeContext<Kind> context, int rootId) {
    final Kind kind = context.kindOf(rootId);
    final int[] children = context.childrenOf(rootId);
    final int newNode = context.mkNode(kind);

    for (int i = 0; i < children.length; i++) {
      final int newChild = copyTree(context, children[i]);
      context.setChild(newNode, i, newChild);
    }

    return newNode;
  }

  static boolean matchAstNode(Object obj, int nodeId) {
    return obj instanceof LabeledTreeNode && ((LabeledTreeNode) obj).nodeId() == nodeId;
  }
}
