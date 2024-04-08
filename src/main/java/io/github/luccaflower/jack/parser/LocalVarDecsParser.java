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
        while (localVarParser.parse(tokenizer).orElse(null) instanceof Map<String, LocalVar> m) {
            m.forEach((n, v) -> locals.put(n, v.type()));
        }
        return locals;
    }

    static class LocalVarParser {

        Optional<Map<String, LocalVar>> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.VAR: break;
                default: return Optional.empty();
            }
            tokenizer.advance();
            var typeAndNames = new VarTypeAndNamesParser().parse(tokenizer);

            return Optional.of(typeAndNames.names()
                    .stream()
                    .collect(Collectors.toMap(name -> name, name -> new LocalVar(name, typeAndNames.type()))));
        }

    }

    record LocalVar(String name, Type.VarType type) {
    }

}
