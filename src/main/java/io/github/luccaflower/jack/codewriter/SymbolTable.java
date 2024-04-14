package io.github.luccaflower.jack.codewriter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record SymbolTable(Map<String, Symbol> symbols, Scope scope) {

    public static SymbolTable create(List<Identifier> entries, Scope scope) {
        return new SymbolTable(IntStream.range(0, entries.size())
            .mapToObj(i -> new Symbol(i, scope, entries.get(i).type()))
            .collect(Collectors.toMap(e -> entries.get(e.index()).name(), e -> e)), scope);
    }

    Optional<Symbol> resolve(String name) {
        return Optional.ofNullable(symbols.get(name));
    }

    public record Identifier(String name, String type) {
    }

    record Symbol(int index, Scope scope, String type) {
    }

    public enum Scope {

        LOCAL, FIELD, STATIC, SUBROUTINE, ARGUMENT

    }
}
