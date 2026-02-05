package com.craftinginterpreters.lox;

import java.util.Map;

public class Trait {
    Map<String, LoxFunction> defaultImpls;

    Trait(Map<String, LoxFunction> defaultImpls) {
        this.defaultImpls = defaultImpls;
    }

    LoxFunction findMethod(String name) {
        return defaultImpls.get(name);
    }
}
