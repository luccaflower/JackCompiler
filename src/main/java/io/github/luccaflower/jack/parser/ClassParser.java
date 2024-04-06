package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

class ClassParser {

    private final StartBlockParser startBlockParser = new StartBlockParser();

    private final ClassVarDecsParser classVarDecsParser = new ClassVarDecsParser();

    private final SubroutinesDecsParser subroutinesDecsParser = new SubroutinesDecsParser();

    private final EndBlockParser endBlockParser = new EndBlockParser();

    Optional<JackClass> parse(IteratingTokenizer tokenizer) {
        var next = tokenizer.advance();
        var className = switch (next) {
            case Token.Identifier i -> i.name();
            default -> throw new SyntaxError("Expected identifer, got %s".formatted(next));
        };
        startBlockParser.parse(tokenizer);
        var classVarDecs = classVarDecsParser.parse(tokenizer);
        var subroutineDecs = subroutinesDecsParser.parse(tokenizer);
        endBlockParser.parse(tokenizer);
        return Optional.of(JackClass.builder()
            .name(className)
            .statics(classVarDecs.statics())
            .fields(classVarDecs.fields())
            .subroutines(subroutineDecs.subroutines())
            .build());
    }

}
