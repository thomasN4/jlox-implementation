package com.craftinginterpreters.lox;

import com.craftinginterpreters.utils.Tuple;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();  // Top level is empty.
    private final Map<String, Object> globals = new HashMap<>();
    private final Map<Expr, Tuple<Integer, Integer>> locals = new HashMap<>();
    private final ArrayList<String> files = new ArrayList<>();

    Interpreter() {
        globals.put("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.put("readLine", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                return scanner.nextLine();
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.put("printw", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                assert arguments.get(0) instanceof String;
                System.out.print(arguments.get(0));
                return null;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.put("loadFile", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                try {
                    byte[] bytes = Files.readAllBytes(Paths.get((String) arguments.get(0)));
                    Lox.run(new String(bytes, Charset.defaultCharset()), false);
                    files.add((String) arguments.get(0));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.put("reload", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                try {
                    for (String file : files) {
                        byte[] bytes = Files.readAllBytes(Paths.get(file));
                        Lox.run(new String(bytes, Charset.defaultCharset()), false);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }
    
    Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth, int index) {
        locals.put(expr, new Tuple<>(depth, index));
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name,
                        "Superclass must be a class.");
            }
        }

        define(stmt.name.lexeme, null);

        if (superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        LoxClass klass = getLoxClass(stmt, (LoxClass) superclass);

        if (superclass != null) {
            environment = environment.enclosing;
        }

        if (environment.enclosing != null) {
            environment.assign(
                    klass, 0, environment.getSize()-1);
        } else {
            globals.put(stmt.name.lexeme, klass);
        }
        return null;
    }
    private LoxClass getLoxClass(Stmt.Class stmt, LoxClass superclass) {
        Map<String, Tuple<LoxFunction, Boolean>> methods = new HashMap<>();
        for (Map.Entry<Stmt.Function, Boolean> entry : stmt.methods.entrySet()) {
            LoxFunction function = new LoxFunction(environment, entry.getKey(),
                    entry.getKey().name.lexeme.equals("init"));
            methods.put(entry.getKey().name.lexeme, new Tuple<>(function, entry.getValue()));
        }

        List<Trait> traits = new ArrayList<>();
        for (Expr.Variable trait : stmt.traits) {
            traits.add((Trait)lookUpVariable(trait.name, trait));
        }

        Map<String, LoxFunction> staticMethods = new HashMap<>();
        for (Stmt.Function method : stmt.staticMethods) {
            LoxFunction function = new LoxFunction(environment, method,
                    method.name.lexeme.equals("init"));
            staticMethods.put(method.name.lexeme, function);
        }

        return new LoxClass(stmt.name.lexeme, superclass, traits, methods, staticMethods);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFlowStmt(Stmt.Flow stmt) {
        throw new FlowException(stmt.type);
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(environment, stmt, false);
        define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitTraitStmt(Stmt.Trait stmt) {
        Map<String, LoxFunction> defaultImpls = new HashMap<>();

        for (Stmt.Function defaultImpl : stmt.defaultImpls) {
            defaultImpls.put(defaultImpl.name.lexeme, new LoxFunction(
                    environment, defaultImpl, false));
        }

        define(stmt.name.lexeme, new Trait(defaultImpls));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (FlowException e) {
                if (e.type == TokenType.BREAK) {
                    break;
                }
            }
            if (stmt.increment != null) {
                execute(stmt.increment);
            }
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        if (locals.containsKey(expr)) {
            Integer distance = locals.get(expr).fst;
            Integer index = locals.get(expr).snd;
            environment.assign(value, distance, index);
        } else {
            globals.put(expr.name.lexeme, value);
        }

        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right); 

        switch (expr.operator.type) {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double)
                    return (double)left + (double)right;
                if (left instanceof String && right instanceof String)
                    return left + (String)right;
                if (left instanceof String)
                    return left + stringify(right);
                if (right instanceof String)
                    return stringify(left) + right;
                return stringify(left) + stringify(right);
            case SLASH:
                checkNumberOperands(expr.operator, left, right);                
                if ((double)left == 0)
                    throw new RuntimeError(expr.operator, "Division by zero.");
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case COMMA:
                return right;
            case MOD:
                checkNumberOperands(expr.operator, left, right);
                return (double)left % (double)right;
        }

        // Unreachable.
        return expr.accept(this);
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name, this);
        }

        throw new RuntimeError(expr.name,
                "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        return new LoxFunction(environment, expr, false);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name,
                    "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        Integer distance = locals.get(expr).fst;

        LoxClass superclass = (LoxClass) environment.get(
                distance, 0);
        LoxInstance object = (LoxInstance)environment.get(
                distance-1, 0);
        LoxFunction method = superclass.findMethod(
                expr.method.lexeme).fst;

        if (method == null) {
            throw new RuntimeError(expr.method,
                    "Undefined property '" + expr.method.lexeme + "'.");
        }
        return method.bind(object);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        if (isTruthy(evaluate(expr.condition))) {
            return evaluate(expr.trueBranch);
        } else {
            return evaluate(expr.falseBranch);
        }
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        return switch (expr.operator.type) {
            case BANG -> !isTruthy(right);
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield -(double) right;
            }
            // Unreachable.
            default -> null;
        };

    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        if (locals.containsKey(expr)) {
            Integer distance = locals.get(expr).fst;
            Integer index = locals.get(expr).snd;
            return environment.get(distance, index);
        } else {
            return globals.get(name.lexeme);
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
    
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private void define(String name, Object value) {
        if (environment.enclosing != null) {
            environment.define(name, value);
        } else {
            globals.put(name, value);
        }
    }

    String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
