package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

class SubroutinesDecsParser {

    public SubroutineDecs parse(IteratingTokenizer tokenizer) {
        var subroutineParser = new SubroutineParser();
        Map<String, Subroutine> subroutines = new HashMap<>();
        while (subroutineParser.parse(tokenizer).orElse(null) instanceof SubroutineDec s) {
            subroutines.put(s.name(), s.subroutine());
        }
        return new SubroutineDecs(subroutines);
    }

    static class SubroutineParser {

        public Optional<SubroutineDec> parse(IteratingTokenizer tokenizer) {
            var subroutineKindToken = tokenizer.peek().orElseThrow(() -> new SyntaxError("Unexpected end of input"));
            return switch (subroutineKindToken) {
                case Token.Keyword k when k.type() == Token.KeywordType.FUNCTION -> {
                    tokenizer.advance();
                    var type = new TypeParser.ReturnTypeParser().parse(tokenizer);
                    var name = new NameParser().parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Identifier expected"));
                    if (!(tokenizer.advance() instanceof Token.Symbol startParam
                            && startParam.type() == Token.SymbolType.OPEN_PAREN)) {
                        throw new SyntaxError("Expected start of parameter list");
                    }
                    if (!(tokenizer.advance() instanceof Token.Symbol endParam
                            && endParam.type() == Token.SymbolType.CLOSE_PAREN)) {
                        throw new SyntaxError("Expected end of parameter list");
                    }
                    if (!(tokenizer.advance() instanceof Token.Symbol startRoutine
                            && startRoutine.type() == Token.SymbolType.OPEN_BRACE)) {
                        throw new SyntaxError("Expected start of subroutine body");
                    }
                    if (!(tokenizer.advance() instanceof Token.Keyword returnKeyWord
                            && returnKeyWord.type() == Token.KeywordType.RETURN)) {
                        throw new SyntaxError("Expected return");
                    }
                    if (!(tokenizer.advance() instanceof Token.Symbol semicolon
                            && semicolon.type() == Token.SymbolType.SEMICOLON)) {
                        throw new SyntaxError("Missing ;");
                    }
                    if (!(tokenizer.advance() instanceof Token.Symbol endRoutine
                            && endRoutine.type() == Token.SymbolType.CLOSE_BRACE)) {
                        throw new SyntaxError("Expected end of subroutine body");
                    }
                    yield Optional.of(new SubroutineDec(name, new Function(type, Map.of(), List.of())));
                }
                default -> Optional.empty();
            };
        }

    }

    record SubroutineDec(String name, Subroutine subroutine) {
    }

    record SubroutineDecs(Map<String, Subroutine> subroutines) {
    }

    sealed interface Subroutine {

    }

    record Function(Type type, Map<String, Type> locals, List<Statement> statements) implements Subroutine {
    }

    static class StatementParser {

        public Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return Optional.empty();
        }

    }

    record Statement() {
    }

}
