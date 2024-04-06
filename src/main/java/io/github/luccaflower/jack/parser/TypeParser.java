package io.github.luccaflower.jack.parser;

import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;
import io.github.luccaflower.jack.tokenizer.SyntaxError;
import io.github.luccaflower.jack.tokenizer.Token;

class TypeParser {

    public Type parse(IteratingTokenizer tokenizer) {
        return switch (tokenizer.advance()) {
            case Token.Keyword k when k.type() == Token.KeywordType.VOID -> new Type.VoidType();
            case Token.Keyword k -> Type.PrimitiveType.from(k);
            case Token.Identifier i -> new Type.ClassType(i.name());
            default -> throw new SyntaxError("Unexpected type: %s".formatted(tokenizer.advance()));
        };
    }

    static class VarTypeParser {

        public Type.VarType parse(IteratingTokenizer tokenizer) {
            var type = new TypeParser().parse(tokenizer);
            if (type instanceof Type.VoidType) {
                throw new SyntaxError("Void type not allowed here");
            }
            return (Type.VarType) type;
        }

    }

    static class ReturnTypeParser {

        public Type.ReturnType parse(IteratingTokenizer tokenizer) {
            return (Type.ReturnType) new TypeParser().parse(tokenizer);
        }

    }

}
