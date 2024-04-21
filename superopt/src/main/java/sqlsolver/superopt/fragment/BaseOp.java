package sqlsolver.superopt.fragment;

abstract class BaseOp implements Op {
  private Fragment fragment;
  private Op successor;
  private final Op[] predecessors;

  protected BaseOp() {
    predecessors = new Op[kind().numPredecessors()];
  }

  @Override
  public Op successor() {
    return successor;
  }

  @Override
  public Op[] predecessors() {
    return predecessors;
  }

  @Override
  public Fragment fragment() {
    return fragment;
  }

  @Override
  public Symbols context() {
    return fragment.symbols();
  }

  @Override
  public void setContext(Symbols context) {}

  @Override
  public void setSuccessor(Op successor) {
    this.successor = successor;
  }

  @Override
  public void setPredecessor(int idx, Op predecessor) {
    this.predecessors[idx] = predecessor;
    if (predecessor != null) predecessor.setSuccessor(this);
  }

  @Override
  public void setFragment(Fragment fragment) {
    this.fragment = fragment;
  }

  @Override
  public void acceptVisitor(OpVisitor visitor) {
    if (visitor.enter(this)) {
      if (accept0(visitor)) {
        final Op[] prevs = predecessors();
        for (int i = 0; i < prevs.length; i++) {
          final Op prev = prevs[i];
          if (prev != null) prev.acceptVisitor(visitor);
          else visitor.enterEmpty(this, i);
        }
      }

      leave0(visitor);
    }
    visitor.leave(this);
  }

  @Override
  public Op copyTree() {
    final Op thisCopy = copy0();

    final Op[] prev = predecessors();
    for (int i = 0; i < prev.length; i++) {
      if (prev[i] == null) thisCopy.setPredecessor(i, null);
      else {
        final Op prevCopy = prev[i].copyTree();
        thisCopy.setPredecessor(i, prevCopy);
      }
    }

    return thisCopy;
  }

  @Override
  public Op copy(Symbols context) {
    final Op copy = copy0();
    context.reBindSymbol(copy, this, context());
    return copy;
  }

  @Override
  public int shadowHash() {
    return kind().hashCode();
  }

  @Override
  public String toString() {
    return kind().name();
  }

  protected Op copy0() {
    return Op.mk(kind());
  }

  protected abstract boolean accept0(OpVisitor visitor);

  protected abstract void leave0(OpVisitor visitor);
}
