package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class LocalVarDecsParser {

    private static final TerminateStatementParser terminateStatementParser = new TerminateStatementParser();

    Map<String, Type.VarType> parse(IteratingTokenizer tokenizer) {
        var locals = new HashMap<String, Type.VarType>();
        var localVarParser = new LocalVarParser();
        while (localVarParser.parse(tokenizer).orElse(null) instanceof LocalVar v) {
            locals.put(v.name(), v.type());
        }
        return locals;
    }

    static class LocalVarParser {

        Optional<LocalVar> parse(IteratingTokenizer tokenizer) {
            return switch (tokenizer.peek().orElseThrow(() -> new SyntaxError("Unexpected end of input"))) {
                case Token.Keyword k when k.type() == Token.KeywordType.VAR -> {
                    tokenizer.advance();
                    var type = new TypeParser.VarTypeParser().parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Invalid type"));
                    var name = new NameParser().parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Invalid identifer"));
                    terminateStatementParser.parse(tokenizer);
                    yield Optional.of(new LocalVar(name, type));
                }
                default -> Optional.empty();
            };
        }

    }

    record LocalVar(String name, Type.VarType type) {
    }

}
