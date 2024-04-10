package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.*;

class ExpressionParser {

    public Optional<Expression> parse(IteratingTokenizer tokens) {
        return new TermParser().parse(tokens)
            .map(term -> new Expression(term, new OperatorParser().parse(tokens)
                .map(op -> new ExpressionParser().parse(tokens)
                    .map(e -> new Expression.OpAndExpression(op, e))
                    .orElseThrow(
                            () -> new SyntaxError("Invalid continuation to %s: %s".formatted(op, tokens.peek()))))));
    }

    static class OperatorParser {

        public Optional<Expression.Operator> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }

            if (!(tokens.peek() instanceof Token.Symbol s)) {
                return Optional.empty();
            }

            return switch (s.type()) {
                case PLUS, MINUS, ASTERISK, SLASH, AMPERSAND, PIPE, LESS_THAN, GREATER_THAN, EQUALS -> {
                    tokens.advance();
                    yield Optional.of(Expression.Operator.from(s));
                }
                default -> Optional.empty();
            };
        }

    }

    static class ExpressionListParser {

        public List<Expression> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.advance()) {
                case Token.Symbol s when s.type() == OPEN_PAREN:
                    break;
                default:
                    throw new SyntaxError("Unexpected token");
            }
            var list = new ArrayList<Expression>();
            loop: while (new ExpressionParser().parse(tokenizer).orElse(null) instanceof Expression e) {
                list.add(e);
                switch (tokenizer.peek()) {
                    case Token.Symbol s when s.type() == COMMA:
                        tokenizer.advance();
                        continue;
                    default:
                        break loop;
                }
            }
            switch (tokenizer.advance()) {
                case Token.Symbol s when s.type() == CLOSE_PAREN:
                    break;
                default:
                    throw new SyntaxError("Unexpected token");
            }
            return list;
        }

    }

}
