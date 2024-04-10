package io.github.luccaflower.jack.codewriter;

import io.github.luccaflower.jack.parser.*;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.stream.Collectors;

public class ClassWriter {

    private final JackClass jackClass;

    public ClassWriter(JackClass jackClass) {
        this.jackClass = jackClass;
    }

    public String write() {
        return jackClass.subroutines()
                .values()
                .stream()
                .map(SubroutineWriter::new)
                .map(SubroutineWriter::write)
                .collect(Collectors.joining("\n"));
    }

    class SubroutineWriter {

        private final Subroutine subroutine;

        private final SymbolTable symbols = new SymbolTable();

        public SubroutineWriter(Subroutine subroutine) {
            this.subroutine = subroutine;
        }

        public String write() {
            var functionDec = "function %s.%s %d".formatted(jackClass.name(), subroutine.name(), subroutine.locals().size());
            var statements = subroutine.statements().stream()
                    .map(StatementWriter::new)
                    .map(StatementWriter::write)
                    .collect(Collectors.joining("\n"));
            return String.join("\n", functionDec, statements);
        }

    }

    class StatementWriter {

        private final Statement statement;
        public StatementWriter(Statement statement) {
            this.statement = statement;
        }

        public String write() {
            return switch (statement) {
                case Term.LocalDoStatement s -> {
                    var push = s.arguments().stream()
                            .map(ExpressionWriter::new)
                            .map(ExpressionWriter::write)
                            .collect(Collectors.joining("\n"));
                    var call = "call %s %d".formatted(s.subroutineName(), s.arguments().size());
                    yield String.join("\n", push, call);
                }
                case Term.ObjectDoStatement s ->  {
                    var push = s.arguments().stream().map(ExpressionWriter::new)
                            .map(ExpressionWriter::write)
                            .collect(Collectors.joining("\n"));
                    var call = "call %s.%s %d".formatted(s.target(), s.subroutineName(), s.arguments().size());
                    yield String.join("\n", push, call);
                }
                case Statement.ReturnStatement r -> {
                    var returnVal = r.returnValue().map(ExpressionWriter::new)
                            .map(ExpressionWriter::write)
                            .orElse("");
                    yield String.join("\n", returnVal, "return");
                }
                default -> "";
            };
        }

    }

    class ExpressionWriter {

        private final Expression expression;

        public ExpressionWriter(Expression expression) {
            this.expression = expression;
        }

        public String write() {
            var firstTerm = new TermWriter(expression.term()).write();
            var continuation = expression.continuation()
                .map(e -> String.join("\n", new ExpressionWriter(e.term()).write(), new OperatorWriter(e.op()).write()))
                .orElse("");
            return String.join("\n", firstTerm, continuation);
        }

    }

    class TermWriter {

        private final Term term;

        public TermWriter(Term term) {
            this.term = term;
        }

        public String write() {
            return switch (term) {
                case Term.Constant(Token.IntegerLiteral(int i)) -> "push constant " + i;
                case Term.Constant(Token.StringLiteral(String s)) ->
                    s.chars().mapToObj(c -> "call String.appendChar 1").collect(Collectors.joining("\n"));
                case Term.ParenthesisExpression(Expression e) -> new ExpressionWriter(e).write();
                default -> "";
            };
        }

    }

    class OperatorWriter {

        private final Expression.Operator operator;

        public OperatorWriter(Expression.Operator operator) {
            this.operator = operator;
        }

        public String write() {
            return switch (operator) {
                case PLUS -> "add";
                case MINUS -> "sub";
                case TIMES -> "call Math.multiply 2";
                case DIVIDED_BY -> "call Math.divide 2";
                case BITWISE_AND -> "and";
                case BITWISE_OR -> "or";
                case LESS_THAN -> "lt";
                case GREATER_THAN -> "gt";
                case EQUALS -> "eq";
            };
        }

    }

}
