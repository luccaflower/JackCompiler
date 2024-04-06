package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.Optional;

class ExpressionParser {

    public Optional<Expression> parse(IteratingTokenizer tokens) {
        return new TermParser().parse(tokens)
            .map(term -> new Expression(term, new OperatorParser().parse(tokens)
                .map(op -> new ExpressionParser().parse(tokens)
                    .map(e -> new OpAndExpression(op, e))
                    .orElseThrow(
                            () -> new SyntaxError("Invalid continuation to %s: %s".formatted(op, tokens.peek()))))));
    }

    static class OperatorParser {

        public Optional<Operator> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }

            if (tokens.peek() instanceof Token.Symbol s) {
                return switch (s.type()) {
                    case PLUS, MINUS, ASTERISK, SLASH, AMPERSAND, PIPE, LESS_THAN, GREATER_THAN, EQUALS -> {
                        tokens.advance();
                        yield Optional.of(Operator.from(s));
                    }
                    default -> Optional.empty();
                };
            }
            else {
                return Optional.empty();
            }
        }

    }

    enum Operator {

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

    record Expression(Term term, Optional<OpAndExpression> continuation) {
    }

    record OpAndExpression(Operator op, Expression term) {
    }

    class TermParser {

        public Optional<Term> parse(IteratingTokenizer tokens) {
            return new ConstantParser().parse(tokens)
                .or(() -> new VarNameParser().parse(tokens))
                .or(() -> new KeywordLiteralParser().parse(tokens))
                .or(() -> new UnaryOpTermParser().parse(tokens));
        }

    }

    static class ConstantParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            return switch (tokens.peek()) {
                case Token token when (token instanceof Token.IntegerLiteral || token instanceof Token.StringLiteral) ->
                    Optional.of(new Term.Constant(tokens.advance()));
                default -> Optional.empty();

            };
        }

    }

    static class VarNameParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            return switch (tokens.peek()) {
                case Token.Identifier i -> {
                    tokens.advance();
                    yield Optional.of(new Term.VarName(i.name()));
                }
                default -> Optional.empty();
            };
        }

    }

    static class KeywordLiteralParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            return switch (tokens.peek()) {
                case Token.Keyword k -> {
                    tokens.advance();
                    yield Optional.of(new Term.KeywordLiteral(k.type()));
                }
                default -> Optional.empty();
            };
        }

    }

    class UnaryOpTermParser {

        public Optional<Term> parse(IteratingTokenizer tokens) {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            return switch (tokens.peek()) {
                case Token.Symbol s when s.type() == Token.SymbolType.TILDE || s.type() == Token.SymbolType.MINUS -> {
                    var nextTerm = new TermParser().parse(tokens.lookAhead(1));
                    if (nextTerm.isPresent()) {
                        tokens.advance();
                        tokens.advance();
                        yield Optional.of(new Term.UnaryOpTerm(Term.UnaryOp.from(s), nextTerm.get()));
                    }
                    else {
                        yield Optional.empty();
                    }
                }
                default -> Optional.empty();
            };
        }

    }

    sealed interface Term {

        record Constant(Token literal) implements Term {
            public Constant {
                if (!(literal instanceof Token.StringLiteral) && !(literal instanceof Token.IntegerLiteral)) {
                    throw new IllegalArgumentException("Constant must be either a string or integer literal");
                }
            }
        }

        record VarName(String name) implements Term {
            public static VarName from(Token token) {
                if (token instanceof Token.Identifier(String name)) {
                    return new VarName(name);
                }
                else {
                    throw new IllegalArgumentException("VarName must be an identifier");
                }
            }
        }

        record KeywordLiteral(Token.KeywordType type) implements Term {
            public KeywordLiteral {
                switch (type) {
                    case TRUE, FALSE, NULL, THIS:
                        break;
                    default:
                        throw new IllegalArgumentException("Only true and false are valid keyword constants");
                }
            }
        }

        record UnaryOpTerm(Term.UnaryOp op, Term term) implements Term {
        }

        enum UnaryOp {

            NEGATIVE, NOT;

            public static UnaryOp from(Token.Symbol symbol) {
                return switch (symbol.type()) {
                    case TILDE -> NOT;
                    case MINUS -> NEGATIVE;
                    default -> throw new IllegalArgumentException("Unary operator must be either ~ or -");
                };
            }

        }

    }

}
