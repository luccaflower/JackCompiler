package io.github.luccaflower.jack.codewriter;

import io.github.luccaflower.jack.parser.*;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.luccaflower.jack.codewriter.SymbolTable.Scope.LOCAL;

public class ClassWriter {

    private final JackClass jackClass;
    private final SymbolTable statics;
    private final SymbolTable fields;
    private final SymbolTable subroutines;
    private int whileCounter = 0;

    public ClassWriter(JackClass jackClass) {
        this.jackClass = jackClass;
        statics = new SymbolTable(jackClass.statics().keySet().stream().toList(), SymbolTable.Scope.STATIC);
        fields = new SymbolTable(jackClass.fields().keySet().stream().toList(), SymbolTable.Scope.FIELD);
        subroutines = new SymbolTable(jackClass.subroutines().keySet().stream().toList(), SymbolTable.Scope.SUBROUTINE);
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

        private final SymbolTable locals;

        public SubroutineWriter(Subroutine subroutine) {
            this.subroutine = subroutine;
            var localNames = new ArrayList<>(subroutine.locals().keySet().stream().toList());
            var argNames = subroutine.arguments().keySet().stream().toList();
            localNames.addAll(argNames);
            locals = new SymbolTable(localNames, LOCAL);
        }

        public String write() {
            var functionDec = "function %s.%s %d".formatted(jackClass.name(), subroutine.name(), subroutine.locals().size());
            var statements = subroutine.statements().stream()
                    .map(s -> new StatementWriter(s, locals))
                    .map(StatementWriter::write)
                    .collect(Collectors.joining("\n"));
            return String.join("\n", functionDec, statements);
        }

    }

    class StatementWriter {

        private final Statement statement;
        private final SymbolTable locals;

        public StatementWriter(Statement statement, SymbolTable locals) {
            this.statement = statement;
            this.locals = locals;
        }

        public String write() {
            return switch (statement) {
                case Term.LocalSubroutineCall s -> {
                    var push = s.arguments().stream()
                            .map(a -> new ExpressionWriter(locals).write(a))
                            .collect(Collectors.joining("\n"));
                    var call = "call %s %d".formatted(s.subroutineName(), s.arguments().size());
                    yield String.join("\n", push, call);
                }
                case Term.ObjectSubroutineCall s ->  {
                    var push = s.arguments().stream()
                            .map(a -> new ExpressionWriter(locals).write(a))
                            .collect(Collectors.joining("\n"));
                    var call = "call %s.%s %d".formatted(s.target(), s.subroutineName(), s.arguments().size());
                    yield String.join("\n", push, call);
                }
                case Statement.NonIndexedLetStatement(String name, Expression value) -> {
                    var symbol = locals.resolve(name)
                            .or(() -> fields.resolve(name))
                            .or(() -> statics.resolve(name))
                            .orElseThrow(() -> new SyntaxError("Unknown identifier '%s'".formatted(name)));
                    var pushValue = new ExpressionWriter(locals).write(value);
                    var popToSymbol = switch (symbol.scope()) {
                        case LOCAL -> "pop local " + symbol.index();
                        case FIELD -> "pop this " + symbol.index();
                        case STATIC -> "pop static %s".formatted(jackClass.name()) + symbol.index();
                        case SUBROUTINE -> throw new SyntaxError("Can't assign value to subroutine");
                    };
                    yield String.join("\n", pushValue, popToSymbol);
                }
                case Statement.ReturnStatement r -> {
                    var returnVal = r.returnValue()
                            .map(v -> new ExpressionWriter(locals).write(v))
                            .orElse("");
                    yield String.join("\n", returnVal, "return");
                }
                case Statement.WhileStatement(Expression condition, List<Statement> statements) -> {
                    var label = "while.%d".formatted(whileCounter++);
                    var startLabel = "label %s.start".formatted(label);
                    var value = new ExpressionWriter(locals).write(condition);
                    var shouldContinue = """
                            eq
                            not
                            if-goto while.%s.end""".formatted(label);
                    var statementInstructions = statements.stream()
                            .map(s -> new StatementWriter(s, locals).write())
                            .collect(Collectors.joining("\n"));
                    var gotoStart = "goto %s.start".formatted(label);
                    var endLabel = "label %s.end".formatted(label);
                    yield String.join("\n", startLabel, value, shouldContinue, statementInstructions, gotoStart, endLabel);
                }
                default -> "";
            };
        }

    }

    class ExpressionWriter {
        private final SymbolTable locals;

        ExpressionWriter(SymbolTable locals) {
            this.locals = locals;
        }

        public String write(Expression expression) {
            var firstTerm = new TermWriter(expression.term(), locals).write();
            var continuation = expression.continuation()
                .map(e -> String.join("\n", new ExpressionWriter(locals).write(e.term()), new OperatorWriter(e.op()).write()))
                .orElse("");
            return String.join("\n", firstTerm, continuation);
        }

    }

    class TermWriter {

        private final Term term;
        private final SymbolTable locals;

        public TermWriter(Term term, SymbolTable locals) {
            this.term = term;
            this.locals = locals;
        }

        public String write() {
            return switch (term) {
                case Term.Constant(Token.IntegerLiteral(int i)) -> "push constant " + i;
                case Term.Constant(Token.StringLiteral(String s)) ->
                    s.chars().mapToObj(c -> "call String.appendChar 1").collect(Collectors.joining("\n"));
                case Term.KeywordLiteral(Token.KeywordType k) -> switch (k) {
                    case TRUE -> "push constant -1";
                    case FALSE -> "push constant 0";
                    default -> "";
                };
                case Term.NonIndexedVarName(String name) -> {
                    var symbol = locals.resolve(name)
                            .or(() -> fields.resolve(name))
                            .or(() -> statics.resolve(name))
                            .orElseThrow(() -> new SyntaxError("Unknown identifier '%s'".formatted(name)));
                    yield switch (symbol.scope()) {
                        case LOCAL -> "push local " + symbol.index();
                        case FIELD -> "push this " + symbol.index();
                        case STATIC -> "push static %s." + symbol.index();
                        case SUBROUTINE -> throw new SyntaxError("Unexpected subroutine name");
                    };
                }
                case Term.ParenthesisExpression(Expression e) ->
                        new ExpressionWriter(locals).write(e);
                case Term.UnaryOpTerm(Term.UnaryOp op, Term t) -> {
                    var pushTerm = new TermWriter(t, locals).write();
                    var doOp = op.instruction();
                    yield String.join("\n", pushTerm, doOp);
                }
                case Term.LocalSubroutineCall call -> {
                    var pushArguments = call.arguments()
                            .stream().map(a -> new ExpressionWriter(locals).write(a))
                            .collect(Collectors.joining("\n"));
                    var doCall = "call %s %d".formatted(call.subroutineName(), call.arguments().size());
                    yield String.join("\n", pushArguments, doCall);
                }
                case Term.ObjectSubroutineCall call -> {
                    var pushArguments = call.arguments()
                            .stream().map(a -> new ExpressionWriter(locals).write(a))
                            .collect(Collectors.joining("\n"));
                    var doCall = "call %s.%s %d".formatted(call.target(), call.subroutineName(), call.arguments().size());
                    yield String.join("\n", pushArguments, doCall);
                }
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
