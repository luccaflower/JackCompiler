package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

public class Parser {

    public JackClass parse(IteratingTokenizer tokenizer) {
        var next = tokenizer.advance();
        return switch (next) {
            case Token.Keyword k when k.type() == Token.KeywordType.CLASS ->
                new ClassParser().parse(tokenizer).orElseThrow(() -> new SyntaxError("failed to parse class"));
            default -> throw new SyntaxError("Unexpected token: %s".formatted(next));
        };
    }

}
