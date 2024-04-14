package io.github.luccaflower.jack.codewriter;

import java.util.List;
import java.util.Optional;

public record SymbolTable(List<String> names, Scope scope) {

    public SymbolTable {

    }

    public static SymbolTable create(List<Entry> entries, Scope scope) {
        return new SymbolTable(entries.stream().map(Entry::name).toList(), scope);
    }

    Optional<Symbol> resolve(String name) {
        if (!names.contains(name)) {
            return Optional.empty();
        }
        else {
            return Optional.of(new Symbol(names.indexOf(name), scope));
        }
    }

    record Entry(String name, String type) {}
    record Symbol(int index, Scope scope) {
    }

    enum Scope {

        LOCAL, FIELD, STATIC, SUBROUTINE, ARGUMENT

    }
}
