package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class LocalVarDecsParser {

    Map<String, Type.VarType> parse(IteratingTokenizer tokenizer) {
        var locals = new HashMap<String, Type.VarType>();
        var localVarParser = new LocalVarParser();
        while (localVarParser.parse(tokenizer).orElse(null) instanceof VarTypeAndNamesParser.VarTypeAndNames v) {
            v.names().forEach(n -> locals.put(n, v.type()));
        }
        return locals;
    }

    static class LocalVarParser {

        Optional<VarTypeAndNamesParser.VarTypeAndNames> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.VAR:
                    break;
                default:
                    return Optional.empty();
            }
            tokenizer.advance();
            var typeAndNames = new VarTypeAndNamesParser().parse(tokenizer);

            return Optional.of(typeAndNames);
        }

    }

}
