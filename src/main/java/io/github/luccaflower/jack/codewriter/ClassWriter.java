package io.github.luccaflower.jack.codewriter;

import io.github.luccaflower.jack.parser.*;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.luccaflower.jack.codewriter.SymbolTable.Scope.*;

public class ClassWriter {

    private final JackClass jackClass;

    private final SymbolTable statics;

    private final SymbolTable fields;

    private int whileCounter = 0;

    private int ifCounter = 0;

    public ClassWriter(JackClass jackClass) {
        this.jackClass = jackClass;
        statics = from(jackClass.statics(), STATIC);
        fields = from(jackClass.fields(), FIELD);
    }

    private SymbolTable from(Map<String, Type.VarType> identifiers, SymbolTable.Scope scope) {
        return SymbolTable.create(getList(identifiers), scope);
    }

    private static List<SymbolTable.Identifier> getList(Map<String, Type.VarType> identifiers) {
        return identifiers.entrySet()
            .stream()
            .map(e -> new SymbolTable.Identifier(e.getKey(), e.getValue().name()))
            .toList();
    }

    public String write() {
        return jackClass.subroutines().values().stream().map(s -> switch (s) {
            case Subroutine.JackFunction f ->
                new SubroutineWriter(f, SymbolTable.create(List.of(), FIELD), from(f.arguments(), ARGUMENT));
            case Subroutine.JackConstructor c -> new SubroutineWriter(c, fields, from(c.arguments(), ARGUMENT));
            case Subroutine.JackMethod m -> {
                var argsNames = new ArrayList<>(getList(m.arguments()));
                argsNames.addFirst(new SymbolTable.Identifier("this", jackClass.name()));
                yield new SubroutineWriter(m, fields, SymbolTable.create(argsNames, ARGUMENT));
            }
        }).map(SubroutineWriter::write).collect(Collectors.joining("\n"));
    }

    class SubroutineWriter {

        private final Subroutine subroutine;

        private final SymbolTable locals;

        private final SymbolTable argNames;

        private SymbolTable fields;

        public SubroutineWriter(Subroutine subroutine, SymbolTable fields, SymbolTable argNames) {
            this.subroutine = subroutine;
            var localNames = new ArrayList<>(getList(subroutine.locals()));
            this.argNames = argNames;
            locals = SymbolTable.create(localNames, LOCAL);
            this.fields = fields;
        }

        public String write() {
            var functionDec = "function %s.%s %d".formatted(jackClass.name(), subroutine.name(),
                    subroutine.locals().size());
            var header = switch (subroutine) {
                case Subroutine.JackConstructor c -> """
                        push constant %d
                        call Memory.alloc 1
                        pop pointer 0""".formatted(jackClass.fields().size());
                case Subroutine.JackFunction f -> {
                    fields = SymbolTable.create(List.of(), FIELD);
                    yield "";
                }
                case Subroutine.JackMethod m -> """
                        push argument 0
                        pop pointer 0""";
            };
            var statements = subroutine.statements()
                .stream()
                .map(s -> new StatementWriter(s, locals, argNames, fields))
                .map(StatementWriter::write)
                .collect(Collectors.joining("\n"));
            return String.join("\n", functionDec, header, statements);
        }

    }

    class StatementWriter {

        private final Statement statement;

        private final SymbolTable locals;

        private final SymbolTable arguments;

        private final SymbolTable fields;

        public StatementWriter(Statement statement, SymbolTable locals, SymbolTable arguments, SymbolTable fields) {
            this.statement = statement;
            this.locals = locals;
            this.arguments = arguments;
            this.fields = fields;
        }

