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

    private static final IndexParser indexParser = new IndexParser();

    private static final StartBlockParser startBlockParser = new StartBlockParser();

    private static final EndBlockParser endBlockParser = new EndBlockParser();

    private static final ConditionAndBlockParser conditionAndBlockParser = new ConditionAndBlockParser();

    private static final StatementParser statementParser = new StatementParser();

    public List<Statement> parse(IteratingTokenizer tokenizer) {
        var statements = new ArrayList<Statement>();
        while (statementParser.parse(tokenizer).orElse(null) instanceof Statement s) {
            statements.add(s);
        }
        return statements;
    }

    static class StatementParser {

        public Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return new ReturnParser().parse(tokenizer)
                .or(() -> new LetStatementParser().parse(tokenizer))
                .or(() -> new IfStatementParser().parse(tokenizer))
                .or(() -> new WhileStatementParser().parse(tokenizer));
        }

    }

    static class WhileStatementParser {

        Optional<Statement> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.WHILE:
                    break;
                default:
                    return Optional.empty();
            }
            tokenizer.advance();
            var conditionAndBlock = conditionAndBlockParser.parse(tokenizer);
            return Optional.of(new WhileStatement(conditionAndBlock.condition(), conditionAndBlock.statements()));

        }

    }

    static class IfStatementParser {

        Optional<Statement> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.IF:
                    break;
                default:
                    return Optional.empty();
            }
            tokenizer.advance();
            var conditionAndBlock = conditionAndBlockParser.parse(tokenizer);
            var elseBlock = new ElseBlockParser().parse(tokenizer);
            return Optional
                .of(new IfStatement(conditionAndBlock.condition(), conditionAndBlock.statements(), elseBlock));
        }

    }

    static class ConditionAndBlockParser {

        ConditionanAndBlock parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.advance()) {
                case Token.Symbol s when s.type() == Token.SymbolType.OPEN_PAREN:
                    break;
                default:
                    throw new SyntaxError("Unexpected token");
            }
            var condition = expressionParser.parse(tokenizer).orElseThrow(() -> new SyntaxError("Missing condition"));
            switch (tokenizer.advance()) {
                case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_PAREN:
                    break;
                default:
                    throw new SyntaxError("Expected end of condition");
            }
            startBlockParser.parse(tokenizer);
            var statements = new StatementsParser().parse(tokenizer);
            endBlockParser.parse(tokenizer);
            return new ConditionanAndBlock(condition, statements);
        }

    }

    record ConditionanAndBlock(ExpressionParser.Expression condition, List<Statement> statements) {
    }

    static class ElseBlockParser {

        Optional<ElseBlock> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.ELSE:
                    break;
                default:
                    return Optional.empty();
            }
            tokenizer.advance();
            startBlockParser.parse(tokenizer);
            var statements = new StatementsParser().parse(tokenizer);
            endBlockParser.parse(tokenizer);
            return Optional.of(new ElseBlock(statements));
        }

    }

    static class LetStatementParser {

        Optional<Statement> parse(IteratingTokenizer tokenizer) {
            return switch (tokenizer.peek()) {
                case Token.Keyword k when k.type() == Token.KeywordType.LET -> {
                    tokenizer.advance();
                    var name = nameParser.parse(tokenizer).orElseThrow(() -> new SyntaxError("Identifier expected"));
                    var index = indexParser.parse(tokenizer);
                    switch (tokenizer.advance()) {
                        case Token.Symbol s when s.type() == Token.SymbolType.EQUALS:
                            break;
                        default:
                            throw new SyntaxError("'=' expected after identifier");
                    }
                    var value = expressionParser.parse(tokenizer)
                        .orElseThrow(() -> new SyntaxError("Expression expected"));
                    terminateStatementParser.parse(tokenizer);
                    yield Optional.of(new LetStatement(name, index, value));
                }
                default -> Optional.empty();
            };
        }

    }

    static class IndexParser {

        public Optional<ExpressionParser.Expression> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Symbol s when s.type() == Token.SymbolType.OPEN_SQUARE:
                    break;
                default:
                    return Optional.empty();
            }
            tokenizer.advance();
            var index = expressionParser.parse(tokenizer);
            switch (tokenizer.advance()) {
                case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_SQUARE:
                    break;
                default:
                    throw new SyntaxError("Unexpected token");
            }
            return index;
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

    record WhileStatement(ExpressionParser.Expression condition, List<Statement> statements) implements Statement {
    }

    record IfStatement(ExpressionParser.Expression condition, List<Statement> statements,
            Optional<ElseBlock> elseBlock) implements Statement {
    }

    record ElseBlock(List<Statement> statements) {
    }

    record LetStatement(String name, Optional<ExpressionParser.Expression> index,
            ExpressionParser.Expression value) implements Statement {
    }

    record ReturnStatement(Optional<ExpressionParser.Expression> returnValue) implements Statement {
    }

}
