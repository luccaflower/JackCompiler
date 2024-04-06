package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

class ClassParser {

    Optional<JackClass> parse(IteratingTokenizer tokenizer) {
        var next = tokenizer.advance();
        var className = switch (next) {
            case Token.Identifier i -> i.name();
            default -> throw new SyntaxError("Expected identifer, got %s".formatted(next));
        };
        var beginClass = tokenizer.advance();
        switch (beginClass) {
            case Token.Symbol s when s.type() == Token.SymbolType.OPEN_BRACE: break;
            default: throw new SyntaxError("Expected {, got %s".formatted(beginClass));
        }
        var classVarDecs = new ClassVarDecsParser().parse(tokenizer);
        var endClass = tokenizer.advance();
        switch (endClass) {
            case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_BRACE: break;
            default: throw new SyntaxError("Expected }, got %s".formatted(endClass));
        }
        return Optional.of(new JackClass(className, classVarDecs.statics(), classVarDecs.fields()));
    }

    enum ClassVarScope {

        STATIC, FIELD;

    }

    static class ClassVarDecsParser {

        public ClassVarDec parse(IteratingTokenizer tokenizer) {
            var statics = new HashMap<String, Type>();
            var fields = new HashMap<String, Type>();
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

    }

    static class FieldDecParser {

        Optional<FieldDec> parse(IteratingTokenizer tokenizer) {
            var newScopeToken = tokenizer.peek().orElseThrow(() -> new SyntaxError("unexpected end of file"));
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
            var typeToken = tokenizer.advance();
            var type = switch (typeToken) {
                case Token.Keyword k -> Type.PrimitiveType.from(k);
                case Token.Identifier i -> new Type.ClassType(i.name());
                default -> throw new SyntaxError("Unexpected type: %s".formatted(typeToken));
            };
            Set<String> names = new HashSet<>();
            loop: while (tokenizer.advance() instanceof Token.Identifier i) {
                names.add(i.name());
                switch (tokenizer.advance()) {
                    case Token.Symbol s when s.type() == Token.SymbolType.COMMA:
                        continue;
                    case Token.Symbol s when s.type() == Token.SymbolType.SEMICOLON:
                        break loop;
                    default:
                        throw new SyntaxError("Unexpected token");
                }
            }
            return Optional.of(new FieldDec(scope, type, names));
        }

    }

    record FieldDec(ClassVarScope scope, Type type, Set<String> names) {

    }

    record ClassVarDec(Map<String, Type> statics, Map<String, Type> fields) {
    }

}
