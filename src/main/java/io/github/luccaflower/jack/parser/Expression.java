package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.Token;

import java.util.Optional;

public record Expression(Term term, Optional<OpAndExpression> continuation) {

    public record OpAndExpression(Operator op, Expression term) {
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
}
