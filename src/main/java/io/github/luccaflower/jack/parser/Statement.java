package io.github.luccaflower.jack.parser;

import java.util.List;
import java.util.Optional;

public sealed interface Statement permits Statement.IfStatement, Statement.LetStatement, Statement.ReturnStatement, Statement.WhileStatement, Term.SubroutineCall {

    record WhileStatement(Expression condition, List<Statement> statements) implements Statement {
    }

    record IfStatement(Expression condition, List<Statement> statements,
            Optional<Statement.ElseBlock> elseBlock) implements Statement {
    }

    record ElseBlock(List<Statement> statements) {
    }


    sealed interface LetStatement extends Statement {
        String name();
        Expression value();
    }
    record OldLetStatement(String name, Optional<Expression> index, Expression value) implements LetStatement {
    }
    record IndexedLetStatement(String name, Expression index, Expression value) implements LetStatement {

    }

    record NonIndexedLetStatement(String name, Expression value) implements LetStatement {

    }

    record ReturnStatement(Optional<Expression> returnValue) implements Statement {
    }

}
