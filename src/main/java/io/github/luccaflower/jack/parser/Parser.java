package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.*;

public class Parser {
    public Class parse(IteratingTokenizer tokenizer) {
        var next = tokenizer.advance();
        return switch (next) {
            case Token.Keyword k when k.type() == Token.KeywordType.CLASS -> new ClassParser().parse(tokenizer).orElseThrow(() -> new SyntaxError("failed to parse class"));
            default -> throw new SyntaxError("Unexpected token: %s".formatted(next));
        };
    }


    public static class ClassParser {
        public Optional<Class> parse(IteratingTokenizer tokenizer) {
            var next = tokenizer.advance();
            var className = switch (next) {
                case Token.Identifier i -> i.name();
                default -> throw new SyntaxError("Expected identifer, got %s".formatted(next));
            };
            var beginClass = tokenizer.advance();
            switch (beginClass) {
                case Token.Symbol s when s.type() == Token.SymbolType.OPEN_BRACE -> {}
                default -> throw new SyntaxError("Expected {, got %s".formatted(beginClass));
            }
            var classVarDecs = new ClassVarDecsParser().parse(tokenizer);
            var endClass = tokenizer.advance();
            switch (endClass) {
                case Token.Symbol s when s.type() == Token.SymbolType.CLOSE_BRACE -> {}
                default -> throw new SyntaxError("Expected }, got %s".formatted(endClass));
            }
            return Optional.of(new Class(className, classVarDecs.statics(), classVarDecs.fields()));
        }
    }

    public static class ClassVarDecsParser {
        public ClassVarDec parse(IteratingTokenizer tokenizer) {
            var statics = new HashMap<String, Type>();
            var fields = new HashMap<String, Type>();
            var fieldParser = new FieldDecParser();
            while (fieldParser.parse(tokenizer).orElse(null) instanceof FieldDec dec) {
                if (dec.scope() == ClassVarScope.STATIC) {
                    dec.names().forEach(name -> statics.put(name, dec.type()));
                }
                if (dec.scope() == ClassVarScope.FIELD) {
                    dec.names().forEach(name -> fields.put(name, dec.type()));
                }
            }
            return new ClassVarDec(statics, fields);
        }
    }

    static class FieldDecParser {
        Optional<FieldDec> parse(IteratingTokenizer tokenizer) {
            var newScopeToken = tokenizer.peek().orElseThrow(() -> new SyntaxError("unexpected end of file"));
            ClassVarScope scope;
            switch (newScopeToken) {
                case Token.Keyword k when k.type() == Token.KeywordType.STATIC:
                    scope = ClassVarScope.STATIC;
                    break;
                case Token.Keyword k when k.type() == Token.KeywordType.FIELD:
                    scope = ClassVarScope.FIELD;
                    break;
                default: return Optional.empty();
            }
            tokenizer.advance();
            var typeToken = tokenizer.advance();
            var type = switch (typeToken) {
                case Token.Keyword k -> PrimitiveType.from(k);
                case Token.Identifier i -> new ClassType(i.name());
                default -> throw  new SyntaxError("Unexpected type: %s".formatted(typeToken));
            };
            Set<String> names = new HashSet<>();
            loop: while (tokenizer.advance() instanceof Token.Identifier i) {
                names.add(i.name());
                switch (tokenizer.advance()) {
                    case Token.Symbol s when s.type() == Token.SymbolType.COMMA:
                        continue;
                    case Token.Symbol s when s.type() == Token.SymbolType.SEMICOLON:
                        break loop;
                    default: throw new SyntaxError("Unexpected token");
                }
            }
            return Optional.of(new FieldDec(scope, type, names));
        }
    }

    record FieldDec(ClassVarScope scope, Type type, Set<String> names) {

    }

    public enum ClassVarScope {
        STATIC, FIELD;
    }

    public record ClassVarDec(Map<String, Type> statics, Map<String, Type> fields) {}

    public static class ExpressionParser {

        public Optional<Expression> parse(IteratingTokenizer tokens) {
            return new TermParser().parse(tokens)
                .map(term -> new Expression(term, new Parser.ExpressionParser.OperatorParser().parse(tokens)
                    .map(op -> new Parser.ExpressionParser().parse(tokens)
                        .map(e -> new Parser.ExpressionParser.OpAndExpression(op, e))
                        .orElseThrow(
                                () -> new SyntaxError("Invalid continuation to %s: %s".formatted(op, tokens.peek()))))));
        }

        public static class OperatorParser {

            public Optional<Operator> parse(IteratingTokenizer tokens) throws SyntaxError {
                return tokens.peek()
                    .filter(t -> t instanceof Token.Symbol)
                    .flatMap(t -> switch (((Token.Symbol) t).type()) {
                        case PLUS, MINUS, ASTERISK, SLASH, AMPERSAND, PIPE, LESS_THAN, GREATER_THAN, EQUALS -> {
                            tokens.advance();
                            yield Optional.of(Operator.from((Token.Symbol) t));
                        }
                        default -> Optional.empty();

                    });
            }

        }

