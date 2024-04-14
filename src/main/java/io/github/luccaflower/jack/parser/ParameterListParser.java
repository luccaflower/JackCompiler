package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

class ParameterListParser {

    List<Parameter> parseAsList(IteratingTokenizer tokenizer) {
        switch (tokenizer.advance()) {
            case Token.Symbol s when s.type() == Token.SymbolType.OPEN_PAREN:
                break;
            default:
                throw new SyntaxError("Expected parameter list");
        }
        var arguments = new ArrayList<Parameter>();
        var parameterParser = new ParameterParser();
        while (parameterParser.parse(tokenizer).orElse(null) instanceof Parameter p) {
            arguments.add(p);
            switch (tokenizer.peek()) {
                case Token.Symbol s when s.type() == Token.SymbolType.COMMA: {
                    tokenizer.advance();
                    break;
                }
                default:
                    break;
            }
        }
        switch (tokenizer.advance()) {
            case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_PAREN:
                break;
            default:
                throw new SyntaxError("Expected end of parameter list");
        }
        return arguments;
    }

    static class ParameterParser {

        Optional<Parameter> parse(IteratingTokenizer tokenizer) {
            return new TypeParser.VarTypeParser().parse(tokenizer)
                .map(t -> new Parameter(
                        new NameParser().parse(tokenizer).orElseThrow(() -> new SyntaxError("Expected name")), t));
        }

    }

}
