package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.Token;

public sealed interface Term {
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
            } else {
                throw new IllegalArgumentException("VarName must be an identifier");
            }
        }
    }

    record KeywordLiteral(Token.KeywordType type) implements Term {
        public KeywordLiteral {
            switch (type) {
                case TRUE, FALSE, NULL, THIS -> {
                }
                default -> throw new IllegalArgumentException("Only true and false are valid keyword constants");
            }
        }
    }

    record UnaryOpTerm(UnaryOp op, Term term) implements Term {
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