        public enum Operator {

            PLUS, MINUS, TIMES, DIVIDED_BY, BITWISE_AND, BITWISE_OR, LESS_THAN, GREATER_THAN, EQUALS;

            public static Operator from(Token.Symbol s) {
                return switch (s.type()) {
                    case PLUS -> PLUS;
                    case MINUS -> MINUS;
                    case ASTERISK -> TIMES;
                    case SLASH -> DIVIDED_BY;
                    case AMPERSAND -> BITWISE_AND;
                    case PIPE -> BITWISE_OR;
                    case LESS_THAN -> LESS_THAN;
                    case GREATER_THAN -> GREATER_THAN;
                    case EQUALS -> EQUALS;
                    default -> throw new IllegalArgumentException("Invalid operator " + s.type());
                };
            }

        }

        public record Expression(Term term, Optional<OpAndExpression> continuation) {
        }

        public record OpAndExpression(Operator op, Expression term) {
        }

        public class TermParser {

            public Optional<Term> parse(IteratingTokenizer tokens) {
                return new ConstantParser().parse(tokens)
                    .or(() -> new VarNameParser().parse(tokens))
                    .or(() -> new KeywordLiteralParser().parse(tokens))
                    .or(() -> new UnaryOpTermParser().parse(tokens));
            }

        }

        public static class ConstantParser {

            public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
                return tokens.peek()
                    .filter(t -> t instanceof Token.StringLiteral || t instanceof Token.IntegerLiteral)
                    .map(t -> new Term.Constant(tokens.remove()));
            }

        }

        public static class VarNameParser {

            public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
                return tokens.peek().filter(t -> t instanceof Token.Identifier).map(Term.VarName::from);
            }

        }

        public static class KeywordLiteralParser {

            public Optional<Term> parse(IteratingTokenizer tokens) throws SyntaxError {
                return tokens.peek()
                    .filter(t -> t instanceof Token.Keyword)
                    .map(t -> new Term.KeywordLiteral(((Token.Keyword) tokens.remove()).type()));
            }

        }

        public class UnaryOpTermParser {

            public Optional<Term> parse(IteratingTokenizer tokens) {
                return tokens.peek()
                    .filter(t -> t instanceof Token.Symbol)
                    .flatMap(t -> switch (((Token.Symbol) t).type()) {
                        case TILDE, MINUS -> Optional.of(Term.UnaryOp.from((Token.Symbol) t));
                        default -> Optional.empty();
                    })
                    .flatMap(op -> new TermParser().parse(tokens.lookAhead(1)).map(term -> new Term.UnaryOpTerm(op, term)));
            }

        }

    }

    public record Class(String name, Map<String, Type> statics, Map<String, Type> fields) {
    }

    public sealed interface Type {
    }
    public record ClassType(String name) implements Type {}

    public enum PrimitiveType implements Type {
        INT, CHAR, BOOLEAN;

        public static PrimitiveType from(Token.Keyword k) {
            return switch (k.type()) {
                case INT -> INT;
                case CHAR -> CHAR;
                case BOOLEAN -> BOOLEAN;
                default -> throw new SyntaxError("Unexpeceted type %s".formatted(k));
            };
        }
    }
    public sealed interface Term {

        record Constant(Token literal) implements Term {
            public Constant {
                if (!(literal instanceof Token.StringLiteral) && !(literal instanceof Token.IntegerLiteral)) {
                    throw new IllegalArgumentException("Constant must be either a string or integer literal");
                }
            }
        }

        record VarName(String name) implements Term {
            public static VarName from(Token token) {
                if (token instanceof Token.Identifier(String name)) {
                    return new VarName(name);
                }
                else {
                    throw new IllegalArgumentException("VarName must be an identifier");
                }
            }
        }

        record KeywordLiteral(Token.KeywordType type) implements Term {
            public KeywordLiteral {
                switch (type) {
                    case TRUE, FALSE, NULL, THIS -> {
                    }
                    default -> throw new IllegalArgumentException("Only true and false are valid keyword constants");
                }
            }
        }

        record UnaryOpTerm(Term.UnaryOp op, Term term) implements Term {
        }

        enum UnaryOp {

            NEGATIVE, NOT;

            public static UnaryOp from(Token.Symbol symbol) {
                return switch (symbol.type()) {
                    case TILDE -> NOT;
                    case MINUS -> NEGATIVE;
                    default -> throw new IllegalArgumentException("Unary operator must be either ~ or -");
                };
            }

        }

    }

}
