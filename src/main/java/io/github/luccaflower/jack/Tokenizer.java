package io.github.luccaflower.jack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tokenizer {

    private final KeywordTokenizer keywordTokenizer = new KeywordTokenizer();
    private final SymbolTokenizer symbolTokenizer = new SymbolTokenizer();
    private final IntegerLiteralTokenizer integerLiteralTokenizer = new IntegerLiteralTokenizer();
    private final StringLiteralTokenizer stringLiteralTokenizer = new StringLiteralTokenizer();
    private final IdentifierTokenizer identifierTokenizer = new IdentifierTokenizer();

    private static final Pattern WHITESPACE = Pattern.compile("(\\s+)|\\n|\\t");
    private static final Pattern LINE_COMMENT = Pattern.compile("//.*");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*\\*/", Pattern.DOTALL);

    public List<Token> parse(String input) throws SyntaxError {
        var cursor = 0;
        var list = new ArrayList<Token>();
        while (cursor < input.length()) {
            var rest = input.substring(cursor);
            var whitespace = WHITESPACE.matcher(rest);
            if (whitespace.lookingAt()) {
                cursor += whitespace.end();
                continue;
            }
            var lineComment = LINE_COMMENT.matcher(rest);
            if (lineComment.lookingAt()) {
                cursor += lineComment.end();
                continue;
            }
            var blockComment = BLOCK_COMMENT.matcher(rest);
            if (blockComment.lookingAt()) {
                cursor+= blockComment.end();
                continue;
            }

            var parsed = keywordTokenizer.parse(rest)
                    .or(() -> symbolTokenizer.parse(rest))
                    .or(() -> integerLiteralTokenizer.parse(rest))
                    .or(() -> stringLiteralTokenizer.parse(rest))
                    .or(() -> identifierTokenizer.parse(rest))
                    .orElseThrow(() -> new SyntaxError("Unexpected token: ".concat(rest.split("\\s")[0])));
            cursor += parsed.length();
            list.add(parsed.token());
        }

        return list;
    }

    private static class KeywordTokenizer {

        private static final Pattern KEYWORD = Pattern
            .compile("(%s)".formatted(Arrays.stream(Token.KeywordType.values())
                .map(Token.KeywordType::keyword)
                .collect(Collectors.joining(")|("))));

        public Optional<ParseResult> parse(String input) {
            var matcher = KEYWORD.matcher(input);
            if (matcher.lookingAt()) {
                var keyword = Token.KeywordType.from(matcher.group());
                return Optional.of(new ParseResult(new Token.Keyword(keyword), matcher.end()));
            }
            else {
                return Optional.empty();
            }
        }

    }

    private static class SymbolTokenizer {
        private static final Pattern SYMBOL = Pattern
                .compile("(%s)".formatted(Arrays.stream(Token.SymbolType.values())
                        .map(Token.SymbolType::symbol)
                        .map(String::valueOf)
                        .map(Pattern::quote)
                        .collect(Collectors.joining(")|("))));


        public Optional<ParseResult> parse(String input) {
            var matcher = SYMBOL.matcher(input);
            if (matcher.lookingAt()) {
                var symbol = Token.SymbolType.from(matcher.group().charAt(0));
                return Optional.of(new ParseResult(new Token.Symbol(symbol), matcher.end()));
            }
            else {
                return Optional.empty();
            }
        }
    }

    private static class IntegerLiteralTokenizer {
        private static final Pattern INTEGER = Pattern.compile("\\d+");

        public Optional<ParseResult> parse(String input) {
            var matcher = INTEGER.matcher(input);
            if (matcher.lookingAt()) {
                var literal = new Token.IntegerLiteral(Integer.parseInt(matcher.group()));
                return Optional.of(new ParseResult(literal, matcher.end()));
            }
            else {
                return Optional.empty();
            }
        }
    }

    private static class StringLiteralTokenizer {
        private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"^\n])*\"");

        public Optional<ParseResult> parse(String input) {
            var matcher = STRING_LITERAL.matcher(input);
            if (matcher.lookingAt()) {
                var literal = new Token.StringLiteral(matcher.group().replace("\"", ""));
                return Optional.of(new ParseResult(literal, matcher.end()));
            }
            else {
                return Optional.empty();
            }
        }

    }

    private static class IdentifierTokenizer {
        private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
        public Optional<ParseResult> parse(String input) {
            var matcher = IDENTIFIER.matcher(input);
            if (matcher.lookingAt()) {
                var literal = new Token.Identifier(matcher.group());
                return Optional.of(new ParseResult(literal, matcher.end()));
            }
            else {
                return Optional.empty();
            }
        }
    }
    private record ParseResult(Token token, int length) {
    }

    public static class SyntaxError extends Exception {

        public SyntaxError(String message) {
            super(message);
        }

    }

}
