package sqlsolver.superopt.uexpr;

import java.util.List;

public interface UFunc extends UTerm {
  enum FuncKind {
    STRING("string"),
    INTEGER("integer");
    private final String text;

    FuncKind(String text) {
            this.text = text;
        }

    UName funcName() {
            return UName.mk(text);
        }
  }

  default boolean isFuncKind(UFunc.FuncKind funcKind) {
        return funcKind() == funcKind;
    }

  @Override
  default UKind kind() {
        return UKind.FUNC;
    }

  FuncKind funcKind();

  UName funcName();

  List<UTerm> args();

  static UFunc mk(FuncKind funcKind, UName funcName, List<UTerm> arguments) {
    return new UFuncImpl(funcKind, funcName, arguments);
  }
}