        public String write() {
            return switch (statement) {
                case Term.LocalSubroutineCall s -> {
                    var subroutine = jackClass.subroutines().get(s.subroutineName());
                    var pushThis = switch (subroutine) {
                        case Subroutine.JackMethod m -> "push pointer 0";
                        default -> "";
                    };
                    var pushArgs = s.arguments()
                        .stream()
                        .map(a -> new ExpressionWriter(locals, arguments).write(a))
                        .collect(Collectors.joining("\n"));
                    var argCount = switch (subroutine) {
                        case Subroutine.JackMethod m -> m.arguments().size() + 1;
                        default -> s.arguments().size();
                    };
                    var call = "call %s.%s %d".formatted(ClassWriter.this.jackClass.name(), s.subroutineName(),
                            argCount);
                    var popReturnToTemp = "pop temp 0";
                    yield String.join("\n", pushThis, pushArgs, call, popReturnToTemp);
                }
                case Term.ObjectSubroutineCall s -> {
                    var push = s.arguments()
                        .stream()
                        .map(a -> new ExpressionWriter(locals, arguments).write(a))
                        .collect(Collectors.joining("\n"));
                    Optional<SymbolTable.Symbol> symbol = arguments.resolve(s.target())
                        .or(() -> locals.resolve(s.target()))
                        .or(() -> fields.resolve(s.target()))
                        .or(() -> statics.resolve(s.target()));
                    var target = symbol.map(SymbolTable.Symbol::type).orElse(s.target());
                    var pushObject = symbol.map(sy -> {
                        var scope = switch (sy.scope()) {
                            case ARGUMENT -> "argument";
                            case LOCAL -> "local";
                            case STATIC -> "static";
                            case FIELD -> "this";
                            case SUBROUTINE -> throw new RuntimeException("what the fuck");
                        };
                        return "push %s %d".formatted(scope, sy.index());
                    }).orElse("");
                    var argCount = symbol.map(ignored -> s.arguments().size() + 1)
                            .orElse(s.arguments().size());
                    var call = "call %s.%s %d".formatted(target, s.subroutineName(), argCount);
                    var popReturn = "pop temp 0";
                    yield String.join("\n", pushObject, push, call, popReturn);
                }
                case Statement.NonIndexedLetStatement(String name, Expression value) -> {
                    var symbol = arguments.resolve(name)
                        .or(() -> locals.resolve(name))
                        .or(() -> fields.resolve(name))
                        .or(() -> statics.resolve(name))
                        .orElseThrow(() -> new SyntaxError("Unknown identifier '%s'".formatted(name)));
                    var pushValue = new ExpressionWriter(locals, arguments).write(value);
                    var popToSymbol = switch (symbol.scope()) {
                        case ARGUMENT -> "pop argument " + symbol.index();
                        case LOCAL -> "pop local " + symbol.index();
                        case FIELD -> "pop this " + symbol.index();
                        case STATIC -> "pop static %s".formatted(jackClass.name()) + symbol.index();
                        case SUBROUTINE -> throw new SyntaxError("Can't assign value to subroutine");
                    };
                    yield String.join("\n", pushValue, popToSymbol);
                }
                case Statement.IndexedLetStatement(String name, Expression index, Expression value) -> {
                    var symbol = arguments.resolve(name)
                            .or(() -> locals.resolve(name))
                            .or(() -> fields.resolve(name))
                            .or(() -> statics.resolve(name))
                            .orElseThrow(() -> new SyntaxError("Unknown identifier '%s'".formatted(name)));
                    var scope = switch (symbol.scope()) {
                        case ARGUMENT -> "argument";
                        case LOCAL -> "local";
                        case FIELD -> "this";
                        case STATIC -> "static";
                        case SUBROUTINE -> throw new SyntaxError("Invalid assignment to subroutine '%s'".formatted(name));
                    };
                    var pushArr = """
                            push %s %d
                            %s
                            add""".formatted(scope, symbol.index(), new ExpressionWriter(locals, arguments).write(index));
                    var pushValue = new ExpressionWriter(locals, arguments).write(value);
                    var popIntoArray = """
                            pop temp 0
                            pop pointer 1
                            push temp 0
                            pop that 0
                            """;
                    yield String.join("\n", pushArr, pushValue, popIntoArray);
                }
                case Statement.ReturnStatement r -> {
                    var returnVal = r.returnValue()
                        .map(v -> new ExpressionWriter(locals, arguments).write(v))
                        .orElse("push constant 0");
                    yield String.join("\n", returnVal, "return");
                }
                case Statement.WhileStatement(Expression condition, List<Statement> statements) -> {
                    var label = "while.%d".formatted(whileCounter++);
                    var startLabel = "label %s.start".formatted(label);
                    var value = new ExpressionWriter(locals, arguments).write(condition);
                    var shouldContinue = """
                            if-goto %s.block
                            goto %s.end""".formatted(label, label);
                    var blockLabel = "label %s.block".formatted(label);
                    var statementInstructions = statements.stream()
                        .map(s -> new StatementWriter(s, locals, arguments, fields).write())
                        .collect(Collectors.joining("\n"));
                    var gotoStart = "goto %s.start".formatted(label);
                    var endLabel = "label %s.end".formatted(label);
                    yield String.join("\n", startLabel, value, shouldContinue, blockLabel, statementInstructions,
                            gotoStart, endLabel);
                }
                case Statement.IfStatement ifStatement -> {
                    var ifCounter = ClassWriter.this.ifCounter++;
                    var condition = new ExpressionWriter(locals, arguments).write(ifStatement.condition());
                    var evaluate = """
                            if-goto if-true.%s
                            goto if-not.%s""".formatted(ifCounter, ifCounter);
                    var ifTrueLabel = "label if-true.%s".formatted(ifCounter);
                    var ifTrueStatements = ifStatement.statements()
                        .stream()
                        .map(s -> new StatementWriter(s, locals, arguments, fields).write())
                        .collect(Collectors.joining("\n"));
                    var gotoEnd = "goto if-end.%s".formatted(ifCounter);
                    var elseLabel = "label if-not.%s".formatted(ifCounter);
                    var elseBlock = ifStatement.elseBlock()
                        .map(b -> b.statements()
                            .stream()
                            .map(s -> new StatementWriter(s, locals, arguments, fields).write())
                            .collect(Collectors.joining("\n")))
                        .orElse("");
                    var endLabel = "label if-end.%s".formatted(ifCounter);

                    yield String.join("\n", condition, evaluate, ifTrueLabel, ifTrueStatements, gotoEnd, elseLabel,
                            elseBlock, endLabel);
                }
                default -> throw new RuntimeException("Not implemented " + statement.getClass().getSimpleName());
            };
        }

    }

