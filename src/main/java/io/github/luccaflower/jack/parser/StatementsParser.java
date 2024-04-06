package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class StatementsParser {

    private static final TerminateStatementParser terminateStatementParser = new TerminateStatementParser();

    public List<Statement> parse(IteratingTokenizer tokenizer) {
        var statements = new ArrayList<Statement>();
        var statementParser = new StatementParser();
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
            return new ReturnParser().parse(tokenizer);
        }

    }

    static class ReturnParser {

        public Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return switch (tokenizer.peek().orElse(null)) {
                case Token.Keyword k when k.type() == Token.KeywordType.RETURN -> {
                    tokenizer.advance();
                    var returnStatement = new ReturnStatement(new ExpressionParser().parse(tokenizer));
                    terminateStatementParser.parse(tokenizer);
                    yield Optional.of(returnStatement);
                }
                default -> Optional.empty();
            };
        }

    }

    sealed interface Statement {

    }

    record ReturnStatement(Optional<ExpressionParser.Expression> returnValue) implements Statement {
        public ReturnStatement() {
            this(Optional.empty());
        }
    }

}
