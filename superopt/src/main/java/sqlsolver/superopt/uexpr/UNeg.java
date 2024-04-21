package sqlsolver.superopt.uexpr;

public interface UNeg extends UUnary {
  @Override
  default UKind kind() {
    return UKind.NEGATION;
  }

  static UNeg mk(UTerm body) {
    return new UNegImpl(body);
  }
}
