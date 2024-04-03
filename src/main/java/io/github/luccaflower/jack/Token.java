package io.github.luccaflower.jack;

import java.util.Arrays;

public sealed interface Token {

    record Keyword(KeywordType type) implements Token {
    }

    enum KeywordType {

        CLASS("class"), METHOD("method"), FUNCTION("function"), CONSTRUCTOR("constructor"), INT("int"),
        BOOLEAN("boolean"), CHAR("char"), VOID("void"), VAR("var"), STATIC("static"), FIELD("field"), LET("let"),
        DO("do"), IF("if"), ELSE("else"), WHILE("while"), RETURN("return"), TRUE("true"), FALSE("false"), NULL("null"),
        THIS("this");

        private final String keyword;

        KeywordType(String keyword) {
            this.keyword = keyword;
        }

        public static KeywordType from(String name) {
            return Arrays.stream(values())
                .filter(k -> k.keyword.equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unknown keyword: ".concat(name)));
        }

        public String keyword() {
            return keyword;
        }

    }

    record Symbol(SymbolType type) implements Token {
    }

    enum SymbolType {

        OPEN_BRACE('{'), CLOSE_BRACE('}'), OPEN_PAREN('('), CLOSE_PAREN(')'), OPEN_SQUARE('['), CLOSE_SQUARE(']'),
        DOT('.'), COMMA(','), SEMICOLON(';'), PLUS('+'), MINUS('-'), ASTERISK('*'), SLASH('/'), AMPERSAND('&'),
        PIPE('|'), LESS_THAN('<'), GREATER_THAN('>'), EQUALS('='), TILDE('~');

        private final char symbol;

        SymbolType(char symbol) {
            this.symbol = symbol;
        }

        public static SymbolType from(char c) {
            return Arrays.stream(values())
                .filter(t -> t.symbol == c)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unknown symbol: ".concat(String.valueOf(c))));
        }

        public char symbol() {
            return symbol;
        }

    }

    record IntegerLiteral(int i) implements Token {
        public IntegerLiteral {
            if (i > 0x7FFF) {
                throw new IllegalArgumentException("Integer literal cannot exceed " + 0x7FFF);
            }
        }
    }

    record StringLiteral(String literal) implements Token {
    }

    record Identifier(String name) implements Token {
    }

}
