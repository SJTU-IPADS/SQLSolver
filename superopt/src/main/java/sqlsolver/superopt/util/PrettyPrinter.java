package sqlsolver.superopt.util;

public class PrettyPrinter extends AbstractPrettyPrinter {

  @Override
  protected void printString(String str) {
    System.out.print(str);
  }

  @Override
  protected void printNewLine() {
    System.out.println();
  }

}
