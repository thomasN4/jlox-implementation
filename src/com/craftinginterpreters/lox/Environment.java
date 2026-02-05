package com.craftinginterpreters.lox;

import com.craftinginterpreters.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

class Environment {
    final Environment enclosing;

    private final List<Tuple<String, Object>> values = new ArrayList<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(int distance, int index) {
        return ancestor(distance).values.get(index).snd;
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            assert environment != null;
            environment = environment.enclosing;
        }

        return environment;
    }

    void define(String name, Object value) {
        values.add(new Tuple<>(name, value));
    }

    void assign(Object value, int distance, int index) {
        ancestor(distance).values.get(index).snd = value;
    }

    int getSize() {
        return values.size();
    }
}
