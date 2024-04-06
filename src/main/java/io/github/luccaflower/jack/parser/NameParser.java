package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.Optional;

public class NameParser {

    public Optional<String> parse(IteratingTokenizer tokenizer) {
        return switch (tokenizer.peek().orElseThrow(() -> new SyntaxError("Unexpected end of input"))) {
            case Token.Identifier ignored -> Optional.of(((Token.Identifier) tokenizer.advance()).name());
            default -> Optional.empty();
        };
    }

}
