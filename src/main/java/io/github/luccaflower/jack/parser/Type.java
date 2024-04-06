package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

sealed interface Type {

    enum PrimitiveType implements Type {

        INT, CHAR, BOOLEAN;

        public static PrimitiveType from(Token.Keyword k) {
            return switch (k.type()) {
                case INT -> INT;
                case CHAR -> CHAR;
                case BOOLEAN -> BOOLEAN;
                default -> throw new SyntaxError("Unexpeceted type %s".formatted(k));
            };
        }

    }

    record ClassType(String name) implements Type {
    }

}