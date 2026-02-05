package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Environment closure;
    private final Decl declaration;
    private final boolean isInitializer;

    private static class Decl {
        Token name;
        List<Token> params;
        List<Stmt> body;
        Decl(Object declaration) {
            if (declaration instanceof Expr.Lambda) {
                name = null;
                params = ((Expr.Lambda) declaration).params;
                body = ((Expr.Lambda) declaration).body;
            } else if (declaration instanceof Stmt.Function) {
                name = ((Stmt.Function) declaration).name;
                params = ((Stmt.Function) declaration).params;
                body = ((Stmt.Function) declaration).body;
            } else {
                throw new RuntimeException();
            }
        }
    }

    private Decl decl(Object o) {
        if (o instanceof Decl) {
            return (Decl) o;
        } else {
            return new Decl(o);
        }
    }

    LoxFunction(Environment closure, Object declaration, boolean isInitializer) {
        this.closure = closure;
        this.declaration = decl(declaration);
        this.isInitializer = isInitializer;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(environment, declaration, isInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme,
                               arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (isInitializer) return closure.get(0, 0);
            return returnValue.value;
        }

        if (isInitializer) return closure.get(0, 0);
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
