package sqlsolver.common.field;

public interface FieldKey<T> {
  String name();

  T getFrom(Fields target);

  T setTo(Fields target, T value);
}
