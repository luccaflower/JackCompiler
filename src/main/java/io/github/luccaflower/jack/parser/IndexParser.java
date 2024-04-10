package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.Optional;

class IndexParser {

    public Optional<Expression> parse(IteratingTokenizer tokenizer) {
        switch (tokenizer.peek()) {
            case Token.Symbol s when s.type() == Token.SymbolType.OPEN_SQUARE:
                break;
            default:
                return Optional.empty();
        }
        tokenizer.advance();
        var index = new ExpressionParser().parse(tokenizer);
        switch (tokenizer.advance()) {
            case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_SQUARE:
                break;
            default:
                throw new SyntaxError("Unexpected token");
        }
        return index;
    }

}
