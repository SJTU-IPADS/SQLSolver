package sqlsolver.superopt.uexpr;

public interface USquash extends UUnary {
  @Override
  default UKind kind() {
    return UKind.SQUASH;
  }

  static USquash mk(UTerm body) {
    return new USquashImpl(body);
  }
}
