package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.Optional;

public class NameParser {

    public Optional<String> parse(IteratingTokenizer tokenizer) {
        return switch (tokenizer.peek()) {
            case Token.Identifier i -> {
                tokenizer.advance();
                yield Optional.of(i.name());
            }
            default -> Optional.empty();
        };
    }

}
