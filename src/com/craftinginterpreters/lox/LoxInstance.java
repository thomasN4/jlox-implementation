package com.craftinginterpreters.lox;

import com.craftinginterpreters.utils.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class LoxInstance {
    private final ClassDescription klass;
    private final Map<String, Object> fields = new HashMap<>();

    private static class ClassDescription {
        private final String name;
        private final Function<String, Tuple<LoxFunction, Boolean>> findMethod;

        ClassDescription(Object klass) {
            if (klass instanceof Metaclass) {
                this.name = ((Metaclass) klass).name;
                this.findMethod = ((Metaclass) klass)::findMethod;
            } else if (klass instanceof LoxClass) {
                this.name = ((LoxClass) klass).name;
                this.findMethod = ((LoxClass) klass)::findMethod;
            } else {
                throw new RuntimeException();
            }
        }
    }

    LoxInstance(Object klass) {
        this.klass = new ClassDescription(klass);
    }

    protected Object get(Token name, Interpreter interpreter) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        Tuple<LoxFunction, Boolean> tuple = klass.findMethod.apply(name.lexeme);
        if (tuple != null) {
            LoxFunction method = tuple.fst.bind(this);
            if (!tuple.snd) return method;
            else return method.call(interpreter, null);
        }

        throw new RuntimeError(name,
                "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
