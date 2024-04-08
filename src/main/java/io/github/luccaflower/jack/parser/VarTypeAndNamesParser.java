package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.HashSet;
import java.util.Set;

class VarTypeAndNamesParser {

    VarTypeAndNames parse(IteratingTokenizer tokenizer) {
        var type = new TypeParser.VarTypeParser().parse(tokenizer)
            .orElseThrow(() -> new SyntaxError("Field must have a type"));
        Set<String> names = new HashSet<>();
        var nameParser = new NameParser();
        loop: while (nameParser.parse(tokenizer).orElse(null) instanceof String name) {
            names.add(name);
            switch (tokenizer.advance()) {
                case Token.Symbol s when s.type() == Token.SymbolType.COMMA:
                    continue;
                case Token.Symbol s when s.type() == Token.SymbolType.SEMICOLON:
                    break loop;
                default:
                    throw new SyntaxError("Unexpected token");
            }
        }
        return new VarTypeAndNames(type, names);
    }

    record VarTypeAndNames(Type.VarType type, Set<String> names) {
    }

}
