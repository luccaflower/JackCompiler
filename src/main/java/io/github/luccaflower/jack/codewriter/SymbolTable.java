package io.github.luccaflower.jack.codewriter;

import java.util.List;
import java.util.Optional;

public record SymbolTable(List<String> names, Scope scope) {
    Optional<Symbol> resolve(String name) {
        if (!names.contains(name)) {
            return Optional.empty();
        } else {
            return Optional.of(new Symbol(names.indexOf(name), scope));
        }
    }

    record Symbol(int index, Scope scope) {}

    enum Scope {
        LOCAL, FIELD, STATIC, SUBROUTINE
    }
}
