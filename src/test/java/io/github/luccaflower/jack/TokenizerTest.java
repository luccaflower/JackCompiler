package io.github.luccaflower.jack;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.luccaflower.jack.Token.KeywordType.CLASS;
import static org.assertj.core.api.Assertions.*;

class TokenizerTest {

    private final Tokenizer tokenizer = new Tokenizer();

    @Test
    void tokenizesClassKeyword() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("class")).isEqualTo(List.of(new Token.Keyword(CLASS)));
    }

    @Test
    void skipsAllKindsOfWhitespace() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse(" \t\nclass")).first().isEqualTo(new Token.Keyword(CLASS));
    }

    @Test
    void parsesSymbols() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("{}")).isEqualTo(List.of(new Token.Symbol(Token.SymbolType.OPEN_BRACE), new Token.Symbol(Token.SymbolType.CLOSE_BRACE)));
    }

    @Test
    void parsesIntegerLiterals() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("12345")).first().isEqualTo(new Token.IntegerLiteral(12345));
    }

    @Test
    void integerLiteralsAreAMaximumOf16BitsSigned() {
        assertThatThrownBy(() -> tokenizer.parse(String.valueOf(0xFFFF)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesStringLiterals() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("\"string literal\"")).first()
                .isEqualTo(new Token.StringLiteral("string literal"));
    }

    @Test
    void stringLiteralsMayNotContainDoubleQuotes() {
        assertThatThrownBy(() -> tokenizer.parse("\"string \" literal\""))
                .isInstanceOf(Tokenizer.SyntaxError.class);
    }

    @Test
    void stringLiteralsAreSingleLineOnly() {
        assertThatThrownBy(() -> tokenizer.parse("\"string \n literal\""))
                .isInstanceOf(Tokenizer.SyntaxError.class);
    }

    @Test
    void parsesIdentifiers() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("identifier")).first()
                .isEqualTo(new Token.Identifier("identifier"));
    }

    @Test
    void identifiersMayNotStartWithNumbers() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("0identifier")).isEqualTo(List.of(new Token.IntegerLiteral(0), new Token.Identifier("identifier")));
    }

    @Test
    void identifiersMayContainerUnderscores() throws Tokenizer.SyntaxError {
        assertThat(tokenizer.parse("_identifier")).first()
                .isEqualTo(new Token.Identifier("_identifier"));
    }

}
