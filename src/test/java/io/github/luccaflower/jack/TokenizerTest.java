package io.github.luccaflower.jack;

import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;
import io.github.luccaflower.jack.tokenizer.AllInOneGoTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.luccaflower.jack.tokenizer.Token.KeywordType.CLASS;
import static org.assertj.core.api.Assertions.*;

class TokenizerTest {

    private final AllInOneGoTokenizer tokenizer = new AllInOneGoTokenizer();

    @Test
    void tokenizesClassKeyword() throws SyntaxError {
        assertThat(tokenizer.parse("class")).flatMap(List::of).isEqualTo(List.of(new Token.Keyword(CLASS)));
    }

    @Test
    void skipsAllKindsOfWhitespace() throws SyntaxError {
        assertThat(tokenizer.parse(" \t\nclass")).first().isEqualTo(new Token.Keyword(CLASS));
    }

    @Test
    void parsesSymbols() throws SyntaxError {
        assertThat(tokenizer.parse("{}")).flatMap(List::of)
            .isEqualTo(List.of(new Token.Symbol(Token.SymbolType.OPEN_BRACE),
                    new Token.Symbol(Token.SymbolType.CLOSE_BRACE)));
    }

    @Test
    void parsesIntegerLiterals() throws SyntaxError {
        assertThat(tokenizer.parse("12345")).first().isEqualTo(new Token.IntegerLiteral(12345));
    }

    @Test
    void integerLiteralsAreAMaximumOf16BitsSigned() {
        assertThatThrownBy(() -> tokenizer.parse(String.valueOf(0xFFFF))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesStringLiterals() throws SyntaxError {
        assertThat(tokenizer.parse("\"string literal\"")).first().isEqualTo(new Token.StringLiteral("string literal"));
    }

    @Test
    void stringLiteralsMayNotContainDoubleQuotes() {
        assertThatThrownBy(() -> tokenizer.parse("\"string \" literal\"")).isInstanceOf(SyntaxError.class);
    }

    @Test
    void stringLiteralsAreSingleLineOnly() {
        String input = """
                "string
                literal"
                """;
        assertThatThrownBy(() -> tokenizer.parse(input)).isInstanceOf(SyntaxError.class);
    }

    @Test
    void parsesIdentifiers() throws SyntaxError {
        assertThat(tokenizer.parse("identifier")).first().isEqualTo(new Token.Identifier("identifier"));
    }

    @Test
    void identifiersMayNotStartWithNumbers() throws SyntaxError {
        assertThat(tokenizer.parse("0identifier")).flatMap(List::of)
            .isEqualTo(List.of(new Token.IntegerLiteral(0), new Token.Identifier("identifier")));
    }

    @Test
    void identifiersMayContainerUnderscores() throws SyntaxError {
        assertThat(tokenizer.parse("_identifier")).first().isEqualTo(new Token.Identifier("_identifier"));
    }

    @Test
    void skipsLineComments() throws SyntaxError {
        String input = "//comment class {}";
        assertThat(tokenizer.parse(input)).isEmpty();
    }

    @Test
    void keepsParsingTokensOnNewLineAfterComment() throws SyntaxError {
        String input = """
                //comment
                class""";
        assertThat(tokenizer.parse(input)).first().isEqualTo(new Token.Keyword(CLASS));
    }

    @Test
    void skipsBlockComments() throws SyntaxError {
        assertThat(tokenizer.parse("/*class {}*/")).isEmpty();
    }

    @Test
    void blockCommentsMaySpanMultipleLines() throws SyntaxError {
        var input = """
                /*block
                comment*/
                """;
        assertThat(tokenizer.parse(input)).isEmpty();
    }

    @Test
    void aStringLiteralMayBeEmpty() throws SyntaxError {
        assertThat(tokenizer.parse("\"\"")).first().isEqualTo(new Token.StringLiteral(""));
    }

}
