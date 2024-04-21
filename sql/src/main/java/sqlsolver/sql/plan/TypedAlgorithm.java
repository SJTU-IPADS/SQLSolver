package sqlsolver.sql.plan;

abstract class TypedAlgorithm<T> {
  protected final PlanContext context;

  protected TypedAlgorithm(PlanContext context) {this.context = context;}

  T on(PlanNode node) {
    prologue(node);

    T val = switch (node.kind()) {
      case Input -> onInput((InputNode) node);
      default -> null;
    };

    epilogue(node);

    if (val == null) return onFallback(node);
    else return val;
  }

  T onInput(InputNode input) { return null; }

  T onFallback(PlanNode node) { return null; }

  void prologue(PlanNode node) { }

  void epilogue(PlanNode node) { }
}
