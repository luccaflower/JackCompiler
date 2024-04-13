package io.github.luccaflower.jack;

import io.github.luccaflower.jack.parser.Parser;
import org.junit.jupiter.api.Test;

import static io.github.luccaflower.jack.TokenizerUtils.tokenize;
import static org.assertj.core.api.Assertions.assertThatCode;

class SevenTest {

    private final String input = """
            // This file is part of www.nand2tetris.org
            // and the book "The Elements of Computing Systems"
            // by Nisan and Schocken, MIT Press.
            // File name: projects/11/Seven/Main.jack

            /**
             * Computes the value of 1 + (2 * 3) and prints the result
             * at the top-left of the screen. \s
             */
            class Main {

               function void main() {
                  do Output.printInt(1 + (2 * 3));
                  return;
               }

            }""";

    @Test
    void parser() {
        assertThatCode(() -> new Parser().parse(tokenize(input))).doesNotThrowAnyException();
    }

}