    class ExpressionWriter {

        private final SymbolTable locals;

        private final SymbolTable arguments;

        ExpressionWriter(SymbolTable locals, SymbolTable arguments) {
            this.locals = locals;
            this.arguments = arguments;
        }

        public String write(Expression expression) {
            var firstTerm = new TermWriter(expression.term(), locals, arguments).write();
            var continuation = expression.continuation()
                .map(e -> String.join("\n", new ExpressionWriter(locals, arguments).write(e.term()),
                        new OperatorWriter(e.op()).write()))
                .orElse("");
            return String.join("\n", firstTerm, continuation);
        }

    }

    class TermWriter {

        private final Term term;

        private final SymbolTable locals;

        private final SymbolTable arguments;

        public TermWriter(Term term, SymbolTable locals, SymbolTable arguments) {
            this.term = term;
            this.locals = locals;
            this.arguments = arguments;
        }

        public String write() {
            return switch (term) {
                case Term.Constant(Token.IntegerLiteral(int i)) -> "push constant " + i;
                case Term.Constant(Token.StringLiteral(String s)) -> {
                    var createString = """
                    push constant %d
                    call String.new 1""".formatted(s.length());
                    var appendChars = s.chars().mapToObj("push constant %s\ncall String.appendChar 2"::formatted).collect(Collectors.joining("\n"));
                    yield String.join("\n", createString, appendChars);
                }
                case Term.KeywordLiteral(Token.KeywordType k) -> switch (k) {
                    case TRUE -> """
                            push constant 0
                            not""";
                    case FALSE, NULL -> "push constant 0";
                    case THIS -> "push pointer 0";
                    default -> throw new RuntimeException("Not implemented " + k.keyword());
                };
                case Term.NonIndexedVarName(String name) -> {
                    var symbol = arguments.resolve(name)
                        .or(() -> locals.resolve(name))
                        .or(() -> fields.resolve(name))
                        .or(() -> statics.resolve(name))
                        .orElseThrow(() -> new SyntaxError("Unknown identifier '%s'".formatted(name)));
                    yield switch (symbol.scope()) {
                        case ARGUMENT -> "push argument " + symbol.index();
                        case LOCAL -> "push local " + symbol.index();
                        case FIELD -> "push this " + symbol.index();
                        case STATIC -> "push static %s." + symbol.index();
                        case SUBROUTINE -> throw new SyntaxError("Unexpected subroutine name");
                    };
                }
                case Term.IndexedVarname(String name, Expression index) -> {
                    var symbol = arguments.resolve(name)
                            .or(() -> locals.resolve(name))
                            .or(() -> fields.resolve(name))
                            .or(() -> statics.resolve(name))
                            .orElseThrow(() -> new SyntaxError("Unknown identifier '%s'".formatted(name)));
                    var scope = switch (symbol.scope()) {
                        case LOCAL -> "local";
                        case FIELD -> "this";
                        case STATIC -> "static";
                        case ARGUMENT -> "argument";
                        case SUBROUTINE -> throw new SyntaxError("Unexpected subroutine name");
                    };
                    var pushArray = "push %s %d".formatted(scope, symbol.index());
                    var pushIndex = new ExpressionWriter(locals, arguments).write(index);
                    var pushValueAtIndex = """
                            add
                            pop pointer 1
                            push that 0
                            """;

                    yield String.join("\n", pushArray, pushIndex, pushValueAtIndex);
                }
                case Term.ParenthesisExpression(Expression e) -> new ExpressionWriter(locals, arguments).write(e);
                case Term.UnaryOpTerm(Term.UnaryOp op, Term t) -> {
                    var pushTerm = new TermWriter(t, locals, arguments).write();
                    var doOp = op.instruction();
                    yield String.join("\n", pushTerm, doOp);
                }
                case Term.LocalSubroutineCall call -> {
                    var pushArguments = call.arguments()
                        .stream()
                        .map(a -> new ExpressionWriter(locals, arguments).write(a))
                        .collect(Collectors.joining("\n"));
                    var doCall = "call %s.%s %d".formatted(ClassWriter.this.jackClass.name(), call.subroutineName(),
                            call.arguments().size());
                    yield String.join("\n", pushArguments, doCall);
                }
                case Term.ObjectSubroutineCall call -> {
                    Optional<SymbolTable.Symbol> symbol = arguments.resolve(call.target())
                        .or(() -> locals.resolve(call.target()))
                        .or(() -> fields.resolve(call.target()))
                        .or(() -> statics.resolve(call.target()));
                    var pushObject = symbol.map(s -> "push %s %d".formatted(s.scope().name(), s.index())).orElse("");
                    var pushArguments = call.arguments()
                        .stream()
                        .map(a -> new ExpressionWriter(locals, arguments).write(a))
                        .collect(Collectors.joining("\n"));
                    var target = symbol.map(SymbolTable.Symbol::type).orElse(call.target());

                    int argSize = call.arguments().size();
                    if (!pushObject.isBlank()) {
                        argSize++;
                    }
                    var doCall = "call %s.%s %d".formatted(target, call.subroutineName(), argSize);
                    yield String.join("\n", pushObject, pushArguments, doCall);
                }
                default -> throw new RuntimeException("Not implemented: " + term.getClass().getSimpleName());
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
