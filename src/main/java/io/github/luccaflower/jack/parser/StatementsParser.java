package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class StatementsParser {

    private static final TerminateStatementParser terminateStatementParser = new TerminateStatementParser();

    private static final NameParser nameParser = new NameParser();

    private static final ExpressionParser expressionParser = new ExpressionParser();

    private final StatementParser statementParser = new StatementParser();

    public List<Statement> parse(IteratingTokenizer tokenizer) {
        var statements = new ArrayList<Statement>();
        while (statementParser.parse(tokenizer).orElse(null) instanceof Statement s) {
            statements.add(s);
        }
        if (statements.isEmpty() || !(statements.getLast() instanceof ReturnStatement)) {
            throw new SyntaxError("Missing return");
        }
        return statements;
    }

    static class StatementParser {

        public Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return new ReturnParser().parse(tokenizer).or(() -> new LetStatementParser().parse(tokenizer));
        }

    }

    static class LetStatementParser {

        Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.LET -> {
                    tokenizer.advance();
                    var name = nameParser.parse(tokenizer).orElseThrow(() -> new SyntaxError("Identifier expected"));
                    switch (tokenizer.advance()) {
                        case Token.Symbol s when s.type() == Token.SymbolType.EQUALS:
                            break;
                        default:
                            throw new SyntaxError("'=' expected after identifier");
                    }
                    var value = expressionParser.parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Expression expected"));
                    terminateStatementParser.parse(tokenizer);
                    yield Optional.of(new LetStatement(name, value));
                }
                default -> Optional.empty();
            };
        }

    }

    static class ReturnParser {

        public Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.RETURN -> {
                    tokenizer.advance();
                    var returnStatement = new ReturnStatement(expressionParser.parse(tokenizer));
                    terminateStatementParser.parse(tokenizer);
                    yield Optional.of(returnStatement);
                }
                default -> Optional.empty();
            };
        }

    }

    sealed interface Statement {

    }

    record LetStatement(String name, ExpressionParser.Expression value) implements Statement {
    }

    record ReturnStatement(Optional<ExpressionParser.Expression> returnValue) implements Statement {
    }

}
