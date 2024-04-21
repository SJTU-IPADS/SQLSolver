package sqlsolver.sql.mysql;

public interface MySQLRecognizerCommon {
  int NoMode = 0;
  int AnsiQuotes = 1;
  int HighNotPrecedence = 1 << 1;
  int PipesAsConcat = 1 << 2;
  int IgnoreSpace = 1 << 3;
  int NoBackslashEscapes = 1 << 4;

  long serverVersion();

  int sqlMode();

  default boolean isSqlModeActive(int mode) {
    return (sqlMode() & mode) != 0;
  }
}
