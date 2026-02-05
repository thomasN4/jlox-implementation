package com.craftinginterpreters.lox;

import com.craftinginterpreters.utils.Tuple;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    final LoxClass superclass;
    final List<Trait> traits;
    private final Map<String, Tuple<LoxFunction, Boolean>> methods;

    LoxClass(String name, LoxClass superclass, List<Trait> traits,
             Map<String, Tuple<LoxFunction, Boolean>> methods,
             Map<String, LoxFunction> staticMethods) {
        super(new Metaclass(name + " class", superclass, staticMethods));
        this.name = name;
        this.superclass = superclass;
        this.traits = traits;
        this.methods = methods;
    }

    Tuple<LoxFunction, Boolean> findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        for (Trait trait : traits) {
            LoxFunction method = trait.findMethod(name);
            if (method != null) {
                return new Tuple<>(method, false);
            }
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init") == null ?
                null : findMethod("init").fst;
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = findMethod("init") == null ?
                null : findMethod("init").fst;
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }
}