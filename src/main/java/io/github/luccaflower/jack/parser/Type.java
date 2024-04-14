package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

sealed public interface Type {
    String name();

    sealed interface VarType extends Type {

    }

    sealed interface ReturnType extends Type {

    }

    enum PrimitiveType implements VarType, ReturnType {

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

    record ClassType(String name) implements VarType, ReturnType {
    }

    record VoidType() implements ReturnType {
        public String name() {
            return "void";
        }
    }

}
