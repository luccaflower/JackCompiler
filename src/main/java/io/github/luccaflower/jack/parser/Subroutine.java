package io.github.luccaflower.jack.parser;

import java.util.List;
import java.util.Map;

public sealed interface Subroutine {

    String name();

    Map<String, Type.VarType> arguments();

    Map<String, Type.VarType> locals();

    List<Statement> statements();

    record JackFunction(String name, Type.ReturnType type, Map<String, Type.VarType> arguments,
            Map<String, Type.VarType> locals, List<Statement> statements) implements Subroutine {

    }

    record JackMethod(String name, Type.ReturnType type, Map<String, Type.VarType> arguments,
            Map<String, Type.VarType> locals, List<Statement> statements) implements Subroutine {

    }

    record JackConstructor(String name, Type.ReturnType type, Map<String, Type.VarType> arguments,
            Map<String, Type.VarType> locals, List<Statement> statements) implements Subroutine {

    }

}
