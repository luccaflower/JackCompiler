package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.TokenizerUtils;
import io.github.luccaflower.jack.parser.Parser.Term.*;
import io.github.luccaflower.jack.tokenizer.Token;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionParserTest {

    private final Parser.ExpressionParser parser = new Parser.ExpressionParser();

    @Test
    void constantsMayBeStringLiterals() {
        assertThat(parser.parse(TokenizerUtils.tokenize("\"literal\""))).map(Parser.ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new Constant(new Token.StringLiteral("literal")));

    }

    @Test
    void constantsMayBeIntegerLiterals() {
        assertThat(parser.parse(TokenizerUtils.tokenize("5"))).map(Parser.ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new Constant(new Token.IntegerLiteral(5)));
    }

    @Test
    void anythingElseMayNotBeAConstant() {
        assertThat(parser.parse(TokenizerUtils.tokenize("identifier"))).isNotInstanceOf(Constant.class);
    }

    @Test
    void aVarNameIsAnIdentifier() {
        assertThat(parser.parse(TokenizerUtils.tokenize("name"))).map(Parser.ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new VarName("name"));
    }

    @Test
    void aKeyWordLiteralIsEitherTrueOrFalseOrNullOrThis() {
        assertThat(parser.parse(TokenizerUtils.tokenize("true"))).map(Parser.ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new KeywordLiteral(Token.KeywordType.TRUE));
    }

    @Test
    void aTermIsAVarNameOrAConstantOrAKeywordLiteral() {
        assertThat(parser.parse(TokenizerUtils.tokenize("name"))).map(Parser.ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new VarName("name"));
    }

    @Test
    void unaryOpTerms() {
        assertThat(parser.parse(TokenizerUtils.tokenize("~5"))).map(Parser.ExpressionParser.Expression::term)
            .get()
            .isEqualTo(new UnaryOpTerm(UnaryOp.NOT, new Constant(new Token.IntegerLiteral(5))));
    }

    @Test
    void severalTermsInOneExpression() {
        assertThat(parser.parse(TokenizerUtils.tokenize("5+\"literal\""))).get()
            .isEqualTo(new Parser.ExpressionParser.Expression(new Constant(new Token.IntegerLiteral(5)),
                    Optional.of(new Parser.ExpressionParser.OpAndExpression(Parser.ExpressionParser.Operator.PLUS,
                            new Parser.ExpressionParser.Expression(new Constant(new Token.StringLiteral("literal")),
                                    Optional.empty())))));
    }


}
