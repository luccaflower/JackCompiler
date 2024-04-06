package io.github.luccaflower.jack;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;

public final class TokenizerUtils {

    private TokenizerUtils() {
    }

    public static IteratingTokenizer tokenize(String input) {
        return new IteratingTokenizer(input);
    }

}
