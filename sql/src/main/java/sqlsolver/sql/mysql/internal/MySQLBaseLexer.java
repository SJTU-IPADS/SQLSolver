package sqlsolver.sql.mysql.internal;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import sqlsolver.sql.mysql.MySQLRecognizerCommon;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public abstract class MySQLBaseLexer extends Lexer implements MySQLRecognizerCommon {
  private long serverVersion = 0;
  private int sqlMode = NoMode;
  public boolean inVersionComment = false;
  private final Set<String> charsets = new HashSet<>();
  private final Queue<Token> _pendingTokens = new LinkedList<>();

  public MySQLBaseLexer(CharStream input) {
    super(input);
  }

  public void setServerVersion(long serverVersion) {
    this.serverVersion = serverVersion;
  }

  public void setSqlMode(int sqlMode) {
    this.sqlMode = sqlMode;
  }

  @Override
  public void reset() {
    inVersionComment = false;
    super.reset();
  }

  @Override
  public int sqlMode() {
    return sqlMode;
  }

  @Override
  public long serverVersion() {
    return serverVersion;
  }

  private static final String long_str = "2147483647";
  private static final int long_len = long_str.length();
  private static final String signed_long_str = "-2147483648";
  private static final String longlong_str = "9223372036854775807";
  private static final int longlong_len = longlong_str.length();
  private static final String signed_longlong_str = "-9223372036854775808";
  private static final int signed_longlong_len = signed_longlong_str.length() - 1;
  private static final String unsigned_longlong_str = "18446744073709551615";
  private static final int unsigned_longlong_len = unsigned_longlong_str.length();

  protected static int determineNumericType(String text) {
    // The original code checks for leading +/- but actually that can never happen, neither in the
    // server parser (as a digit is used to trigger processing in the lexer) nor in our parser
    // as our rules are defined without signs. But we do it anyway for maximum compatibility.
    int length = text.length() - 1;
    if (length < long_len) return MySQLLexer.INT_NUMBER;

    final char[] chars = text.toCharArray();
    int idx = 0;
    boolean negative = false;

    if (chars[idx] == '+') {
      idx++;
      length--;
    } else if (chars[idx] == '-') {
      idx++;
      length--;
      negative = true;
    }

    while (chars[idx] == '0' && length > 0) {
      idx++;
      length--;
    }

    if (length < long_len) return MySQLLexer.INT_NUMBER;

    int smaller, bigger;
    String cmp;
    if (negative) {
      if (length == long_len) {
        cmp = signed_long_str.substring(1);
        smaller = MySQLLexer.INT_NUMBER; // If <= signed_long_str
        bigger = MySQLLexer.LONG_NUMBER; // If >= signed_long_str
      } else if (length < signed_longlong_len) return MySQLLexer.LONG_NUMBER;
      else if (length > signed_longlong_len) return MySQLLexer.DECIMAL_NUMBER;
      else {
        cmp = signed_longlong_str.substring(1);
        smaller = MySQLLexer.LONG_NUMBER; // If <= signed_longlong_str
        bigger = MySQLLexer.DECIMAL_NUMBER;
      }
    } else {
      if (length == long_len) {
        cmp = long_str;
        smaller = MySQLLexer.INT_NUMBER;
        bigger = MySQLLexer.LONG_NUMBER;
      } else if (length < longlong_len) return MySQLLexer.LONG_NUMBER;
      else if (length > longlong_len) {
        if (length > unsigned_longlong_len) return MySQLLexer.DECIMAL_NUMBER;
        cmp = unsigned_longlong_str;
        smaller = MySQLLexer.ULONGLONG_NUMBER;
        bigger = MySQLLexer.DECIMAL_NUMBER;
      } else {
        cmp = longlong_str;
        smaller = MySQLLexer.LONG_NUMBER;
        bigger = MySQLLexer.ULONGLONG_NUMBER;
      }
    }

    final var cmpChars = cmp.toCharArray();
    int cmpIdx = 0;
    while (cmpIdx < cmpChars.length && cmpChars[cmpIdx] == chars[idx]) {
      cmpIdx++;
      idx++;
    }

    return chars[idx] <= cmpChars[idx] ? smaller : bigger;
  }

  protected int determineFunction(int proposed) {
    // Skip any whitespace character if the sql mode says they should be ignored,
    // before actually trying to match the open parenthesis.
    if (isSqlModeActive(IgnoreSpace)) {
      int input = _input.LA(1);
      while (input == ' ' || input == '\t' || input == '\r' || input == '\n') {
        getInterpreter().consume(_input);
        _channel = HIDDEN;
        _type = MySQLLexer.WHITESPACE;
        input = _input.LA(1);
      }
    }

    return _input.LA(1) == '(' ? proposed : MySQLLexer.IDENTIFIER;
  }

  protected int checkCharset(String text) {
    return charsets.contains(text) ? MySQLLexer.UNDERSCORE_CHARSET : MySQLLexer.IDENTIFIER;
  }

  protected boolean checkVersion(String text) {
    if (text.length() < 8) // Minimum is: /*!12345
    return false;

    // Skip version comment introducer.
    final long version = Long.parseLong(text.substring(3));
    if (version <= serverVersion) {
      inVersionComment = true;
      return true;
    }
    return false;
  }

  protected void emitDot() {
    _pendingTokens.offer(
        _factory.create(
            new Pair<>(this, _input),
            MySQLLexer.DOT_SYMBOL,
            _text,
            _channel,
            _tokenStartCharIndex,
            _tokenStartCharIndex,
            _tokenStartLine,
            _tokenStartCharPositionInLine));
    ++_tokenStartCharIndex;
  }

  @Override
  public Token nextToken() {
    // First respond with pending tokens to the next token request, if there are any.
    if (!_pendingTokens.isEmpty()) return _pendingTokens.poll();

    // Let the main lexer class run the next token recognition.
    // This might create additional tokens again.
    final Token next = super.nextToken();
    if (!_pendingTokens.isEmpty()) {
      final Token pending = _pendingTokens.poll();
      _pendingTokens.offer(next);
      return pending;
    }
    return next;
  }
}
