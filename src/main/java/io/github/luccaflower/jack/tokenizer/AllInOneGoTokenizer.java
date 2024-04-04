package io.github.luccaflower.jack.tokenizer;

import java.util.*;

public class AllInOneGoTokenizer {

    public Queue<Token> parse(String input) throws SyntaxError {
        var list = new ArrayDeque<Token>();
        var iterator = new IteratingTokenizer(input);
        while (iterator.hasMoreTokens()) {
            list.add(iterator.advance());
        }

        return list;
    }

}
