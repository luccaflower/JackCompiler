package io.github.luccaflower.jack;

import io.github.luccaflower.jack.parser.Parser;
import io.github.luccaflower.jack.parser.StatementsParser;
import org.junit.jupiter.api.Test;

import static io.github.luccaflower.jack.TokenizerUtils.tokenize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ConvertToBinTest {

    private static final String input = """
            // This file is part of www.nand2tetris.org
            // and the book "The Elements of Computing Systems"
            // by Nisan and Schocken, MIT Press.
            // File name: projects/11/ConvertToBin/Main.jack

            /**
             * Unpacks a 16-bit number into its binary representation:
             * Takes the 16-bit number stored in RAM[8000] and stores its individual\s
             * bits in RAM[8001..8016] (each location will contain 0 or 1).
             * Before the conversion, RAM[8001]..RAM[8016] are initialized to -1.
             *\s
             * The program should be tested as follows:
             * 1) Load the program into the supplied VM emulator
             * 2) Put some value in RAM[8000]
             * 3) Switch to "no animation"
             * 4) Run the program (give it enough time to run)
             * 5) Stop the program
             * 6) Check that RAM[8001]..RAM[8016] contain the correct bits, and
             *    that none of these memory locations contains -1.
             */
            class Main {
               \s
                /**
                 * Initializes RAM[8001]..RAM[8016] to -1,
                 * and converts the value in RAM[8000] to binary.
                 */
                function void main() {
            	    var int value;
                    do Main.fillMemory(8001, 16, -1); // sets RAM[8001]..RAM[8016] to -1
                    let value = Memory.peek(8000);    // Uses an OS routine to read the input
                    do Main.convert(value);           // performs the conversion
                    return;
                }
               \s
                /** Converts the given decimal value to binary, and puts\s
                 *  the resulting bits in RAM[8001]..RAM[8016]. */
                function void convert(int value) {
                	var int mask, position;
                	var boolean loop;

                	let loop = true;
                	while (loop) {
                	    let position = position + 1;
                	    let mask = Main.nextMask(mask);

                	    if (~(position > 16)) {

                	        if (~((value & mask) = 0)) {
                	            do Memory.poke(8000 + position, 1);
                   	        }
                	        else {
                	            do Memory.poke(8000 + position, 0);
                  	        }   \s
                	    }
                	    else {
                	        let loop = false;
                	    }
                	}
                	return;
                }
            \s
                /** Returns the next mask (the mask that should follow the given mask). */
                function int nextMask(int mask) {
                	if (mask = 0) {
                	    return 1;
                	}
                	else {
            	    return mask * 2;
                	}
                }
               \s
                /** Fills 'length' consecutive memory locations with 'value',
                  * starting at 'address'. */
                function void fillMemory(int address, int length, int value) {
                    while (length > 0) {
                        do Memory.poke(address, value);
                        let length = length - 1;
                        let address = address + 1;
                    }
                    return;
                }
            }
            """;

    @Test
    void convertToBin() {
        assertThatCode(() -> new Parser().parse(tokenize(input))).doesNotThrowAnyException();
    }

    @Test
    void handlesConditionsProperly() {
        var input = """
                if (~(position > 16)) {

                    	        if (~((value & mask) = 0)) {
                    	            do Memory.poke(8000 + position, 1);
                       	        }
                    	        else {
                    	            do Memory.poke(8000 + position, 0);
                      	        }   \s
                    	    }
                """;
        assertThatCode(() -> new StatementsParser.StatementParser().parse(tokenize(input))).doesNotThrowAnyException();

    }

}
