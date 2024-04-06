package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

class SubroutinesDecsParser {

    private static final TypeParser.ReturnTypeParser returnTypeParser = new TypeParser.ReturnTypeParser();

    private static final StartBlockParser startBlockParser = new StartBlockParser();

    private static final NameParser nameParser = new NameParser();

    private static final ParameterListParser parameterListParser = new ParameterListParser();

    private static final LocalVarDecsParser localVarDecsParser = new LocalVarDecsParser();

    private static final StatementsParser statementsParser = new StatementsParser();

    private static final EndBlockParser endBlockParser = new EndBlockParser();

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
            var subroutineKindToken = tokenizer.peek();
            return switch (subroutineKindToken) {
                case Token.Keyword k when k.type() == Token.KeywordType.FUNCTION -> {
                    tokenizer.advance();
                    var type = returnTypeParser.parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Function must have a return type"));
                    var name = nameParser.parse(tokenizer).orElseThrow(() -> new SyntaxError("Identifier expected"));
                    var arguments = parameterListParser.parse(tokenizer);
                    startBlockParser.parse(tokenizer);
                    var locals = localVarDecsParser.parse(tokenizer);
                    var statements = statementsParser.parse(tokenizer);
                    endBlockParser.parse(tokenizer);
                    yield Optional.of(new SubroutineDec(name,
                            Function.builder(type).arguments(arguments).locals(locals).statements(statements).build()));
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

        List<StatementsParser.Statement> statements();

    }

    record Function(Type.ReturnType type, Map<String, Type.VarType> arguments, Map<String, Type.VarType> locals,
            List<StatementsParser.Statement> statements) implements Subroutine {

        public static Builder builder(Type.ReturnType type) {
            return new Builder(type);
        }
        static class Builder {

            private final Type.ReturnType type;

            private Map<String, Type.VarType> arguments = new HashMap<>();

            private Map<String, Type.VarType> locals = new HashMap<>();

            private List<StatementsParser.Statement> statements = new ArrayList<>();

            private Builder(Type.ReturnType type) {
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

            public Builder statements(List<StatementsParser.Statement> statements) {
                this.statements = statements;
                return this;
            }

            public Function build() {
                return new Function(type, arguments, locals, statements);
            }

        }
    }

}
