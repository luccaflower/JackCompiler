package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

class ClassParser {

    Optional<JackClass> parse(IteratingTokenizer tokenizer) {
        var next = tokenizer.advance();
        var className = switch (next) {
            case Token.Identifier i -> i.name();
            default -> throw new SyntaxError("Expected identifer, got %s".formatted(next));
        };
        var beginClass = tokenizer.advance();
        switch (beginClass) {
            case Token.Symbol s when s.type() == Token.SymbolType.OPEN_BRACE:
                break;
            default:
                throw new SyntaxError("Expected {, got %s".formatted(beginClass));
        }
        var classVarDecs = new ClassVarDecsParser().parse(tokenizer);
        var subroutineDecs = new SubroutinesDecsParser().parse(tokenizer);
        var endClass = tokenizer.advance();
        switch (endClass) {
            case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_BRACE:
                break;
            default:
                throw new SyntaxError("Expected }, got %s".formatted(endClass));
        }
        return Optional.of(JackClass.builder()
            .name(className)
            .statics(classVarDecs.statics())
            .fields(classVarDecs.fields())
            .subroutines(subroutineDecs.subroutines())
            .build());
    }

}
