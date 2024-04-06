package io.github.luccaflower.jack.parser;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.github.luccaflower.jack.TokenizerUtils.tokenize;
import static io.github.luccaflower.jack.parser.Type.PrimitiveType.INT;
import static org.assertj.core.api.Assertions.*;

class ParserTest {

    private final Parser parser = new Parser();

    @Test
    void theCompilationUnitIsAClass() {
        assertThat(parser.parse(tokenize("class Name {}")))
            .isEqualTo(new JackClass("Name", new HashMap<>(), new HashMap<>()));
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
    void subroutinesMustEndWithAReturn() {
        var input = """
                class Name {
                    function void name() {
                        return;
                    }
                }""";
        assertThatCode(() -> parser.parse(tokenize(input))).doesNotThrowAnyException();
    }

}