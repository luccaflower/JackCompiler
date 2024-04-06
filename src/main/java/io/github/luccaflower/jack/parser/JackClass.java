package io.github.luccaflower.jack.parser;

import java.util.Map;

public record JackClass(String name, Map<String, Type> statics, Map<String, Type> fields) {
}
