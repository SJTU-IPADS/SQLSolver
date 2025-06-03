package sqlsolver.sql.mysql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.mysql.internal.MySQLLexer;
import sqlsolver.sql.mysql.internal.MySQLParser;
import sqlsolver.sql.parser.AstParser;
import sqlsolver.sql.parser.ThrowingErrorListener;
import sqlsolver.sql.ast.SqlNode;

import java.util.Properties;
import java.util.function.Function;
import java.util.function.BiConsumer;
import sqlsolver.sql.RefreshableParserInitializer;

public class MySQLAstParser implements AstParser {
  private long serverVersion = 0;
  private int sqlMode = MySQLRecognizerCommon.NoMode;

  public void setServerVersion(long serverVersion) {
    this.serverVersion = serverVersion;
  }

  public void setSqlMode(int sqlMode) {
    this.sqlMode = sqlMode;
  }

  public SqlNode parse(String str, Function<MySQLParser, ParserRuleContext> rule) {
    final MySQLLexer lexer = new MySQLLexer(CharStreams.fromString(str));
    lexer.setServerVersion(serverVersion);
    lexer.setSqlMode(sqlMode);
    if (str.contains("OVER (")) lexer.setServerVersion(Integer.MAX_VALUE);

    final MySQLParser parser = new MySQLParser(new CommonTokenStream(lexer));
    parser.setServerVersion(serverVersion);
    parser.setSqlMode(sqlMode);
    if (str.contains("OVER (")) parser.setServerVersion(Integer.MAX_VALUE);

    lexer.removeErrorListeners();
    lexer.addErrorListener(ThrowingErrorListener.instance());
    parser.removeErrorListeners();
    parser.addErrorListener(ThrowingErrorListener.instance());

    // 注意，此处需要构造一个匿名类
    BiConsumer<MySQLLexer, MySQLParser> initializer = new RefreshableParserInitializer<MySQLLexer, MySQLParser>(){};
    // refresh lexer和parser
    initializer.accept(lexer, parser);

    return rule.apply(parser).accept(new MySQLAstBuilder());
  }

  @Override
  public SqlNode parse(String string) {
    final SqlNode ast = parse(string, MySQLParser::query);
    if (ast == null) return null;
    ast.context().setDbType(DbSupport.MySQL);
    return ast;
  }

  @Override
  public void setProperties(Properties props) {
    setServerVersion((int) props.getOrDefault("serverVersion", this.serverVersion));
    setSqlMode((int) props.getOrDefault("sqlMode", this.sqlMode));
  }
}
