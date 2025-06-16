package sqlsolver.sql;

/**
 * fix antlr memory leak
 * @see <a href="https://github.com/antlr/antlr4/issues/499"> Memory Leak </a>
 * @author victorchu
 * @date 2022/8/8 11:29
 */
import lombok.SneakyThrows;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class RefreshableParserInitializer<L extends Lexer,P extends Parser> implements BiConsumer<L , P > {
    private final AtomicReference<ParserAndLexerATNCaches<L,P>> caches = new AtomicReference<>();

    public RefreshableParserInitializer() {
        Type superClass = this.getClass().getGenericSuperclass();
        lexer = (Class<?>)((ParameterizedType)superClass).getActualTypeArguments()[0];
        parser = (Class<?>)((ParameterizedType)superClass).getActualTypeArguments()[1];
        refresh();
    }

    public void refresh() {
        caches.set(buildATNCache());
    }

    // ================= 泛型处理 =====================
    private final Class<?> lexer;
    private final Class<?> parser;

    @SneakyThrows({SecurityException.class, NoSuchFieldException.class,IllegalAccessException.class})
    private static ATN getATNField(Class<?> clazz){
        Field field = clazz.getDeclaredField("_ATN");
        field.setAccessible(true);
        return (ATN) field.get(null);

    }

    private ParserAndLexerATNCaches<L,P> buildATNCache(){
        ATN lexerATN =getATNField(lexer);
        ATN parserATN = getATNField(parser);
        return new ParserAndLexerATNCaches(new AntlrATNCacheFields(lexerATN),new AntlrATNCacheFields(parserATN));
    }
    // ================= 泛型处理 =====================

    @Override
    public void accept(L l, P p) {
        ParserAndLexerATNCaches<L,P> cache =caches.get();
        cache.lexer.configureLexer(l);
        cache.parser.configureParser(p);
    }

    private static final class ParserAndLexerATNCaches<L extends Lexer,P extends Parser>
    {
        public ParserAndLexerATNCaches(AntlrATNCacheFields lexer, AntlrATNCacheFields parser) {
            this.lexer = lexer;
            this.parser = parser;
        }

        public final AntlrATNCacheFields lexer;
        public final AntlrATNCacheFields parser;
    }
    public static final class AntlrATNCacheFields
    {
        private final ATN atn;
        private final PredictionContextCache predictionContextCache;
        private final DFA[] decisionToDFA;

        public AntlrATNCacheFields(ATN atn)
        {
            this.atn = requireNonNull(atn, "atn is null");
            this.predictionContextCache = new PredictionContextCache();
            this.decisionToDFA = createDecisionToDFA(atn);
        }

        @SuppressWarnings("ObjectEquality")
        public void configureLexer(Lexer lexer)
        {
            requireNonNull(lexer, "lexer is null");
            // Intentional identity equals comparison
            checkArgument(atn == lexer.getATN(), "Lexer ATN mismatch: expected %s, found %s", atn, lexer.getATN());
            lexer.setInterpreter(new LexerATNSimulator(lexer, atn, decisionToDFA, predictionContextCache));
        }

        @SuppressWarnings("ObjectEquality")
        public void configureParser(Parser parser)
        {
            requireNonNull(parser, "parser is null");
            // Intentional identity equals comparison
            checkArgument(atn == parser.getATN(), "Parser ATN mismatch: expected %s, found %s", atn, parser.getATN());
            parser.setInterpreter(new ParserATNSimulator(parser, atn, decisionToDFA, predictionContextCache));
        }

        private static DFA[] createDecisionToDFA(ATN atn)
        {
            DFA[] decisionToDFA = new DFA[atn.getNumberOfDecisions()];
            for (int i = 0; i < decisionToDFA.length; i++) {
                decisionToDFA[i] = new DFA(atn.getDecisionState(i), i);
            }
            return decisionToDFA;
        }
    }
}
