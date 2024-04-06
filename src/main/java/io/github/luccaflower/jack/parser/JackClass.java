package io.github.luccaflower.jack.parser;

import java.util.HashMap;
import java.util.Map;

public record JackClass(String name, Map<String, Type.VarType> statics, Map<String, Type.VarType> fields,
        Map<String, SubroutinesDecsParser.Subroutine> subroutines) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name = "";

        private Map<String, Type.VarType> statics = new HashMap<>();

        private Map<String, Type.VarType> fields = new HashMap<>();

        private Map<String, SubroutinesDecsParser.Subroutine> subroutines = new HashMap<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder statics(Map<String, Type.VarType> statics) {
            this.statics = statics;
            return this;
        }

        public Builder fields(Map<String, Type.VarType> fields) {
            this.fields = fields;
            return this;
        }

        public Builder subroutines(Map<String, SubroutinesDecsParser.Subroutine> subroutines) {
            this.subroutines = subroutines;
            return this;
        }

        public JackClass build() {
            return new JackClass(name, statics, fields, subroutines);
        }

    }
}
