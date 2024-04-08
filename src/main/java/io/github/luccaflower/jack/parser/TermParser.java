package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.List;
import java.util.Optional;

import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.*;

class TermParser {

    private static final NameParser nameParser = new NameParser();
    private static final ExpressionParser.ExpressionListParser expressionListParser = new ExpressionParser.ExpressionListParser();

    public Optional<Term> parse(IteratingTokenizer tokens) {
        return new ConstantParser().parse(tokens)
                .or(() -> new VarNameParser().parse(tokens))
                .or(() -> new KeywordLiteralParser().parse(tokens))
                .or(() -> new UnaryOpTermParser().parse(tokens))
                .or(() -> new SubroutineCallParser().parse(tokens))
                .or(() -> new ParenExpressionParser().parse(tokens));
    }

    static class ConstantParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            switch (tokens.peek()) {
                case Token.IntegerLiteral ignored:
                    break;
                case Token.StringLiteral ignored:
                    break;
                default:
                    return Optional.empty();
            }
            return Optional.of(new Term.Constant(tokens.advance()));
        }

    }

    static class ParenExpressionParser {
        Optional<ParenthesisExpression> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Symbol s when s.type() == OPEN_PAREN: break;
                default: return Optional.empty();
            }
            tokenizer.advance();
            var expression = new ExpressionParser().parse(tokenizer).orElseThrow(() -> new SyntaxError("Expected expression inside parenthesis"));
            switch (tokenizer.advance()) {
                case Token.Symbol s when s.type() == CLOSE_PAREN: break;
                default: throw new SyntaxError("Expected ) after expression");
            }
            return Optional.of(new ParenthesisExpression(expression));
        }
    }

    static class VarNameParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            if (!(tokens.peek() instanceof Token.Identifier i)) {
                return Optional.empty();
            }
            tokens.advance();
            var index = new IndexParser().parse(tokens);
            return Optional.of(new Term.VarName(i.name(), index));
        }

    }

    static class KeywordLiteralParser {

        public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            if (!(tokens.peek() instanceof Token.Keyword k)) {
                return Optional.empty();
            }
            tokens.advance();
            return Optional.of(new Term.KeywordLiteral(k.type()));
        }

    }

    static class UnaryOpTermParser {

        public Optional<Term> parse(IteratingTokenizer tokens) {
            if (!tokens.hasMoreTokens()) {
                return Optional.empty();
            }
            switch (tokens.peek()) {
                case Token.Symbol s when s.type() == TILDE || s.type() == Token.SymbolType.MINUS:
                    break;
                default:
                    return Optional.empty();
            }
            var nextTerm = new TermParser().parse(tokens.lookAhead(1));
            if (nextTerm.isPresent()) {
                var op = tokens.advance();
                tokens.advance();
                return Optional.of(new Term.UnaryOpTerm(Term.UnaryOp.from(op), nextTerm.get()));
            }
            else {
                return Optional.empty();
            }
        }

    }

    static class SubroutineCallParser {

        Optional<Term.SubroutineCall> parse(IteratingTokenizer tokenizer) {
            switch (tokenizer.peek()) {
                case Token.Identifier i: {
                    break;
                }
                default:
                    return Optional.empty();
            }
            switch (tokenizer.lookAhead(1).peek()) {
                case Token.Symbol s when s.type() == OPEN_PAREN: {
                    //identifier assured at the start of the function
                    var subroutineName = nameParser.parse(tokenizer).get();
                    var expressions = expressionListParser.parse(tokenizer);
                    return Optional.of(new Term.SubroutineCall(Optional.empty(), subroutineName, expressions));
                }
                case Token.Symbol s when s.type() == DOT: {
                    var className = nameParser.parse(tokenizer).get();
                    tokenizer.advance();
                    var subroutineName = nameParser.parse(tokenizer).orElseThrow(() -> new SyntaxError("Identifier expected after dot"));
                    var expressions = expressionListParser.parse(tokenizer);
                    return Optional.of(new Term.SubroutineCall(Optional.of(className), subroutineName, expressions));
                }
                default: return Optional.empty();
            }
        }

    }
    sealed interface Term permits ParenthesisExpression, Term.Constant, Term.KeywordLiteral, Term.SubroutineCall, Term.UnaryOpTerm, Term.VarName {

        record Constant(Token literal) implements Term {
            public Constant {
                if (!(literal instanceof Token.StringLiteral) && !(literal instanceof Token.IntegerLiteral)) {
                    throw new IllegalArgumentException("Constant must be either a string or integer literal");
                }
            }
        }

        record VarName(String name, Optional<ExpressionParser.Expression> index) implements Term {
        }

        record KeywordLiteral(Token.KeywordType type) implements Term {
            public KeywordLiteral {
                switch (type) {
                    case TRUE, FALSE, NULL, THIS:
                        break;
                    default:
                        throw new IllegalArgumentException("Only true and false are valid keyword constants");
                }
            }
        }

        record UnaryOpTerm(Term.UnaryOp op, Term term) implements Term {
        }

        enum UnaryOp {

            NEGATIVE, NOT;

            public static UnaryOp from(Token symbol) {
                return switch (symbol) {
                    case Token.Symbol s when s.type() == TILDE -> NOT;
                    case Token.Symbol s when s.type() == MINUS -> NEGATIVE;
                    default -> throw new IllegalArgumentException("Unary operator must be either ~ or -");
                };
            }

        }

        record SubroutineCall(Optional<String> target, String subroutineName,
                List<ExpressionParser.Expression> arguments) implements Term, StatementsParser.Statement {
        }

    }

    record ParenthesisExpression(ExpressionParser.Expression expression) implements Term {}
}
