package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.Token;

import java.util.List;

import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.MINUS;
import static io.github.luccaflower.jack.tokenizer.Token.SymbolType.TILDE;

public sealed interface Term permits Term.Constant, Term.KeywordLiteral, Term.ParenthesisExpression,
        Term.SubroutineCall, Term.UnaryOpTerm, Term.VarName {

    record Constant(Token literal) implements Term {
        public Constant {
            if (!(literal instanceof Token.StringLiteral) && !(literal instanceof Token.IntegerLiteral)) {
                throw new IllegalArgumentException("Constant must be either a string or integer literal");
            }
        }
    }

    sealed interface VarName extends Term {

        String name();

    }

    record NonIndexedVarName(String name) implements VarName {
    }

    record IndexedVarname(String name, Expression index) implements VarName {
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

        NEGATIVE("neg"), NOT("not");

        private final String c;

        UnaryOp(String c) {

            this.c = c;
        }

        public String instruction() {
            return c;
        }

        public static UnaryOp from(Token symbol) {
            return switch (symbol) {
                case Token.Symbol s when s.type() == TILDE -> NOT;
                case Token.Symbol s when s.type() == MINUS -> NEGATIVE;
                default -> throw new IllegalArgumentException("Unary operator must be either ~ or -");
            };
        }

    }

    sealed interface SubroutineCall extends Term, Statement {

    }

    record LocalSubroutineCall(String subroutineName, List<Expression> arguments) implements SubroutineCall {
    }

    record ObjectSubroutineCall(String target, String subroutineName,
            List<Expression> arguments) implements SubroutineCall {
    }

    record ParenthesisExpression(Expression expression) implements Term {
    }

}
