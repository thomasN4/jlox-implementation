package com.craftinginterpreters.lox;

import java.util.*;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;

    // 1st 'boolean' tracks whether the symbol has been defined.
    // 2nd tracks usage.
    // 3rd integer is for line #.
    // 4th integer is for index in Environment instance.
    private final Stack<Map<String, List<Integer>>> scopes = new Stack<>();

    private FunctionType currentFunction = FunctionType.NONE;
    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD,
    }

    private ClassType currentClass = ClassType.NONE;
    private enum ClassType {
        NONE,
        CLASS,
        STATIC_METHOD_BODY,  // ¯\_(ツ)_/¯
        SUBCLASS,
        TRAIT,
    }

    private final Map<String, Map<String, Integer>> traits = new HashMap<>();

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        for (Map.Entry<String, List<Integer>> entry : scopes.peek().entrySet()) {
            if (entry.getValue().get(1) == 0) {
                Lox.warning(entry.getValue().get(2),
                        "Unused variable '" + entry.getKey() + "'.");
            }
        }
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, List<Integer>> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,
                    "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme, List.of(0, 0, name.line, scope.size()));
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().compute(
            name.lexeme, (k, x) -> {
                assert x != null;
                return List.of(1, x.get(1), x.get(2), x.get(3));
            });
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                List<Integer> x = scopes.get(i).get(name.lexeme);
                interpreter.resolve(expr,
                        scopes.size() - 1 - i, x.get(3));
                scopes.get(i).replace(name.lexeme,
                        List.of(x.get(0), 1, x.get(2), x.get(3)));
                return;
            }
        }
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda expr) {
        beginScope();
        for (Token param : expr.params) {
            declare(param);
            define(param);
        }
        resolve(expr.body);
        endScope();
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword,
                    "Can't use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword,
                    "Can't use 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.condition);
        resolve(expr.falseBranch);
        resolve(expr.trueBranch);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword,
                    "Can't use 'this' outside of a class.");
            return null;
        } else if (currentClass == ClassType.STATIC_METHOD_BODY) {
            Lox.error(expr.keyword,
                    "Can't use 'this' within a static method.");
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().containsKey(expr.name.lexeme) &&
                scopes.peek().get(expr.name.lexeme).get(0) == 0) {
            Lox.error(expr.name,
                    "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null) {
            if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
                Lox.error(stmt.superclass.name,
                        "A class can't inherit from itself.");
            }
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
            beginScope();
            scopes.peek().put("super", List.of(1, 1, 0, 0));
        }

        beginScope();
        scopes.peek().put("this", List.of(1, 1, 0, 0));

        Map<String, Map<String, Integer>> traitsToImplement = new HashMap<>();
        Map<String, Integer> methodsToImplement = new HashMap<>();

        for (Expr.Variable trait : stmt.traits) {
            if (traits.containsKey(trait.name.lexeme)) {
                traitsToImplement.put(trait.name.lexeme, traits.get(trait.name.lexeme));
            } else {
                Lox.error(stmt.name,
                        "Undefined trait: '" + trait.name.lexeme + "'.");
            }
        }

        for (Map<String, Integer> entry : traitsToImplement.values()) {
            methodsToImplement.putAll(entry);
        }

        for (Map.Entry<Stmt.Function, Boolean> entry : stmt.methods.entrySet()) {
            FunctionType declaration;
            if (entry.getKey().name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            } else {
                declaration = FunctionType.METHOD;
                if (methodsToImplement.containsKey(entry.getKey().name.lexeme)) {
                    int expectedArity = methodsToImplement.get(entry.getKey().name.lexeme);
                    int actualArity = entry.getKey().params.size();
                    if (expectedArity != actualArity) {
                        Lox.error(stmt.name,
                                "Inherited method '" + stmt.name.lexeme
                                        + "' isn't implemented with correct parity." +
                                        " (Expected: " + expectedArity
                                        + "; Actual: " + actualArity  + ")");
                    }
                    methodsToImplement.remove(entry.getKey().name.lexeme);
                }
            }

            resolveFunction(entry.getKey(), declaration);
        }

        if (!methodsToImplement.isEmpty()) {
            int i = 0;
            StringBuilder unimplementedMethods = new StringBuilder();
            for (String method : methodsToImplement.keySet()) {
                unimplementedMethods.append("'");
                unimplementedMethods.append(method);
                unimplementedMethods.append("'");
                if (i + 1 < methodsToImplement.size()) {
                    unimplementedMethods.append(", ");
                }
                i++;
            }
            Lox.error(stmt.name,
                    "Trait methods " + unimplementedMethods +
                            " not all properly implemented.");
        }

        endScope();
        staticMethods(stmt);
        if (stmt.superclass != null) endScope();

        currentClass = enclosingClass;
        return null;
    }
    private void staticMethods(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.STATIC_METHOD_BODY;

        for (Stmt.Function method : stmt.staticMethods) {
            FunctionType declaration = FunctionType.METHOD;
            resolveFunction(method, declaration);
        }

        currentClass = enclosingClass;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFlowStmt(Stmt.Flow stmt) {
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword,
                        "Can't return a value from an initializer.");
            }
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitTraitStmt(Stmt.Trait stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.TRAIT;

        declare(stmt.name);
        define(stmt.name);

        beginScope();
        scopes.peek().put("this", List.of(1, 1, 0, 0));

        for (Stmt.Function method : stmt.defaultImpls) {
            resolveFunction(method, FunctionType.METHOD);
        }
        endScope();

        Map<String, Integer> methods = new HashMap<>();
        for (Map.Entry<Token, Integer> method : stmt.methods.entrySet()) {
            methods.put(method.getKey().lexeme, method.getValue());
        }

        traits.put(stmt.name.lexeme, methods);

        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        if (stmt.increment != null) resolve(stmt.increment);
        return null;
    }
}