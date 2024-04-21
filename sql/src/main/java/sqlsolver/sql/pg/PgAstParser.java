package sqlsolver.sql.pg;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sqlsolver.common.datasource.DbSupport;
import sqlsolver.sql.parser.AstParser;
import sqlsolver.sql.parser.ThrowingErrorListener;
import sqlsolver.sql.pg.internal.PGLexer;
import sqlsolver.sql.pg.internal.PGParser;
import sqlsolver.sql.ast.SqlNode;

import java.util.function.Function;

public class PgAstParser implements AstParser {
  public SqlNode parse(String str, Function<PGParser, ParserRuleContext> rule) {
    final PGLexer lexer = new PGLexer(CharStreams.fromString(str));
    final PGParser parser = new PGParser(new CommonTokenStream(lexer));

    lexer.removeErrorListeners();
    lexer.addErrorListener(ThrowingErrorListener.instance());
    parser.removeErrorListeners();
    parser.addErrorListener(ThrowingErrorListener.instance());

    //    lexer.getInterpreter().clearDFA();
    //    parser.getInterpreter().clearDFA();
    return rule.apply(parser).accept(new PgAstBuilder());
  }

  @Override
  public SqlNode parse(String string) {
    final SqlNode ast = parse(string, PGParser::statement);
    if (ast == null) return null;
    ast.context().setDbType(DbSupport.PostgreSQL);
    return ast;
  }
}
