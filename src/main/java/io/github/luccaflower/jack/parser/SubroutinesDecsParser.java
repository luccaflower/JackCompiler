package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

public class SubroutinesDecsParser {

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
            Class<? extends Subroutine> subroutineKind;
            switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.FUNCTION: {
                    subroutineKind = Subroutine.JackFunction.class;
                    break;
                }
                case Token.Keyword k when k.type() == Token.KeywordType.METHOD:
                    subroutineKind = Subroutine.JackMethod.class;
                    break;
                case Token.Keyword k when k.type() == Token.KeywordType.CONSTRUCTOR:
                    subroutineKind = Subroutine.JackConstructor.class;
                    break;
                default:
                    return Optional.empty();
            }
            tokenizer.advance();
            var type = returnTypeParser.parse(tokenizer)
                .orElseThrow(() -> new SyntaxError("Function must have a return type"));
            var name = nameParser.parse(tokenizer).orElseThrow(() -> new SyntaxError("Identifier expected"));
            var arguments = parameterListParser.parse(tokenizer);
            startBlockParser.parse(tokenizer);
            var locals = localVarDecsParser.parse(tokenizer);
            var statements = statementsParser.parse(tokenizer);
            endBlockParser.parse(tokenizer);
            return Optional.of(new SubroutineDec(name,
                    builder(subroutineKind).type(type)
                        .name(name)
                        .arguments(arguments)
                        .locals(locals)
                        .statements(statements)
                        .build()));
        }

    }

    record SubroutineDec(String name, Subroutine subroutine) {
    }

    record SubroutineDecs(Map<String, Subroutine> subroutines) {
    }

    public static <T extends Subroutine> Builder<T> builder(Class<T> kind) {
        return new Builder<>(kind);
    }

    static class Builder<T extends Subroutine> {

        private final Class<T> kind;

        private String name = "";

        private Type.ReturnType type = new Type.VoidType();

        private Map<String, Type.VarType> arguments = new HashMap<>();

        private Map<String, Type.VarType> locals = new HashMap<>();

        private List<Statement> statements = new ArrayList<>();

        private static final Map<String, Factory> factories = Map.of(Subroutine.JackFunction.class.getSimpleName(),
                b -> new Subroutine.JackFunction(b.name, b.type, b.arguments, b.locals, b.statements),
                Subroutine.JackMethod.class.getSimpleName(),
                b -> new Subroutine.JackMethod(b.name, b.type, b.arguments, b.locals, b.statements),
                Subroutine.JackConstructor.class.getSimpleName(),
                b -> new Subroutine.JackConstructor(b.name, b.type, b.arguments, b.locals, b.statements));

        @FunctionalInterface
        private interface Factory {

            Subroutine apply(Builder<?> builder);

        }

        private Builder(Class<T> kind) {
            this.kind = kind;
        }

        public Builder<T> type(Type.ReturnType type) {
            this.type = type;
            return this;
        }

        public Builder<T> arguments(Map<String, Type.VarType> arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder<T> locals(Map<String, Type.VarType> locals) {
            this.locals = locals;
            return this;
        }

        public Builder<T> statements(List<Statement> statements) {
            this.statements = statements;
            return this;
        }

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Subroutine build() {
            return factories.get(kind.getSimpleName()).apply(this);
        }

    }

}
