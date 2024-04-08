package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

class ClassVarDecsParser {

    public ClassVarDec parse(IteratingTokenizer tokenizer) {
        var statics = new HashMap<String, Type.VarType>();
        var fields = new HashMap<String, Type.VarType>();
        var fieldParser = new FieldDecParser();
        while (fieldParser.parse(tokenizer).orElse(null) instanceof FieldDec dec) {
            if (dec.scope() == ClassVarScope.STATIC) {
                dec.names().forEach(name -> statics.put(name, dec.type()));
            }
            if (dec.scope() == ClassVarScope.FIELD) {
                dec.names().forEach(name -> fields.put(name, dec.type()));
            }
        }
        return new ClassVarDec(statics, fields);
    }

    enum ClassVarScope {

        STATIC, FIELD;

    }

    static class FieldDecParser {

        Optional<FieldDec> parse(IteratingTokenizer tokenizer) {
            var newScopeToken = tokenizer.peek();
            ClassVarScope scope;
            switch (newScopeToken) {
                case Token.Keyword k when k.type() == Token.KeywordType.STATIC:
                    scope = ClassVarScope.STATIC;
                    break;
                case Token.Keyword k when k.type() == Token.KeywordType.FIELD:
                    scope = ClassVarScope.FIELD;
                    break;
                default:
                    return Optional.empty();
            }
            tokenizer.advance();
            var typeAndNames = new VarTypeAndNamesParser().parse(tokenizer);
            return Optional.of(new FieldDec(scope, typeAndNames.type(), typeAndNames.names()));
        }

    }

    record FieldDec(ClassVarScope scope, Type.VarType type, Set<String> names) {

    }

    record ClassVarDec(Map<String, Type.VarType> statics, Map<String, Type.VarType> fields) {
    }

}
