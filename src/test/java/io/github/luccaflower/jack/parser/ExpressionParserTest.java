package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.parser.Term.*;
import io.github.luccaflower.jack.tokenizer.Token;
import io.github.luccaflower.jack.tokenizer.Token.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.github.luccaflower.jack.TokenizerUtils.tokenize;
import static org.assertj.core.api.Assertions.assertThat;

class ExpressionParserTest {

    private final ExpressionParser parser = new ExpressionParser();

    @Test
    void constantsMayBeStringLiterals() {
        assertThat(parser.parse(tokenize("\"literal\""))).map(Expression::term)
            .get()
            .isEqualTo(new Constant(new Token.StringLiteral("literal")));

    }

    @Test
    void constantsMayBeIntegerLiterals() {
        assertThat(parser.parse(tokenize("5"))).map(Expression::term)
            .get()
            .isEqualTo(new Constant(new IntegerLiteral(5)));
    }

    @Test
    void aKeyWordLiteralIsEitherTrueOrFalseOrNullOrThis() {
        assertThat(parser.parse(tokenize("true"))).map(Expression::term)
            .get()
            .isEqualTo(new KeywordLiteral(Token.KeywordType.TRUE));
    }

    @Test
    void unaryOpTerms() {
        assertThat(parser.parse(tokenize("~5"))).map(Expression::term)
            .get()
            .isEqualTo(new UnaryOpTerm(UnaryOp.NOT, new Constant(new IntegerLiteral(5))));
    }

    @Test
    void severalTermsInOneExpression() {
        assertThat(parser.parse(tokenize("5+\"literal\""))).get()
            .isEqualTo(new Expression(new Constant(new IntegerLiteral(5)),
                    Optional.of(new Expression.OpAndExpression(Expression.Operator.PLUS,
                            new Expression(new Constant(new StringLiteral("literal")), Optional.empty())))));
    }

}
