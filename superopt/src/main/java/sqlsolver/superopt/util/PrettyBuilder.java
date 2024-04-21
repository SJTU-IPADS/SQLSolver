package sqlsolver.superopt.util;

public class PrettyBuilder extends AbstractPrettyPrinter {

  private StringBuilder builder = new StringBuilder();

  @Override
  protected void printString(String str) {
    builder.append(str);
  }

  @Override
  protected void printNewLine() {
    builder.append("\n");
  }

  @Override
  public String toString() {
    return builder.toString();
  }

}
