package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.Token;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.luccaflower.jack.TokenizerUtils.tokenize;
import static io.github.luccaflower.jack.parser.Type.PrimitiveType.INT;
import static org.assertj.core.api.Assertions.*;

class ParserTest {

    private final Parser parser = new Parser();

    @Test
    void theCompilationUnitIsAClass() {
        assertThat(parser.parse(tokenize("class Name {}"))).isEqualTo(JackClass.builder().name("Name").build());
    }

    @Test
    void classesCanHaveStaticClassVars() {
        var input = """
                class Name {
                    static int varName;
                }""";
        assertThat(parser.parse(tokenize(input)).statics()).isEqualTo(Map.of("varName", INT));
    }

    @Test
    void classesCanHaveSeveralClassVars() {
        var input = """
                class Name {
                    static int var1;
                    static int var2;
                }""";
        assertThat(parser.parse(tokenize(input)).statics()).isEqualTo(Map.of("var1", INT, "var2", INT));
    }

    @Test
    void multipleDeclarationsOneTheSameLine() {
        var input = """
                class Name {
                    field int var1, var2;
                }""";
        assertThat(parser.parse(tokenize(input)).fields()).isEqualTo(Map.of("var1", INT, "var2", INT));
    }

    @Test
    void aFieldCanBeAClassType() {
        var input = """
                class Name {
                    field Other var1;
                }""";
        assertThat(parser.parse(tokenize(input)).fields()).isEqualTo(Map.of("var1", new Type.ClassType("Other")));
    }

    @Test
    void subroutinesAreIdentifiedByTheirName() {
        var input = """
                class Name {
                    function void name() {
                        return;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines()).containsKey("name");
    }

    @Test
    void subroutinesMayHaveParameters() {
        var input = """
                class Name {
                    function void name(int arg1) {
                        return;
                    }
                }""";

        assertThat(parser.parse(tokenize(input)).subroutines().get("name").arguments()).containsKey("arg1");
    }

    @Test
    void localVarsAreDeclaredInTheBeginningOfTheBody() {
        var input = """
                class Name {
                    function void name() {
                        var int local1;
                        return;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").locals()).containsKey("local1");
    }

    @Test
    void subroutineMayReturnAValue() {
        var input = """
                class Name {
                    function int name() {
                        return 0;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).last()
            .isEqualTo(new Statement.ReturnStatement(Optional.of(constantExpression(0))));

    }

    @Test
    void subroutinesMayHaveLetStatements() {
        var input = """
                class Name {
                    function void name() {
                        var int var1;
                        let var1 = 0;
                        return;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.LetStatement.class))
            .isEqualTo(new Statement.NonIndexedLetStatement("var1", constantExpression(0)));
    }

    @Test
    void variableNamesMayHaveAnIndex() {
        var input = """
                class Name {
                    function void name() {
                        var Array var1;
                        let var1[0] = 0;
                        return;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.IndexedLetStatement.class))
            .extracting(Statement.IndexedLetStatement::index)
            .isEqualTo(constantExpression(0));
    }

    @Test
    void ifStatements() {
        var input = """
                class Name {
                    function void name() {
                        if (true) { return; }
                        return;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .isEqualTo(new Statement.IfStatement(constantExpression(true),
                    List.of(new Statement.ReturnStatement(Optional.empty())), Optional.empty()));
    }

    @Test
    void elseBlocks() {
        var input = """
                class Name {
                    function void name() {
                        if (true) { return; } else { return; }
                        return;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.IfStatement.class))
            .extracting(Statement.IfStatement::elseBlock)
            .isEqualTo(Optional.of(new Statement.ElseBlock(List.of(new Statement.ReturnStatement(Optional.empty())))));

    }

    @Test
    void whileStatements() {
        var input = """
                class Name {
                    function void name() {
                        var int x;
                        while (true) { }
                        return;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .isEqualTo(new Statement.WhileStatement(constantExpression(true), List.of()));
    }

    @Test
    void methods() {
        var input = """
                class Name {
                    method void name() {
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name")).isInstanceOf(Subroutine.JackMethod.class);
    }

    @Test
    void multipleLocalDeclarationsInOneStatement() {
        var input = """
                class Name {
                    function void name() {
                        var int name1, name2;
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").locals()).size().isEqualTo(2);
    }

    @Test
    void subroutineCalls() {
        var input = """
                class Name {
                    function void name() {
                        do Other.call();
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .isInstanceOf(Term.SubroutineCall.class);
    }

    @Test
    void indexingInExpressions() {
        var input = """
                class Name {
                    function void name() {
                        var int i;
                        var Array arr;
                        let i = arr[0];
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).last()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.LetStatement.class))
            .extracting(s -> s.value().term())
            .asInstanceOf(InstanceOfAssertFactories.type(Term.IndexedVarname.class))
            .extracting(Term.IndexedVarname::index)
            .isEqualTo(constantExpression(0));
    }

    @Test
    void parenthesisExpression() {
        var input = """
                class Name {
                    function void name() {
                        var int i;
                        let i = 1 + (2 + 3);
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).last()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.LetStatement.class))
            .extracting(Statement.LetStatement::value)
            .extracting(Expression::continuation)
            .extracting(Optional::get)
            .extracting(Expression.OpAndExpression::term)
            .isInstanceOf(Expression.class);

    }

    @Test
    void equalityOperatorInConditions() {
        var input = """
                class Name {
                    function void name() {
                        if (1 = 1) { }
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.IfStatement.class))
            .extracting(Statement.IfStatement::condition)
            .isEqualTo(new Expression(new Term.Constant(new Token.IntegerLiteral(1)),
                    Optional.of(new Expression.OpAndExpression(Expression.Operator.EQUALS, constantExpression(1)))));
    }

    @Test
    void andOperatorInConditions() {
        var input = """
                class Name {
                    function void name() {
                        if (1 & 1) { }
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.IfStatement.class))
            .extracting(Statement.IfStatement::condition)
            .isEqualTo(new Expression(new Term.Constant(new Token.IntegerLiteral(1)), Optional
                .of(new Expression.OpAndExpression(Expression.Operator.BITWISE_AND, constantExpression(1)))));
    }

    @Test
    void unaryOpExpressionInConditions() {
        var input = """
                class Name {
                    function void name() {
                        if (~(1 & 1)) { }
                    }
                }""";
        assertThat(parser.parse(tokenize(input)).subroutines().get("name").statements()).first()
            .asInstanceOf(InstanceOfAssertFactories.type(Statement.IfStatement.class))
            .extracting(Statement.IfStatement::condition)
            .extracting(Expression::term)
            .asInstanceOf(InstanceOfAssertFactories.type(Term.UnaryOpTerm.class))
            .extracting(Term.UnaryOpTerm::op)
            .isEqualTo(Term.UnaryOp.NOT);
    }

    private static Expression constantExpression(int i) {
        return new Expression(new Term.Constant(new Token.IntegerLiteral(i)), Optional.empty());
    }

    private static Expression constantExpression(String s) {
        return new Expression(new Term.Constant(new Token.StringLiteral(s)), Optional.empty());
    }

    private static Expression constantExpression(boolean b) {
        return new Expression(new Term.KeywordLiteral(Token.KeywordType.from(String.valueOf(b))), Optional.empty());

    }

}