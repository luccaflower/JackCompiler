package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

import java.util.Optional;

class TypeParser {

    public Optional<Type> parse(IteratingTokenizer tokenizer) {
        return switch (tokenizer.peek()) {
            case Token.Keyword k when k.type() == Token.KeywordType.VOID -> {
                tokenizer.advance();
                yield Optional.of(new Type.VoidType());
            }
            case Token.Keyword k -> {
                tokenizer.advance();
                yield Optional.of(Type.PrimitiveType.from(k));
            }
            case Token.Identifier i -> {
                tokenizer.advance();
                yield Optional.of(new Type.ClassType(i.name()));
            }
            default -> Optional.empty();
        };
    }

    static class VarTypeParser {

        public Optional<Type.VarType> parse(IteratingTokenizer tokenizer) {
            var type = new TypeParser().parse(tokenizer);
            if (type.orElse(null) instanceof Type.VoidType) {
                throw new SyntaxError("Void type not allowed here");
            }
            return type.map(t -> (Type.VarType) t);
        }

    }

    static class ReturnTypeParser {

        public Optional<Type.ReturnType> parse(IteratingTokenizer tokenizer) {
            return new TypeParser().parse(tokenizer).map(t -> (Type.ReturnType) t);
        }

    }

}
