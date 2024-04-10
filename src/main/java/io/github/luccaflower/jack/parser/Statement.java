package io.github.luccaflower.jack.parser;

import java.util.List;
import java.util.Optional;

public sealed interface Statement permits Statement.IfStatement, Statement.LetStatement, Statement.ReturnStatement, Statement.WhileStatement, Term.DoStatement {

    record WhileStatement(Expression condition, List<Statement> statements) implements Statement {
    }

    record IfStatement(Expression condition, List<Statement> statements,
            Optional<Statement.ElseBlock> elseBlock) implements Statement {
    }

    record ElseBlock(List<Statement> statements) {
    }

    record LetStatement(String name, Optional<Expression> index, Expression value) implements Statement {
    }

    record ReturnStatement(Optional<Expression> returnValue) implements Statement {
    }

}
