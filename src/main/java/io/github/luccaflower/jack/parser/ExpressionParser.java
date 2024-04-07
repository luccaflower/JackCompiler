package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.Optional;

import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.MINUS;
import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.TILDE;

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

            if(!(tokens.peek() instanceof Token.Symbol s)) {
                return Optional.empty();
            }

            return switch (s.type()) {
                case PLUS, MINUS, ASTERISK, SLASH, AMPERSAND, PIPE, LESS_THAN, GREATER_THAN, EQUALS -> {
                    tokens.advance();
                    yield Optional.of(Operator.from(s));
                }
                default -> Optional.empty();
            };
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
            switch (tokens.peek()) {
                case Token.IntegerLiteral ignored: break;
                case Token.StringLiteral ignored: break;
                default: return Optional.empty();
            }
            return Optional.of(new Term.Constant(tokens.advance()));
        }

    }

    static class VarNameParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            if (!(tokens.peek() instanceof Token.Identifier i)) {
                return Optional.empty();
            }
            tokens.advance();
            return Optional.of(new Term.VarName(i.name()));
        }

    }

    static class KeywordLiteralParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            if (!(tokens.peek() instanceof Token.Keyword k)) {
                return Optional.empty();
            }
            tokens.advance();
            return Optional.of(new Term.KeywordLiteral(k.type()));
        }

    }

    class UnaryOpTermParser {

        public Optional<Term> parse(IteratingTokenizer tokens) {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            switch (tokens.peek()) {
                case Token.Symbol s when s.type() == TILDE || s.type() == Token.SymbolType.MINUS: break;
                default: return Optional.empty();
            }
            var nextTerm = new TermParser().parse(tokens.lookAhead(1));
            if (nextTerm.isPresent()) {
                var op = tokens.advance();
                tokens.advance();
                return Optional.of(new Term.UnaryOpTerm(Term.UnaryOp.from(op), nextTerm.get()));
            }
            else {
                return Optional.empty();
            }
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

            public static UnaryOp from(Token symbol) {
                return switch (symbol) {
                    case Token.Symbol s when s.type() == TILDE -> NOT;
                    case Token.Symbol s when s.type() == MINUS  -> NEGATIVE;
                    default -> throw new IllegalArgumentException("Unary operator must be either ~ or -");
                };
            }

        }

    }

}
