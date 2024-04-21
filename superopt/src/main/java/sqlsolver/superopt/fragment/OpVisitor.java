package sqlsolver.superopt.fragment;

import java.util.function.Consumer;

public interface OpVisitor {
  static OpVisitor traverse(Consumer<Op> consumer) {
    return new OpVisitor() {
      @Override
      public boolean enter(Op op) {
        consumer.accept(op);
        return true;
      }
    };
  }

  default void enterEmpty(Op parent, int idx) {}

  default boolean enter(Op op) {
    return true;
  }

  default void leave(Op op) {}

  default boolean enterAgg(Agg op) {
    return true;
  }

  default void leaveAgg(Agg op) {}

  default boolean enterProj(Proj op) {
    return true;
  }

  default void leaveProj(Proj op) {}

  default boolean enterInnerJoin(InnerJoin op) {
    return true;
  }

  default void leaveInnerJoin(InnerJoin op) {}

  default boolean enterCrossJoin(CrossJoin op) {
    return true;
  }

  default void leaveCrossJoin(CrossJoin op) {}

  default boolean enterLeftJoin(LeftJoin op) {
    return true;
  }

  default void leaveLeftJoin(LeftJoin op) {}

  default boolean enterRightJoin(RightJoin op) {
    return true;
  }

  default void leaveRightJoin(RightJoin op) {}

  default boolean enterFullJoin(FullJoin op) {
    return true;
  }

  default void leaveFullJoin(FullJoin op) {}

  default boolean enterLimit(Limit op) {
    return true;
  }

  default void leaveLimit(Limit op) {}

  default boolean enterSimpleFilter(SimpleFilter op) {
    return true;
  }

  default void leaveSimpleFilter(SimpleFilter op) {}

  default boolean enterExistsFilter(ExistsFilter op) {
    return true;
  }

  default void leaveExistsFilter(ExistsFilter op) {}

  default boolean enterInSubFilter(InSubFilter op) {
    return true;
  }

  default void leaveInSubFilter(InSubFilter op) {}

  default boolean enterUnion(Union op) {
    return true;
  }

  default void leaveUnion(Union op) {}

  default boolean enterIntersect(Intersect op) {
    return true;
  }

  default void leaveIntersect(Intersect op) {}

  default boolean enterExcept(Except op) {
    return true;
  }

  default void leaveExcept(Except op) {}

  default boolean enterSort(Sort op) {
    return true;
  }

  default void leaveSort(Sort op) {}

  default boolean enterInput(Input input) {
    return true;
  }

  default void leaveInput(Input input) {}
}
