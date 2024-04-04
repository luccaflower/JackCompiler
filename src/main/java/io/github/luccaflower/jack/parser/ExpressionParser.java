package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.Token;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public class ExpressionParser {

    public Optional<Expression> parse(Queue<Token> tokens) {
        return new TermParser().parse(tokens)
                .map(t -> new Expression(t, new OperatorParser().parse(tokens)
                        .map(o -> new OpAndExpression(o, new ExpressionParser().parse(tokens).get()))));
    }

    public static class OperatorParser {
        public Optional<Operator> parse (Queue<Token> tokens) {
            if (tokens.peek() instanceof Token.Symbol s) {
                return switch (s.type()) {
                    case PLUS, MINUS, ASTERISK, SLASH, AMPERSAND, PIPE, LESS_THAN, GREATER_THAN, EQUALS -> {
                        tokens.remove();
                        yield Optional.of(Operator.from(s));
                    }
                    default -> Optional.empty();
                };
            } else {
                return Optional.empty();
            }
        }
    }
    public enum Operator {
        PLUS,
        MINUS,
        TIMES,
        DIVIDED_BY,
        BITWISE_AND,
        BITWISE_OR,
        LESS_THAN,
        GREATER_THAN,
        EQUALS;

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

    public record Expression(Term term, Optional<OpAndExpression> continuation) {}

    public record OpAndExpression(Operator op, Expression term) {}

    public class TermParser {
        public Optional<Term> parse(Queue<Token> tokens) {
            return new ConstantParser().parse(tokens)
                    .or(() -> new VarNameParser().parse(tokens))
                    .or(() -> new KeywordLiteralParser().parse(tokens))
                    .or(() -> new UnaryOpTermParser().parse(tokens));
        }

    }
    public static class ConstantParser {
        public Optional<Term> parse(Queue<Token> tokens) {
            return switch (Objects.requireNonNull(tokens.peek())) {
                case Token t when (t instanceof Token.StringLiteral || t instanceof Token.IntegerLiteral) ->
                        Optional.of(new Term.Constant(tokens.remove()));
                default -> Optional.empty();
            };
        }
    }

    public class VarNameParser {
        public Optional<Term> parse(Queue<Token> tokens) {
            if (tokens.peek() instanceof Token.Identifier) {
                return Optional.of(Term.VarName.from(tokens.remove()));
            } else {
                return Optional.empty();
            }
        }
    }

    public static class KeywordLiteralParser {
        public Optional<Term> parse(Queue<Token> tokens) {
            if (tokens.peek() instanceof Token.Keyword k) {
                tokens.remove();
                return Optional.of(new Term.KeywordLiteral(k.type()));
            } else {
                return Optional.empty();
            }
        }
    }

    public class UnaryOpTermParser {
        public Optional<Term> parse(Queue<Token> tokens) {
            if (tokens.peek() instanceof Token.Symbol s) {
                //TODO: refactor this mess
                return switch (s.type()) {
                    case TILDE, MINUS -> {
                        var copy = new ArrayDeque<>(tokens);
                        copy.remove(); //what are we even DOING
                        Optional<Term> term = new TermParser().parse(copy)
                                .map(t -> new Term.UnaryOpTerm(Term.UnaryOp.from(s), t));
                        term.ifPresent(t -> {
                            tokens.remove(); //this is what happens when you try to mix functional and procedural programming
                            tokens.remove();
                        });
                        yield term;
                    }
                    default -> Optional.empty();
                };

            } else {
                return Optional.empty();
            }
        }
    }
}
