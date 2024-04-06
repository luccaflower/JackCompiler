package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

class EndBlockParser {

    void parse(IteratingTokenizer tokenizer) {

        switch (tokenizer.advance()) {
            case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_BRACE:
                break;
            default:
                throw new SyntaxError("Expected }");
        }
    }

}
