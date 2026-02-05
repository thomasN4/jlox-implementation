package com.craftinginterpreters.lox;

import com.craftinginterpreters.utils.Tuple;

import java.util.List;
import java.util.Map;

class Metaclass implements LoxCallable {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;

    Metaclass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    Tuple<LoxFunction, Boolean> findMethod(String name) {
        if (methods.containsKey(name)) {
            return new Tuple<>(methods.get(name), false);
        }

        return null;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new LoxInstance(this);
    }
}
