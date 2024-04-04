package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public class ExpressionParser {

    public Optional<Expression> parse(IteratingTokenizer tokens) {
        return new TermParser().parse(tokens)
            .map(term -> new Expression(term, new OperatorParser().parse(tokens)
                .map(op -> new ExpressionParser().parse(tokens)
                    .map(e -> new OpAndExpression(op, e))
                    .orElseThrow(
                            () -> new SyntaxError("Invalid continuation to %s: %s".formatted(op, tokens.peek()))))));
    }

    public static class OperatorParser {

        public Optional<Operator> parse(IteratingTokenizer tokens) throws SyntaxError {
            return tokens.peek()
                .filter(t -> t instanceof Token.Symbol)
                .flatMap(t -> switch (((Token.Symbol) t).type()) {
                    case PLUS, MINUS, ASTERISK, SLASH, AMPERSAND, PIPE, LESS_THAN, GREATER_THAN, EQUALS -> {
                        tokens.advance();
                        yield Optional.of(Operator.from((Token.Symbol) t));
                    }
                    default -> Optional.empty();

                });
        }

    }

    public enum Operator {

        PLUS, MINUS, TIMES, DIVIDED_BY, BITWISE_AND, BITWISE_OR, LESS_THAN, GREATER_THAN, EQUALS;

        public static Operator from(Token.Symbol s) {
            return switch (s.type()) {
                case PLUS -> PLUS;
                case MINUS -> MINUS;
                case ASTERISK -> TIMES;
                case SLASH -> DIVIDED_BY;
                case AMPERSAND -> BITWISE_AND;
                case PIPE -> BITWISE_OR;
                case LESS_THAN -> LESS_THAN;
                case GREATER_THAN -> GREATER_THAN;
                case EQUALS -> EQUALS;
                default -> throw new IllegalArgumentException("Invalid operator " + s.type());
            };
        }

    }

    public record Expression(Term term, Optional<OpAndExpression> continuation) {
    }

    public record OpAndExpression(Operator op, Expression term) {
    }

    public class TermParser {

        public Optional<Term> parse(IteratingTokenizer tokens) {
            return new ConstantParser().parse(tokens)
                .or(() -> new VarNameParser().parse(tokens))
                .or(() -> new KeywordLiteralParser().parse(tokens))
                .or(() -> new UnaryOpTermParser().parse(tokens));
        }

    }

    public static class ConstantParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            return tokens.peek()
                .filter(t -> t instanceof Token.StringLiteral || t instanceof Token.IntegerLiteral)
                .map(t -> new Term.Constant(tokens.remove()));
        }

    }

    public class VarNameParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            return tokens.peek().filter(t -> t instanceof Token.Identifier).map(Term.VarName::from);
        }

    }

    public static class KeywordLiteralParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            return tokens.peek()
                .filter(t -> t instanceof Token.Keyword)
                .map(t -> new Term.KeywordLiteral(((Token.Keyword) tokens.remove()).type()));
        }

    }

    public class UnaryOpTermParser {

        public Optional<Term> parse(IteratingTokenizer tokens) {
            return tokens.peek()
                .filter(t -> t instanceof Token.Symbol)
                .flatMap(t -> switch (((Token.Symbol) t).type()) {
                    case TILDE, MINUS -> Optional.of(Term.UnaryOp.from((Token.Symbol) t));
                    default -> Optional.empty();
                })
                .flatMap(op -> new TermParser().parse(tokens.lookAhead(1)).map(term -> new Term.UnaryOpTerm(op, term)));
        }

    }

}
