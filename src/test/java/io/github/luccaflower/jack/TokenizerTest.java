package io.github.luccaflower.jack;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static io.github.luccaflower.jack.Token.KeywordType.CLASS;
import static org.assertj.core.api.Assertions.*;

class TokenizerTest {

    private final Tokenizer tokenizer = new Tokenizer();

    @Test
    void tokenizesClassKeyword() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("class")).flatMap(List::of).isEqualTo(List.of(new Token.Keyword(CLASS)));
    }

    @Test
    void skipsAllKindsOfWhitespace() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse(" \t\nclass")).first().isEqualTo(new Token.Keyword(CLASS));
    }

    @Test
    void parsesSymbols() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("{}")).flatMap(List::of)
            .isEqualTo(List.of(new Token.Symbol(Token.SymbolType.OPEN_BRACE),
                    new Token.Symbol(Token.SymbolType.CLOSE_BRACE)));
    }

    @Test
    void parsesIntegerLiterals() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("12345")).first().isEqualTo(new Token.IntegerLiteral(12345));
    }

    @Test
    void integerLiteralsAreAMaximumOf16BitsSigned() {
        assertThatThrownBy(() -> tokenizer.parse(String.valueOf(0xFFFF))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesStringLiterals() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("\"string literal\"")).first().isEqualTo(new Token.StringLiteral("string literal"));
    }

    @Test
    void stringLiteralsMayNotContainDoubleQuotes() {
        assertThatThrownBy(() -> tokenizer.parse("\"string \" literal\"")).isInstanceOf(Tokenizer.SyntaxError.class);
    }

    @Test
    void stringLiteralsAreSingleLineOnly() {
        String input = """
                "string
                literal"
                """;
        assertThatThrownBy(() -> tokenizer.parse(input)).isInstanceOf(Tokenizer.SyntaxError.class);
    }

    @Test
    void parsesIdentifiers() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("identifier")).first().isEqualTo(new Token.Identifier("identifier"));
    }

    @Test
    void identifiersMayNotStartWithNumbers() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("0identifier")).flatMap(List::of)
            .isEqualTo(List.of(new Token.IntegerLiteral(0), new Token.Identifier("identifier")));
    }

    @Test
    void identifiersMayContainerUnderscores() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("_identifier")).first().isEqualTo(new Token.Identifier("_identifier"));
    }

    @Test
    void skipsLineComments() throws Tokenizer.SyntaxError {
        String input = "//comment class {}";
        assertThat(tokenizer.parse(input)).isEmpty();
    }

    @Test
    void keepsParsingTokensOnNewLineAfterComment() throws Tokenizer.SyntaxError {
        String input = """
                //comment
                class""";
        assertThat(tokenizer.parse(input)).first().isEqualTo(new Token.Keyword(CLASS));
    }

    @Test
    void skipsBlockComments() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("/*class {}*/")).isEmpty();
    }

    @Test
    void blockCommentsMaySpanMultipleLines() throws Tokenizer.SyntaxError {
        var input = """
                /*block
                comment*/
                """;
        assertThat(tokenizer.parse(input)).isEmpty();
    }

    @Test
    void aStringLiteralMayBeEmpty() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("\"\"")).first().isEqualTo(new Token.StringLiteral(""));
    }

}
