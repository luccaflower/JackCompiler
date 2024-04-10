package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.Token;

import java.util.List;
import java.util.Optional;

import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.MINUS;
import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.TILDE;

public sealed interface Term permits Term.Constant, Term.DoStatement, Term.KeywordLiteral, Term.UnaryOpTerm, Term.VarName, Term.ParenthesisExpression {

    record Constant(Token literal) implements Term {
        public Constant {
            if (!(literal instanceof Token.StringLiteral) && !(literal instanceof Token.IntegerLiteral)) {
                throw new IllegalArgumentException("Constant must be either a string or integer literal");
            }
        }
    }

    record VarName(String name, Optional<Expression> index) implements Term {
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

    record UnaryOpTerm(UnaryOp op, Term term) implements Term {
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

    sealed interface DoStatement extends Term, Statement {

    }
    record LocalDoStatement(String subroutineName,
                            List<Expression> arguments) implements DoStatement {
    }
    record ObjectDoStatement(String target, String subroutineName,
                             List<Expression> arguments) implements DoStatement {
    }

    record ParenthesisExpression(Expression expression) implements Term {
    }
}
