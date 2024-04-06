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
                    var type = new TypeParser.ReturnTypeParser().parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Function must have a return type"));
                    var name = new NameParser().parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Identifier expected"));
                    var arguments = new ParameterListParser().parse(tokenizer);
                    switch (tokenizer.advance()) {
                        case Token.Symbol s when s.type() == Token.SymbolType.OPEN_BRACE:
                            break;
                        default:
                            throw new SyntaxError("Expected {");
                    }
                    var locals = new LocalVarDecsParser().parse(tokenizer);
                    switch (tokenizer.advance()) {
                        case Token.Keyword returnKeyword when returnKeyword.type() == Token.KeywordType.RETURN:
                            break;
                        default:
                            throw new SyntaxError("Missing return");
                    }
                    switch (tokenizer.advance()) {
                        case Token.Symbol s when s.type() == Token.SymbolType.SEMICOLON:
                            break;
                        default:
                            throw new SyntaxError("Missing ;");
                    }
                    switch (tokenizer.advance()) {
                        case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_BRACE:
                            break;
                        default:
                            throw new SyntaxError("Missing }");
                    }
                    yield Optional.of(new SubroutineDec(name,
                            Function.builder(type).arguments(arguments).locals(locals).build()));
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

        Map<String, Type.VarType> arguments();

        Map<String, Type.VarType> locals();

    }

    record Function(Type type, Map<String, Type.VarType> arguments, Map<String, Type.VarType> locals,
            List<Statement> statements) implements Subroutine {

        public static Builder builder(Type type) {
            return new Builder(type);
        }
        static class Builder {

            private final Type type;

            private Map<String, Type.VarType> arguments = new HashMap<>();

            private Map<String, Type.VarType> locals = new HashMap<>();

            private List<Statement> statements = new ArrayList<>();

            private Builder(Type type) {
                this.type = type;
            }

            public Builder arguments(Map<String, Type.VarType> arguments) {
                this.arguments = arguments;
                return this;
            }

            public Builder locals(Map<String, Type.VarType> locals) {
                this.locals = locals;
                return this;
            }

            public Builder statements(List<Statement> statements) {
                this.statements = statements;
                return this;
            }

            public Function build() {
                return new Function(type, arguments, locals, statements);
            }

        }
    }

    static class StatementParser {

        public Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return Optional.empty();
        }

    }

    record Statement() {
    }

}
