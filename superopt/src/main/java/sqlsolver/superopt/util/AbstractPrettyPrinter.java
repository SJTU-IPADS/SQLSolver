package sqlsolver.superopt.util;

public abstract class AbstractPrettyPrinter {

  private int indent = 0, lineLength = 0, lineNumber = 0;
  private int autoNewLine = 0;
  private boolean autoNewLineEnabled = false;

  protected abstract void printString(String str);

  protected abstract void printNewLine();

  /**
   * Set when to switch to a new line automatically.
   * When the current line reaches <code>autoNewLine</code>,
   * the printer switches to a new line.
   * <code>setAutoNewLineEnabled(true)</code> should also be called
   * to enable auto new line.
   * @param autoNewLine the threshold of auto new line, or a non-positive value indicating no auto new line
   */
  public void setAutoNewLine(int autoNewLine) {
    this.autoNewLine = autoNewLine;
  }

  public void setAutoNewLineEnabled(boolean enabled) {
    autoNewLineEnabled = enabled;
  }

  public AbstractPrettyPrinter indent(int i) {
    indent += i;
    assert indent >= 0;
    return this;
  }

  public int getCurrentLineLength(boolean includesIndent) {
    return includesIndent ? lineLength + indent : lineLength;
  }

  public int getCurrentLineLength() {
        return getCurrentLineLength(false);
    }

  public int getLineNumber() {
        return lineNumber;
    }

  /**
   * Print <code>o.toString()</code>.
   * Note that <code>o.toString()</code> is assumed not to contain newlines.
   * @param o the object to print
   * @return <code>this</code>
   */
  public AbstractPrettyPrinter print(Object o) {
    String str = o.toString();
    if (autoNewLineEnabled && autoNewLine > 0 && lineLength + str.length() > autoNewLine) {
      println();
    }
    printString(str);
    lineLength += str.length();
    return this;
  }

  /**
   * Append a new line and indent.
   * @return <code>this</code>
   */
  public AbstractPrettyPrinter println() {
    printNewLine();
    printString(" ".repeat(indent));
    lineLength = 0;
    lineNumber++;
    return this;
  }

  /**
   * <code>print(o)</code> and then <code>println()</code>.
   * @param o the object to print
   * @return <code>this</code>
   */
  public AbstractPrettyPrinter println(Object o) {
    print(o);
    println();
    return this;
  }

}
