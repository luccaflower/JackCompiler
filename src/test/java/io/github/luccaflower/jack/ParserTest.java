package io.github.luccaflower.jack;

import io.github.luccaflower.jack.parser.ExpressionParser;
import io.github.luccaflower.jack.parser.Term.*;
import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {

    private final ExpressionParser parser = new ExpressionParser();

    @Test
    void constantsMayBeStringLiterals() {
        assertThat(parser.parse(tokenizer("\"literal\""))).map(ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new Constant(new Token.StringLiteral("literal")));

    }

    @Test
    void constantsMayBeIntegerLiterals() {
        assertThat(parser.parse(tokenizer("5"))).map(ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new Constant(new Token.IntegerLiteral(5)));
    }

    @Test
    void anythingElseMayNotBeAConstant() {
        assertThat(parser.parse(tokenizer("identifier"))).isNotInstanceOf(Constant.class);
    }

    @Test
    void aVarNameIsAnIdentifier() {
        assertThat(parser.parse(tokenizer("name"))).map(ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new VarName("name"));
    }

    @Test
    void aKeyWordLiteralIsEitherTrueOrFalseOrNullOrThis() {
        assertThat(parser.parse(tokenizer("true"))).map(ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new KeywordLiteral(Token.KeywordType.TRUE));
    }

    @Test
    void aTermIsAVarNameOrAConstantOrAKeywordLiteral() {
        assertThat(parser.parse(tokenizer("name"))).map(ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new VarName("name"));
    }

    @Test
    void unaryOpTerms() {
        assertThat(parser.parse(tokenizer("~5"))).map(ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new UnaryOpTerm(UnaryOp.NOT, new Constant(new Token.IntegerLiteral(5))));
    }

    @Test
    void severalTermsInOneExpression() {
        assertThat(parser.parse(tokenizer("5+\"literal\""))).get()
            .isEqualTo(new ExpressionParser.Expression(new Constant(new Token.IntegerLiteral(5)),
                    Optional.of(new ExpressionParser.OpAndExpression(ExpressionParser.Operator.PLUS,
                            new ExpressionParser.Expression(new Constant(new Token.StringLiteral("literal")),
                                    Optional.empty())))));
    }

    private static IteratingTokenizer tokenizer(String input) {
        return new IteratingTokenizer(input);
    }

}
