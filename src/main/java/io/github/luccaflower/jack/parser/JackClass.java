package io.github.luccaflower.jack.parser;

import java.util.Map;

public record JackClass(String name, Map<String, Type.VarType> statics, Map<String, Type.VarType> fields) {
}
